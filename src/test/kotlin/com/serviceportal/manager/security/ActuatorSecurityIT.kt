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
package com.serviceportal.manager.security

import com.ninjasquad.springmockk.MockkBean
import com.serviceportal.manager.repository.ContractDocumentRepository
import com.serviceportal.manager.repository.FlowDocumentRepository
import com.serviceportal.manager.repository.IntegrationDocumentRepository
import com.serviceportal.manager.repository.ValidationDocumentRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability
@TestPropertySource(
    properties = [
        "manager.security.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123ABCD",
        "manager.security.jwt.expiration-seconds=3600",
        "manager.security.jwt.issuer=manager-test",
        "manager.security.actuator.username=ops",
        "manager.security.actuator.password=s3cret-actuator",
        "spring.data.mongodb.uri=mongodb://localhost:27017/test-db",
        "spring.data.mongodb.auto-index-creation=false",
        "spring.autoconfigure.exclude=" +
            "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration," +
            "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration," +
            "org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration",
    ],
)
class ActuatorSecurityIT {
    @Autowired private lateinit var mvc: MockMvc

    @Autowired private lateinit var jwtService: JwtService

    @MockkBean(relaxed = true)
    private lateinit var repo: FlowDocumentRepository

    @MockkBean(relaxed = true)
    private lateinit var integrationRepo: IntegrationDocumentRepository

    @MockkBean(relaxed = true)
    private lateinit var contractRepo: ContractDocumentRepository

    @MockkBean(relaxed = true)
    private lateinit var validationRepo: ValidationDocumentRepository

    @Test
    @DisplayName("health and prometheus are public")
    fun publicEndpoints() {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk)
        mvc.perform(get("/actuator/prometheus")).andExpect(status().isOk)
    }

    @Test
    @DisplayName("env and loggers require authentication")
    fun protectedEndpointsRejectAnonymous() {
        mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized)
        mvc.perform(get("/actuator/loggers")).andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("heapdump is not exposed even with credentials")
    fun heapdumpNotExposed() {
        mvc.perform(get("/actuator/heapdump").with(httpBasic("ops", "s3cret-actuator")))
            .andExpect { assertThat(it.response.status).isNotEqualTo(200) }
    }

    @Test
    @DisplayName("ACTUATOR user with Basic credentials reads env and loggers")
    fun basicCredentialsOpenProtectedEndpoints() {
        mvc.perform(get("/actuator/env").with(httpBasic("ops", "s3cret-actuator"))).andExpect(status().isOk)
        mvc.perform(get("/actuator/loggers").with(httpBasic("ops", "s3cret-actuator"))).andExpect(status().isOk)
    }

    @Test
    @DisplayName("wrong credentials are rejected")
    fun wrongCredentials() {
        mvc.perform(get("/actuator/env").with(httpBasic("ops", "wrong"))).andExpect(status().isUnauthorized)
        mvc.perform(get("/actuator/env").with(httpBasic("admin", "admin"))).andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("a Bearer JWT does not open actuator endpoints")
    fun bearerDoesNotOpenActuator() {
        val token = jwtService.generateToken("admin")
        mvc.perform(get("/actuator/env").header("Authorization", "Bearer $token"))
            .andExpect(status().isUnauthorized)
    }
}
