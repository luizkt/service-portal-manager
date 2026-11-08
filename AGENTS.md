# AGENTS.md — service-portal-manager

Índice para agentes de IA. Leia antes de abrir código. Método: `../docs/plans/PLAN-repo-indexing.md` (monorepo).

<!-- index:meta
repo: service-portal-manager
tipo: spring-boot-api (CRUD versionado de workflows, contratos, integrações e validações)
fonte-de-verdade: build.gradle.kts, src/main/resources/application.yml, src/main/kotlin/com/serviceportal/manager, mongodb-manager/init-mongo.js
atualizado-em: 2026-11-07
-->

## Resumo

Dono das collections `workflows`, `integrations`, `contracts` e `validations` no MongoDB. Guarda o YAML do fluxo como texto bruto e só faz validação leve (`flow.id`, `flow.version`, `flow.contract`, ao menos uma integração). O parse profundo é do `generic-orchestrator`, que busca o YAML aqui. Quando um fluxo muda ou é desativado, o Manager invalida o cache do orquestrador.

<!-- gen:size -->Tamanho: 33 arquivos .kt em `src/main`, 24 em `src/test`.<!-- /gen:size -->

Stack: Kotlin 2.0.21, Java 21, Spring Boot 3.4.5 (não atualizar), Gradle Kotlin DSL, Spring MVC, MongoDB 7, RestClient, JWT HS512 (jjwt 0.13.0), Spotless/ktlint. Porta `SERVER_PORT` (8082).

## Endpoints

Base do código: `src/main/kotlin/com/serviceportal/manager/`.

| Rota | Auth | Onde |
|---|---|---|
| `POST /api/auth/tokens` (201, `{token, expiresIn}`) | público | `security/AuthController` |
| `/actuator/**`, `/swagger-ui.html`, `/v3/api-docs/**` | público | `security/SecurityConfig` |
| `/manager/flows`: `POST`, `GET` (paginado; `?status=active` devolve lista sem paginação) | JWT | `controller/FlowController` → `service/FlowDocumentService` |
| `/manager/flows/{flowId}/versions` (`?status=active\|inactive`) | JWT | mesmo controller |
| `/manager/flows/{flowId}/versions/{version}`: `GET`, `PUT` (201 + `Location`), `DELETE` (204) | JWT | mesmo controller |
| `GET /manager/flows/{flowId}/versions/{version}/yaml` e `/examples` | JWT | mesmo controller (`getYaml`, `getExample`) |
| `/manager/contracts`, `/manager/integrations`, `/manager/validations` (mesmo desenho, versão inteira) | JWT | `controller/ContractController`, `IntegrationController`, `ValidationController` |

Erros: `FlowNotFoundException` (404, `FLOW_NOT_FOUND`), `InvalidFlowDefinitionException` (400, `INVALID_FLOW`), `FlowAlreadyExistsException` (409, `FLOW_ALREADY_EXISTS`). Contrato, integração e validação têm trios equivalentes. Tudo em `exception/GlobalExceptionHandler`.

## Fluxo de um PUT em workflow

1. `FlowController.update` chama `FlowDocumentService.update`.
2. `YamlValidationService.extractMetadata` valida o YAML; `flow.id` deve igualar o `flowId` do path. A versão do path não é comparada com a do YAML.
3. Carrega o documento com `findByFlowIdAndVersionWithYaml`.
4. `VersioningService.detectChangeType`: `contract` mudou é MAJOR, `integrations` mudou é MINOR, o resto é PATCH.
5. Grava um documento novo com a versão calculada. O antigo permanece. Se a versão nova já existir, 409.
6. `OrchestratorCacheClient.evictWorkflow` invalida a versão antiga no orquestrador. Falha só gera log.

Contrato, integração e validação usam versão inteira: `update` grava `version + 1` via `SequentialVersioningService`.

## Mapa

| Caminho | Papel |
|---|---|
| `ManagerApplication` | Entry point |
| `controller/*` | REST e anotações OpenAPI |
| `service/FlowDocumentService` | CRUD de workflow, exemplo de payload, soft-delete |
| `service/{Contract,Integration,Validation}Service` | CRUD versionado dos três recursos |
| `service/DuplicateKeySupport` | `mapDuplicateKey`: `DuplicateKeyException` do `save` vira a exceção `*AlreadyExistsException` (409) |
| `service/YamlValidationService` | Metadados do YAML e campos do contrato |
| `service/VersioningService`, `service/SequentialVersioningService` | SemVer do workflow; versão inteira dos demais |
| `repository/*` | Interfaces Mongo. `FlowDocumentRepository` usa projeção sem `yamlContent` |
| `domain/*` | `@Document` das 4 collections e `ResourceRef` |
| `dto/*` | DTOs de request e response (`Dtos.kt`) |
| `security/*` | `JwtService`, `JwtAuthenticationFilter`, `SecurityConfig`, `AuthController` |
| `client/*`, `config/OrchestratorClientConfig` | Login e invalidação de cache no orquestrador, bean `orchestratorRestClient` |
| `config/OpenApiConfig`, `exception/*` | Swagger e tratamento de erros |

