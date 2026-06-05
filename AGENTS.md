# AGENTS.md — service-portal-manager

Guia de contexto para agentes de IA trabalhando neste componente.

---

## Stack

| Item | Versão |
|---|---|
| Kotlin | 2.0.21 |
| Java toolchain | 21 (LTS) |
| Spring Boot | 3.4.5 (LTS) |
| Build | Gradle Kotlin DSL |
| Banco de dados | MongoDB 7 (`spring-boot-starter-data-mongodb`) |
| Segurança | JWT HS512 via jjwt 0.12.6 — autenticação interna server-to-server |
| Porta | 8082 |

---

## Responsabilidade

O Manager é o **dono da collection `workflows`** no MongoDB. Ele:

- Persiste e gerencia o ciclo de vida de workflows (CRUD via YAML)
- Serve o YAML bruto para o orquestrador (`GET .../yaml`)
- Serve a lista de workflows ativos para o warm-up do cache do orquestrador (`?status=active`)

O parse profundo do YAML (validação de integrações, contrato, etc.) é responsabilidade do orquestrador — o Manager faz apenas validação leve (extração de `flowId`, `version`, `description`, `active`).

---

## Endpoints

### Público (sem token)

| Método | Path | Descrição |
|---|---|---|
| `POST` | `/api/auth/tokens` | Gera JWT (admin/admin) — retorna 201 |
| `GET` | `/actuator/health` | Health check |
| `GET` | `/actuator/info` | Info da aplicação |
| `GET` | `/actuator/metrics` | Métricas |

### Protegidos (Bearer JWT obrigatório)

| Método | Path | Descrição |
|---|---|---|
| `POST` | `/manager/flows` | Cria workflow (body: YAML como texto) — retorna 201 |
| `GET` | `/manager/flows` | Lista paginada (`?page=&size=&sort=`) — sem `yamlContent` |
| `GET` | `/manager/flows?status=active` | Lista de ativos (sem paginação) — consumida pelo orquestrador |
| `GET` | `/manager/flows/{flowId}/versions/{version}` | Metadados do workflow — sem `yamlContent` |
| `GET` | `/manager/flows/{flowId}/versions/{version}/yaml` | YAML bruto (`application/x-yaml`) |
| `PUT` | `/manager/flows/{flowId}/versions/{version}` | Atualiza workflow (rejeita 400 se id/version divergem do YAML) |
| `DELETE` | `/manager/flows/{flowId}/versions/{version}` | Soft-delete (`active=false`), idempotente |

---

## Schema do documento MongoDB

Collection: `workflows` | Database: `service-portal-manager`

```json
{
  "_id": "ObjectId",
  "flowId": "create-order-v1",
  "version": "1.0.0",
  "description": "Order creation flow",
  "active": true,
  "yamlContent": "flow:\n  id: ...",
  "createdAt": "ISODate",
  "updatedAt": "ISODate",
  "_class": "com.serviceportal.manager.domain.FlowDocument"
}
```

**Índice composto único** em `flowId` + `version` criado pelo script `mongodb-manager/init-mongo.js` (neste repositório). O `auto-index-creation` do Spring Data está desabilitado (padrão do Spring Boot 3) — o init-mongo.js é a fonte de verdade do índice.

**Atenção:** `yamlContent` é excluído por projeção MongoDB nas queries de listagem e `get` para economizar banda. Apenas `findByFlowIdAndVersionWithYaml` traz o campo completo — use-o sempre que for fazer `repository.save()` após mutação, caso contrário o save sobrescreve o documento inteiro zerando `yamlContent`.

---

## Inicialização do MongoDB

O arquivo `mongodb-manager/init-mongo.js` cria o database `service-portal-manager`
com as collections `workflows`, `integrations`, `contracts` e `validations`, cada
uma com índice único composto `(id, version)`. Também popula **dados de exemplo**
(workflow `create-order-v1` + contract/integrations/validations referenciados).

Este script é montado em `/docker-entrypoint-initdb.d/` no container MongoDB e
executado automaticamente na primeira inicialização (volume vazio). Para reexecutá-lo
após mudanças, é necessário destruir o volume: `docker compose down -v && docker compose up -d`.

---

## Respostas de erro

| Situação | HTTP | Código no body |
|---|---|---|
| YAML sem `flowId`/`version` | 400 | `INVALID_FLOW` |
| `flowId`/`version` do path divergem do YAML | 400 | `INVALID_FLOW` |
| Workflow não encontrado | 404 | `FLOW_NOT_FOUND` |
| Workflow já existe (`flowId`+`version`) | 409 | `FLOW_ALREADY_EXISTS` |

---

## Como rodar localmente

### Pré-requisitos

- Java 21 instalado
- Docker + Docker Compose

### Opção 1 — App no host, MongoDB via Docker

```bash
# Na raiz do repositório
docker compose -f docker-compose-service-portal.yml up -d mongodb

cd service-portal-manager
./gradlew bootRun
```

### Opção 2 — Stack completa containerizada

