@echo off
REM ============================================================================
REM Deploy da API "Cade minha consulta" no Azure App Service (Linux, Java SE 21)
REM
REM Cria (ou atualiza) os recursos e publica o jar:
REM   1. grupo de recursos
REM   2. App Service Plan Linux B1
REM   3. Web App com runtime Java 21 (Java SE), Always On, somente HTTPS e
REM      health check em /actuator/health
REM   4. variaveis de ambiente do app (app settings) lidas do arquivo ARQUIVO_ENV
REM   5. build do jar (mvnw clean package) e publicacao (az webapp deploy --type jar)
REM
REM Pre-requisitos: Azure CLI instalado, "az login" feito, Java 21 e o arquivo de
REM ambiente preenchido com os valores de PRODUCAO (Oracle, JWT, IA e CloudAMQP).
REM
REM Uso, na raiz do projeto ou em qualquer pasta:   scripts\deploy-azure.cmd
REM Para publicar de novo depois, sem recriar nada: scripts\redeploy-azure.cmd
REM
REM Nenhum segredo fica neste arquivo: os valores saem do ARQUIVO_ENV, que nao
REM vai para o Git (.env e .env.azure estao no .gitignore).
REM ============================================================================

setlocal

REM ---------------------------------------------------------------------------
REM Variaveis editaveis
REM ---------------------------------------------------------------------------
REM Grupo de recursos
set GRUPO=rg-cade-minha-consulta
REM Nome do app: precisa ser unico no Azure inteiro (vira parte da URL)
set APP=cade-minha-consulta-api
REM Regiao
set REGIAO=brazilsouth
REM Nome do App Service Plan
set PLANO=plan-cade-minha-consulta
REM Arquivo de ambiente com os valores de producao, relativo a raiz do projeto.
REM Use .env.azure para nao misturar com o .env local (RabbitMQ local x CloudAMQP).
set ARQUIVO_ENV=.env
REM ---------------------------------------------------------------------------

REM Jar gerado pelo Maven (artifactId + version do pom.xml)
set JAR=target\cademinhaconsulta-0.0.1-SNAPSHOT.jar
REM Arquivos temporarios (o de settings tem segredos e e apagado logo depois do uso)
set ARQ_SETTINGS=%TEMP%\cade-appsettings-%RANDOM%.json
set ARQ_SITECONFIG=%TEMP%\cade-siteconfig-%RANDOM%.json

REM Roda a partir da raiz do projeto (a pasta acima de scripts\)
cd /d "%~dp0.."

REM Em arquivos .cmd, "az" e "mvnw.cmd" sao chamados com "call": sem ele, o script
REM terminaria logo depois do primeiro comando.

REM --- Verificacoes iniciais -------------------------------------------------
where az >nul 2>nul
if errorlevel 1 (
    echo ERRO: Azure CLI nao encontrado. Instale em https://aka.ms/installazurecliwindows
    exit /b 1
)
call az account show --output none >nul 2>nul
if errorlevel 1 (
    echo ERRO: voce nao esta logado no Azure. Rode "az login" e tente de novo.
    exit /b 1
)
if not exist "%ARQUIVO_ENV%" (
    echo ERRO: arquivo de ambiente "%ARQUIVO_ENV%" nao encontrado na raiz do projeto.
    echo Copie o .env.example e preencha com os valores de producao.
    exit /b 1
)

echo.
echo Assinatura do Azure em uso:
call az account show --query "{assinatura:name, id:id}" --output table
echo.
echo Grupo: %GRUPO%   App: %APP%   Regiao: %REGIAO%   Plano: %PLANO%   Ambiente: %ARQUIVO_ENV%
echo.

REM --- 1. Grupo de recursos --------------------------------------------------
echo [1/8] Criando o grupo de recursos %GRUPO%...
call az group create --name %GRUPO% --location %REGIAO% --output none
if errorlevel 1 goto erro

REM --- 2. App Service Plan Linux B1 ------------------------------------------
REM B1 e o menor plano pago: permite Always On e health check.
echo [2/8] Criando o App Service Plan %PLANO% (Linux, B1)...
call az appservice plan create --name %PLANO% --resource-group %GRUPO% --location %REGIAO% --is-linux --sku B1 --output none
if errorlevel 1 goto erro

REM --- 3. Web App Java 21 (Java SE) ------------------------------------------
REM Runtime no formato Framework:Versao (lista completa: az webapp list-runtimes --os linux).
REM O App Service roda o jar com java -jar e informa a porta na variavel PORT,
REM que o application.yml usa em server.port.
echo [3/8] Criando o Web App %APP% (Java 21, Java SE)...
call az webapp create --name %APP% --resource-group %GRUPO% --plan %PLANO% --runtime "JAVA:21-java21" --output none
if errorlevel 1 goto erro

