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

import com.serviceportal.manager.config.OrchestratorClientConfig
import com.sun.net.httpserver.HttpServer
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import java.net.InetSocketAddress
import java.util.concurrent.Executors

class OrchestratorTimeoutTest {
    private lateinit var server: HttpServer
    private val delayMs = 3000L
    private val readTimeoutMs = 300L

    @BeforeEach
    fun start() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.executor = Executors.newCachedThreadPool()
        server.createContext("/") { exchange ->
            try {
                Thread.sleep(delayMs)
                exchange.sendResponseHeaders(204, -1)
            } catch (_: Exception) {
                // cliente desistiu
            } finally {
                exchange.close()
            }
        }
        server.start()
    }

    @AfterEach
    fun stop() {
        server.stop(0)
        (server.executor as java.util.concurrent.ExecutorService).shutdownNow()
    }

    private fun client(): RestClient =
        OrchestratorClientConfig()
            .orchestratorRestClient(
                RestClient.builder(),
                "http://127.0.0.1:${server.address.port}",
                connectTimeoutMs = 300,
                readTimeoutMs = readTimeoutMs,
            )

    @Test
    @DisplayName("RestClient falha rápido quando o orquestrador demora mais que o read timeout")
    fun failsFastOnSlowServer() {
        val client = client()
        val start = System.nanoTime()
        assertThatThrownBy {
            client.delete().uri("/slow").retrieve().toBodilessEntity()
        }.isInstanceOf(ResourceAccessException::class.java)
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        assertThat(elapsedMs).isLessThan(delayMs - 500)
    }

    @Test
    @DisplayName("evictWorkflow não propaga nem bloqueia além do timeout")
    fun evictIsBestEffortAndFast() {
        val auth: OrchestratorAuthService = mockk()
        every { auth.token() } returns "jwt"
        val cacheClient = OrchestratorCacheClient(client(), auth)
        val start = System.nanoTime()
        cacheClient.evictWorkflow("f", "1.0.0")
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        assertThat(elapsedMs).isLessThan(delayMs - 500)
    }
}
