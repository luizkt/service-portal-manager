package com.serviceportal.manager.security

import com.serviceportal.manager.dto.LoginRequest
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class AuthControllerTest {

    private val jwtService: JwtService = mockk()
    private val controller = AuthController(jwtService, "admin", "admin")

    @Test @DisplayName("Valid credentials -> 201 with token + expiresIn")
    fun validCredentials() {
        every { jwtService.generateToken("admin") } returns "tok-123"
        every { jwtService.expirationSeconds() } returns 3600

        val resp = controller.createToken(LoginRequest("admin", "admin"))

        assertThat(resp.statusCode.value()).isEqualTo(201)
        assertThat(resp.body!!.token).isEqualTo("tok-123")
        assertThat(resp.body!!.expiresIn).isEqualTo(3600)
    }

    @Test @DisplayName("Wrong username -> 401")
    fun wrongUsername() {
        val resp = controller.createToken(LoginRequest("other", "admin"))
        assertThat(resp.statusCode.value()).isEqualTo(401)
        assertThat(resp.body).isNull()
    }

    @Test @DisplayName("Wrong password -> 401")
    fun wrongPassword() {
        val resp = controller.createToken(LoginRequest("admin", "wrong"))
        assertThat(resp.statusCode.value()).isEqualTo(401)
    }
}
