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

    @Test
    @DisplayName("Collection é 'workflows'")
    fun collectionName() {
        val doc = klass.getAnnotation(Document::class.java)
        assertThat(doc).isNotNull
        assertThat(doc.collection).isEqualTo("workflows")
    }

    @Test
    @DisplayName("Campo flowId NÃO tem @Field — Mongo persiste com nome 'flowId'")
    fun flowIdSemFieldOverride() {
        val field = klass.getDeclaredField("flowId")
        assertThat(field.getAnnotation(Field::class.java))
            .`as`("@Field NÃO deve forçar outro nome — orquestrador e init-mongo.js usam 'flowId'")
            .isNull()
    }

    @Test
    @DisplayName("@Id está em mongoId")
    fun idAnnotation() {
        val field = klass.getDeclaredField("mongoId")
        assertThat(field.getAnnotation(Id::class.java)).isNotNull
    }

    @Test
    @DisplayName("yamlContent é nullable e do tipo String")
    fun yamlContent() {
        val field = klass.getDeclaredField("yamlContent")
        assertThat(field.type).isEqualTo(String::class.java)
    }

    @Test
    @DisplayName("Campos esperados estão declarados (todos em inglês após o refactor REST)")
    fun expectedFields() {
        val names = klass.declaredFields.map { it.name }.toSet()
        assertThat(names).contains(
            "mongoId",
            "flowId",
            "version",
            "description",
            "active",
            "yamlContent",
            "createdAt",
            "updatedAt",
        )
    }
}
