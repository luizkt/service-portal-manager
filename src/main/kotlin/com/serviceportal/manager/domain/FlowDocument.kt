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

    var createdAt: LocalDateTime = LocalDateTime.now(),

    var updatedAt: LocalDateTime = LocalDateTime.now()
)
