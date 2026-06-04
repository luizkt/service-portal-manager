package com.serviceportal.manager.controller

import com.serviceportal.manager.dto.ValidationDto
import com.serviceportal.manager.dto.ValidationRequest
import com.serviceportal.manager.service.ValidationService
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
 * Endpoints REST para gerenciamento de validations.
 *
 * Acesso (aplicado pelo BFF via @PreAuthorize ao proxy):
 *   - ADMIN (it)      : CRUD completo
 *   - RULES (bizop)   : CRUD completo
 *   - WORKFLOWS (workop): sem acesso
 */
@RestController
@RequestMapping("/manager/validations")
class ValidationController(private val service: ValidationService) {

    @PostMapping
    fun create(@RequestBody request: ValidationRequest): ResponseEntity<ValidationDto> =
        ResponseEntity.status(HttpStatus.CREATED).body(service.create(request))

    @GetMapping
    fun list(
        @PageableDefault(size = 20, sort = ["validationId", "version"], direction = Sort.Direction.ASC)
        pageable: Pageable,
        @RequestParam(required = false) status: String?
    ): ResponseEntity<Any> {
        if (status == "active") return ResponseEntity.ok<Any>(service.listActive())
        return ResponseEntity.ok<Any>(service.listAll(pageable))
    }

    @GetMapping("/{validationId}/versions")
    fun listVersions(
        @PathVariable validationId: String,
        @RequestParam(required = false) status: String?
    ): ResponseEntity<List<ValidationDto>> =
        ResponseEntity.ok(service.listVersions(validationId, status))

    @GetMapping("/{validationId}/versions/{version}")
    fun get(
        @PathVariable validationId: String,
        @PathVariable version: Int
    ): ResponseEntity<ValidationDto> = ResponseEntity.ok(service.get(validationId, version))

    @PutMapping("/{validationId}/versions/{version}")
    fun update(
        @PathVariable validationId: String,
        @PathVariable version: Int,
        @RequestBody request: ValidationRequest
    ): ResponseEntity<ValidationDto> {
        val result = service.update(validationId, version, request)
        val location = URI.create("/manager/validations/$validationId/versions/${result.version}")
        return ResponseEntity.created(location).body(result)
    }

    @DeleteMapping("/{validationId}/versions/{version}")
    fun delete(
        @PathVariable validationId: String,
        @PathVariable version: Int
    ): ResponseEntity<Void> {
        service.deactivate(validationId, version)
        return ResponseEntity.noContent().build()
    }
}
