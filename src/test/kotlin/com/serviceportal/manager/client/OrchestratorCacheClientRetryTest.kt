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

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.web.client.RestClient

class OrchestratorCacheClientRetryTest {
    private val url = "http://orchestrator:8080/api/admin/cache/workflows/f/versions/1.0"
    private val authService: OrchestratorAuthService = mockk(relaxed = true)

    private fun setup(): Pair<OrchestratorCacheClient, MockRestServiceServer> {
        val builder = RestClient.builder().baseUrl("http://orchestrator:8080")
        val server = MockRestServiceServer.bindTo(builder).build()
        return OrchestratorCacheClient(builder.build(), authService) to server
    }

    @Test
    @DisplayName("401 invalida o token e repete uma vez com token novo")
    fun retriesOnceOn401() {
        every { authService.token() } returnsMany listOf("old", "new")
        val (client, server) = setup()
        server.expect(requestTo(url)).andExpect(header("Authorization", "Bearer old"))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED))
        server.expect(requestTo(url)).andExpect(header("Authorization", "Bearer new"))
            .andRespond(withStatus(HttpStatus.NO_CONTENT))

        client.evictWorkflow("f", "1.0")

        server.verify()
        verify(exactly = 1) { authService.invalidate("old") }
    }

    @Test
    @DisplayName("401 persistente não gera loop: só uma repetição e sem propagar")
    fun noLoopOnRepeated401() {
        every { authService.token() } returnsMany listOf("old", "new")
        val (client, server) = setup()
        server.expect(requestTo(url)).andRespond(withStatus(HttpStatus.UNAUTHORIZED))
        server.expect(requestTo(url)).andRespond(withStatus(HttpStatus.UNAUTHORIZED))

        client.evictWorkflow("f", "1.0")

        server.verify()
        verify(exactly = 1) { authService.invalidate(any()) }
    }

    @Test
    @DisplayName("erros diferentes de 401 não repetem nem invalidam o token")
    fun noRetryOnOtherErrors() {
        every { authService.token() } returns "tok"
        val (client, server) = setup()
        server.expect(requestTo(url)).andRespond(withStatus(HttpStatus.FORBIDDEN))

        client.evictWorkflow("f", "1.0")

        server.verify()
        verify(exactly = 0) { authService.invalidate(any()) }
    }
}
