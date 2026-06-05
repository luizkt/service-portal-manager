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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.serviceportal.manager.exception.InvalidFlowDefinitionException
import org.springframework.stereotype.Service

/**
 * Regras de versionamento semântico (SemVer 2.0.0) para workflows:
 *   - MAJOR: alteração na seção `contract`
 *   - MINOR: alteração na seção `integrations`
 *   - PATCH: alteração na `description` ou qualquer outra mudança menor
 */
@Service
class VersioningService {
    private val yamlMapper = ObjectMapper(YAMLFactory())
    private val jsonMapper = ObjectMapper()

    enum class ChangeType { MAJOR, MINOR, PATCH }

    fun detectChangeType(
        oldYaml: String,
        newYaml: String,
    ): ChangeType {
        val oldFlow = parseFlow(oldYaml)
        val newFlow = parseFlow(newYaml)

        if (normalize(oldFlow["contract"]) != normalize(newFlow["contract"])) return ChangeType.MAJOR
        if (normalize(oldFlow["integrations"]) != normalize(newFlow["integrations"])) return ChangeType.MINOR
        return ChangeType.PATCH
    }

    fun calculateNextVersion(
        currentVersion: String,
        changeType: ChangeType,
    ): String {
        val parts = currentVersion.trim().split(".")
        val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
        return when (changeType) {
            ChangeType.MAJOR -> "${major + 1}.0.0"
            ChangeType.MINOR -> "$major.${minor + 1}.0"
            ChangeType.PATCH -> "$major.$minor.${patch + 1}"
        }
    }

    /** Atualiza o campo `flow.version` no YAML armazenado para ser consistente com a versão calculada. */
    fun updateVersionInYaml(
        yamlContent: String,
        newVersion: String,
    ): String {
        @Suppress("UNCHECKED_CAST")
        val root = yamlMapper.readValue(yamlContent, LinkedHashMap::class.java) as LinkedHashMap<Any, Any>

        @Suppress("UNCHECKED_CAST")
        val flow = root["flow"] as? MutableMap<String, Any> ?: return yamlContent
        flow["version"] = newVersion
        return yamlMapper.writeValueAsString(root)
    }

    private fun parseFlow(yaml: String): Map<String, Any?> {
        @Suppress("UNCHECKED_CAST")
        val root = yamlMapper.readValue(yaml, Map::class.java) as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        return root["flow"] as? Map<String, Any?>
            ?: throw InvalidFlowDefinitionException("Missing 'flow' root key in YAML")
    }

    private fun normalize(obj: Any?): String = jsonMapper.writeValueAsString(obj)
}
