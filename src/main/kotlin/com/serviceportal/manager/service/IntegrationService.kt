package com.serviceportal.manager.service

import com.serviceportal.manager.domain.IntegrationDocument
import com.serviceportal.manager.dto.IntegrationDto
import com.serviceportal.manager.dto.IntegrationRequest
import com.serviceportal.manager.exception.IntegrationAlreadyExistsException
import com.serviceportal.manager.exception.IntegrationNotFoundException
import com.serviceportal.manager.exception.InvalidIntegrationException
import com.serviceportal.manager.repository.IntegrationDocumentRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class IntegrationService(
    private val repository: IntegrationDocumentRepository,
    private val versioning: SequentialVersioningService
) {

    fun create(request: IntegrationRequest): IntegrationDto {
        if (repository.existsByIntegrationIdAndVersion(request.integrationId, 1)) {
            throw IntegrationAlreadyExistsException(
                "Integration '${request.integrationId}' version 1 already exists"
            )
        }
        val now = LocalDateTime.now()
        val saved = repository.save(
            IntegrationDocument(
                integrationId = request.integrationId,
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
                updatedAt = now
            )
        )
        return saved.toDto()
    }

    /**
     * Atualiza uma integration criando nova versão sequencial.
     * A versão atual permanece ativa — recursos que a referenciam não são afetados.
     */
    fun update(integrationId: String, version: Int, request: IntegrationRequest): IntegrationDto {
        if (request.integrationId != integrationId) {
            throw InvalidIntegrationException(
                "Path integrationId ($integrationId) does not match body integrationId (${request.integrationId})"
            )
        }
        repository.findByIntegrationIdAndVersion(integrationId, version)
            ?: throw IntegrationNotFoundException("Integration '$integrationId' version $version not found")

        val newVersion = versioning.nextVersion(version)
        if (repository.existsByIntegrationIdAndVersion(integrationId, newVersion)) {
            throw IntegrationAlreadyExistsException(
                "Integration '$integrationId' version $newVersion already exists"
            )
        }
        val now = LocalDateTime.now()
        val newDoc = repository.save(
            IntegrationDocument(
                integrationId = integrationId,
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
                updatedAt = now
            )
        )
        return newDoc.toDto()
    }

    fun get(integrationId: String, version: Int): IntegrationDto =
        (repository.findByIntegrationIdAndVersion(integrationId, version)
            ?: throw IntegrationNotFoundException("Integration '$integrationId' version $version not found")
        ).toDto()

    fun listAll(pageable: Pageable): Page<IntegrationDto> =
        repository.findAllByActiveTrue(pageable).map { it.toDto() }

    fun listActive(): List<IntegrationDto> =
        repository.findAllByActiveTrue().map { it.toDto() }

    fun listVersions(integrationId: String, status: String?): List<IntegrationDto> {
        val sort = Sort.by(Sort.Direction.ASC, "version")
        val docs = when (status) {
            "active"   -> repository.findAllByIntegrationIdAndActive(integrationId, true, sort)
            "inactive" -> repository.findAllByIntegrationIdAndActive(integrationId, false, sort)
            else       -> repository.findAllByIntegrationId(integrationId, sort)
        }
        return docs.map { it.toDto() }
    }

    fun deactivate(integrationId: String, version: Int) {
        val doc = repository.findByIntegrationIdAndVersion(integrationId, version)
            ?: throw IntegrationNotFoundException("Integration '$integrationId' version $version not found")
        if (!doc.active) return
        doc.active = false
        doc.updatedAt = LocalDateTime.now()
        repository.save(doc)
    }

    private fun IntegrationDocument.toDto() = IntegrationDto(
        integrationId = integrationId,
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
        updatedAt = updatedAt
    )
}
