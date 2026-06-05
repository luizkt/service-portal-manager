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
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.web.client.RestClient

class OrchestratorCacheClientTest {
    private val authService: OrchestratorAuthService = mockk()

    private fun setup(): Pair<OrchestratorCacheClient, MockRestServiceServer> {
        val builder = RestClient.builder().baseUrl("http://orchestrator:8080")
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = OrchestratorCacheClient(builder.build(), authService)
        return client to server
    }

    @Test
    @DisplayName("evictWorkflow chama DELETE no endpoint admin com Bearer token")
    fun evictsWithBearer() {
        every { authService.token() } returns "jwt-abc"
        val (client, server) = setup()
        server.expect(requestTo("http://orchestrator:8080/api/admin/cache/workflows/create-order-v1/versions/1.0.0"))
            .andExpect(method(HttpMethod.DELETE))
            .andExpect(header("Authorization", "Bearer jwt-abc"))
            .andRespond(withStatus(HttpStatus.NO_CONTENT))

        client.evictWorkflow("create-order-v1", "1.0.0")
        server.verify()
    }

    @Test
    @DisplayName("evictWorkflow engole erro do orquestrador (não propaga)")
    fun swallowsServerError() {
        every { authService.token() } returns "jwt-abc"
        val (client, server) = setup()
        server.expect(requestTo("http://orchestrator:8080/api/admin/cache/workflows/f/versions/1.0"))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR))

        assertThatCode { client.evictWorkflow("f", "1.0") }.doesNotThrowAnyException()
    }

    @Test
    @DisplayName("evictWorkflow engole falha de autenticação (não propaga)")
    fun swallowsAuthFailure() {
        every { authService.token() } throws IllegalStateException("no token")
        val (client, _) = setup()

        assertThatCode { client.evictWorkflow("f", "1.0") }.doesNotThrowAnyException()
    }
}
