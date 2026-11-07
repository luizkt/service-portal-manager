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
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class FlowExampleDto(
    @Schema(description = "Exemplo de payload gerado a partir dos campos do contrato")
    val example: Map<String, Any?>,
    @Schema(description = "Campos do contrato com nome, tipo e obrigatoriedade")
    val fields: List<ContractField>,
)

/** Resposta resumida de um fluxo — usada nos GETs e na lista de ativos. */
data class FlowSummaryDto(
    @Schema(description = "Identificador único do flow", example = "create-order-v1")
    val flowId: String,
    @Schema(description = "Versão SemVer gerada automaticamente no PUT", example = "1.0.0")
    val version: String,
    @Schema(description = "Descrição legível do flow", example = "Fluxo de criação de pedido")
    val description: String?,
    @Schema(description = "Se false, foi desativado por uma atualização (versão antiga)", example = "true")
    val active: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

/** Login JWT (mesmo schema compartilhado com o orquestrador). */
data class LoginRequest(
    @Schema(description = "Nome de usuário", example = "admin")
    val username: String,
    @Schema(description = "Senha do usuário", example = "admin")
    val password: String,
)

data class LoginResponse(
    @Schema(description = "JWT HS512", example = "eyJhbGciOiJIUzUxMiJ9...")
    val token: String,
    @Schema(description = "Tempo de expiração em segundos", example = "3600")
    val expiresIn: Long,
)

// --- Integration ---

data class IntegrationRequest(
    @Schema(description = "Identificador único da integração", example = "validate-client")
    val integrationId: String,
    @Schema(description = "Tipo da integração", allowableValues = ["HTTP"], example = "HTTP")
    val type: String = "HTTP",
    @Schema(
        description = "URL com suporte a templates {{contract.field}}",
        example = "http://api.exemplo.com/clients/{{contract.clientId}}",
    )
    val url: String,
    @Schema(description = "Método HTTP", allowableValues = ["GET", "POST", "PUT", "DELETE", "PATCH"], example = "GET")
    val method: String,
    val headers: Map<String, String> = emptyMap(),
    @Schema(description = "Timeout em milissegundos", example = "5000")
    val timeout: Int = 5000,
    val bodyTemplate: String? = null,
    val responseBody: Map<String, Any>? = null,
)

data class IntegrationDto(
    @Schema(description = "Identificador único da integração", example = "validate-client")
    val integrationId: String,
    @Schema(description = "Versão da integração (auto-incremento)", example = "1")
    val version: Int,
    @Schema(description = "Tipo da integração", example = "HTTP")
    val type: String,
    @Schema(
        description = "URL com suporte a templates {{contract.field}}",
        example = "http://api.exemplo.com/clients/{{contract.clientId}}",
    )
    val url: String,
    @Schema(description = "Método HTTP", example = "GET")
    val method: String,
    val headers: Map<String, String>,
    @Schema(description = "Timeout em milissegundos", example = "5000")
    val timeout: Int,
    val bodyTemplate: String?,
    val responseBody: Map<String, Any>?,
    @Schema(description = "Se false, foi substituída por uma versão mais recente", example = "true")
    val active: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

// --- Contract ---

data class ContractRequest(
    @Schema(description = "Identificador único do contrato", example = "create-order")
    val contractId: String,
    @Schema(description = "Campos do contrato com nome, tipo e obrigatoriedade")
    val fields: List<ContractField> = emptyList(),
)

data class ContractDto(
    @Schema(description = "Identificador único do contrato", example = "create-order")
    val contractId: String,
    @Schema(description = "Versão do contrato (auto-incremento)", example = "1")
    val version: Int,
    @Schema(description = "Campos do contrato")
    val fields: List<ContractField>,
    @Schema(description = "Se false, foi substituído por uma versão mais recente", example = "true")
    val active: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

// --- Validation ---

data class ValidationRequest(
    @Schema(description = "Identificador único da validação", example = "check-credit-limit")
    val validationId: String,
    @Schema(description = "Tipo da validação", allowableValues = ["HTTP"], example = "HTTP")
    val type: String = "HTTP",
    @Schema(description = "URL do endpoint de validação", example = "http://api.exemplo.com/credit/{{contract.clientId}}")
    val url: String,
    @Schema(description = "Método HTTP", allowableValues = ["GET", "POST", "PUT", "DELETE", "PATCH"], example = "GET")
    val method: String,
    val headers: Map<String, String> = emptyMap(),
    @Schema(description = "Timeout em milissegundos", example = "5000")
    val timeout: Int = 5000,
    val bodyTemplate: String? = null,
    val responseBody: Map<String, Any>? = null,
)

data class ValidationDto(
    @Schema(description = "Identificador único da validação", example = "check-credit-limit")
    val validationId: String,
    @Schema(description = "Versão da validação (auto-incremento)", example = "1")
    val version: Int,
    @Schema(description = "Tipo da validação", example = "HTTP")
    val type: String,
    @Schema(description = "URL do endpoint de validação", example = "http://api.exemplo.com/credit/{{contract.clientId}}")
    val url: String,
    @Schema(description = "Método HTTP", example = "GET")
    val method: String,
    val headers: Map<String, String>,
    @Schema(description = "Timeout em milissegundos", example = "5000")
    val timeout: Int,
    val bodyTemplate: String?,
    val responseBody: Map<String, Any>?,
    @Schema(description = "Se false, foi substituída por uma versão mais recente", example = "true")
    val active: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
