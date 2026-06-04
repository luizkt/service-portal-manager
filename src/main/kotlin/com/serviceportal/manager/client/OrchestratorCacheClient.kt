package com.serviceportal.manager.client

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * Invalida o cache de workflows do generic-orchestrator após mutações no Manager.
 *
 * Falha tolerante por design: se a invalidação falhar (orquestrador indisponível,
 * etc.), apenas loga warning e NÃO propaga — o workflow já foi atualizado com
 * sucesso e o orquestrador recarregará a versão correta no próximo cache miss ou
 * após o TTL.
 */
@Component
class OrchestratorCacheClient(
    @Qualifier("orchestratorRestClient") private val restClient: RestClient,
    private val authService: OrchestratorAuthService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun evictWorkflow(flowId: String, version: String) {
        try {
            restClient.delete()
                .uri("/api/admin/cache/workflows/{flowId}/versions/{version}", flowId, version)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authService.token()}")
                .retrieve()
                .toBodilessEntity()
            log.info("Cache do orquestrador invalidado para {}/{}", flowId, version)
        } catch (ex: Exception) {
            log.warn("Falha ao invalidar cache do orquestrador para {}/{}: {}", flowId, version, ex.message)
        }
    }
}
