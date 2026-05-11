package com.serviceportal.manager.service

import com.serviceportal.manager.domain.FlowDocument
import com.serviceportal.manager.dto.FlowSummaryDto
import com.serviceportal.manager.exception.FlowAlreadyExistsException
import com.serviceportal.manager.exception.FlowNotFoundException
import com.serviceportal.manager.exception.InvalidFlowDefinitionException
import com.serviceportal.manager.repository.FlowDocumentRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class FlowDocumentService(
    private val repository: FlowDocumentRepository,
    private val yamlValidator: YamlValidationService
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

    fun update(flowId: String, version: String, yamlContent: String): FlowSummaryDto {
        val meta = yamlValidator.extractMetadata(yamlContent)
        if (meta.flowId != flowId || meta.version != version) {
            throw InvalidFlowDefinitionException(
                "Path id/version (${flowId}/${version}) does not match YAML (${meta.flowId}/${meta.version})"
            )
        }
        // WithYaml: precisamos do doc completo para que repository.save() não
        // zere o yamlContent (save substitui o doc inteiro por _id).
        val existing = repository.findByFlowIdAndVersionWithYaml(flowId, version)
            ?: throw FlowNotFoundException("Flow '$flowId' version '$version' not found")

        existing.apply {
            this.description = meta.description
            this.active = meta.active
            this.yamlContent = yamlContent
            this.updatedAt = LocalDateTime.now()
        }
        return repository.save(existing).toSummary()
    }

    /** GET leve — sem yamlContent. */
    fun get(flowId: String, version: String): FlowSummaryDto =
        (repository.findByFlowIdAndVersion(flowId, version)
            ?: throw FlowNotFoundException("Flow '$flowId' version '$version' not found")
        ).toSummary()

    /** Listagem paginada — sem yamlContent. */
    fun listAll(pageable: Pageable): Page<FlowSummaryDto> =
        repository.findAll(pageable).map { it.toSummary() }

    /** Lista de ativos para o orquestrador — sem yamlContent. */
    fun listActive(): List<FlowSummaryDto> = repository.findByActiveTrue().map { it.toSummary() }

    fun deactivate(flowId: String, version: String) {
        // WithYaml para preservar yamlContent ao re-salvar (vide comentário em update).
        val doc = repository.findByFlowIdAndVersionWithYaml(flowId, version)
            ?: throw FlowNotFoundException("Flow '$flowId' version '$version' not found")
        if (!doc.active) return
        doc.active = false
        doc.updatedAt = LocalDateTime.now()
        repository.save(doc)
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
