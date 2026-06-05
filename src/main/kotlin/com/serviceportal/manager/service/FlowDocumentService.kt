package com.serviceportal.manager.service

import com.serviceportal.manager.client.OrchestratorCacheClient
import com.serviceportal.manager.domain.FlowDocument
import com.serviceportal.manager.dto.FlowSummaryDto
import com.serviceportal.manager.exception.FlowAlreadyExistsException
import com.serviceportal.manager.exception.FlowNotFoundException
import com.serviceportal.manager.exception.InvalidFlowDefinitionException
import com.serviceportal.manager.repository.FlowDocumentRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class FlowDocumentService(
    private val repository: FlowDocumentRepository,
    private val yamlValidator: YamlValidationService,
    private val versioning: VersioningService,
    private val orchestratorCacheClient: OrchestratorCacheClient
) {

    fun create(yamlContent: String): FlowSummaryDto {
        val meta = yamlValidator.extractMetadata(yamlContent)
        if (repository.existsByFlowIdAndVersion(meta.flowId, meta.version)) {
            throw FlowAlreadyExistsException(
                "Flow '${meta.flowId}' version '${meta.version}' already exists"
            )
        }
        val now = LocalDateTime.now()
        val saved = repository.save(
            FlowDocument(
                flowId = meta.flowId,
                version = meta.version,
                description = meta.description,
                active = meta.active,
                yamlContent = yamlContent,
                createdAt = now,
                updatedAt = now
            )
        )
        return saved.toSummary()
    }

    /**
     * Atualiza um workflow aplicando versionamento semântico:
     * - detecta o tipo de mudança (MAJOR/MINOR/PATCH) comparando seções contract/integrations/description
     * - mantém a versão corrente ativa (plano de migração: clientes migram para a nova URL quando prontos)
     * - persiste a nova versão ativa com a versão calculada
     *
     * Retorna o FlowSummaryDto da nova versão criada.
     */
    fun update(flowId: String, version: String, yamlContent: String): FlowSummaryDto {
        val meta = yamlValidator.extractMetadata(yamlContent)
        if (meta.flowId != flowId) {
            throw InvalidFlowDefinitionException(
                "Path flowId ($flowId) does not match YAML flow.id (${meta.flowId})"
            )
        }

        val existing = repository.findByFlowIdAndVersionWithYaml(flowId, version)
            ?: throw FlowNotFoundException("Flow '$flowId' version '$version' not found")

        val oldYaml = existing.yamlContent
            ?: throw FlowNotFoundException(
                "Flow '$flowId' version '$version' has no yamlContent — cannot determine version bump"
            )

        val changeType = versioning.detectChangeType(oldYaml, yamlContent)
        val newVersion = versioning.calculateNextVersion(version, changeType)

        if (repository.existsByFlowIdAndVersion(flowId, newVersion)) {
            throw FlowAlreadyExistsException(
                "Calculated version '$newVersion' for flow '$flowId' already exists"
            )
        }

        val updatedYaml = versioning.updateVersionInYaml(yamlContent, newVersion)
        val now = LocalDateTime.now()
        val newDoc = repository.save(
            FlowDocument(
                flowId = flowId,
                version = newVersion,
                description = meta.description,
                active = true,
                yamlContent = updatedYaml,
                createdAt = now,
                updatedAt = now
            )
        )
        // Invalida a versão alvo do PUT no cache do orquestrador (versão antiga
        // foi superada pela nova versão semântica recém-criada).
        orchestratorCacheClient.evictWorkflow(flowId, version)
        return newDoc.toSummary()
    }

    /** GET leve — sem yamlContent. */
    fun get(flowId: String, version: String): FlowSummaryDto =
        (repository.findByFlowIdAndVersion(flowId, version)
            ?: throw FlowNotFoundException("Flow '$flowId' version '$version' not found")
        ).toSummary()

    /** Listagem paginada — somente workflows ativos, sem yamlContent. */
    fun listAll(pageable: Pageable): Page<FlowSummaryDto> =
        repository.findAllByActiveTrue(pageable).map { it.toSummary() }

    /**
     * Histórico de versões de um flow específico — sem yamlContent.
     * [status] aceita "active", "inactive" ou null (todas as versões).
     */
    fun listVersions(flowId: String, status: String?): List<FlowSummaryDto> {
        val sort = Sort.by(Sort.Direction.ASC, "version")
        val docs = when (status) {
            "active"   -> repository.findAllByFlowIdAndActive(flowId, true, sort)
            "inactive" -> repository.findAllByFlowIdAndActive(flowId, false, sort)
            else       -> repository.findAllByFlowId(flowId, sort)
        }
        return docs.map { it.toSummary() }
    }

    /** Lista de ativos para o orquestrador — sem yamlContent. */
    fun listActive(): List<FlowSummaryDto> = repository.findByActiveTrue().map { it.toSummary() }

    fun deactivate(flowId: String, version: String) {
        val doc = repository.findByFlowIdAndVersionWithYaml(flowId, version)
            ?: throw FlowNotFoundException("Flow '$flowId' version '$version' not found")
        if (!doc.active) return
        doc.active = false
        doc.updatedAt = LocalDateTime.now()
        repository.save(doc)
        orchestratorCacheClient.evictWorkflow(flowId, version)
    }

    fun getYaml(flowId: String, version: String): String {
        val doc = repository.findByFlowIdAndVersionWithYaml(flowId, version)
            ?: throw FlowNotFoundException("Flow '$flowId' version '$version' not found")
        return doc.yamlContent
            ?: throw FlowNotFoundException(
                "Flow '$flowId' version '$version' has no yamlContent — likely created outside the Manager"
            )
    }

    private fun FlowDocument.toSummary() = FlowSummaryDto(
        flowId = flowId,
        version = version,
        description = description,
        active = active,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
