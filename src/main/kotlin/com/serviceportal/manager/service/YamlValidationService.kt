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

import com.serviceportal.manager.domain.ContractField
import com.serviceportal.manager.exception.InvalidFlowDefinitionException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.dataformat.yaml.YAMLMapper
import tools.jackson.module.kotlin.kotlinModule

/**
 * Validação leve do YAML antes de persistir.
 *
 * Não duplica os modelos do orquestrador — só extrai os metadados que o Manager
 * precisa indexar (`id`, `version`, `description`, `active`) e garante que a estrutura
 * mínima existe. A validação profunda continua no orquestrador, na execução.
 */
@Service
class YamlValidationService {
    private val log = LoggerFactory.getLogger(javaClass)

    // O módulo Kotlin é necessário para montar o data class ContractField (sem ele a conversão falha em silêncio).
    private val mapper = YAMLMapper.builder().addModule(kotlinModule()).build()

    data class FlowMetadata(
        val flowId: String,
        val version: String,
        val description: String?,
        val active: Boolean,
    )

    @Suppress("UNCHECKED_CAST")
    fun extractContractFields(yamlContent: String): List<ContractField> {
        val root =
            try {
                mapper.readValue(yamlContent, Map::class.java) as Map<String, Any?>
            } catch (e: Exception) {
                log.warn("YAML ilegível ao extrair campos do contrato: {}", e.firstLine())
                return emptyList()
            }
        val flow = root["flow"] as? Map<String, Any?> ?: return emptyList()
        val contract = flow["contract"] as? Map<String, Any?> ?: return emptyList()
        val rawFields =
            (contract["fields"] as? List<*>)?.filterIsInstance<Map<String, Any?>>()
                ?: return emptyList()
        return rawFields.mapNotNull { fieldMap ->
            try {
                mapper.convertValue(fieldMap, ContractField::class.java)
            } catch (e: Exception) {
                log.warn("Campo do contrato ignorado (name={}): {}", fieldMap["name"], e.firstLine())
                null
            }
        }
    }

    /** Só a primeira linha da mensagem: evita despejar trechos do YAML no log. */
    private fun Exception.firstLine(): String? = message?.lineSequence()?.firstOrNull()

    fun extractMetadata(yamlContent: String): FlowMetadata {
        if (yamlContent.isBlank()) {
            throw InvalidFlowDefinitionException("Empty YAML")
        }

        val root: Map<String, Any?> =
            try {
                @Suppress("UNCHECKED_CAST")
                mapper.readValue(yamlContent, Map::class.java) as Map<String, Any?>
            } catch (e: Exception) {
                throw InvalidFlowDefinitionException("Invalid YAML: ${e.message}")
            }

        @Suppress("UNCHECKED_CAST")
        val flow =
            root["flow"] as? Map<String, Any?>
                ?: throw InvalidFlowDefinitionException("YAML must contain root key 'flow'")

        val flowId =
            (flow["id"] as? String)?.takeIf { it.isNotBlank() }
                ?: throw InvalidFlowDefinitionException("Field 'flow.id' is required")

        val version =
            (flow["version"] as? String)?.takeIf { it.isNotBlank() }
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
