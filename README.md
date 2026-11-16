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
