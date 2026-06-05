# Service Portal Manager

Gerenciador de workflows do Service Portal, baseado em Kotlin 2.0 + Spring Boot 3.4 LTS + Gradle (Kotlin DSL).

## Visão Geral

O Manager é a aplicação **dona** da collection `workflows` no MongoDB — única com permissão de escrita (após a migração) e fonte de verdade do YAML de cada fluxo. Recebe os workflows em YAML, valida estrutura mínima e expõe três grupos de APIs:

- **CRUD** — criar, listar, atualizar e desativar fluxos (`/manager/flows`)
- **Consulta para o orquestrador** — lista de ativos e download do YAML (`/manager/workflows`)
- **Auth interna** — JWT HS512 (mesmo padrão do `generic-orchestrator`)

Esse serviço foi extraído do `generic-orchestrator` para separar **gerenciamento dos fluxos** da **execução dos fluxos**:

```
Antes:  generic-orchestrator                   ─── Mongo (workflows)
                              ↓
Agora:  service-portal-manager  ── Mongo (workflows)  ←── generic-orchestrator (consome)
```

A migração das integrações do BFF e do orquestrador acontece em tarefas subsequentes.

---

## Stack

| Componente | Versão |
|---|---|
| Kotlin | 2.0.21 |
| Java toolchain | 21 LTS |
| Spring Boot | 3.4.5 LTS |
| Gradle | Kotlin DSL |
| MongoDB | 7 (DB `service-portal-manager` — dono das collections de workflows e recursos) |
| Segurança | Spring Security + JWT HS512 (jjwt 0.12.6) |
| YAML | jackson-dataformat-yaml |
| Testes | JUnit 5 + mockk + springmockk |
| Cobertura | JaCoCo (gate ≥ 95% INSTRUCTION) |

---

## Estrutura do Projeto

```
src/main/kotlin/com/serviceportal/manager/
├── ManagerApplication.kt
├── config/                                     (reservado para configs futuras)
├── domain/
│   └── FlowDocument.kt                         # @Document(collection="workflows") com yamlContent
├── repository/
│   └── FlowDocumentRepository.kt               # Projection (sem yamlContent) + paginação + WithYaml
├── service/
│   ├── YamlValidationService.kt                # extrai metadados (id, version, description, active)
│   └── FlowDocumentService.kt                  # CRUD paginado + listActive + getYaml
├── controller/
│   ├── FlowController.kt                       # /manager/flows (CRUD)
│   └── WorkflowQueryController.kt              # /manager/workflows (active + yaml)
├── security/
│   ├── SecurityConfig.kt                       # JWT + 401 entry point
│   ├── JwtService.kt                           # HS512 — gerar/validar tokens
│   ├── JwtAuthenticationFilter.kt              # OncePerRequestFilter
│   └── AuthController.kt                       # /api/auth/tokens
├── dto/
│   └── Dtos.kt                                 # FlowSummaryDto, LoginRequest, LoginResponse
└── exception/
    ├── Exceptions.kt                           # FlowNotFoundException, InvalidFlowDefinitionException, ...
    └── GlobalExceptionHandler.kt               # 400/404/409
src/main/resources/
├── application.yml
└── application-docker.yml
src/test/kotlin/com/serviceportal/manager/
├── service/
│   ├── YamlValidationServiceTest.kt            (12 testes)
│   └── FlowDocumentServiceTest.kt              (15 testes)
├── security/
│   ├── JwtServiceTest.kt                       (4 testes)
│   ├── JwtAuthenticationFilterTest.kt          (5 testes)
│   ├── AuthControllerTest.kt                   (3 testes)
│   └── SecurityConfigIT.kt                     (4 testes — @SpringBootTest com MockMvc)
├── controller/
│   ├── FlowControllerTest.kt                   (5 testes)
│   └── WorkflowQueryControllerTest.kt          (2 testes)
└── exception/
    └── GlobalExceptionHandlerTest.kt           (3 testes)
```

---

## Configuração

