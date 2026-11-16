# Cadê minha consulta — API

API Java do app **Cadê minha consulta**, Global Solution FIAP 2026/2, tema CitizenTech (ODS 3: Saúde e bem-estar).

## O problema

No SUS, cerca de 1 em cada 4 pacientes falta à consulta com especialista sem avisar. A vaga, que levou meses para sair, é desperdiçada enquanto outras pessoas seguem esperando na fila.

## A solução

- O **cidadão** acompanha pelo app as suas solicitações (fila) e consultas. Com um toque, ele confirma presença ou libera a vaga.
- Quando uma vaga é liberada, o sistema **agenda automaticamente** o próximo paciente compatível da fila.
- O **gestor** da unidade agenda pacientes da fila para médicos, acompanha as próximas consultas e registra presença ou falta.
- A **IA generativa** escreve lembretes e orientações de preparo em linguagem simples.
- Um modelo de **ML** prevê o risco de falta (integrado em etapa posterior).

O contrato completo entre a API e o app mobile está em [docs/CONTRATO_API.md](docs/CONTRATO_API.md).

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 4 (Web MVC, Data JPA, Validation, Actuator) |
| Segurança | Spring Security + JWT via OAuth2 Resource Server (JwtEncoder/JwtDecoder Nimbus, HS256), BCrypt |
| Banco | Oracle (schema e regras em PL/SQL nos scripts de `/scripts`) |
| IA | Spring AI (OpenAI) |
| Mensageria | RabbitMQ |
| Documentação | springdoc-openapi (Swagger UI) |
| Outros | Lombok, Maven Wrapper |

## Como rodar

### Pré-requisitos

- Java 21
- Acesso a um banco Oracle
- Docker, para o RabbitMQ (ver o passo 3). Sem o broker a API sobe normalmente, mas a vaga liberada não é realocada automaticamente.
- (Opcional nesta etapa) Uma chave da OpenAI. A API sobe sem ela.

### 1. Criar o banco

Execute os scripts de [scripts/](scripts/) no Oracle, nesta ordem:

1. `script-bd.sql`: tabelas, constraints, sequences e triggers de ID
2. `02-triggers.sql`: triggers de auditoria e regra de data futura
3. `03-pkg-gestao-consultas.sql`: package de agendamento
4. `04-pkg-consulta-dados.sql`: package de consultas e dados para o modelo de risco
5. `05-carga-dados.sql`: carga de dados (inclui os usuários de teste)

### 2. Configurar as variáveis de ambiente

Copie o `.env.example` para `.env` na raiz do projeto e preencha os valores. O `.env` não vai para o Git.

```bash
cp .env.example .env
```

### 3. Subir o RabbitMQ

O [docker-compose.yml](docker-compose.yml) da raiz sobe só o RabbitMQ (imagem `rabbitmq:management`):

```bash
docker compose up -d
```

- AMQP: `localhost:5672` (usuário e senha `guest`, os mesmos do `.env.example`)
- Painel de administração: http://localhost:15672 (`guest` / `guest`). Em **Queues** aparece a fila `vaga.liberada.queue`, criada pela API ao conectar.
- Para parar: `docker compose down`

### 4. Subir a API

```bash
./mvnw spring-boot:run
```

No Windows (PowerShell/CMD): `mvnw.cmd spring-boot:run`

Na primeira execução, o `SenhaHashRunner` converte para BCrypt as senhas em texto puro da carga. Isso leva alguns segundos.

### 5. Testar

- Swagger UI: http://localhost:8080/swagger-ui.html. Faça o login em `POST /api/auth/login`, copie o `token`, clique em **Authorize** e cole o token.
- Health: http://localhost:8080/actuator/health. Inclui o RabbitMQ: com o broker parado, responde `DOWN` (503).
- Arquivo [test.http](test.http) com exemplos de requisições (extensão REST Client do VS Code).

## Arquitetura distribuída: opção A (RabbitMQ)

Quando o cidadão libera a vaga, a realocação acontece de forma assíncrona, por mensageria:

```
Cidadão --PATCH /api/consultas/{id}/cancelar--> API
                                                 | transação: consulta e fila viram CANCELADA -> COMMIT
                                                 | depois do commit: VagaLiberadaPublisher
                                                 v
                        consultas.exchange (topic) --vaga.liberada--> vaga.liberada.queue
                                                                            |
                                                                            v
                        VagaLiberadaConsumer --> PRC_AGENDAR_CONSULTA(idMedico) no Oracle
                                                 = nova consulta AGENDADA para o próximo da fila
```