Recursos: `src/main/resources/application.yml`, `application-docker.yml`, `mongodb-manager/init-mongo.js`, `docker-compose.yml`.

## Quero mudar... onde

| Mudança | Onde |
|---|---|
| Validação leve do YAML | `service/YamlValidationService.extractMetadata` |
| Regra de bump de versão | `VersioningService.detectChangeType` e `calculateNextVersion` |
| Exemplo de payload do contrato | `FlowDocumentService.getExample` (mapa tipo para valor de exemplo) |
| Nova rota ou filtro de workflow | `controller/FlowController` e `repository/FlowDocumentRepository` |
| Campo novo em workflow | `domain/FlowDocument`, `FlowSummaryDto` em `dto/Dtos.kt`, `FlowDocumentService.create` e `update` |
| Campo novo em integração | `domain/IntegrationDocument`, `IntegrationRequest` e `IntegrationDto`, `IntegrationService.create` e `update` |
| Novo erro HTTP | classe em `exception/Exceptions.kt` e `@ExceptionHandler` em `GlobalExceptionHandler` |
| Rota pública ou 401/403 | `security/SecurityConfig` |
| Credenciais ou expiração do JWT | `manager.security.*` no `application.yml`, `security/JwtService`, `AuthController` |
| Invalidação de cache do orquestrador | `client/OrchestratorCacheClient`, `client/OrchestratorAuthService` |
| Índice ou dados de exemplo no Mongo | `mongodb-manager/init-mongo.js` |
| Novo tipo de recurso versionado | `domain`, `repository`, `service`, `controller` por analogia com `Contract*` e exceções novas |

## Integrações e config

- MongoDB: `MONGODB_URI`, `MONGODB_DATABASE`. Database `service-portal-manager`.
- Orquestrador: `ORCHESTRATOR_URL` (`http://localhost:8080`), `ORCHESTRATOR_ADMIN_USERNAME`, `ORCHESTRATOR_ADMIN_PASSWORD`. Rotas usadas: `POST /api/auth/tokens` e `DELETE /api/admin/cache/workflows/{flowId}/versions/{version}`. O token fica em memória e renova 60 s antes de expirar; a expiração vem do claim `exp` do JWT (sem verificar assinatura), senão `expiresIn` do login, senão 3600 s. Em 401 o `OrchestratorCacheClient` invalida o token (só se ainda for o que falhou) e repete a chamada uma única vez.
- Timeouts do `RestClient` do orquestrador: `orchestrator.connect-timeout-ms` (`ORCHESTRATOR_CONNECT_TIMEOUT_MS`, 2000) e `orchestrator.read-timeout-ms` (`ORCHESTRATOR_READ_TIMEOUT_MS`, 5000).
- JWT: `JWT_SECRET` (trocar em produção), `JWT_EXPIRATION` (3600), `MANAGER_ADMIN_USERNAME` e `MANAGER_ADMIN_PASSWORD` (`admin`).
- Demais variáveis (SBA, OTLP, actuator): `src/main/resources/application.yml`. Stack completa: `../docker-compose-service-portal.yml`.

## Comandos

```bash
docker compose up -d && ./gradlew bootRun   # só o MongoDB (compose local) e o app
./gradlew test                              # unitários; relatório JaCoCo ao final
./gradlew jacocoTestCoverageVerification    # gate >= 95% de instruções
./gradlew spotlessApply                     # antes de commit
```

## Armadilhas

- Não atualizar Kotlin 2.0.21, Java 21 nem Spring Boot 3.4.5.
- Consultas leves de workflow excluem `yamlContent` por projeção. Para `save()` use sempre `findByFlowIdAndVersionWithYaml`, senão o save apaga o YAML.
- O Spring Boot 3 não cria índices sozinho e não há `@CompoundIndexes`. O índice único `(id, version)` vem de `mongodb-manager/init-mongo.js`, que só roda com volume vazio (`docker compose down -v` para reaplicar).
- `DELETE` é soft-delete (`active=false`) e idempotente. Recriar o mesmo `flowId` e `version` dá 409. `GET /manager/flows` lista só ativos.
- Duas requisições concorrentes passam do `existsBy...` e o índice único `(id, version)` rejeita a segunda com `DuplicateKeyException`. Os 4 services convertem isso em `*AlreadyExistsException` (409, mesmo corpo) via `mapDuplicateKey`, sem vazar a mensagem do Mongo. Todo `save` de versão nova deve usá-lo.
- O `PUT` de workflow cria versão nova em vez de alterar a existente.
- O `ObjectMapper` de `YamlValidationService` precisa de `registerKotlinModule()`. Sem ele a conversão para `ContractField` falha em silêncio e `getExample` devolve exemplo vazio.
- A invalidação de cache é best-effort e síncrona dentro do PUT/DELETE: engole exceções (se o orquestrador estiver fora, o cache dele fica velho), mas falha rápido pelos timeouts de conexão e leitura.
- Todo `.kt` precisa do header de `license-header.txt`, senão o Spotless falha. O arquivo é uma cópia do `license-header.txt` da raiz do monorepo: mantenha igual.
- O gate JaCoCo exclui `ManagerApplication`, `OpenApiConfig`, `dto/**`, `exception/**` e `domain/**`. O resto precisa de teste.
