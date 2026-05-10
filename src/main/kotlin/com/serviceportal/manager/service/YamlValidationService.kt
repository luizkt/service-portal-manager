package com.serviceportal.manager.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.serviceportal.manager.exception.InvalidFlowDefinitionException
import org.springframework.stereotype.Service

/**
 * Validação leve do YAML antes de persistir.
 *
 * Não duplica os modelos do orquestrador — só extrai os metadados que o Manager
 * precisa indexar (`id`, `versao`, `descricao`, `ativo`) e garante que a estrutura
 * mínima existe. A validação profunda continua no orquestrador, na execução.
 */
@Service
class YamlValidationService {

    private val mapper = ObjectMapper(YAMLFactory())

    data class FlowMetadata(
        val flowId: String,
        val versao: String,
        val descricao: String?,
        val ativo: Boolean
    )

    fun extractMetadata(yamlContent: String): FlowMetadata {
        if (yamlContent.isBlank()) {
            throw InvalidFlowDefinitionException("YAML vazio")
        }

        val root: Map<String, Any?> = try {
            @Suppress("UNCHECKED_CAST")
            mapper.readValue(yamlContent, Map::class.java) as Map<String, Any?>
        } catch (e: Exception) {
            throw InvalidFlowDefinitionException("YAML inválido: ${e.message}")
        }

        @Suppress("UNCHECKED_CAST")
        val fluxo = root["fluxo"] as? Map<String, Any?>
            ?: throw InvalidFlowDefinitionException("YAML deve conter a chave raiz 'fluxo'")

        val flowId = (fluxo["id"] as? String)?.takeIf { it.isNotBlank() }
            ?: throw InvalidFlowDefinitionException("O campo 'fluxo.id' é obrigatório")

        val versao = (fluxo["versao"] as? String)?.takeIf { it.isNotBlank() }
            ?: throw InvalidFlowDefinitionException("O campo 'fluxo.versao' é obrigatório")

        if (fluxo["contrato"] == null) {
            throw InvalidFlowDefinitionException("O campo 'fluxo.contrato' é obrigatório")
        }

        val integracoes = fluxo["integracoes"] as? List<*>
        if (integracoes.isNullOrEmpty()) {
            throw InvalidFlowDefinitionException("O fluxo deve ter ao menos uma integração")
        }

        val descricao = fluxo["descricao"] as? String
        val ativo = fluxo["ativo"] as? Boolean ?: true

        return FlowMetadata(flowId, versao, descricao, ativo)
    }
}
