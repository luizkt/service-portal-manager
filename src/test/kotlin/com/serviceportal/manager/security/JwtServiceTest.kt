package com.serviceportal.manager.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class JwtServiceTest {

    private val secret = "0123456789012345678901234567890123456789012345678901234567890123ABC"
    private val service = JwtService(secret = secret, expirationSeconds = 3600, issuer = "manager-test")

    @Test @DisplayName("Gera token e extrai username")
    fun roundTrip() {
        val token = service.generateToken("admin")
        assertThat(service.extractUsername(token)).isEqualTo("admin")
        assertThat(service.isValid(token)).isTrue()
        assertThat(service.expirationSeconds()).isEqualTo(3600)
    }

    @Test @DisplayName("Token inválido falha validação sem lançar")
    fun tokenInvalido() {
        assertThat(service.isValid("not-a-token")).isFalse()
    }

    @Test @DisplayName("Token expirado retorna isValid=false")
    fun tokenExpirado() {
        val curto = JwtService(secret = secret, expirationSeconds = -1, issuer = "manager-test")
        val token = curto.generateToken("admin")
        assertThat(curto.isValid(token)).isFalse()
    }

    @Test @DisplayName("parseToken devolve claims completas")
    fun parseClaims() {
        val token = service.generateToken("user-x")
        val claims = service.parseToken(token)
        assertThat(claims.subject).isEqualTo("user-x")
        assertThat(claims.issuer).isEqualTo("manager-test")
    }
}
