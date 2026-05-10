package com.serviceportal.manager.controller

import com.serviceportal.manager.dto.FlowSummaryDto
import com.serviceportal.manager.service.FlowDocumentService
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Endpoints consumidos pelo orquestrador (após a migração) para descobrir
 * quais workflows existem e recuperar o YAML para execução.
 */
@RestController
@RequestMapping("/manager/workflows")
class WorkflowQueryController(private val service: FlowDocumentService) {

    /** Lista compacta dos fluxos ativos. */
    @GetMapping("/active")
    fun listActive(): ResponseEntity<List<FlowSummaryDto>> =
        ResponseEntity.ok(service.listActive())

    /** YAML cru do fluxo `{id}/{versao}` — para o orquestrador parsear e executar. */
    @GetMapping("/{flowId}/{versao}/yaml", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getYaml(
        @PathVariable flowId: String,
        @PathVariable versao: String
    ): ResponseEntity<String> {
        val yaml = service.getYaml(flowId, versao)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, "application/x-yaml")
            .body(yaml)
    }
}
