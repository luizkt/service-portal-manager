package com.serviceportal.manager.client

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.time.Instant

/**
 * Auth server-to-server contra o generic-orchestrator. Mantém o JWT em memória
 * e renova quando faltar menos de 60s para expirar (TTL assumido de 3600s, mesmo
 * padrão do orquestrador). Espelha o `ManagerAuthService` do orquestrador.
 */
@Service
class OrchestratorAuthService(
    @Qualifier("orchestratorRestClient") private val restClient: RestClient,
    @Value("\${orchestrator.admin-username:admin}") private val username: String,
    @Value("\${orchestrator.admin-password:admin}") private val password: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Volatile private var token: String? = null
    @Volatile private var expiry: Instant = Instant.EPOCH

    @Synchronized
    fun token(): String {
        if (Instant.now().isAfter(expiry.minusSeconds(60))) {
            refresh()
        }
        return token ?: throw IllegalStateException("Orchestrator did not return a token")
    }

    private fun refresh() {
        log.debug("Renovando token do generic-orchestrator")
        val response = restClient.post()
            .uri("/api/auth/tokens")
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("username" to username, "password" to password))
            .retrieve()
            .body(TokenResponse::class.java)
            ?: throw IllegalStateException("Orchestrator returned an empty token response")
        token = response.token
        expiry = Instant.now().plusSeconds(3600)
    }

    data class TokenResponse(val token: String? = null, val type: String? = null)
}
