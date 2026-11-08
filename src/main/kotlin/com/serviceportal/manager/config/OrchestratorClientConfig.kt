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
package com.serviceportal.manager.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration

/**
 * RestClient síncrono para chamadas server-to-server ao generic-orchestrator
 * (invalidação de cache de workflows). RestClient faz parte do spring-web — não
 * exige WebFlux/Netty, alinhado ao estilo MVC bloqueante do Manager.
 *
 * Tem timeouts de conexão e de leitura: a invalidação roda de forma síncrona dentro
 * do PUT/DELETE de workflow, então um orquestrador lento não pode segurar o usuário.
 */
@Configuration
class OrchestratorClientConfig {
    @Bean("orchestratorRestClient")
    fun orchestratorRestClient(
        builder: RestClient.Builder,
        @Value("\${orchestrator.url:http://localhost:8080}") url: String,
        @Value("\${orchestrator.connect-timeout-ms:2000}") connectTimeoutMs: Long = DEFAULT_CONNECT_TIMEOUT_MS,
        @Value("\${orchestrator.read-timeout-ms:5000}") readTimeoutMs: Long = DEFAULT_READ_TIMEOUT_MS,
    ): RestClient {
        val httpClient =
            HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build()
        val requestFactory =
            JdkClientHttpRequestFactory(httpClient).apply {
                setReadTimeout(Duration.ofMillis(readTimeoutMs))
            }
        return builder.baseUrl(url).requestFactory(requestFactory).build()
    }

    companion object {
        const val DEFAULT_CONNECT_TIMEOUT_MS = 2000L
        const val DEFAULT_READ_TIMEOUT_MS = 5000L
    }
}
