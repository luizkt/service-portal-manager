package com.serviceportal.manager.controller

import com.serviceportal.manager.dto.FlowSummaryDto
import com.serviceportal.manager.service.FlowDocumentService
import org.springframework.data.domain.Page
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
 */
@RestController
@RequestMapping("/manager/flows")
class FlowController(private val service: FlowDocumentService) {

    @PostMapping(consumes = [
        MediaType.TEXT_PLAIN_VALUE,
        "application/x-yaml",
        "text/yaml",
        MediaType.APPLICATION_JSON_VALUE
    ])
    fun create(@RequestBody yaml: String): ResponseEntity<FlowSummaryDto> =
        ResponseEntity.status(HttpStatus.CREATED).body(service.create(yaml))

    /**
     * Lista paginada de fluxos. Aceita `status=active` para filtrar.
     *
     * Query params padrão (Spring Data): `page`, `size`, `sort`.
     */
    @GetMapping
    fun list(
        @PageableDefault(
            size = 20,
            sort = ["flowId", "version"],
            direction = Sort.Direction.ASC
        ) pageable: Pageable,
        @RequestParam(required = false) status: String?
    ): ResponseEntity<Any> {
        // Quando `status=active` é solicitado, devolvemos a lista de ativos
        // (sem paginação — o orquestrador consome assim no warm-up).
        if (status == "active") {
            return ResponseEntity.ok<Any>(service.listActive())
        }
        return ResponseEntity.ok<Any>(service.listAll(pageable))
    }

    @GetMapping("/{flowId}/versions/{version}")
    fun get(
        @PathVariable flowId: String,
        @PathVariable version: String
    ): ResponseEntity<FlowSummaryDto> = ResponseEntity.ok(service.get(flowId, version))

    /** YAML cru — sub-recurso da versão. Substitui o antigo `/manager/workflows/{id}/{ver}/yaml`. */
    @GetMapping("/{flowId}/versions/{version}/yaml", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getYaml(
        @PathVariable flowId: String,
        @PathVariable version: String
    ): ResponseEntity<String> {
        val yaml = service.getYaml(flowId, version)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, "application/x-yaml")
            .body(yaml)
    }

    @PutMapping("/{flowId}/versions/{version}", consumes = [
        MediaType.TEXT_PLAIN_VALUE,
        "application/x-yaml",
        "text/yaml",
        MediaType.APPLICATION_JSON_VALUE
    ])
    fun update(
        @PathVariable flowId: String,
        @PathVariable version: String,
        @RequestBody yaml: String
    ): ResponseEntity<FlowSummaryDto> = ResponseEntity.ok(service.update(flowId, version, yaml))

    @DeleteMapping("/{flowId}/versions/{version}")
    fun delete(
        @PathVariable flowId: String,
        @PathVariable version: String
    ): ResponseEntity<Void> {
        service.deactivate(flowId, version)
        return ResponseEntity.noContent().build()
    }
}
