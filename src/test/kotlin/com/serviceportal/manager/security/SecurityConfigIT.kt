package com.serviceportal.manager.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import com.serviceportal.manager.dto.LoginRequest
import com.serviceportal.manager.repository.FlowDocumentRepository
import io.mockk.every
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = [
    "manager.security.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123ABCD",
    "manager.security.jwt.expiration-seconds=3600",
    "manager.security.jwt.issuer=manager-test",
    "spring.data.mongodb.uri=mongodb://localhost:27017/test-db",
    "spring.data.mongodb.auto-index-creation=false",
    "spring.autoconfigure.exclude=" +
            "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration," +
            "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration," +
            "org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration"
])
class SecurityConfigIT {

    @Autowired private lateinit var mvc: MockMvc
    @Autowired private lateinit var jwtService: JwtService
    @Autowired private lateinit var jackson: ObjectMapper

    // Repositório mockado — não precisamos de Mongo real para validar a security
    @MockkBean(relaxed = true)
    private lateinit var repo: FlowDocumentRepository

    @Test @DisplayName("/manager/flows sem token -> 401")
    fun semToken() {
        mvc.perform(get("/manager/flows")).andExpect(status().isUnauthorized)
    }

    @Test @DisplayName("/api/auth/login é público")
    fun loginPublico() {
        mvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jackson.writeValueAsString(LoginRequest("admin", "admin")))
        ).andExpect(status().isOk).andExpect(jsonPath("$.token").exists())
    }

    @Test @DisplayName("/actuator/health é público")
    fun healthPublico() {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk)
    }

    @Test @DisplayName("Bearer token válido libera /manager/flows")
    fun comToken() {
        every { repo.findAll(any<Pageable>()) } returns PageImpl(emptyList(), Pageable.unpaged(), 0)
        val token = jwtService.generateToken("admin")
        mvc.perform(get("/manager/flows").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
    }
}
