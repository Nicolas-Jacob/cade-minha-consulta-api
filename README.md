# Cadê minha consulta — API

API Java do app **Cadê minha consulta**, Global Solution FIAP 2026/2, tema **CitizenTech** (ODS 3: Saúde e bem-estar).

| | |
|---|---|
| API em produção | https://cademinhaconsulta-rm564205.azurewebsites.net |
| Swagger UI em produção | https://cademinhaconsulta-rm564205.azurewebsites.net/swagger-ui.html |
| Repositório do app mobile | https://github.com/Nicolas-Jacob/cade-minha-consulta-mobile |
| Vídeo pitch | LINK_VIDEO_PITCH |
| Contrato entre a API e o app | [docs/CONTRATO_API.md](docs/CONTRATO_API.md) |

## Equipe

| Nome | RM | Turma |
|---|---|---|
| João Victor Vendrameto | RM563665 | 2TDSPV |
| Nicolas de Oliveira Jacob | RM564205 | 2TDSPX |
| Gabriel Ambrósio Saraiva | RM566552 | 2TDSPV |
| Vinicius Romaguera Cardozo | RM562308 | 2TDSPX |
| Yuri Fuzinatto Garzoli Barreto | RM561450 | 2TDSPX |

## O problema

No SUS, cerca de 1 em cada 4 pacientes falta à consulta com especialista sem avisar. A vaga, que levou meses para sair, é desperdiçada enquanto outras pessoas seguem esperando na fila.

## A solução

- O **cidadão** acompanha pelo app as suas solicitações (fila) e consultas. Com um toque, ele confirma presença ou libera a vaga.
- Quando uma vaga é liberada, o sistema **agenda automaticamente** o próximo paciente compatível da fila.
- O **gestor** da unidade agenda pacientes da fila para médicos, acompanha as próximas consultas e registra presença ou falta.
- A **IA generativa** escreve lembretes e orientações de preparo em linguagem simples.
- Um modelo de **ML** prevê o risco de falta. A API já tem o ponto de integração; o modelo, feito por outro integrante, entra depois.

**ODS 3 (Saúde e bem-estar):** menos vagas de especialista desperdiçadas significa mais atendimentos com a mesma estrutura e menos tempo de espera na fila.

### Público beneficiado

- **Pacientes do SUS** que aguardam consulta com especialista: acompanham a fila e as consultas sem precisar ir à unidade ou ligar, e recebem orientações claras de preparo.
- **Quem está na fila**: as vagas liberadas são reaproveitadas na hora, em vez de ficarem vazias.
- **Gestores e reguladores das unidades de saúde**: agendam pela fila, registram presença e falta e, com o modelo de ML, sabem quais consultas têm mais risco de falta.

## Arquitetura

```mermaid
flowchart LR
    app["App mobile<br/>React Native + Expo"] -->|"HTTPS + JWT"| api["API Spring Boot<br/>Azure App Service"]
    api -->|"Spring Data JPA / JDBC"| db[("Oracle<br/>tabelas + PL/SQL")]
    api -->|"publica vaga.liberada<br/>depois do commit"| mq[["RabbitMQ<br/>consultas.exchange"]]
    mq -->|"vaga.liberada.queue"| consumer["VagaLiberadaConsumer"]
    consumer -->|"PRC_AGENDAR_CONSULTA"| db
    api -->|"Spring AI / ChatClient"| llm["LLM<br/>OpenAI"]
    api -.->|"futuro: HTTP"| ml["Serviço de risco de falta<br/>modelo de ML"]
```

