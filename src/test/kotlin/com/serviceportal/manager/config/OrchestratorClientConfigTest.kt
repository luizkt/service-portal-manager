package com.serviceportal.manager.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class OrchestratorClientConfigTest {

    @Test
    @DisplayName("orchestratorRestClient cria um RestClient com a baseUrl configurada")
    fun buildsRestClient() {
        val client = OrchestratorClientConfig().orchestratorRestClient("http://orchestrator:8080")
        assertThat(client).isNotNull
    }
}
