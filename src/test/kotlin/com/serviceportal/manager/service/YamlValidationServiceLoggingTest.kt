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

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class YamlValidationServiceLoggingTest {
    private val service = YamlValidationService()
    private val logger = LoggerFactory.getLogger(YamlValidationService::class.java) as Logger
    private val appender = ListAppender<ILoggingEvent>()

    @BeforeEach
    fun attach() {
        appender.start()
        logger.addAppender(appender)
    }

    @AfterEach
    fun detach() {
        logger.detachAppender(appender)
    }

    @Test
    @DisplayName("YAML ilegível: devolve lista vazia e registra warn")
    fun unreadableYaml() {
        val result = service.extractContractFields("flow: [unclosed")

        assertThat(result).isEmpty()
        assertThat(appender.list).hasSize(1)
        assertThat(appender.list[0].level).isEqualTo(Level.WARN)
    }

    @Test
    @DisplayName("Campo inválido: é descartado, os válidos permanecem, e o warn cita o nome do campo")
    fun invalidField() {
        val yaml =
            """
            flow:
              contract:
                fields:
                  - name: ok
                    type: STRING
                  - name: broken
                    type: STRING
                    required: [not, a, boolean]
            """.trimIndent()

        val result = service.extractContractFields(yaml)

        assertThat(result.map { it.name }).containsExactly("ok")
        assertThat(appender.list).hasSize(1)
        assertThat(appender.list[0].level).isEqualTo(Level.WARN)
        assertThat(appender.list[0].formattedMessage).contains("broken")
    }
}
