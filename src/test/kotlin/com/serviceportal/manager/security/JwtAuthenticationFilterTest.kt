package com.serviceportal.manager.security

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.FilterChain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder

class JwtAuthenticationFilterTest {

    private val jwtService: JwtService = mockk()
    private val filter = JwtAuthenticationFilter(jwtService)

    @AfterEach
    fun cleanup() {
        SecurityContextHolder.clearContext()
    }

    private fun req(authHeader: String?) = MockHttpServletRequest().apply {
        if (authHeader != null) addHeader("Authorization", authHeader)
    }

    @Test @DisplayName("Sem Authorization header — chain segue, sem auth no contexto")
    fun semHeader() {
        val chain: FilterChain = mockk(relaxed = true)
        filter.doFilter(req(null), MockHttpServletResponse(), chain)

        verify(exactly = 1) { chain.doFilter(any(), any()) }
        assertThat(SecurityContextHolder.getContext().authentication).isNull()
    }

    @Test @DisplayName("Header sem Bearer — chain segue, sem auth")
    fun headerSemBearer() {
        val chain: FilterChain = mockk(relaxed = true)
        filter.doFilter(req("Basic xyz"), MockHttpServletResponse(), chain)
        assertThat(SecurityContextHolder.getContext().authentication).isNull()
    }

    @Test @DisplayName("Token válido — popula SecurityContext com ROLE_USER")
    fun tokenValido() {
        every { jwtService.isValid("tok") } returns true
        every { jwtService.extractUsername("tok") } returns "admin"

        filter.doFilter(req("Bearer tok"), MockHttpServletResponse(), mockk(relaxed = true))

        val auth = SecurityContextHolder.getContext().authentication
        assertThat(auth).isNotNull
        assertThat(auth.name).isEqualTo("admin")
        assertThat(auth.authorities.map { it.authority }).contains("ROLE_USER")
    }

    @Test @DisplayName("Token inválido — não popula contexto")
    fun tokenInvalido() {
        every { jwtService.isValid("bad") } returns false

        filter.doFilter(req("Bearer bad"), MockHttpServletResponse(), mockk(relaxed = true))

        assertThat(SecurityContextHolder.getContext().authentication).isNull()
    }

    @Test @DisplayName("Exception ao validar — não popula contexto e não propaga")
    fun excecaoNaValidacao() {
        every { jwtService.isValid("boom") } throws RuntimeException("erro")

        filter.doFilter(req("Bearer boom"), MockHttpServletResponse(), mockk(relaxed = true))

        assertThat(SecurityContextHolder.getContext().authentication).isNull()
    }
}
