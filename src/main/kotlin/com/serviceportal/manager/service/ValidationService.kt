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

import com.serviceportal.manager.domain.ValidationDocument
import com.serviceportal.manager.dto.ValidationDto
import com.serviceportal.manager.dto.ValidationRequest
import com.serviceportal.manager.exception.InvalidValidationException
import com.serviceportal.manager.exception.ValidationAlreadyExistsException
import com.serviceportal.manager.exception.ValidationNotFoundException
import com.serviceportal.manager.repository.ValidationDocumentRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ValidationService(
    private val repository: ValidationDocumentRepository,
    private val versioning: SequentialVersioningService,
) {
    fun create(request: ValidationRequest): ValidationDto {
        if (repository.existsByValidationIdAndVersion(request.validationId, 1)) {
            throw ValidationAlreadyExistsException(
                "Validation '${request.validationId}' version 1 already exists",
            )
        }
        val now = LocalDateTime.now()
        val saved =
            repository.save(
                ValidationDocument(
                    validationId = request.validationId,
                    version = 1,
                    type = request.type,
                    url = request.url,
                    method = request.method,
                    headers = request.headers,
                    timeout = request.timeout,
                    bodyTemplate = request.bodyTemplate,
                    responseBody = request.responseBody,
                    active = true,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        return saved.toDto()
    }

    /**
     * Atualiza uma validation criando nova versão sequencial.
     * A versão atual permanece ativa — workflows que a referenciam não são afetados.
     */
    fun update(
        validationId: String,
        version: Int,
        request: ValidationRequest,
    ): ValidationDto {
        if (request.validationId != validationId) {
            throw InvalidValidationException(
                "Path validationId ($validationId) does not match body validationId (${request.validationId})",
            )
        }
        repository.findByValidationIdAndVersion(validationId, version)
            ?: throw ValidationNotFoundException("Validation '$validationId' version $version not found")

        val newVersion = versioning.nextVersion(version)
        if (repository.existsByValidationIdAndVersion(validationId, newVersion)) {
            throw ValidationAlreadyExistsException(
                "Validation '$validationId' version $newVersion already exists",
            )
        }
        val now = LocalDateTime.now()
        val newDoc =
            repository.save(
                ValidationDocument(
                    validationId = validationId,
                    version = newVersion,
                    type = request.type,
                    url = request.url,
                    method = request.method,
                    headers = request.headers,
                    timeout = request.timeout,
                    bodyTemplate = request.bodyTemplate,
                    responseBody = request.responseBody,
                    active = true,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        return newDoc.toDto()
    }

    fun get(
        validationId: String,
        version: Int,
    ): ValidationDto =
        (
            repository.findByValidationIdAndVersion(validationId, version)
                ?: throw ValidationNotFoundException("Validation '$validationId' version $version not found")
        ).toDto()

    fun listAll(pageable: Pageable): Page<ValidationDto> = repository.findAllByActiveTrue(pageable).map { it.toDto() }

    fun listActive(): List<ValidationDto> = repository.findAllByActiveTrue().map { it.toDto() }

    fun listVersions(
        validationId: String,
        status: String?,
    ): List<ValidationDto> {
        val sort = Sort.by(Sort.Direction.ASC, "version")
        val docs =
            when (status) {
                "active" -> repository.findAllByValidationIdAndActive(validationId, true, sort)
                "inactive" -> repository.findAllByValidationIdAndActive(validationId, false, sort)
                else -> repository.findAllByValidationId(validationId, sort)
            }
        return docs.map { it.toDto() }
    }

    fun deactivate(
        validationId: String,
        version: Int,
    ) {
        val doc =
            repository.findByValidationIdAndVersion(validationId, version)
                ?: throw ValidationNotFoundException("Validation '$validationId' version $version not found")
        if (!doc.active) return
        doc.active = false
        doc.updatedAt = LocalDateTime.now()
        repository.save(doc)
    }

    private fun ValidationDocument.toDto() =
        ValidationDto(
            validationId = validationId,
            version = version,
            type = type,
            url = url,
            method = method,
            headers = headers,
            timeout = timeout,
            bodyTemplate = bodyTemplate,
            responseBody = responseBody,
            active = active,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
