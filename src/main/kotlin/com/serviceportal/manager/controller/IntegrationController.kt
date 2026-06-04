package com.serviceportal.manager.controller

import com.serviceportal.manager.dto.IntegrationDto
import com.serviceportal.manager.dto.IntegrationRequest
import com.serviceportal.manager.service.IntegrationService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
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
 * Endpoints REST para gerenciamento de integrations.
 *
 * Acesso (aplicado pelo BFF via @PreAuthorize ao proxy):
 *   - ADMIN (it)      : CRUD completo
 *   - WORKFLOWS (workop): leitura apenas
 *   - RULES (bizop)   : sem acesso
 */
@RestController
@RequestMapping("/manager/integrations")
class IntegrationController(private val service: IntegrationService) {

    @PostMapping
    fun create(@RequestBody request: IntegrationRequest): ResponseEntity<IntegrationDto> =
        ResponseEntity.status(HttpStatus.CREATED).body(service.create(request))

    @GetMapping
    fun list(
        @PageableDefault(size = 20, sort = ["integrationId", "version"], direction = Sort.Direction.ASC)
        pageable: Pageable,
        @RequestParam(required = false) status: String?
    ): ResponseEntity<Any> {
        if (status == "active") return ResponseEntity.ok<Any>(service.listActive())
        return ResponseEntity.ok<Any>(service.listAll(pageable))
    }

    @GetMapping("/{integrationId}/versions")
    fun listVersions(
        @PathVariable integrationId: String,
        @RequestParam(required = false) status: String?
    ): ResponseEntity<List<IntegrationDto>> =
        ResponseEntity.ok(service.listVersions(integrationId, status))

    @GetMapping("/{integrationId}/versions/{version}")
    fun get(
        @PathVariable integrationId: String,
        @PathVariable version: Int
    ): ResponseEntity<IntegrationDto> = ResponseEntity.ok(service.get(integrationId, version))

    @PutMapping("/{integrationId}/versions/{version}")
    fun update(
        @PathVariable integrationId: String,
        @PathVariable version: Int,
        @RequestBody request: IntegrationRequest
    ): ResponseEntity<IntegrationDto> {
        val result = service.update(integrationId, version, request)
        val location = URI.create("/manager/integrations/$integrationId/versions/${result.version}")
        return ResponseEntity.created(location).body(result)
    }

    @DeleteMapping("/{integrationId}/versions/{version}")
    fun delete(
        @PathVariable integrationId: String,
        @PathVariable version: Int
    ): ResponseEntity<Void> {
        service.deactivate(integrationId, version)
        return ResponseEntity.noContent().build()
    }
}
