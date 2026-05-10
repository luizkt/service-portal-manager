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

    @Test @DisplayName("login válido devolve token + expiresIn")
    fun loginValido() {
        every { jwtService.generateToken("admin") } returns "tok-123"
        every { jwtService.expirationSeconds() } returns 3600

        val resp = controller.login(LoginRequest("admin", "admin"))

        assertThat(resp.statusCode.value()).isEqualTo(200)
        assertThat(resp.body!!.token).isEqualTo("tok-123")
        assertThat(resp.body!!.expiresIn).isEqualTo(3600)
    }

    @Test @DisplayName("usuário errado retorna 401")
    fun usuarioErrado() {
        val resp = controller.login(LoginRequest("outro", "admin"))
        assertThat(resp.statusCode.value()).isEqualTo(401)
        assertThat(resp.body).isNull()
    }

    @Test @DisplayName("senha errada retorna 401")
    fun senhaErrada() {
        val resp = controller.login(LoginRequest("admin", "errado"))
        assertThat(resp.statusCode.value()).isEqualTo(401)
    }
}
