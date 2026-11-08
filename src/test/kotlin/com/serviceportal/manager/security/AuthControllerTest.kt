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

import com.serviceportal.manager.dto.LoginRequest
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class AuthControllerTest {
    private val jwtService: JwtService = mockk()
    private val controller = AuthController(jwtService, "admin", "admin")

    @Test
    @DisplayName("Valid credentials -> 201 with token + expiresIn")
    fun validCredentials() {
        every { jwtService.generateToken("admin") } returns "tok-123"
        every { jwtService.expirationSeconds() } returns 3600

        val resp = controller.createToken(LoginRequest("admin", "admin"))

        assertThat(resp.statusCode.value()).isEqualTo(201)
        assertThat(resp.body!!.token).isEqualTo("tok-123")
        assertThat(resp.body!!.expiresIn).isEqualTo(3600)
    }

    @Test
    @DisplayName("Wrong username -> 401")
    fun wrongUsername() {
        val resp = controller.createToken(LoginRequest("other", "admin"))
        assertThat(resp.statusCode.value()).isEqualTo(401)
        assertThat(resp.body).isNull()
    }

    @Test
    @DisplayName("Wrong password -> 401")
    fun wrongPassword() {
        val resp = controller.createToken(LoginRequest("admin", "wrong"))
        assertThat(resp.statusCode.value()).isEqualTo(401)
    }

    @Test
    @DisplayName("Credentials come from properties and prefix/suffix variants are rejected")
    fun customCredentials() {
        val custom = AuthController(jwtService, "ops", "pässwörd")
        every { jwtService.generateToken("ops") } returns "tok-ops"
        every { jwtService.expirationSeconds() } returns 60

        assertThat(custom.createToken(LoginRequest("ops", "pässwörd")).statusCode.value()).isEqualTo(201)
        assertThat(custom.createToken(LoginRequest("ops", "pässwör")).statusCode.value()).isEqualTo(401)
        assertThat(custom.createToken(LoginRequest("ops", "pässwörd!")).statusCode.value()).isEqualTo(401)
        assertThat(custom.createToken(LoginRequest("admin", "admin")).statusCode.value()).isEqualTo(401)
    }
}
