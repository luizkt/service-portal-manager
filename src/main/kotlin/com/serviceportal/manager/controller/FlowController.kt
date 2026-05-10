package com.serviceportal.manager.controller

import com.serviceportal.manager.dto.FlowSummaryDto
import com.serviceportal.manager.service.FlowDocumentService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
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
import org.springframework.web.bind.annotation.RestController

/**
 * CRUD dos fluxos. O Manager é a única aplicação com permissão de escrita
 * na collection `workflows` após a migração.
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
     * Listagem paginada — query params: `page`, `size`, `sort` (ex.: `?sort=flowId,asc`).
     * Default: 20 por página ordenado por `flowId` asc, `versao` asc. Resposta sem `yamlContent`.
     */
    @GetMapping
    fun listAll(
        @PageableDefault(
            size = 20,
            sort = ["flowId", "versao"],
            direction = Sort.Direction.ASC
        ) pageable: Pageable
    ): ResponseEntity<Page<FlowSummaryDto>> = ResponseEntity.ok(service.listAll(pageable))

    @GetMapping("/{flowId}/{versao}")
    fun get(
        @PathVariable flowId: String,
        @PathVariable versao: String
    ): ResponseEntity<FlowSummaryDto> = ResponseEntity.ok(service.get(flowId, versao))

    @PutMapping("/{flowId}/{versao}", consumes = [
        MediaType.TEXT_PLAIN_VALUE,
        "application/x-yaml",
        "text/yaml",
        MediaType.APPLICATION_JSON_VALUE
    ])
    fun update(
        @PathVariable flowId: String,
        @PathVariable versao: String,
        @RequestBody yaml: String
    ): ResponseEntity<FlowSummaryDto> = ResponseEntity.ok(service.update(flowId, versao, yaml))

    @DeleteMapping("/{flowId}/{versao}")
    fun delete(
        @PathVariable flowId: String,
        @PathVariable versao: String
    ): ResponseEntity<Void> {
        service.deactivate(flowId, versao)
        return ResponseEntity.noContent().build()
    }
}
