package com.serviceportal.manager.domain

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

/**
 * Documento da collection `workflows` gerenciada pelo service-portal-manager.
 *
 * O campo `yamlContent` armazena o YAML original do workflow tal como foi recebido —
 * é a fonte de verdade. O parse para uma estrutura tipada continua acontecendo
 * no orquestrador no momento da execução.
 *
 * Compatível com a coexistência durante a migração: o orquestrador hoje salva
 * documentos sem `yamlContent`. O Manager só serve YAML para documentos que tenham
 * o campo preenchido (criados via `POST /manager/flows`).
 *
 * Schema:
 *   - O nome da propriedade `flowId` é usado tal qual no MongoDB (sem `@Field` override),
 *     alinhado com o orquestrador (`generic-orchestrator/.../FlowDefinition.java`) e com
 *     o índice composto único `flowId` + `versao` definido em `mongodb-workflows/init-mongo.js`.
 *   - O índice de unicidade NÃO é declarado via anotação aqui — `auto-index-creation`
 *     é `false` no Spring Boot 3 por default; o init-mongo.js é a fonte de verdade.
 */
@Document(collection = "workflows")
data class FlowDocument(
    @Id
    var mongoId: String? = null,

    var flowId: String,

    var versao: String,

    var descricao: String? = null,

    var ativo: Boolean = true,

    /** YAML original recebido na criação/atualização. */
    var yamlContent: String? = null,

    var criadoEm: LocalDateTime = LocalDateTime.now(),

    var atualizadoEm: LocalDateTime = LocalDateTime.now()
)
