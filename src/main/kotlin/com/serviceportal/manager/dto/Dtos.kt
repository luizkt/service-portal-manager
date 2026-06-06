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
package com.serviceportal.manager.dto

import com.serviceportal.manager.domain.ContractField
import java.time.LocalDateTime

data class FlowExampleDto(
    val example: Map<String, Any?>,
    val fields: List<ContractField>,
)

/** Resposta resumida de um fluxo — usada nos GETs e na lista de ativos. */
data class FlowSummaryDto(
    val flowId: String,
    val version: String,
    val description: String?,
    val active: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

/** Login JWT (mesmo schema compartilhado com o orquestrador). */
data class LoginRequest(
    val username: String,
    val password: String,
)

data class LoginResponse(
    val token: String,
    val expiresIn: Long,
)

// --- Integration ---

data class IntegrationRequest(
    val integrationId: String,
    val type: String = "HTTP",
    val url: String,
    val method: String,
    val headers: Map<String, String> = emptyMap(),
    val timeout: Int = 5000,
    val bodyTemplate: String? = null,
    val responseBody: Map<String, Any>? = null,
)

data class IntegrationDto(
    val integrationId: String,
    val version: Int,
    val type: String,
    val url: String,
    val method: String,
    val headers: Map<String, String>,
    val timeout: Int,
    val bodyTemplate: String?,
    val responseBody: Map<String, Any>?,
    val active: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

// --- Contract ---

data class ContractRequest(
    val contractId: String,
    val fields: List<ContractField> = emptyList(),
)

data class ContractDto(
    val contractId: String,
    val version: Int,
    val fields: List<ContractField>,
    val active: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

// --- Validation ---

data class ValidationRequest(
    val validationId: String,
    val type: String = "HTTP",
    val url: String,
    val method: String,
    val headers: Map<String, String> = emptyMap(),
    val timeout: Int = 5000,
    val bodyTemplate: String? = null,
    val responseBody: Map<String, Any>? = null,
)

data class ValidationDto(
    val validationId: String,
    val version: Int,
    val type: String,
    val url: String,
    val method: String,
    val headers: Map<String, String>,
    val timeout: Int,
    val bodyTemplate: String?,
    val responseBody: Map<String, Any>?,
    val active: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