- **App mobile → API:** REST com JSON e JWT no header `Authorization: Bearer <token>`.
- **API → Oracle:** JPA para o CRUD. As regras de agendamento ficam em PL/SQL (`PKG_GS_GESTAO_CONSULTAS`, `PKG_GS_CONSULTA_DADOS`) e são chamadas com `SimpleJdbcCall`. O schema vem dos scripts de [scripts/](scripts/), nunca do Hibernate (`ddl-auto: none`).
- **API → RabbitMQ → consumer → procedure:** a realocação da vaga liberada é assíncrona (ver [Arquitetura distribuída](#arquitetura-distribuída-opção-a-rabbitmq)). Local: RabbitMQ no Docker. Produção: CloudAMQP.
- **API → LLM:** orientações da consulta com Spring AI (ver [IA generativa](#ia-generativa-spring-ai)).
- **API → serviço de risco de falta (futuro):** ponto de integração pronto, hoje com um stub (ver [Risco de falta](#risco-de-falta)).

Pacotes do código (`br.com.fiap.cademinhaconsulta`): `controller`, `service`, `repository`, `model` (entidades JPA), `dto` (records), `enums`, `exception` (`@RestControllerAdvice`), `config` (segurança, JWT, OpenAPI), `messaging` (RabbitMQ) e `risco` (cliente do modelo de ML).

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 4 (Web MVC, Data JPA, Validation, Actuator) |
| Segurança | Spring Security + JWT via OAuth2 Resource Server (JwtEncoder/JwtDecoder Nimbus, HS256), BCrypt |
| Banco | Oracle (schema e regras em PL/SQL nos scripts de `/scripts`) |
| IA | Spring AI (OpenAI) |
| Mensageria | RabbitMQ (Spring AMQP); CloudAMQP em produção |
| Documentação | springdoc-openapi (Swagger UI) |
| Deploy | Azure App Service (Linux, Java SE 21), Azure CLI |
| Outros | Lombok, Maven Wrapper |

## Como rodar localmente

### Pré-requisitos

- Java 21
- Acesso a um banco Oracle
- Docker, para o RabbitMQ (ver o passo 3). Sem o broker a API sobe normalmente, mas a vaga liberada não é realocada automaticamente.
- (Opcional) Uma chave da OpenAI. Sem ela, a API sobe e as orientações usam o texto padrão (ver [IA generativa](#ia-generativa-spring-ai)).

### 1. Criar o banco

Execute os scripts de [scripts/](scripts/) no Oracle, nesta ordem:

1. `script-bd.sql`: tabelas, constraints, sequences e triggers de ID
2. `02-triggers.sql`: triggers de auditoria e regra de data futura
3. `03-pkg-gestao-consultas.sql`: package de agendamento
4. `04-pkg-consulta-dados.sql`: package de consultas e dados para o modelo de risco
5. `05-carga-dados.sql`: carga de dados (inclui os usuários de teste)

### 2. Configurar as variáveis de ambiente

Copie o `.env.example` para `.env` na raiz do projeto e preencha os valores (ver [Variáveis de ambiente](#variáveis-de-ambiente)). O `.env` não vai para o Git. A API lê o arquivo com `spring.config.import=optional:file:.env[.properties]`.

```bash
cp .env.example .env
```

### 3. Subir o RabbitMQ

O [docker-compose.yml](docker-compose.yml) da raiz sobe só o RabbitMQ (imagem `rabbitmq:management`):

```bash
docker compose up -d
```

- AMQP: `localhost:5672`, vhost `/`, sem SSL (usuário e senha `guest`, os mesmos do `.env.example`)
- Painel de administração: http://localhost:15672 (`guest` / `guest`). Em **Queues** aparece a fila `vaga.liberada.queue`, criada pela API ao conectar.
- Para parar: `docker compose down`

### 4. Subir a API

```bash
./mvnw spring-boot:run
```

No Windows (PowerShell/CMD): `mvnw.cmd spring-boot:run`

A API sobe na porta 8080. Na primeira execução, o `SenhaHashRunner` converte para BCrypt as senhas em texto puro da carga. Isso leva alguns segundos.

### 5. Testar

- Swagger UI: http://localhost:8080/swagger-ui.html. Faça o login em `POST /api/auth/login`, copie o `token`, clique em **Authorize** e cole o token.
- Health: http://localhost:8080/actuator/health. Reflete só o banco (ver [Health check](#health-check)).
- Arquivo [test.http](test.http) com exemplos de requisições (extensão REST Client do VS Code).

### Testes automatizados

```bash
./mvnw clean package
```

Os testes são unitários e **não precisam de banco, broker nem chave de IA**:

- `OrientacaoServiceTest`: prompt da IA, fallback para o texto padrão e minimização de dados (o prompt não leva CPF, telefone, e-mail etc.).
- `TokenServiceTest`: JWT com as claims do contrato (`sub`, `id`, `credencial`), conversão para `ROLE_GESTOR` e `ROLE_CIDADAO`, rejeição de token assinado com outra chave e de segredo com menos de 32 caracteres.

## Usuários de teste

Criados pelo `05-carga-dados.sql`:

| Perfil | Login | Senha |
|---|---|---|
| GESTOR | `gestor` | `SenhaGestor2026` |
| CIDADAO | `cidadao` | `Senha123` |

## Endpoints

Papéis: **pub** = público; **logado** = qualquer usuário autenticado; **CID** = CIDADAO; **GES** = GESTOR; **dono** = o recurso (solicitação ou consulta) pertence ao usuário do token.

Todo GET de lista é paginado (`?page=0&size=10&sort=campo,asc`) e devolve `content`, `totalElements`, `totalPages`, `number` e `size`. Erros seguem o formato padrão da seção 5 do [contrato](docs/CONTRATO_API.md): `status`, `erro`, `mensagem`, `campos` (só em erro de validação), `caminho` e `timestamp`.

### Auth e usuário

| Método | Rota | Papel | Observação |
|---|---|---|---|
| POST | `/api/auth/register` | pub | Cadastra cidadão e devolve o token (201) |
| POST | `/api/auth/login` | pub | Devolve o token JWT |
| GET | `/api/usuarios/me` | logado | Dados do usuário do token |
| PUT | `/api/usuarios/me` | logado | Atualiza nome, contato e endereço |

### Catálogo

| Método | Rota | Papel | Observação |
|---|---|---|---|
| GET | `/api/especialidades` | logado | Padrão `sort=nome,asc` |
| GET | `/api/especialidades/{id}` | logado | |
| POST | `/api/especialidades` | GES | 201 |
| PUT | `/api/especialidades/{id}` | GES | |
| DELETE | `/api/especialidades/{id}` | GES | 204. 409 se estiver em uso por médico ou fila |
| GET | `/api/unidades?status=&especialidade=` | logado | Padrão `sort=nome,asc`. CID só vê ATIVO; `status` só vale para o GES. `especialidade`: só unidades com médico ATIVO dessa especialidade |
| GET | `/api/unidades/{id}` | logado | CID recebe 404 se a unidade estiver INATIVO |
| POST | `/api/unidades` | GES | 201. Grava contato e endereço. `status` opcional (padrão ATIVO) |
| PUT | `/api/unidades/{id}` | GES | Atualiza contato e endereço. `status` opcional (permite reativar) |
| DELETE | `/api/unidades/{id}` | GES | 204. Status INATIVO |
| GET | `/api/medicos?especialidade=&unidade=` | GES | Padrão `sort=nome,sobrenome,asc` |
| GET | `/api/medicos/{id}` | GES | |
| POST | `/api/medicos` | GES | 201. 409 se o CRM já existir na mesma UF. `status` opcional (padrão ATIVO) |
| PUT | `/api/medicos/{id}` | GES | Atualiza especialidades e unidades. `status` opcional (permite reativar) |
| DELETE | `/api/medicos/{id}` | GES | 204. Status INATIVO |

### Fila (solicitações de consulta)

| Método | Rota | Papel | Observação |
|---|---|---|---|
| POST | `/api/filas` | CID | 201. Prioridade 1, AGUARDANDO. 404 especialidade/unidade inexistente, 422 unidade inativa, 409 se já houver solicitação AGUARDANDO para a especialidade |
| GET | `/api/filas/minhas` | CID | Padrão `sort=dataEntrada,desc`. `posicaoEstimada` só nas AGUARDANDO |
| GET | `/api/filas/{id}` | CID dono ou GES | Outro cidadão recebe 403 |
| PUT | `/api/filas/{id}` | CID dono | Troca a unidade. Só AGUARDANDO (senão 409) |
| DELETE | `/api/filas/{id}` | CID dono | 204. Desistência: CANCELADA + data de saída. Só AGUARDANDO (senão 409) |
| GET | `/api/filas?status=&especialidade=&unidade=` | GES | Padrão `sort=prioridade,desc` e `dataEntrada,asc` (ordem real da fila) |
| PATCH | `/api/filas/{id}/prioridade` | GES | Prioridade de 1 a 5. Só AGUARDANDO (senão 409) |

### Consultas

| Método | Rota | Papel | Observação |
|---|---|---|---|
| GET | `/api/consultas/minhas?periodo=futuras` | CID | `futuras` (padrão): AGENDADA/CONFIRMADA com data futura, padrão `sort=dataHora,asc`. `historico`: o resto, padrão `sort=dataHora,desc` |
| GET | `/api/consultas/{id}` | CID dono ou GES | Outro cidadão recebe 403. O GES recebe também `paciente` |
| PATCH | `/api/consultas/{id}/confirmar` | CID dono | AGENDADA futura → CONFIRMADA (senão 409) |
| PATCH | `/api/consultas/{id}/cancelar` | CID dono | AGENDADA/CONFIRMADA futura → CANCELADA (senão 409). A fila vira CANCELADA e o evento `vaga.liberada` é publicado após o commit |
| GET | `/api/consultas/{id}/orientacoes` | CID dono ou GES | Orientações da consulta com Spring AI. `origem` IA ou PADRAO (texto padrão se a IA estiver indisponível). Ver [IA generativa](#ia-generativa-spring-ai) |

`ConsultaResponse` traz `diasAteConsulta` (dias de calendário até a consulta; 0 se já passou), o médico com nome completo e CRM, e a unidade com o endereço formatado.

### Gestor

| Método | Rota | Papel | Observação |
|---|---|---|---|
| POST | `/api/gestor/agendamentos` | GES | Body `{idMedico}`. Chama `PRC_AGENDAR_CONSULTA` e devolve 201 com a consulta criada (com `paciente`). Erros da procedure: 404 médico inexistente, 409 inativo/sem horário/fila alterada, 422 fila vazia |
| GET | `/api/gestor/consultas/proximas?dias=15` | GES | AGENDADA/CONFIRMADA nos próximos `dias` (1 a 365), padrão `sort=dataHora,asc`, com `paciente` (nome e telefone) |
| PATCH | `/api/gestor/consultas/{id}/presenca` | GES | Body `{status: "PRESENTE" \| "FALTA"}`. Só AGENDADA/CONFIRMADA com data passada (senão 409) |
| GET | `/api/gestor/consultas/risco-falta?dias=15` | GES | Exceção à paginação: array de `RiscoFaltaResponse` ordenado por data. `risco` e `nivel` null até o modelo de ML ser integrado. Ver [Risco de falta](#risco-de-falta) |

### Infra

| Método | Rota | Papel |
|---|---|---|
| GET | `/actuator/health` | pub |
| GET | `/swagger-ui.html` | pub |

## Arquitetura distribuída: opção A (RabbitMQ)

Dentre as opções do requisito 5 de Java Advanced, escolhemos a **opção A: mensageria com RabbitMQ**. O motivo é o próprio problema: quando o cidadão libera a vaga, a resposta para ele precisa ser imediata, e a realocação para o próximo da fila pode acontecer em segundo plano, sem travar a requisição nem depender de o broker estar no ar. O fluxo:

```mermaid
sequenceDiagram
    actor Cidadao as Cidadão (app)
    participant API as API (ConsultaService)
    participant Pub as VagaLiberadaPublisher
    participant MQ as RabbitMQ
    participant Con as VagaLiberadaConsumer
    participant DB as Oracle
    Cidadao->>API: PATCH /api/consultas/{id}/cancelar
    API->>DB: consulta e fila = CANCELADA (COMMIT)
    API-->>Cidadao: 200 ConsultaResponse
    API->>Pub: VagaLiberadaEvent (depois do commit)
    Pub->>MQ: consultas.exchange / vaga.liberada
    MQ->>Con: vaga.liberada.queue
    Con->>DB: PRC_AGENDAR_CONSULTA(idMedico)
    Note over DB: nova consulta AGENDADA<br/>para o próximo da fila
```

1. `PATCH /api/consultas/{id}/cancelar` (`ConsultaService.cancelar`): numa única transação, a consulta vira CANCELADA e a solicitação (fila) dela vira CANCELADA com `data_saida`. O serviço publica um `VagaLiberadaEvent { idConsulta, idMedico, idUnidade, idEspecialidade, dataHora }` como evento do Spring.
2. `VagaLiberadaPublisher` escuta esse evento com `@TransactionalEventListener(AFTER_COMMIT)`: só envia a mensagem para a exchange `consultas.exchange` (topic, routing key `vaga.liberada`) **depois do commit**. Se a transação falhar, nada é publicado.
3. Se o RabbitMQ estiver fora do ar, o publisher só registra o erro no log. O cancelamento continua valendo; a vaga apenas não é realocada automaticamente (o gestor pode agendar pelo `POST /api/gestor/agendamentos`).
4. `VagaLiberadaConsumer` (`@RabbitListener` na fila `vaga.liberada.queue`) chama `AgendamentoService.agendarProximo(idMedico)`, que executa `PKG_GS_GESTAO_CONSULTAS.PRC_AGENDAR_CONSULTA` via `SimpleJdbcCall`. A procedure escolhe o próximo paciente compatível com o médico (prioridade + antiguidade), cria a consulta AGENDADA, tira a solicitação da fila e faz `COMMIT`.
5. Erros no consumidor não são reprocessados em loop (`default-requeue-rejected: false`): fila vazia (`-20003`) gera só um log informativo; qualquer outro erro gera log de erro e a mensagem é descartada.

As mensagens trafegam em JSON (`JacksonJsonMessageConverter`). Exchange, fila e binding são declarados pela própria API (`RabbitConfig`) ao conectar no broker.

**A API funciona sem o broker:** ela sobe normalmente, o listener tenta reconectar em segundo plano e todas as rotas continuam respondendo. Só a realocação automática da vaga fica parada até o broker voltar. Por isso o RabbitMQ não entra no health check (ver [Health check](#health-check)).

**Local e produção:** o mesmo código conecta no RabbitMQ local (Docker, porta 5672, sem SSL) e no CloudAMQP (porta 5671 com SSL e vhost da instância), só trocando as variáveis `RABBITMQ_*`.

**Limitação conhecida:** a procedure agenda no primeiro horário útil livre do médico a partir de `SYSDATE + 5` (8h–17h, seg–sex), para o paciente ter pelo menos cinco dias de aviso. Por isso, o próximo paciente nem sempre ocupa exatamente o horário que foi liberado: se a consulta cancelada era nos próximos cinco dias, aquele horário fica livre.

## IA generativa (Spring AI)

`GET /api/consultas/{id}/orientacoes` devolve uma orientação curta para o paciente sobre **aquela consulta**. A rota não é um chat livre: o texto é gerado a partir dos dados da consulta, dentro do fluxo de acompanhamento do app. Acesso: o dono da consulta ou o GESTOR. Outro cidadão recebe 403 e consulta inexistente, 404.

**Como funciona** (`OrientacaoService`, com o `ChatClient` do Spring AI):

1. O `ConsultaService.dadosParaOrientacao` valida o acesso e monta um `DadosOrientacao` só com os campos permitidos.
2. O prompt é montado a partir de templates em [src/main/resources/prompts/](src/main/resources/prompts/), não no código:
   - `orientacao-sistema.st` (mensagem de sistema): português do Brasil, linguagem simples para quem usa o SUS, tom acolhedor, no máximo 150 palavras e texto puro sem markdown (listas com "• "). Conteúdo: lembrete da consulta, o que levar (documento com foto, cartão do SUS, exames e receitas anteriores), chegar com antecedência, cuidados gerais de preparo da especialidade e o pedido de liberar a vaga pelo app se não puder ir. Restrições: **nunca diagnosticar, nunca recomendar ou alterar medicamentos e não inventar dados** (nome do médico, exames específicos etc.). Se a consulta estiver cancelada ou encerrada, só informa a situação.
   - `orientacao-usuario.st` (mensagem do usuário): os dados da consulta.
3. A resposta traz `origem` (`IA` ou `PADRAO`) e o `aviso` correspondente: "Orientação gerada por IA. Não substitui a orientação médica." ou "Orientações gerais. Não substituem a orientação médica.".

**O que vai no prompt:** especialidade e descrição, data e hora (por extenso, com o dia da semana), dias até a consulta, nome e endereço da unidade de saúde, primeiro nome do paciente e situação da consulta.

**Minimização de dados (LGPD):** o modelo **não** recebe CPF, telefone, e-mail, endereço do paciente, sobrenome nem os outros nomes do paciente, nem o nome dos pais ou o login. A garantia está no tipo: `DadosOrientacao` simplesmente não tem esses campos. O `OrientacaoServiceTest` confere que o prompt enviado não contém esses dados.

**Fallback:** a API nunca devolve 500 por causa da IA. Nestes casos ela devolve um texto padrão montado com os mesmos dados (`orientacao-padrao.st`, ou `orientacao-padrao-encerrada.st` para consulta cancelada ou passada), com `origem: "PADRAO"`, e registra o motivo no log:

- `AI_API_KEY` vazia ou não definida: a IA nem é chamada;
- erro na chamada (chave inválida, rede, limite de uso etc.);
- sem resposta em 20 s (`orientacao.timeout-segundos`; o SDK também usa `timeout: 20s` e não faz novas tentativas);
- resposta vazia.

**Limitações:** o texto é gerado por um modelo de linguagem e **pode conter erros ou imprecisões**. A IA **não diagnostica e não substitui a orientação médica**: as orientações de preparo são gerais, e o paciente deve seguir o que a unidade e o médico indicarem. O app mostra o `aviso` junto do texto. Cada chamada gera um texto novo, que não fica salvo, e consome a cota da chave da OpenAI.

## Risco de falta

`GET /api/gestor/consultas/risco-falta?dias=15` (só GESTOR) lista as consultas AGENDADA ou CONFIRMADA dos próximos `dias` (1 a 365) com o risco de falta de cada uma. Pelo contrato, é uma **exceção à paginação**: devolve um array simples de `RiscoFaltaResponse`, ordenado pela data da consulta.

**Fluxo** (`RiscoFaltaService`):

1. Chama a função `PKG_GS_CONSULTA_DADOS.FN_DADOS_PREVISAO_FALTA(p_dias)` via `SimpleJdbcCall`. Ela devolve um `SYS_REFCURSOR`, e cada linha vira um `DadosPrevisaoFalta` com os atributos do modelo: dias até a consulta, dia da semana, hora e turno, idade do paciente, prioridade da fila, especialidade, unidade, faltas, presenças e cancelamentos anteriores, e taxa histórica de faltas.
2. Envia a lista ao `RiscoFaltaClient.prever(...)`, que devolve uma `PrevisaoRisco { idConsulta, risco (0 a 1), nivel (BAIXO/MEDIO/ALTO) }` por consulta.
3. Junta o nome do paciente, buscado pela própria API. O nome não vem do cursor e não vai para o modelo.

**Ponto de integração com o modelo de ML:** o `RiscoFaltaConfig` escolhe a implementação pela variável `RISCO_FALTA_URL` (propriedade `risco-falta.url`):

- vazia (padrão): `StubRiscoFaltaClient`, que devolve `risco` e `nivel` null para todas as consultas;
- preenchida: é onde entra o cliente HTTP real (um `HttpRiscoFaltaClient` com `RestClient`, que envia os atributos ao serviço do modelo e lê as previsões). Esse cliente ainda não foi implementado: por enquanto, a API registra um aviso no log e continua usando o stub.

Para integrar o modelo, basta implementar `RiscoFaltaClient` e devolver o novo cliente no `RiscoFaltaConfig`. O service, o controller e o contrato não mudam. Depois, é só definir `RISCO_FALTA_URL` no App Service e rodar o `scripts\redeploy-azure.cmd`.

## Deploy (Azure App Service)

| | |
|---|---|
| API | https://cademinhaconsulta-rm564205.azurewebsites.net |
| Swagger UI | https://cademinhaconsulta-rm564205.azurewebsites.net/swagger-ui.html |
| Health | https://cademinhaconsulta-rm564205.azurewebsites.net/actuator/health |

A API roda no **Azure App Service** (Linux, Java SE 21, plano B1), com o banco Oracle e o RabbitMQ no **CloudAMQP**.

### Como publicar

Pré-requisitos: [Azure CLI](https://aka.ms/installazurecliwindows) instalado, `az login` feito, Java 21, uma instância do CloudAMQP e o Oracle acessível a partir do Azure.

1. Crie o arquivo de ambiente de produção a partir do `.env.example`. Pode ser o próprio `.env` ou um `.env.azure`, que também fica fora do Git. Preencha com os valores de produção. No CloudAMQP: host da instância, `RABBITMQ_PORT=5671`, usuário, senha, `RABBITMQ_VIRTUAL_HOST` da instância (em geral igual ao usuário) e `RABBITMQ_SSL=true`.
2. Edite as variáveis no topo de [scripts/deploy-azure.cmd](scripts/deploy-azure.cmd): `GRUPO`, `APP` (nome **único no Azure**, vira parte da URL), `REGIAO` (`brazilsouth`), `PLANO` e `ARQUIVO_ENV` (`.env` ou `.env.azure`).
3. No cmd do Windows, na raiz do projeto:

   ```bat
   scripts\deploy-azure.cmd
   ```

O script, nesta ordem:

1. cria o grupo de recursos e o App Service Plan Linux B1;
2. cria o Web App com runtime `JAVA:21-java21` (Java SE);
3. ativa o Always On, o health check em `/actuator/health` e o modo somente HTTPS;
4. lê o arquivo de ambiente e envia as variáveis como **app settings**. Antes, confere as obrigatórias e avisa se o RabbitMQ ainda aponta para `localhost`. Os valores passam por um JSON temporário que é apagado em seguida: nenhum segredo fica no script, na tela ou na linha de comando;
5. gera o jar com `mvnw clean package` (com os testes) e publica com `az webapp deploy --type jar`;
6. imprime a URL https da API e a do Swagger.

Para publicar uma nova versão sem recriar nada (por exemplo, depois da integração do modelo de risco), use [scripts/redeploy-azure.cmd](scripts/redeploy-azure.cmd), com os mesmos `GRUPO` e `APP`. Logs em tempo real: `az webapp log tail --name <APP> --resource-group <GRUPO>`.

O plano B1 é pago. Depois da avaliação, para apagar tudo: `az group delete --name <GRUPO>`.

### Ajustes para produção

- **Porta:** `server.port: ${PORT:8080}`. No App Service Linux, a plataforma informa na variável `PORT` a porta em que o runtime Java deve escutar. Localmente, sem `PORT`, fica 8080.
- **Fuso horário:** o App Service Linux roda em **UTC**, mas o contrato usa datas no horário de Brasília, e as regras usam a hora atual (consultas futuras, data de entrada na fila, registro de presença). Por isso o `main` define `TimeZone.setDefault(America/Sao_Paulo)`. Assim, a API se comporta igual em qualquer servidor.
- **RabbitMQ:** `RABBITMQ_VIRTUAL_HOST` e `RABBITMQ_SSL` permitem usar o CloudAMQP (porta 5671 com SSL) com o mesmo código do ambiente local.
- **Segredos:** nenhum segredo tem valor padrão no `application.yml`. Banco, JWT e RabbitMQ vêm só das variáveis de ambiente. A chave da IA tem um placeholder que não é credencial e só serve para a API subir sem chave (as orientações usam o texto padrão).

### Health check

`GET /actuator/health` (público) **reflete só o banco**. O RabbitMQ fica fora (`management.health.rabbit.enabled: false`) porque a API funciona sem o broker. Se ele entrasse, uma queda do CloudAMQP marcaria a instância como doente no health check do App Service e poderia provocar reinícios sem necessidade. Com o Oracle fora do ar, o health responde `DOWN` (503).

## Variáveis de ambiente

| Variável | Descrição | Local | Produção |
|---|---|---|---|
| `DB_URL` | URL JDBC do Oracle | `jdbc:oracle:thin:@HOST:1521:SID` | |
| `DB_USERNAME` | Usuário do banco | | |
| `DB_PASSWORD` | Senha do banco | | |
| `JWT_SECRET` | Chave de assinatura do JWT (HS256), com no mínimo 32 caracteres | | |
| `AI_API_KEY` | Chave da API da OpenAI. Vazia = orientações com o texto padrão | | |
| `AI_BASE_URL` | URL base da API de IA | `https://api.openai.com/v1` | igual |
| `AI_MODEL` | Modelo de chat | `gpt-4o-mini` | igual |
| `RABBITMQ_HOST` | Host do RabbitMQ | `localhost` | host do CloudAMQP |
| `RABBITMQ_PORT` | Porta do RabbitMQ | `5672` | `5671` |
| `RABBITMQ_USERNAME` | Usuário do RabbitMQ | `guest` | usuário da instância |
| `RABBITMQ_PASSWORD` | Senha do RabbitMQ | `guest` | senha da instância |
| `RABBITMQ_VIRTUAL_HOST` | Vhost do RabbitMQ | `/` | vhost da instância |
| `RABBITMQ_SSL` | Conexão com SSL | `false` | `true` |
| `RISCO_FALTA_URL` | URL do serviço do modelo de risco de falta. Vazio = stub (risco null) | vazio | vazio até a integração |
| `PORT` | Porta do servidor. **Definida pelo App Service**, não vai no `.env` | (8080) | automática |

## Requisitos de Java Advanced

| Requisito | Onde está |
|---|---|
| 3+ entidades relacionadas, com CRUD e paginação | Entidades JPA em `model` (Usuario, Especialidade, Unidade, Medico, Fila, Consulta e as tabelas de contato e endereço). Medico tem `@ManyToMany` com Especialidade e Unidade; Fila tem `@ManyToOne` com Usuario, Especialidade e Unidade; Consulta, com Fila, Usuario, Medico e Unidade. CRUD completo de especialidades, unidades, médicos e filas, com GETs paginados (`Pageable`) |
| Bean Validation e tratamento de erros | DTOs (records) com `@NotBlank`, `@Size`, `@Pattern`, `@Email`, `@Past`, `@Min`/`@Max` etc. e `@Valid` nos controllers. `GlobalExceptionHandler` (`@RestControllerAdvice`) devolve o erro padrão do contrato, com `campos` nos erros de validação e os códigos do Oracle convertidos em HTTP |
| Spring Security + JWT com 2 papéis | OAuth2 Resource Server com `JwtEncoder`/`JwtDecoder` Nimbus (HS256), BCrypt, papéis `CIDADAO` e `GESTOR` (`ROLE_*`) nas rotas e regra de "dono" nos services |
| Spring AI integrado ao fluxo | Orientações da consulta com `ChatClient`, prompts em templates, minimização de dados e fallback (ver [IA generativa](#ia-generativa-spring-ai)) |
| Arquitetura distribuída | Opção A: RabbitMQ com publisher após o commit e consumer que chama a procedure (ver [Arquitetura distribuída](#arquitetura-distribuída-opção-a-rabbitmq)) |
| Documentação da API | Swagger UI com springdoc-openapi, `@Tag`/`@Operation` nos controllers e esquema Bearer para testar com o token |
| Deploy | Azure App Service com os scripts de [scripts/](scripts/) (ver [Deploy](#deploy-azure-app-service)) |

## Declaração de uso de IA

- **Claude (Anthropic):** apoio no planejamento da arquitetura, na geração de partes do código, na revisão de erros e na documentação.
- Todo o código foi revisado, testado e integrado pela equipe, que entende e responde pelo funcionamento da solução.
- A funcionalidade de IA do produto (orientações da consulta) usa o Spring AI e está descrita na seção [IA generativa (Spring AI)](#ia-generativa-spring-ai).
