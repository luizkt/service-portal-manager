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
    @Value("\${manager.security.admin.password:admin}") private val adminPass: String
) {

    @PostMapping("/tokens")
    fun createToken(@RequestBody request: LoginRequest): ResponseEntity<LoginResponse> {
        if (request.username != adminUser || request.password != adminPass) {
            return ResponseEntity.status(401).build()
        }
        val token = jwtService.generateToken(request.username)
        return ResponseEntity.status(HttpStatus.CREATED).body(
            LoginResponse(token, jwtService.expirationSeconds())
        )
    }
}
