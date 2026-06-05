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
import com.serviceportal.manager.dto.LoginResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * `POST /api/auth/tokens` cria um novo JWT — REST-shape (em vez do antigo `/login`).
 *
 * Autenticação simplificada para fins de demonstração — mesmo padrão do
 * generic-orchestrator. Em produção: integrar com banco de usuários + BCrypt.
 */
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val jwtService: JwtService,
    @Value("\${manager.security.admin.username:admin}") private val adminUser: String,
    @Value("\${manager.security.admin.password:admin}") private val adminPass: String,
) {
    @PostMapping("/tokens")
    fun createToken(
        @RequestBody request: LoginRequest,
    ): ResponseEntity<LoginResponse> {
        if (request.username != adminUser || request.password != adminPass) {
            return ResponseEntity.status(401).build()
        }
        val token = jwtService.generateToken(request.username)
        return ResponseEntity.status(HttpStatus.CREATED).body(
            LoginResponse(token, jwtService.expirationSeconds()),
        )
    }
}
