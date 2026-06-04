package com.serviceportal.manager.controller

import com.serviceportal.manager.dto.ContractDto
import com.serviceportal.manager.dto.ContractRequest
import com.serviceportal.manager.service.ContractService
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
 * Endpoints REST para gerenciamento de contracts.
 *
 * Acesso (aplicado pelo BFF via @PreAuthorize ao proxy):
 *   - ADMIN (it)      : CRUD completo
 *   - WORKFLOWS (workop): CRUD completo
 *   - RULES (bizop)   : leitura apenas
 */
@RestController
@RequestMapping("/manager/contracts")
class ContractController(private val service: ContractService) {

    @PostMapping
    fun create(@RequestBody request: ContractRequest): ResponseEntity<ContractDto> =
        ResponseEntity.status(HttpStatus.CREATED).body(service.create(request))

    @GetMapping
    fun list(
        @PageableDefault(size = 20, sort = ["contractId", "version"], direction = Sort.Direction.ASC)
        pageable: Pageable,
        @RequestParam(required = false) status: String?
    ): ResponseEntity<Any> {
        if (status == "active") return ResponseEntity.ok<Any>(service.listActive())
        return ResponseEntity.ok<Any>(service.listAll(pageable))
    }

    @GetMapping("/{contractId}/versions")
    fun listVersions(
        @PathVariable contractId: String,
        @RequestParam(required = false) status: String?
    ): ResponseEntity<List<ContractDto>> =
        ResponseEntity.ok(service.listVersions(contractId, status))

    @GetMapping("/{contractId}/versions/{version}")
    fun get(
        @PathVariable contractId: String,
        @PathVariable version: Int
    ): ResponseEntity<ContractDto> = ResponseEntity.ok(service.get(contractId, version))

    @PutMapping("/{contractId}/versions/{version}")
    fun update(
        @PathVariable contractId: String,
        @PathVariable version: Int,
        @RequestBody request: ContractRequest
    ): ResponseEntity<ContractDto> {
        val result = service.update(contractId, version, request)
        val location = URI.create("/manager/contracts/$contractId/versions/${result.version}")
        return ResponseEntity.created(location).body(result)
    }

    @DeleteMapping("/{contractId}/versions/{version}")
    fun delete(
        @PathVariable contractId: String,
        @PathVariable version: Int
    ): ResponseEntity<Void> {
        service.deactivate(contractId, version)
        return ResponseEntity.noContent().build()
    }
}
