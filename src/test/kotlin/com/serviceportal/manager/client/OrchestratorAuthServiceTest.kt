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

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

class OrchestratorAuthServiceTest {
    private fun setup(): Pair<OrchestratorAuthService, MockRestServiceServer> {
        val builder = RestClient.builder().baseUrl("http://orchestrator:8080")
        val server = MockRestServiceServer.bindTo(builder).build()
        val service = OrchestratorAuthService(builder.build(), "admin", "admin")
        return service to server
    }

    @Test
    @DisplayName("token() faz login no orquestrador e devolve o JWT")
    fun fetchesToken() {
        val (service, server) = setup()
        server.expect(requestTo("http://orchestrator:8080/api/auth/tokens"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess("""{"token":"jwt-abc","type":"Bearer"}""", MediaType.APPLICATION_JSON))

        assertThat(service.token()).isEqualTo("jwt-abc")
        server.verify()
    }

    @Test
    @DisplayName("token() reaproveita o token em cache (sem novo login)")
    fun cachesToken() {
        val (service, server) = setup()
        server.expect(requestTo("http://orchestrator:8080/api/auth/tokens"))
            .andRespond(withSuccess("""{"token":"jwt-abc","type":"Bearer"}""", MediaType.APPLICATION_JSON))

        // Uma única expectativa registrada: a segunda chamada não pode bater no servidor.
        assertThat(service.token()).isEqualTo("jwt-abc")
        assertThat(service.token()).isEqualTo("jwt-abc")
        server.verify()
    }

    @Test
    @DisplayName("token() lança quando o orquestrador devolve corpo vazio")
    fun throwsOnEmptyBody() {
        val (service, server) = setup()
        server.expect(requestTo("http://orchestrator:8080/api/auth/tokens"))
            .andRespond(withStatus(org.springframework.http.HttpStatus.OK))

        assertThatThrownBy { service.token() }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    @DisplayName("token() lança quando a resposta não contém o campo token")
    fun throwsWhenTokenMissing() {
        val (service, server) = setup()
        server.expect(requestTo("http://orchestrator:8080/api/auth/tokens"))
            .andRespond(withSuccess("""{"type":"Bearer"}""", MediaType.APPLICATION_JSON))

        assertThatThrownBy { service.token() }
            .isInstanceOf(IllegalStateException::class.java)
    }
}
