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
package com.serviceportal.manager.service

import com.serviceportal.manager.domain.ContractDocument
import com.serviceportal.manager.dto.ContractDto
import com.serviceportal.manager.dto.ContractRequest
import com.serviceportal.manager.exception.ContractAlreadyExistsException
import com.serviceportal.manager.exception.ContractNotFoundException
import com.serviceportal.manager.exception.InvalidContractException
import com.serviceportal.manager.repository.ContractDocumentRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ContractService(
    private val repository: ContractDocumentRepository,
    private val versioning: SequentialVersioningService,
) {
    fun create(request: ContractRequest): ContractDto {
        if (repository.existsByContractIdAndVersion(request.contractId, 1)) {
            throw ContractAlreadyExistsException(
                "Contract '${request.contractId}' version 1 already exists",
            )
        }
        val now = LocalDateTime.now()
        val saved =
            repository.save(
                ContractDocument(
                    contractId = request.contractId,
                    version = 1,
                    fields = request.fields,
                    active = true,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        return saved.toDto()
    }

    /**
     * Atualiza um contract criando nova versão sequencial.
     * A versão atual permanece ativa — workflows que a referenciam não são afetados.
     */
    fun update(
        contractId: String,
        version: Int,
        request: ContractRequest,
    ): ContractDto {
        if (request.contractId != contractId) {
            throw InvalidContractException(
                "Path contractId ($contractId) does not match body contractId (${request.contractId})",
            )
        }
        repository.findByContractIdAndVersion(contractId, version)
            ?: throw ContractNotFoundException("Contract '$contractId' version $version not found")

        val newVersion = versioning.nextVersion(version)
        if (repository.existsByContractIdAndVersion(contractId, newVersion)) {
            throw ContractAlreadyExistsException(
                "Contract '$contractId' version $newVersion already exists",
            )
        }
        val now = LocalDateTime.now()
        val newDoc =
            repository.save(
                ContractDocument(
                    contractId = contractId,
                    version = newVersion,
                    fields = request.fields,
                    active = true,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        return newDoc.toDto()
    }

    fun get(
        contractId: String,
        version: Int,
    ): ContractDto =
        (
            repository.findByContractIdAndVersion(contractId, version)
                ?: throw ContractNotFoundException("Contract '$contractId' version $version not found")
        ).toDto()

    fun listAll(pageable: Pageable): Page<ContractDto> = repository.findAllByActiveTrue(pageable).map { it.toDto() }

    fun listActive(): List<ContractDto> = repository.findAllByActiveTrue().map { it.toDto() }

    fun listVersions(
        contractId: String,
        status: String?,
    ): List<ContractDto> {
        val sort = Sort.by(Sort.Direction.ASC, "version")
        val docs =
            when (status) {
                "active" -> repository.findAllByContractIdAndActive(contractId, true, sort)
                "inactive" -> repository.findAllByContractIdAndActive(contractId, false, sort)
                else -> repository.findAllByContractId(contractId, sort)
            }
        return docs.map { it.toDto() }
    }

    fun deactivate(
        contractId: String,
        version: Int,
    ) {
        val doc =
            repository.findByContractIdAndVersion(contractId, version)
                ?: throw ContractNotFoundException("Contract '$contractId' version $version not found")
        if (!doc.active) return
        doc.active = false
        doc.updatedAt = LocalDateTime.now()
        repository.save(doc)
    }

    private fun ContractDocument.toDto() =
        ContractDto(
            contractId = contractId,
            version = version,
            fields = fields,
            active = active,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
