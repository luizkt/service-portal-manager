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

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.stereotype.Component

/**
 * Detecta defaults de desenvolvimento conhecidos na subida. Loga WARN com os NOMES das
 * configurações (nunca os valores) e aborta a subida com profile `prod` ou
 * `manager.security.strict=true`.
 */
@Component
class SecurityDefaultsGuard(private val env: Environment) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun check() {
        val defaults = mutableListOf<String>()
        if (env.getProperty("manager.security.jwt.secret", "").startsWith(DEFAULT_SECRET_PREFIX)) {
            defaults += "JWT_SECRET (manager.security.jwt.secret)"
        }
        if (env.getProperty("manager.security.admin.username") == DEFAULT_CREDENTIAL &&
            env.getProperty("manager.security.admin.password") == DEFAULT_CREDENTIAL
        ) {
            defaults += "MANAGER_ADMIN_USERNAME/MANAGER_ADMIN_PASSWORD (manager.security.admin.*)"
        }
        if (env.getProperty("orchestrator.admin-password") == DEFAULT_CREDENTIAL) {
            defaults += "ORCHESTRATOR_ADMIN_PASSWORD (orchestrator.admin-password)"
        }
        if (env.getProperty("spring.boot.admin.client.password") == DEFAULT_CREDENTIAL) {
            defaults += "SBA_PASSWORD (spring.boot.admin.client.password)"
        }
        if (defaults.isEmpty()) return

        val strict =
            env.acceptsProfiles(Profiles.of("prod")) ||
                env.getProperty("manager.security.strict", Boolean::class.java, false)
        val message = "Insecure default settings in use: " + defaults.joinToString(", ")
        check(!strict) { "$message. Refusing to start (prod/strict mode)." }
        log.warn("{}. Change them before deploying (prod/strict mode refuses to start).", message)
    }

    private companion object {
        const val DEFAULT_SECRET_PREFIX = "CHANGE_ME"
        const val DEFAULT_CREDENTIAL = "admin"
    }
}
