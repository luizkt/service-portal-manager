package com.serviceportal.manager.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.util.Date
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${manager.security.jwt.secret}") private val secret: String,
    @Value("\${manager.security.jwt.expiration-seconds:3600}") private val expirationSeconds: Long,
    @Value("\${manager.security.jwt.issuer:service-portal-manager}") private val issuer: String
) {

    private fun key(): SecretKey =
        Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))

    fun generateToken(username: String): String {
        val now = Date()
        val exp = Date(now.time + expirationSeconds * 1000)
        return Jwts.builder()
            .subject(username)
            .issuer(issuer)
            .issuedAt(now)
            .expiration(exp)
            .signWith(key(), Jwts.SIG.HS512)
            .compact()
    }

    fun parseToken(token: String): Claims = Jwts.parser()
        .verifyWith(key())
        .build()
        .parseSignedClaims(token)
        .payload

    fun extractUsername(token: String): String = parseToken(token).subject

    fun isValid(token: String): Boolean = try {
        parseToken(token).expiration.after(Date())
    } catch (_: Exception) {
        false
    }

    fun expirationSeconds(): Long = expirationSeconds
}
