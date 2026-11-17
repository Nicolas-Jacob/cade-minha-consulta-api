@echo off
REM ============================================================================
REM Republica a API "Cade minha consulta" no Azure App Service ja criado pelo
REM scripts\deploy-azure.cmd: so gera o jar e publica de novo.
REM Nao cria recursos e nao altera as variaveis de ambiente do app.
REM
REM Uso: scripts\redeploy-azure.cmd
REM (por exemplo, depois da integracao do modelo de risco de falta)
REM
REM Para mudar uma variavel de ambiente sem rodar o deploy completo:
REM   az webapp config appsettings set --name APP --resource-group GRUPO --settings CHAVE=VALOR
REM ============================================================================

setlocal

REM ---------------------------------------------------------------------------
REM Variaveis editaveis: use os MESMOS valores do deploy-azure.cmd
REM ---------------------------------------------------------------------------
set GRUPO=rg-cade-minha-consulta
set APP=cade-minha-consulta-api
REM ---------------------------------------------------------------------------

set JAR=target\cademinhaconsulta-0.0.1-SNAPSHOT.jar

REM Roda a partir da raiz do projeto (a pasta acima de scripts\)
cd /d "%~dp0.."

REM "az" e "mvnw.cmd" sao chamados com "call" para o script continuar depois deles.

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
call az webapp show --name %APP% --resource-group %GRUPO% --output none >nul 2>nul
if errorlevel 1 (
    echo ERRO: o app %APP% nao existe no grupo %GRUPO%. Rode antes o scripts\deploy-azure.cmd.
    exit /b 1
)

REM --- 1. Build do jar -------------------------------------------------------
echo [1/3] Gerando o jar com mvnw clean package...
call mvnw.cmd clean package
if errorlevel 1 goto erro
if not exist "%JAR%" (
    echo ERRO: jar %JAR% nao encontrado depois do build.
    goto erro
)

REM --- 2. Publicacao ---------------------------------------------------------
REM --type jar: grava como /home/site/wwwroot/app.jar e reinicia o app.
echo [2/3] Publicando %JAR% no %APP%...
call az webapp deploy --name %APP% --resource-group %GRUPO% --src-path "%JAR%" --type jar
if errorlevel 1 goto erro

REM --- 3. URLs ---------------------------------------------------------------
echo [3/3] Consultando a URL do app...
set APP_HOST=
for /f "delims=" %%H in ('az webapp show --name %APP% --resource-group %GRUPO% --query defaultHostName --output tsv') do set APP_HOST=%%H
if "%APP_HOST%"=="" goto erro

echo.
echo ============================================================================
echo Republicado. A subida do Spring Boot pode levar 1 a 2 minutos.
echo   API:     https://%APP_HOST%
echo   Swagger: https://%APP_HOST%/swagger-ui.html
echo   Health:  https://%APP_HOST%/actuator/health
echo ============================================================================
endlocal
exit /b 0

:erro
echo.
echo ERRO: o redeploy parou no passo acima.
endlocal
exit /b 1