REM --- 4. Always On e health check -------------------------------------------
REM Always On: o app nao e descarregado quando fica sem acesso (evita a demora da
REM primeira requisicao). Health check: o App Service consulta /actuator/health
REM (que reflete o banco). A documentacao pede arquivo JSON no cmd do Windows.
echo [4/8] Ativando Always On e o health check em /actuator/health...
call az webapp config set --name %APP% --resource-group %GRUPO% --always-on true --output none
if errorlevel 1 goto erro
> "%ARQ_SITECONFIG%" echo {"healthCheckPath": "/actuator/health"}
call az webapp config set --name %APP% --resource-group %GRUPO% --generic-configurations "@%ARQ_SITECONFIG%" --output none
set RESULTADO=%ERRORLEVEL%
del /q "%ARQ_SITECONFIG%" >nul 2>nul
if not "%RESULTADO%"=="0" goto erro

REM Somente HTTPS: requisicoes http sao redirecionadas para https
call az webapp update --name %APP% --resource-group %GRUPO% --https-only true --output none
if errorlevel 1 goto erro

REM --- 5. Variaveis de ambiente (app settings) -------------------------------
REM O PowerShell le o ARQUIVO_ENV (linhas CHAVE=VALOR; ignora comentarios e
REM valores vazios, que ficam com o padrao do application.yml), confere as
REM variaveis obrigatorias e grava um JSON temporario no formato do Azure CLI
REM ([{name, value, slotSetting}]). Os valores nunca aparecem na tela nem na
REM linha de comando; so os nomes sao listados. O JSON e apagado em seguida.
echo [5/8] Enviando as variaveis de ambiente lidas de %ARQUIVO_ENV%...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $obrigatorias='DB_URL','DB_USERNAME','DB_PASSWORD','JWT_SECRET','RABBITMQ_HOST','RABBITMQ_USERNAME','RABBITMQ_PASSWORD'; $cfg=[ordered]@{}; foreach ($linha in Get-Content -LiteralPath $env:ARQUIVO_ENV -Encoding UTF8) { $t=$linha.Trim(); if ($t -eq '' -or $t.StartsWith('#')) { continue }; $i=$t.IndexOf('='); if ($i -lt 1) { continue }; $chave=$t.Substring(0,$i).Trim(); $valor=$t.Substring($i+1).Trim(); if ($valor -ne '') { $cfg[$chave]=$valor } }; $faltando=@($obrigatorias | Where-Object { -not $cfg.Contains($_) }); if ($faltando.Count -gt 0) { Write-Host ('ERRO: faltam valores no ' + $env:ARQUIVO_ENV + ': ' + ($faltando -join ', ')); exit 1 }; if ($cfg['RABBITMQ_HOST'] -eq 'localhost') { Write-Host 'AVISO: RABBITMQ_HOST=localhost. No Azure, use o host do CloudAMQP (porta 5671, RABBITMQ_SSL=true).' }; $lista=@($cfg.Keys | ForEach-Object { [pscustomobject]@{ name=$_; value=[string]$cfg[$_]; slotSetting=$false } }); [IO.File]::WriteAllText($env:ARQ_SETTINGS, (ConvertTo-Json -InputObject $lista)); Write-Host ('Variaveis (somente os nomes): ' + (@($cfg.Keys) -join ', '))"
if errorlevel 1 (
    del /q "%ARQ_SETTINGS%" >nul 2>nul
    goto erro
)
call az webapp config appsettings set --name %APP% --resource-group %GRUPO% --settings "@%ARQ_SETTINGS%" --output none
set RESULTADO=%ERRORLEVEL%
del /q "%ARQ_SETTINGS%" >nul 2>nul
if not "%RESULTADO%"=="0" goto erro

REM --- 6. Build do jar -------------------------------------------------------
REM Os testes rodam e nao precisam de banco nem de broker.
echo [6/8] Gerando o jar com mvnw clean package...
call mvnw.cmd clean package
if errorlevel 1 goto erro
if not exist "%JAR%" (
    echo ERRO: jar %JAR% nao encontrado depois do build.
    goto erro
)

REM --- 7. Publicacao ---------------------------------------------------------
REM --type jar: o App Service grava o arquivo como /home/site/wwwroot/app.jar e
REM reinicia o app. O upload do jar (cerca de 160 MB) pode levar alguns minutos.
echo [7/8] Publicando %JAR% no %APP%...
call az webapp deploy --name %APP% --resource-group %GRUPO% --src-path "%JAR%" --type jar
if errorlevel 1 goto erro

REM --- 8. URLs ---------------------------------------------------------------
REM O host vem do Azure (pode ter sufixo regional), por isso nao e montado a mao.
echo [8/8] Consultando a URL do app...
set APP_HOST=
for /f "delims=" %%H in ('az webapp show --name %APP% --resource-group %GRUPO% --query defaultHostName --output tsv') do set APP_HOST=%%H
if "%APP_HOST%"=="" goto erro

echo.
echo ============================================================================
echo Deploy concluido. A primeira subida do Spring Boot pode levar 1 a 2 minutos.
echo   API:     https://%APP_HOST%
echo   Swagger: https://%APP_HOST%/swagger-ui.html
echo   Health:  https://%APP_HOST%/actuator/health
echo Logs em tempo real: az webapp log tail --name %APP% --resource-group %GRUPO%
echo ============================================================================
endlocal
exit /b 0

:erro
echo.
echo ERRO: o deploy parou no passo acima. Corrija o problema e rode o script de novo.
endlocal
exit /b 1
