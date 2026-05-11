package com.serviceportal.manager.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.serviceportal.manager.exception.InvalidFlowDefinitionException
import org.springframework.stereotype.Service

/**
 * Validação leve do YAML antes de persistir.
 *
 * Não duplica os modelos do orquestrador — só extrai os metadados que o Manager
 * precisa indexar (`id`, `version`, `description`, `active`) e garante que a estrutura
 * mínima existe. A validação profunda continua no orquestrador, na execução.
 */
@Service
class YamlValidationService {

    private val mapper = ObjectMapper(YAMLFactory())

    data class FlowMetadata(
        val flowId: String,
        val version: String,
        val description: String?,
        val active: Boolean
    )

    fun extractMetadata(yamlContent: String): FlowMetadata {
        if (yamlContent.isBlank()) {
            throw InvalidFlowDefinitionException("Empty YAML")
        }

        val root: Map<String, Any?> = try {
            @Suppress("UNCHECKED_CAST")
            mapper.readValue(yamlContent, Map::class.java) as Map<String, Any?>
        } catch (e: Exception) {
            throw InvalidFlowDefinitionException("Invalid YAML: ${e.message}")
        }

        @Suppress("UNCHECKED_CAST")
        val flow = root["flow"] as? Map<String, Any?>
            ?: throw InvalidFlowDefinitionException("YAML must contain root key 'flow'")

        val flowId = (flow["id"] as? String)?.takeIf { it.isNotBlank() }
            ?: throw InvalidFlowDefinitionException("Field 'flow.id' is required")

        val version = (flow["version"] as? String)?.takeIf { it.isNotBlank() }
            ?: throw InvalidFlowDefinitionException("Field 'flow.version' is required")

        if (flow["contract"] == null) {
            throw InvalidFlowDefinitionException("Field 'flow.contract' is required")
        }

        val integrations = flow["integrations"] as? List<*>
        if (integrations.isNullOrEmpty()) {
            throw InvalidFlowDefinitionException("Flow must have at least one integration")
        }

        val description = flow["description"] as? String
        val active = flow["active"] as? Boolean ?: true

        return FlowMetadata(flowId, version, description, active)
    }
}