1. `PATCH /api/consultas/{id}/cancelar` (`ConsultaService.cancelar`): numa única transação, a consulta vira CANCELADA e a solicitação (fila) dela vira CANCELADA com `data_saida`. O serviço publica um `VagaLiberadaEvent { idConsulta, idMedico, idUnidade, idEspecialidade, dataHora }` como evento do Spring.
2. `VagaLiberadaPublisher` escuta esse evento com `@TransactionalEventListener(AFTER_COMMIT)`: só envia a mensagem para a exchange `consultas.exchange` (topic, routing key `vaga.liberada`) **depois do commit**. Se a transação falhar, nada é publicado.
3. Se o RabbitMQ estiver fora do ar, o publisher só registra o erro no log. O cancelamento continua valendo; a vaga apenas não é realocada automaticamente (o gestor pode agendar pelo `POST /api/gestor/agendamentos`).
4. `VagaLiberadaConsumer` (`@RabbitListener` na fila `vaga.liberada.queue`) chama `AgendamentoService.agendarProximo(idMedico)`, que executa `PKG_GS_GESTAO_CONSULTAS.PRC_AGENDAR_CONSULTA` via `SimpleJdbcCall`. A procedure escolhe o próximo paciente compatível com o médico (prioridade + antiguidade), cria a consulta AGENDADA, tira a solicitação da fila e faz `COMMIT`.
5. Erros no consumidor não são reprocessados em loop (`default-requeue-rejected: false`): fila vazia (`-20003`) gera só um log informativo; qualquer outro erro gera log de erro e a mensagem é descartada.

As mensagens trafegam em JSON (`JacksonJsonMessageConverter`). Exchange, fila e binding são declarados pela própria API (`RabbitConfig`) ao conectar no broker. A API sobe mesmo sem o broker: o listener tenta reconectar em segundo plano.

**Limitação conhecida:** a procedure agenda no primeiro horário útil livre do médico a partir de `SYSDATE + 5` (8h–17h, seg–sex), para o paciente ter pelo menos cinco dias de aviso. Por isso, o próximo paciente nem sempre ocupa exatamente o horário que foi liberado: se a consulta cancelada era nos próximos cinco dias, aquele horário fica livre.

## Endpoints

Papéis: **pub** = público; **logado** = qualquer usuário autenticado; **CID** = CIDADAO; **GES** = GESTOR; **dono** = o recurso (solicitação ou consulta) pertence ao usuário do token.

Todo GET de lista é paginado (`?page=0&size=10&sort=campo,asc`) e devolve `content`, `totalElements`, `totalPages`, `number` e `size`. Erros seguem o formato padrão da seção 5 do [contrato](docs/CONTRATO_API.md).

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

`ConsultaResponse` traz `diasAteConsulta` (dias de calendário até a consulta; 0 se já passou), o médico com nome completo e CRM, e a unidade com o endereço formatado.

### Gestor

| Método | Rota | Papel | Observação |
|---|---|---|---|
| POST | `/api/gestor/agendamentos` | GES | Body `{idMedico}`. Chama `PRC_AGENDAR_CONSULTA` e devolve 201 com a consulta criada (com `paciente`). Erros da procedure: 404 médico inexistente, 409 inativo/sem horário/fila alterada, 422 fila vazia |
| GET | `/api/gestor/consultas/proximas?dias=15` | GES | AGENDADA/CONFIRMADA nos próximos `dias` (1 a 365), padrão `sort=dataHora,asc`, com `paciente` (nome e telefone) |
| PATCH | `/api/gestor/consultas/{id}/presenca` | GES | Body `{status: "PRESENTE" \| "FALTA"}`. Só AGENDADA/CONFIRMADA com data passada (senão 409) |

### Infra

| Método | Rota | Papel |
|---|---|---|
| GET | `/actuator/health` | pub |
| GET | `/swagger-ui.html` | pub |

## Variáveis de ambiente

| Variável | Descrição | Exemplo |
|---|---|---|
| `DB_URL` | URL JDBC do Oracle | `jdbc:oracle:thin:@HOST:1521:SID` |
| `DB_USERNAME` | Usuário do banco | |
| `DB_PASSWORD` | Senha do banco | |
| `JWT_SECRET` | Chave de assinatura do JWT (HS256), com no mínimo 32 caracteres | |
| `AI_API_KEY` | Chave da API da OpenAI | |
| `AI_BASE_URL` | URL base da API de IA | `https://api.openai.com/v1` |
| `AI_MODEL` | Modelo de chat | `gpt-4o-mini` |
| `RABBITMQ_HOST` | Host do RabbitMQ | `localhost` |
| `RABBITMQ_PORT` | Porta do RabbitMQ | `5672` |
| `RABBITMQ_USERNAME` | Usuário do RabbitMQ | `guest` |
| `RABBITMQ_PASSWORD` | Senha do RabbitMQ | `guest` |

## Usuários de teste

Criados pelo `05-carga-dados.sql`:

| Perfil | Login | Senha |
|---|---|---|
| GESTOR | `gestor` | `SenhaGestor2026` |
| CIDADAO | `cidadao` | `Senha123` |

## Equipe

| Nome | RM |
|---|---|
| | |
| | |
| | |

## Declaração de uso de IA

<!-- Preencher -->
