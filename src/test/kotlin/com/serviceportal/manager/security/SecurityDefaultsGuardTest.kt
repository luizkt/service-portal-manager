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
package com.serviceportal.manager.security

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.mock.env.MockEnvironment

class SecurityDefaultsGuardTest {
    private val logger = LoggerFactory.getLogger(SecurityDefaultsGuard::class.java) as Logger
    private lateinit var appender: ListAppender<ILoggingEvent>

    @BeforeEach
    fun attach() {
        appender = ListAppender<ILoggingEvent>().also { it.start() }
        logger.addAppender(appender)
    }

    @AfterEach
    fun detach() {
        logger.detachAppender(appender)
    }

    private fun defaults() =
        MockEnvironment()
            .withProperty("manager.security.jwt.secret", SECRET_VALUE)
            .withProperty("manager.security.admin.username", "admin")
            .withProperty("manager.security.admin.password", "admin")
            .withProperty("orchestrator.admin-password", "admin")
            .withProperty("spring.boot.admin.client.password", "admin")

    @Test
    @DisplayName("warns naming each default setting without printing values")
    fun warnsInNormalProfile() {
        SecurityDefaultsGuard(defaults()).check()

        assertThat(appender.list).hasSize(1)
        val event = appender.list[0]
        assertThat(event.level).isEqualTo(Level.WARN)
        assertThat(event.formattedMessage)
            .contains("JWT_SECRET", "MANAGER_ADMIN_PASSWORD", "ORCHESTRATOR_ADMIN_PASSWORD", "SBA_PASSWORD")
            .doesNotContain(SECRET_VALUE)
    }

    @Test
    @DisplayName("fails startup in strict mode")
    fun failsWhenStrict() {
        val env = defaults().withProperty("manager.security.strict", "true")

        assertThatThrownBy { SecurityDefaultsGuard(env).check() }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("JWT_SECRET")
            .hasMessageNotContaining(SECRET_VALUE)
    }

    @Test
    @DisplayName("fails startup with profile prod")
    fun failsWhenProd() {
        val env = defaults()
        env.setActiveProfiles("prod")

        assertThatThrownBy { SecurityDefaultsGuard(env).check() }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    @DisplayName("passes silently when nothing uses defaults, even in strict mode")
    fun passesWithRealSecrets() {
        val env =
            MockEnvironment()
                .withProperty("manager.security.strict", "true")
                .withProperty("manager.security.jwt.secret", "real-secret")
                .withProperty("manager.security.admin.username", "ops")
                .withProperty("manager.security.admin.password", "pw")
                .withProperty("orchestrator.admin-password", "pw")
                .withProperty("spring.boot.admin.client.password", "pw")

        assertThatCode { SecurityDefaultsGuard(env).check() }.doesNotThrowAnyException()
        assertThat(appender.list).isEmpty()
    }

    private companion object {
        const val SECRET_VALUE = "CHANGE_ME_dev_secret_value"
    }
}
