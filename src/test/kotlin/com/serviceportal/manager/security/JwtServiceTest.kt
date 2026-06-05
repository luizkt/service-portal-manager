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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class JwtServiceTest {
    private val secret = "0123456789012345678901234567890123456789012345678901234567890123ABC"
    private val service = JwtService(secret = secret, expirationSeconds = 3600, issuer = "manager-test")

    @Test
    @DisplayName("Gera token e extrai username")
    fun roundTrip() {
        val token = service.generateToken("admin")
        assertThat(service.extractUsername(token)).isEqualTo("admin")
        assertThat(service.isValid(token)).isTrue()
        assertThat(service.expirationSeconds()).isEqualTo(3600)
    }

    @Test
    @DisplayName("Token inválido falha validação sem lançar")
    fun tokenInvalido() {
        assertThat(service.isValid("not-a-token")).isFalse()
    }

    @Test
    @DisplayName("Token expirado retorna isValid=false")
    fun tokenExpirado() {
        val curto = JwtService(secret = secret, expirationSeconds = -1, issuer = "manager-test")
        val token = curto.generateToken("admin")
        assertThat(curto.isValid(token)).isFalse()
    }

    @Test
    @DisplayName("parseToken devolve claims completas")
    fun parseClaims() {
        val token = service.generateToken("user-x")
        val claims = service.parseToken(token)
        assertThat(claims.subject).isEqualTo("user-x")
        assertThat(claims.issuer).isEqualTo("manager-test")
    }
}