### `application.yml` — variáveis principais

```yaml
spring:
  data:
    mongodb:
      uri: ${MONGODB_URI:mongodb://localhost:27017/service-portal-manager}
      database: ${MONGODB_DATABASE:service-portal-manager}

server:
  port: ${SERVER_PORT:8082}

manager:
  security:
    jwt:
      secret: ${JWT_SECRET:CHANGE_ME_PLEASE_THIS_IS_A_DEV_ONLY_SECRET_AT_LEAST_64_CHARS_LONG_FOR_HS512}
      expiration-seconds: ${JWT_EXPIRATION:3600}
      issuer: ${JWT_ISSUER:service-portal-manager}
    admin:
      username: ${MANAGER_ADMIN_USERNAME:admin}
      password: ${MANAGER_ADMIN_PASSWORD:admin}
```

### Variáveis de ambiente

| Variável | Descrição | Default |
|---|---|---|
| `SERVER_PORT` | Porta do Manager | `8082` |
| `MONGODB_URI` | URI do MongoDB | `mongodb://localhost:27017/service-portal-manager` |
| `MONGODB_DATABASE` | Database do MongoDB | `service-portal-manager` |
| `JWT_SECRET` | Chave HS512 (≥ 64 chars) | placeholder de dev |
| `JWT_EXPIRATION` | TTL do token em segundos | `3600` |
| `JWT_ISSUER` | Claim `iss` dos tokens emitidos | `service-portal-manager` |
| `MANAGER_ADMIN_USERNAME` | Usuário admin | `admin` |
| `MANAGER_ADMIN_PASSWORD` | Senha admin | `admin` |

> O Manager é o dono do database `service-portal-manager` e da collection `workflows`. O orquestrador não acessa o MongoDB diretamente — consome os workflows via API REST do Manager. Documentos têm o campo `yamlContent` com o YAML cru.

---

## Profiles Spring

| Profile | Arquivo | Uso |
|---|---|---|
| default | `application.yml` | Desenvolvimento local |
| `docker` | `application-docker.yml` | Infraestrutura via `docker compose -f docker-compose-service-portal.yml up` |

```bash
./gradlew bootRun --args='--spring.profiles.active=docker'
```

---

## Como Executar

```bash
# 1. Subir infraestrutura (MongoDB no mínimo)
docker compose -f docker-compose-service-portal.yml up -d mongodb

# 2. Build
./gradlew build

# 3. Apenas testes + relatório JaCoCo
./gradlew test jacocoTestReport

# 4. Verificar gate de cobertura (≥ 95%)
./gradlew jacocoTestCoverageVerification

# 5. Rodar localmente
./gradlew bootRun
```

O Manager sobe em `http://localhost:8082`.

---

## API

Todos os endpoints (exceto `/api/auth/**`, `/actuator/health` e `/actuator/info`) exigem `Authorization: Bearer <token>`.

### Autenticação

```bash
curl -X POST http://localhost:8082/api/auth/tokens \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'
# → {"token":"eyJ...","expiresIn":3600}
```

### Endpoints

| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/api/auth/tokens` | Gera token JWT |
| POST | `/manager/flows` | Cria fluxo (body: YAML) |
| GET | `/manager/flows?page=&size=&sort=` | **Lista paginada** de todos os fluxos — sem `yamlContent` |
| GET | `/manager/flows/{flowId}/versions/{version}` | Busca metadados de um fluxo (sem `yamlContent`) |
| PUT | `/manager/flows/{flowId}/versions/{version}` | Atualiza fluxo (body: YAML) |
| DELETE | `/manager/flows/{flowId}/versions/{version}` | Soft-delete (seta `active=false`) |
| GET | `/manager/flows?status=active` | Lista compacta de fluxos ativos (sem `yamlContent`) — consumido pelo orquestrador |
| GET | `/manager/flows/{flowId}/versions/{version}/yaml` | YAML cru — consumido pelo orquestrador |
| GET | `/actuator/health` | Health check (público) |

### Paginação em `GET /manager/flows`

Query params padrão de Spring Data (`page`, `size`, `sort`). Defaults: `page=0`, `size=20`, ordenação por `flowId,version` ascendente. Limite máximo de 100 itens por página (`spring.data.web.pageable.max-page-size`).

```bash
curl -s "http://localhost:8082/manager/flows?page=0&size=20&sort=createdAt,desc" \
  -H "Authorization: Bearer $TOKEN" | jq
