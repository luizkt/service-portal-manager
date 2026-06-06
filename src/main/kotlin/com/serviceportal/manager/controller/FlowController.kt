/*
 * MIT License
 *
 * Copyright (c) 2026 Luiz
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.serviceportal.manager.controller

import com.serviceportal.manager.dto.FlowExampleDto
import com.serviceportal.manager.dto.FlowSummaryDto
import com.serviceportal.manager.service.FlowDocumentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI

/**
 * Endpoints REST para gerenciamento de workflows.
 *
 * Recursos:
 *   - `/manager/flows`                                (coleção de fluxos)
 *   - `/manager/flows/{flowId}/versions`              (coleção de versões de um fluxo)
 *   - `/manager/flows/{flowId}/versions/{version}`    (versão específica)
 *   - `/manager/flows/{flowId}/versions/{version}/yaml` (YAML cru — consumido pelo orquestrador)
 *
 * Filtros como query params (não no path):
 *   - `GET /manager/flows?status=active`              (substitui o antigo `/workflows/active`)
 *   - `GET /manager/flows/{flowId}/versions?status=active|inactive` (histórico de versões)
 */
@io.swagger.v3.oas.annotations.tags.Tag(name = "Flows")
@RestController
@RequestMapping("/manager/flows")
class FlowController(private val service: FlowDocumentService) {
    @Operation(summary = "Criar flow via YAML", description = "Versão inicial sempre 1.0.0.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Flow criado",
                content = [Content(examples = [ExampleObject(
                    value = """{"flowId":"create-order-v1","version":"1.0.0","active":true}""",
                )])]),
            ApiResponse(responseCode = "409", description = "Flow já existe"),
        ],
    )
    @PostMapping(
        consumes = [
            MediaType.TEXT_PLAIN_VALUE,
            "application/x-yaml",
            "text/yaml",
            MediaType.APPLICATION_JSON_VALUE,
        ],
    )
    fun create(
        @RequestBody yaml: String,
    ): ResponseEntity<FlowSummaryDto> = ResponseEntity.status(HttpStatus.CREATED).body(service.create(yaml))

    /**
     * Lista paginada de fluxos. Aceita `status=active` para filtrar.
     *
     * Query params padrão (Spring Data): `page`, `size`, `sort`.
     */
    @Operation(summary = "Listar flows (paginado)", description = "Use status=active para retornar todos os ativos sem paginação.")
    @ApiResponse(responseCode = "200", description = "Página de flows ou lista de ativos")
    @GetMapping
    fun list(
        @PageableDefault(
            size = 20,
            sort = ["flowId", "version"],
            direction = Sort.Direction.ASC,
        ) pageable: Pageable,
        @Parameter(description = "Use 'active' para listar sem paginação") @RequestParam(required = false) status: String?,
    ): ResponseEntity<Any> {
        // Quando `status=active` é solicitado, devolvemos a lista de ativos
        // (sem paginação — o orquestrador consome assim no warm-up).
        if (status == "active") {
            return ResponseEntity.ok<Any>(service.listActive())
        }
        return ResponseEntity.ok<Any>(service.listAll(pageable))
    }

    /**
     * Lista todas as versões de um flow específico.
     * - `?status=active`   → somente versões ativas
     * - `?status=inactive` → somente versões desativadas (histórico)
     * - sem parâmetro      → todas as versões
     */
    @Operation(summary = "Histórico de versões de um flow")
    @ApiResponse(responseCode = "200", description = "Lista de versões",
        content = [Content(examples = [ExampleObject(
            value = """[{"flowId":"create-order-v1","version":"1.0.0","active":false},{"flowId":"create-order-v1","version":"1.1.0"}]""",
        )])])
    @GetMapping("/{flowId}/versions")
    fun listVersions(
        @Parameter(description = "ID do flow", example = "create-order-v1") @PathVariable flowId: String,
        @Parameter(description = "Filtro: active | inactive") @RequestParam(required = false) status: String?,
    ): ResponseEntity<List<FlowSummaryDto>> = ResponseEntity.ok(service.listVersions(flowId, status))

    @Operation(summary = "Buscar flow por versão")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Flow encontrado"),
        ApiResponse(responseCode = "404", description = "Não encontrado"),
    ])
    @GetMapping("/{flowId}/versions/{version}")
    fun get(
        @Parameter(description = "ID do flow", example = "create-order-v1") @PathVariable flowId: String,
        @Parameter(description = "Versão SemVer", example = "1.0.0") @PathVariable version: String,
    ): ResponseEntity<FlowSummaryDto> = ResponseEntity.ok(service.get(flowId, version))

    @Operation(summary = "Exemplo de payload para execução", description = "Gera JSON de exemplo a partir dos campos do contrato.")
    @ApiResponse(responseCode = "200", description = "Exemplo gerado",
        content = [Content(examples = [ExampleObject(
            value = """{"example":{"clientId":"exemplo","amount":0},"fields":[{"name":"clientId","type":"STRING","required":true}]}""",
        )])])
    @GetMapping("/{flowId}/versions/{version}/examples")
    fun getExample(
        @Parameter(description = "ID do flow", example = "create-order-v1") @PathVariable flowId: String,
        @Parameter(description = "Versão SemVer", example = "1.0.0") @PathVariable version: String,
    ): ResponseEntity<FlowExampleDto> = ResponseEntity.ok(service.getExample(flowId, version))

    @Operation(summary = "Obter YAML cru do flow")
    @ApiResponse(responseCode = "200", description = "YAML do flow")
    @GetMapping("/{flowId}/versions/{version}/yaml", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getYaml(
        @Parameter(description = "ID do flow", example = "create-order-v1") @PathVariable flowId: String,
        @Parameter(description = "Versão SemVer", example = "1.0.0") @PathVariable version: String,
    ): ResponseEntity<String> {
        val yaml = service.getYaml(flowId, version)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, "application/x-yaml")
            .body(yaml)
    }

    /**
     * Atualiza um workflow gerando nova versão via SemVer:
     * - MAJOR se `contract` mudou
     * - MINOR se `integrations` mudou
     * - PATCH se apenas `description` (ou outro campo menor) mudou
     *
     * Retorna 201 com cabeçalho `Location` apontando para a nova versão criada.
     * A versão antiga é desativada automaticamente.
     */
    @Operation(
        summary = "Atualizar flow (gera nova versão SemVer)",
        description = "MAJOR se contract mudou, MINOR se integrations mudou, PATCH para demais campos. Retorna 201 + Location.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Nova versão criada",
                headers = [Header(name = "Location", description = "URL da nova versão",
                    schema = io.swagger.v3.oas.annotations.media.Schema(example = "/manager/flows/create-order-v1/versions/1.1.0"))]),
            ApiResponse(responseCode = "404", description = "Flow não encontrado"),
        ],
    )
    @PutMapping(
        "/{flowId}/versions/{version}",
        consumes = [
            MediaType.TEXT_PLAIN_VALUE,
            "application/x-yaml",
            "text/yaml",
            MediaType.APPLICATION_JSON_VALUE,
        ],
    )
    fun update(
        @Parameter(description = "ID do flow", example = "create-order-v1") @PathVariable flowId: String,
        @Parameter(description = "Versão SemVer atual", example = "1.0.0") @PathVariable version: String,
        @RequestBody yaml: String,
    ): ResponseEntity<FlowSummaryDto> {
        val result = service.update(flowId, version, yaml)
        val location = URI.create("/manager/flows/$flowId/versions/${result.version}")
        return ResponseEntity.created(location).body(result)
    }

    @Operation(summary = "Desativar flow")
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "Desativado"),
        ApiResponse(responseCode = "404", description = "Não encontrado"),
    ])
    @DeleteMapping("/{flowId}/versions/{version}")
    fun delete(
        @Parameter(description = "ID do flow", example = "create-order-v1") @PathVariable flowId: String,
        @Parameter(description = "Versão SemVer", example = "1.0.0") @PathVariable version: String,
    ): ResponseEntity<Void> {
        service.deactivate(flowId, version)
        return ResponseEntity.noContent().build()
    }
}
