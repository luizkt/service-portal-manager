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
    @Value("\${manager.security.jwt.issuer:service-portal-manager}") private val issuer: String,
) {
    private fun key(): SecretKey = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))

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

    fun parseToken(token: String): Claims =
        Jwts.parser()
            .verifyWith(key())
            .build()
            .parseSignedClaims(token)
            .payload

    fun extractUsername(token: String): String = parseToken(token).subject

    fun isValid(token: String): Boolean =
        try {
            parseToken(token).expiration.after(Date())
        } catch (_: Exception) {
            false
        }

    fun expirationSeconds(): Long = expirationSeconds
}
