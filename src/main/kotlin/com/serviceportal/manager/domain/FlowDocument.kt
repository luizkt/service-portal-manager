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
package com.serviceportal.manager.domain

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

/**
 * Documento da collection `workflows` gerenciada pelo service-portal-manager.
 *
 * O campo `yamlContent` armazena o YAML original do workflow tal como foi recebido —
 * é a fonte de verdade. O parse para uma estrutura tipada acontece no orquestrador.
 *
 * Schema (todos os campos em inglês após o refactor REST):
 *   - `flowId` + `version` formam a chave de negócio única (índice composto
 *     em `mongodb-workflows/init-mongo.js`)
 *   - O índice de unicidade NÃO é declarado via anotação aqui — `auto-index-creation`
 *     é `false` no Spring Boot 3 por default; o init-mongo.js é a fonte de verdade.
 */
@Document(collection = "workflows")
data class FlowDocument(
    @Id
    var mongoId: String? = null,
    var flowId: String,
    var version: String,
    var description: String? = null,
    var active: Boolean = true,
    /** YAML original recebido na criação/atualização. */
    var yamlContent: String? = null,
    /** Referência ao contract utilizado por este workflow (id + version). */
    var contract: ResourceRef? = null,
    /** Referências às integrations utilizadas por este workflow (id + version de cada uma). */
    var integrationRefs: List<ResourceRef> = emptyList(),
    /** Referências às validations utilizadas por este workflow (id + version de cada uma). */
    var validationRefs: List<ResourceRef> = emptyList(),
    var createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)