```

Resposta:

```json
{
  "content": [
    {"flowId": "create-order-v1", "version": "1.0.0", "description": "...", "active": true,
     "createdAt": "...", "updatedAt": "..."}
  ],
  "totalElements": 3,
  "totalPages": 1,
  "size": 20,
  "number": 0,
  "first": true,
  "last": true
}
```

### Por que listas não trazem `yamlContent`

`findAll(Pageable)` e `findByAtivoTrue()` usam projection MongoDB excluindo `yamlContent` — evita trafegar dezenas/centenas de KB por fluxo quando o caller só quer metadados. Para receber o YAML, usar exclusivamente `GET /manager/flows/{flowId}/versions/{version}/yaml`.

### Exemplos

```bash
TOKEN=$(curl -s -X POST http://localhost:8082/api/auth/tokens \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}' | jq -r '.token')

# Criar fluxo a partir de um arquivo YAML
curl -X POST http://localhost:8082/manager/flows \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: text/plain" \
  --data-binary @meu-fluxo.yml

# Listar fluxos ativos (orquestrador consome após migração)
curl -s http://localhost:8082/manager/flows?status=active \
  -H "Authorization: Bearer $TOKEN" | jq

# Recuperar YAML cru de um fluxo (orquestrador consome após migração)
curl -s http://localhost:8082/manager/workflows/create-order-v1/1.0.0/yaml \
  -H "Authorization: Bearer $TOKEN"
```

### Códigos de erro

| Código | Quando |
|---|---|
| `400 INVALID_FLOW` | YAML inválido, sem `flow`/`id`/`version`/`contract`/`integrations`, ou `id`/`version` do path divergente do YAML em PUT |
| `401` | Sem token ou token inválido |
| `404 FLOW_NOT_FOUND` | Fluxo `{flowId}/versions/{version}` não existe — ou existe mas foi criado antes do Manager (sem `yamlContent`) e o cliente pediu o YAML |
| `409 FLOW_ALREADY_EXISTS` | POST tentando criar `{flowId}/versions/{version}` que já existe |

---

## Modelo de dados

### `workflows` (MongoDB)

Collection do database `service-portal-manager`, da qual o Manager é o dono. Documentos têm o campo `yamlContent`:

```js
{
  "_id": ObjectId("6a0091c36edcca219a4f692f"),
  "flowId": "create-order-v1",
  "version": "1.0.0",
  "description": "Fluxo de criação de pedido",
  "active": true,
  "yamlContent": "fluxo:\n  id: \"create-order-v1\"\n  ...",
  "createdAt": ISODate("2026-05-10T14:10:10.999Z"),
  "updatedAt": ISODate("2026-05-10T14:10:10.999Z"),
  "_class": "com.serviceportal.manager.domain.FlowDocument"
}
```

| Campo | Tipo | Origem | Observação |
|---|---|---|---|
| `_id` | ObjectId | Mongo | gerado automaticamente |
| `flowId` | String | YAML `flow.id` | mesmo nome usado pelo orquestrador (sem `@Field` override) |
| `version` | String | YAML `flow.version` | participa do índice composto único |
| `description` | String? | YAML `flow.description` | nullable |
| `ativo` | Boolean | YAML `flow.active` | default `true` se ausente; soft-delete usa `false` |
| `yamlContent` | String | body do `POST /manager/flows` | YAML cru, **fonte de verdade** |
| `createdAt` / `updatedAt` | ISODate | Manager | timestamps do serviço |
| `_class` | String | Spring Data Mongo | nome qualificado da classe |

Índice composto único em `flowId` + `version` declarado em [`mongodb-manager/init-mongo.js`](mongodb-manager/init-mongo.js), que também cria as collections `integrations`, `contracts` e `validations` e popula dados de exemplo. O Manager **não** versiona automaticamente workflows — quem decide a versão é quem submete o YAML; tentativa de POST com `{flowId, version}` existente devolve 409.

> ⚠️ **Save sobrescreve o doc inteiro.** Para evitar zerar `yamlContent` em mutações (update/deactivate), o serviço carrega o doc completo via `findByFlowIdAndVersaoWithYaml(...)` antes de salvar. As listas (`findAll(Pageable)`, `findByAtivoTrue`) e o `get` simples usam projection sem `yamlContent` — operações somente-leitura por design.

### Validação leve do YAML

O Manager **não** duplica os modelos do orquestrador. Apenas valida:

1. `flow` é a chave raiz
2. `flow.id` e `flow.version` são strings não-vazias
3. `flow.contract` está presente
4. `flow.integrations` é uma lista não-vazia

A validação profunda (campos por tipo de integração, regras de validação do contrato, etc.) continua no orquestrador, na execução. Se o YAML passar pelo Manager mas falhar no orquestrador, o erro acontece no momento do `POST /api/flows/{flowId}/versions/{version}/executions`.

---

## Segurança

### Inbound (chamadas externas → Manager)

`SecurityConfig` exige Bearer JWT em todos os endpoints exceto `/api/auth/**` e `/actuator/health|info`. Tokens são HS512 com `jjwt 0.12.6` — mesmo padrão do `generic-orchestrator`. Sem token → **401** (via `HttpStatusEntryPoint`).

> **Atenção:** o `AuthController` aceita `admin/admin` (mesmo padrão do orquestrador) — apenas para desenvolvimento. Em produção, integrar com banco de usuários + BCrypt e/ou trocar pelo Authentik (igual ao BFF).

### Outbound

Sem chamadas externas — o Manager só fala com o MongoDB.

---

## Testes e cobertura

```bash
./gradlew test jacocoTestReport jacocoTestCoverageVerification
# Relatório HTML: build/reports/jacoco/test/html/index.html
```

63 testes em 10 arquivos. Gate `jacocoTestCoverageVerification` exige **≥ 95% INSTRUCTION** no código de aplicação (excluindo `ManagerApplication`, `dto/**` e `exception/**` que são meros holders).

Distribuição:

| Arquivo | Testes | Foco |
|---|---|---|
| `YamlValidationServiceTest` | 12 | Estrutura mínima do YAML + casos negativos |
| `FlowDocumentServiceTest` | 15 | CRUD + paginação + verifica que `WithYaml` é usado em update/deactivate/getYaml |
| `FlowControllerTest` | 5 | Roteamento, status HTTP, paginação |
| `WorkflowQueryControllerTest` | 2 | `/active` + YAML cru |
| `JwtServiceTest` | 4 | Round-trip + expirado + inválido |
| `JwtAuthenticationFilterTest` | 5 | Cenários de header (sem/Bearer/Basic/inválido/exception) |
| `AuthControllerTest` | 3 | login válido + 401 |
| `SecurityConfigIT` | 4 | `@SpringBootTest` com MockMvc — endpoints públicos vs protegidos |
| `GlobalExceptionHandlerTest` | 3 | 400/404/409 |
| `FlowDocumentSchemaTest` | 5 | Garante schema Mongo (`flowId`, `_id`, sem `@Field` override) |

---

## Docker

```bash
docker build -t service-portal-manager .
docker run --rm -p 8082:8082 \
  -e MONGODB_URI=mongodb://mongo:27017/service-portal-manager \
  -e JWT_SECRET=... \
  service-portal-manager
```

O `Dockerfile` faz build em duas etapas (Gradle 8.10 + Temurin JRE 21) e expõe a porta `8082`. Variáveis de ambiente já têm defaults sensatos para desenvolvimento local.
