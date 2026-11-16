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
- (Opcional nesta etapa) RabbitMQ e uma chave da OpenAI. A API sobe sem eles.

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

### 3. Subir a API

```bash
./mvnw spring-boot:run
```

No Windows (PowerShell/CMD): `mvnw.cmd spring-boot:run`

Na primeira execução, o `SenhaHashRunner` converte para BCrypt as senhas em texto puro da carga. Isso leva alguns segundos.

### 4. Testar

- Swagger UI: http://localhost:8080/swagger-ui.html. Faça o login em `POST /api/auth/login`, copie o `token`, clique em **Authorize** e cole o token.
- Health: http://localhost:8080/actuator/health
- Arquivo [test.http](test.http) com exemplos de requisições (extensão REST Client do VS Code).

## Endpoints

Papéis: **pub** = público; **logado** = qualquer usuário autenticado; **CID** = CIDADAO; **GES** = GESTOR; **dono** = a solicitação pertence ao usuário do token.

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