```bash
# Na raiz do repositório
docker compose -f docker-compose-service-portal.yml up -d
```

---

## Como testar

```bash
cd service-portal-manager

# Testes unitários (gera relatório JaCoCo automaticamente ao final)
./gradlew test
# Relatório em: build/reports/jacoco/jacocoTestReport/html/index.html

# Verificação do gate de cobertura
./gradlew jacocoTestCoverageVerification

# Build sem testes
./gradlew bootJar -x test

# Build da imagem Docker
docker build -t service-portal-manager:local .
```

### Gate de cobertura JaCoCo

Gate ≥ 95% INSTRUCTION sobre todo o código de aplicação, excluindo:
- `ManagerApplication` (apenas o `main`)
- `dto/**` (holders sem lógica)
- `exception/**` (apenas declarações de classe)

Cobertura atual: **96% INSTRUCTION**. O relatório é gerado automaticamente ao final de `test` (via `finalizedBy`).

---

## Estrutura de pacotes relevante

```
src/main/kotlin/com/serviceportal/manager/
├── controller/
│   └── FlowController                 # CRUD + /yaml + listagem ?status=active
├── service/
│   ├── FlowDocumentService            # create/update/get/listAll/listActive/deactivate/getYaml
│   └── YamlValidationService          # validação leve: extrai flowId, version, description, active
├── repository/
│   └── FlowDocumentRepository         # queries MongoDB com projeção: leve vs WithYaml
├── domain/
│   └── FlowDocument                   # @Document("workflows") — sem @CompoundIndexes
├── security/
│   ├── AuthController                 # POST /api/auth/tokens
│   ├── JwtService                     # geração e validação JWT HS512
│   ├── JwtAuthenticationFilter        # filtro de Bearer token
│   └── SecurityConfig                 # libera /api/auth/tokens e actuator; protege o resto
├── exception/
│   ├── Exceptions.kt                  # FlowNotFoundException, InvalidFlowDefinitionException, FlowAlreadyExistsException
│   └── GlobalExceptionHandler         # mapeia exceções → HTTP com código de erro no body
└── dto/
    └── Dtos.kt                        # FlowSummaryDto (sem yamlContent), FlowMetadata
```

---

## Decisões de design

**`yamlContent` como string.** O YAML original é armazenado como string bruta — não parseado em schema próprio. O parse tipado (integrações, contrato, templates) é feito pelo orquestrador. Alternativas como GridFS ou schema normalizado foram descartadas por serem desnecessárias para o volume atual.

**Projeção MongoDB em vez de DTO parcial.** Queries de listagem e `get` usam `@Query(fields = "{ 'yamlContent': 0 }")` para excluir o campo no banco. `findByFlowIdAndVersionWithYaml` é o único método que retorna o documento completo — use-o apenas quando for ler ou modificar `yamlContent`.

**Nunca chamar `save()` em doc sem projeção WithYaml.** Um doc carregado por query leve tem `yamlContent = null`. Chamar `repository.save(doc)` nesse estado sobrescreve o documento inteiro e apaga `yamlContent` no banco.

**Soft-delete.** `DELETE` não remove o documento — seta `active = false` e é idempotente. O índice de unicidade permanece intacto: criar um workflow com o mesmo `flowId`+`version` após o delete resulta em 409.

**Índice de unicidade gerenciado externamente.** Não há `@CompoundIndexes` no `FlowDocument` — o Spring Boot 3 não cria índices automaticamente por padrão. O índice composto único (`flowId` + `version`) é criado pelo `mongodb-manager/init-mongo.js` (neste repositório).

**Validação leve no Manager.** `YamlValidationService` extrai apenas os metadados mínimos para persistência. Não valida integrações, templates, contratos nem tipos de campo — isso fica no orquestrador.

---

## Variáveis de ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `SERVER_PORT` | `8082` | Porta do servidor |
| `SPRING_PROFILES_ACTIVE` | `` | Perfil Spring ativo |
| `MONGODB_URI` | `mongodb://localhost:27017/service-portal-manager` | URI de conexão com o MongoDB |
| `MONGODB_DATABASE` | `service-portal-manager` | Nome do banco de dados |
| `JWT_SECRET` | (dev secret) | Segredo HS512 — **trocar em produção** |
| `JWT_EXPIRATION` | `3600` | Expiração do JWT em segundos |
| `JWT_ISSUER` | `service-portal-manager` | Claim `iss` do token gerado |
| `MANAGER_ADMIN_USERNAME` | `admin` | Login para `POST /api/auth/tokens` |
| `MANAGER_ADMIN_PASSWORD` | `admin` | Senha para `POST /api/auth/tokens` |

---

## Restrições

- Kotlin 2.0 + Java 21 LTS, Spring Boot 3.4.5 LTS — não atualizar versões
- Gradle com Kotlin DSL
- Não adicionar `@CompoundIndexes` no `FlowDocument` — o init-mongo.js é a fonte de verdade do índice
- Não chamar `repository.save()` em documentos carregados por queries sem projeção `WithYaml`
- Soft-delete apenas — sem remoção física de documentos
