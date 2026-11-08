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

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.User
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint

/**
 * Chain dedicada ao prefixo `/actuator`, avaliada antes da chain JWT da API.
 *
 * Públicos: `health`, `info` e `prometheus` (o Prometheus coleta sem credenciais). O resto exige
 * HTTP Basic do usuário com role `ACTUATOR`. Sem senha default: com
 * `manager.security.actuator.password` vazio não existe usuário e os protegidos sempre dão 401.
 * Um Bearer JWT nunca abre esses endpoints.
 */
@Configuration
class ActuatorSecurityConfig {
    @Bean
    @Order(1)
    fun actuatorSecurityFilterChain(
        http: HttpSecurity,
        @Value("\${manager.security.actuator.username:actuator}") username: String,
        @Value("\${manager.security.actuator.password:}") password: String,
    ): SecurityFilterChain {
        val users = InMemoryUserDetailsManager()
        if (password.isNotEmpty()) {
            users.createUser(
                User.withUsername(username)
                    .password(PasswordEncoderFactories.createDelegatingPasswordEncoder().encode(password))
                    .roles("ACTUATOR")
                    .build(),
            )
        }
        http
            .securityMatcher("/actuator/**")
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .userDetailsService(users)
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info", "/actuator/prometheus")
                    .permitAll()
                    .anyRequest().hasRole("ACTUATOR")
            }
            .httpBasic { it.authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)) }
        return http.build()
    }
}
