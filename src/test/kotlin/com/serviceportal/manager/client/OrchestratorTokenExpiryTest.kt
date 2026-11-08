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
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount.times
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.time.Instant
import java.util.Base64

class OrchestratorTokenExpiryTest {
    private val loginUrl = "http://orchestrator:8080/api/auth/tokens"

    private fun jwt(exp: Long?): String {
        val enc = Base64.getUrlEncoder().withoutPadding()
        val payload = if (exp == null) """{"sub":"admin"}""" else """{"sub":"admin","exp":$exp}"""
        return enc.encodeToString("""{"alg":"HS256"}""".toByteArray()) + "." +
            enc.encodeToString(payload.toByteArray()) + ".sig"
    }

    private fun setup(): Pair<OrchestratorAuthService, MockRestServiceServer> {
        val builder = RestClient.builder().baseUrl("http://orchestrator:8080")
        val server = MockRestServiceServer.bindTo(builder).build()
        return OrchestratorAuthService(builder.build(), "admin", "admin") to server
    }

    private fun login(
        server: MockRestServiceServer,
        body: String,
        count: Int = 1,
    ) {
        server.expect(times(count), requestTo(loginUrl))
            .andRespond(withSuccess(body, MediaType.APPLICATION_JSON))
    }

    @Test
    @DisplayName("exp curto do JWT (dentro da margem de 60s) força novo login")
    fun shortExpRefreshesAgain() {
        val (service, server) = setup()
        val soon = Instant.now().plusSeconds(30).epochSecond
        login(server, """{"token":"${jwt(soon)}"}""", count = 2)

        service.token()
        service.token()
        server.verify()
    }

    @Test
    @DisplayName("exp longo do JWT mantém o token em cache")
    fun longExpCaches() {
        val (service, server) = setup()
        val later = Instant.now().plusSeconds(7200).epochSecond
        login(server, """{"token":"${jwt(later)}"}""")

        service.token()
        service.token()
        server.verify()
    }

    @Test
    @DisplayName("sem exp no JWT usa expiresIn da resposta")
    fun usesExpiresIn() {
        val (service, server) = setup()
        login(server, """{"token":"${jwt(null)}","expiresIn":30}""", count = 2)

        service.token()
        service.token()
        server.verify()
    }

    @Test
    @DisplayName("sem exp nem expiresIn (token opaco) cai para 3600s")
    fun fallsBackTo3600() {
        val (service, server) = setup()
        login(server, """{"token":"opaque"}""")

        service.token()
        service.token()
        server.verify()
    }

    @Test
    @DisplayName("invalidate descarta o token só se ainda for o que falhou")
    fun invalidateOnlyIfSame() {
        val (service, server) = setup()
        login(server, """{"token":"t1"}""")
        login(server, """{"token":"t2"}""")
        assertThat(service.token()).isEqualTo("t1")

        service.invalidate("outro-token")
        assertThat(service.token()).isEqualTo("t1")

        service.invalidate("t1")
        assertThat(service.token()).isEqualTo("t2")
        server.verify()
    }
}
