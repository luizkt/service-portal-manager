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
        if (repository.existsByFlowIdAndVersao(meta.flowId, meta.versao)) {
            throw FlowAlreadyExistsException(
                "Fluxo '${meta.flowId}' versão '${meta.versao}' já existe"
            )
        }
        val now = LocalDateTime.now()
        val saved = repository.save(
            FlowDocument(
                flowId = meta.flowId,
                versao = meta.versao,
                descricao = meta.descricao,
                ativo = meta.ativo,
                yamlContent = yamlContent,
                criadoEm = now,
                atualizadoEm = now
            )
        )
        return saved.toSummary()
    }

    fun update(flowId: String, versao: String, yamlContent: String): FlowSummaryDto {
        val meta = yamlValidator.extractMetadata(yamlContent)
        if (meta.flowId != flowId || meta.versao != versao) {
            throw InvalidFlowDefinitionException(
                "id/versão do path (${flowId}/${versao}) não bate com o YAML (${meta.flowId}/${meta.versao})"
            )
        }
        // WithYaml: precisamos do doc completo para que repository.save() não
        // zere o yamlContent (save substitui o doc inteiro por _id).
        val existing = repository.findByFlowIdAndVersaoWithYaml(flowId, versao)
            ?: throw FlowNotFoundException("Fluxo '$flowId' versão '$versao' não encontrado")

        existing.apply {
            this.descricao = meta.descricao
            this.ativo = meta.ativo
            this.yamlContent = yamlContent
            this.atualizadoEm = LocalDateTime.now()
        }
        return repository.save(existing).toSummary()
    }

    /** GET leve — sem yamlContent. */
    fun get(flowId: String, versao: String): FlowSummaryDto =
        (repository.findByFlowIdAndVersao(flowId, versao)
            ?: throw FlowNotFoundException("Fluxo '$flowId' versão '$versao' não encontrado")
        ).toSummary()

    /** Listagem paginada — sem yamlContent. */
    fun listAll(pageable: Pageable): Page<FlowSummaryDto> =
        repository.findAll(pageable).map { it.toSummary() }

    /** Lista de ativos para o orquestrador — sem yamlContent. */
    fun listActive(): List<FlowSummaryDto> = repository.findByAtivoTrue().map { it.toSummary() }

    fun deactivate(flowId: String, versao: String) {
        // WithYaml para preservar yamlContent ao re-salvar (vide comentário em update).
        val doc = repository.findByFlowIdAndVersaoWithYaml(flowId, versao)
            ?: throw FlowNotFoundException("Fluxo '$flowId' versão '$versao' não encontrado")
        if (!doc.ativo) return
        doc.ativo = false
        doc.atualizadoEm = LocalDateTime.now()
        repository.save(doc)
    }

    fun getYaml(flowId: String, versao: String): String {
        val doc = repository.findByFlowIdAndVersaoWithYaml(flowId, versao)
            ?: throw FlowNotFoundException("Fluxo '$flowId' versão '$versao' não encontrado")
        return doc.yamlContent
            ?: throw FlowNotFoundException(
                "Fluxo '$flowId' versão '$versao' não tem yamlContent — provavelmente foi criado fora do Manager"
            )
    }

    private fun FlowDocument.toSummary() = FlowSummaryDto(
        flowId = flowId,
        versao = versao,
        descricao = descricao,
        ativo = ativo,
        criadoEm = criadoEm,
        atualizadoEm = atualizadoEm
    )
}
