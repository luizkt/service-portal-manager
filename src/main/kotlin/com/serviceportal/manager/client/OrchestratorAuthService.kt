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
package com.serviceportal.manager.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.time.Instant
import java.util.Base64

/**
 * Auth server-to-server contra o generic-orchestrator. Mantém o JWT em memória
 * e renova quando faltar menos de 60s para expirar. A expiração vem do claim `exp` do
 * JWT (decodificado sem verificar assinatura: só dirige o cache), senão de `expiresIn`
 * da resposta de login, senão 3600s. [invalidate] descarta o token que o orquestrador
 * rejeitou com 401. Espelha o `ManagerAuthService` do orquestrador.
 */
@Service
class OrchestratorAuthService(
    @Qualifier("orchestratorRestClient") private val restClient: RestClient,
    @Value("\${orchestrator.admin-username:admin}") private val username: String,
    @Value("\${orchestrator.admin-password:admin}") private val password: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Volatile private var token: String? = null

    @Volatile private var expiry: Instant = Instant.EPOCH

    /** Descarta o token em cache, mas só se ainda for [failedToken] (outro thread pode já ter renovado). */
    @Synchronized
    fun invalidate(failedToken: String) {
        if (token == failedToken) {
            token = null
            expiry = Instant.EPOCH
        }
    }

    @Synchronized
    fun token(): String {
        if (Instant.now().isAfter(expiry.minusSeconds(60))) {
            refresh()
        }
        return token ?: throw IllegalStateException("Orchestrator did not return a token")
    }

    private fun refresh() {
        log.debug("Renovando token do generic-orchestrator")
        val response =
            restClient.post()
                .uri("/api/auth/tokens")
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapOf("username" to username, "password" to password))
                .retrieve()
                .body(TokenResponse::class.java)
                ?: throw IllegalStateException("Orchestrator returned an empty token response")
        token = response.token
        expiry = resolveExpiry(response)
    }

    private fun resolveExpiry(response: TokenResponse): Instant {
        val now = Instant.now()
        val fromJwt = response.token?.let { jwtExpiry(it) }
        if (fromJwt != null) return fromJwt
        val ttl = response.expiresIn?.takeIf { it > 0 } ?: DEFAULT_TTL_SECONDS
        return now.plusSeconds(ttl)
    }

    private fun jwtExpiry(jwt: String): Instant? =
        try {
            val payload = Base64.getUrlDecoder().decode(jwt.split('.')[1])
            mapper.readTree(payload).get("exp")?.takeIf { it.canConvertToLong() }?.let { Instant.ofEpochSecond(it.asLong()) }
        } catch (_: Exception) {
            null
        }

    data class TokenResponse(
        val token: String? = null,
        val type: String? = null,
        val expiresIn: Long? = null,
    )

    private companion object {
        const val DEFAULT_TTL_SECONDS = 3600L
        val mapper = ObjectMapper()
    }
}
