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

import com.serviceportal.manager.dto.ContractDto
import com.serviceportal.manager.dto.ContractRequest
import com.serviceportal.manager.service.ContractService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
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
@io.swagger.v3.oas.annotations.tags.Tag(name = "Contracts")
@RestController
@RequestMapping("/manager/contracts")
class ContractController(private val service: ContractService) {
    @Operation(summary = "Criar contrato")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Criado"),
            ApiResponse(responseCode = "409", description = "Já existe"),
        ],
    )
    @PostMapping
    fun create(
        @RequestBody request: ContractRequest,
    ): ResponseEntity<ContractDto> = ResponseEntity.status(HttpStatus.CREATED).body(service.create(request))

    @Operation(summary = "Listar contratos (paginado)")
    @ApiResponse(responseCode = "200", description = "Página de contratos ou lista de ativos")
    @GetMapping
    fun list(
        @PageableDefault(size = 20, sort = ["contractId", "version"], direction = Sort.Direction.ASC)
        pageable: Pageable,
        @Parameter(description = "Use 'active' para listar todos os ativos") @RequestParam(required = false) status: String?,
    ): ResponseEntity<Any> {
        if (status == "active") return ResponseEntity.ok<Any>(service.listActive())
        return ResponseEntity.ok<Any>(service.listAll(pageable))
    }

    @Operation(summary = "Histórico de versões de um contrato")
    @ApiResponse(responseCode = "200", description = "Lista de versões")
    @GetMapping("/{contractId}/versions")
    fun listVersions(
        @Parameter(description = "ID do contrato", example = "create-order") @PathVariable contractId: String,
        @Parameter(description = "Filtro: active | inactive") @RequestParam(required = false) status: String?,
    ): ResponseEntity<List<ContractDto>> = ResponseEntity.ok(service.listVersions(contractId, status))

    @Operation(summary = "Buscar contrato por versão")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Contrato encontrado"),
            ApiResponse(responseCode = "404", description = "Não encontrado"),
        ],
    )
    @GetMapping("/{contractId}/versions/{version}")
    fun get(
        @Parameter(description = "ID do contrato", example = "create-order") @PathVariable contractId: String,
        @Parameter(description = "Número da versão", example = "1") @PathVariable version: Int,
    ): ResponseEntity<ContractDto> = ResponseEntity.ok(service.get(contractId, version))

    @Operation(summary = "Atualizar contrato (nova versão)")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Nova versão criada"),
            ApiResponse(responseCode = "404", description = "Não encontrado"),
        ],
    )
    @PutMapping("/{contractId}/versions/{version}")
    fun update(
        @Parameter(description = "ID do contrato", example = "create-order") @PathVariable contractId: String,
        @Parameter(description = "Número da versão atual", example = "1") @PathVariable version: Int,
        @RequestBody request: ContractRequest,
    ): ResponseEntity<ContractDto> {
        val result = service.update(contractId, version, request)
        val location = URI.create("/manager/contracts/$contractId/versions/${result.version}")
        return ResponseEntity.created(location).body(result)
    }

    @Operation(summary = "Desativar contrato")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Desativado"),
            ApiResponse(responseCode = "404", description = "Não encontrado"),
        ],
    )
    @DeleteMapping("/{contractId}/versions/{version}")
    fun delete(
        @Parameter(description = "ID do contrato", example = "create-order") @PathVariable contractId: String,
        @Parameter(description = "Número da versão", example = "1") @PathVariable version: Int,
    ): ResponseEntity<Void> {
        service.deactivate(contractId, version)
        return ResponseEntity.noContent().build()
    }
}
