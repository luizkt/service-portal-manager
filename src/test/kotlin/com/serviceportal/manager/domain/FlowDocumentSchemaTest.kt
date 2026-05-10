package com.serviceportal.manager.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

/**
 * Garante o contrato do schema persistido — coexistência com o orquestrador
 * (`generic-orchestrator/.../FlowDefinition.java`) e com o índice composto
 * `flowId` + `versao` declarado em `mongodb-workflows/init-mongo.js`.
 *
 * Se alguém adicionar `@Field("id")` (ou similar) a `flowId` de novo, este teste falha.
 */
class FlowDocumentSchemaTest {

    private val klass = FlowDocument::class.java

    @Test @DisplayName("Collection é 'workflows'")
    fun collectionName() {
        val doc = klass.getAnnotation(Document::class.java)
        assertThat(doc).isNotNull
        assertThat(doc.collection).isEqualTo("workflows")
    }

    @Test @DisplayName("Campo flowId NÃO tem @Field — Mongo persiste com nome 'flowId'")
    fun flowIdSemFieldOverride() {
        val field = klass.getDeclaredField("flowId")
        assertThat(field.getAnnotation(Field::class.java))
            .`as`("@Field NÃO deve forçar outro nome — orquestrador e init-mongo.js usam 'flowId'")
            .isNull()
    }

    @Test @DisplayName("@Id está em mongoId")
    fun idAnnotation() {
        val field = klass.getDeclaredField("mongoId")
        assertThat(field.getAnnotation(Id::class.java)).isNotNull
    }

    @Test @DisplayName("yamlContent é nullable e do tipo String")
    fun yamlContent() {
        val field = klass.getDeclaredField("yamlContent")
        assertThat(field.type).isEqualTo(String::class.java)
    }

    @Test @DisplayName("Campos esperados estão declarados")
    fun camposDeclarados() {
        val nomes = klass.declaredFields.map { it.name }.toSet()
        assertThat(nomes).contains(
            "mongoId", "flowId", "versao", "descricao",
            "ativo", "yamlContent", "criadoEm", "atualizadoEm"
        )
    }
}
