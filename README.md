# Cadê minha consulta — API

API Java do app **Cadê minha consulta**, Global Solution FIAP 2026/2, tema CitizenTech (ODS 3: Saúde e bem-estar).

## O problema

No SUS, cerca de 1 em cada 4 pacientes falta à consulta com especialista sem avisar. A vaga, que levou meses para sair, é desperdiçada enquanto outras pessoas seguem esperando na fila.

## A solução

- O **cidadão** acompanha pelo app as suas solicitações (fila) e consultas. Com um toque, ele confirma presença ou libera a vaga.
- Quando uma vaga é liberada, o sistema **agenda automaticamente** o próximo paciente compatível da fila.
- O **gestor** da unidade agenda pacientes da fila para médicos, acompanha as próximas consultas e registra presença ou falta.
- A **IA generativa** escreve lembretes e orientações de preparo em linguagem simples.
- Um modelo de **ML** prevê o risco de falta. A API já tem o ponto de integração; o modelo, feito por outro integrante, entra depois.

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
- (Opcional) Uma chave da OpenAI. Sem ela, a API sobe e as orientações usam o texto padrão (ver [IA generativa](#ia-generativa-spring-ai)).

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
3. Junta o nome do paciente, buscado na API. O nome não vem do cursor e não vai para o modelo.

**Ponto de integração com o modelo de ML:** o `RiscoFaltaConfig` escolhe a implementação pela variável `RISCO_FALTA_URL` (propriedade `risco-falta.url`):

- vazia (padrão): `StubRiscoFaltaClient`, que devolve `risco` e `nivel` null para todas as consultas;
- preenchida: é onde entra o cliente HTTP real (um `HttpRiscoFaltaClient` com `RestClient`, que envia os atributos ao serviço do modelo e lê as previsões). Esse cliente ainda não foi implementado: por enquanto, a API registra um aviso no log e continua usando o stub.

Para integrar o modelo, basta implementar `RiscoFaltaClient` e devolver o novo cliente no `RiscoFaltaConfig`. O service, o controller e o contrato não mudam.

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

## Variáveis de ambiente

| Variável | Descrição | Exemplo |
|---|---|---|
| `DB_URL` | URL JDBC do Oracle | `jdbc:oracle:thin:@HOST:1521:SID` |
| `DB_USERNAME` | Usuário do banco | |
| `DB_PASSWORD` | Senha do banco | |
| `JWT_SECRET` | Chave de assinatura do JWT (HS256), com no mínimo 32 caracteres | |
| `AI_API_KEY` | Chave da API da OpenAI. Vazia = orientações com o texto padrão | |
| `AI_BASE_URL` | URL base da API de IA | `https://api.openai.com/v1` |
| `AI_MODEL` | Modelo de chat | `gpt-4o-mini` |
| `RABBITMQ_HOST` | Host do RabbitMQ | `localhost` |
| `RABBITMQ_PORT` | Porta do RabbitMQ | `5672` |
| `RABBITMQ_USERNAME` | Usuário do RabbitMQ | `guest` |
| `RABBITMQ_PASSWORD` | Senha do RabbitMQ | `guest` |
| `RISCO_FALTA_URL` | URL do serviço do modelo de risco de falta. Vazio = stub (risco null) | |

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
