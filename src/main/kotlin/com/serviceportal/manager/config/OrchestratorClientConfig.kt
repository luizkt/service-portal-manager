package com.serviceportal.manager.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

/**
 * RestClient síncrono para chamadas server-to-server ao generic-orchestrator
 * (invalidação de cache de workflows). RestClient faz parte do spring-web — não
 * exige WebFlux/Netty, alinhado ao estilo MVC bloqueante do Manager.
 */
@Configuration
class OrchestratorClientConfig {

    @Bean("orchestratorRestClient")
    fun orchestratorRestClient(
        @Value("\${orchestrator.url:http://localhost:8080}") url: String
    ): RestClient = RestClient.builder().baseUrl(url).build()
}
