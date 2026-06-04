package com.serviceportal.manager.dto

import com.serviceportal.manager.domain.ContractField
import java.time.LocalDateTime

/** Resposta resumida de um fluxo — usada nos GETs e na lista de ativos. */
data class FlowSummaryDto(
    val flowId: String,
    val version: String,
    val description: String?,
    val active: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

/** Login JWT (mesmo schema compartilhado com o orquestrador). */
data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val expiresIn: Long
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
    val responseBody: Map<String, Any>? = null
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
    val updatedAt: LocalDateTime
)

// --- Contract ---

data class ContractRequest(
    val contractId: String,
    val fields: List<ContractField> = emptyList()
)

data class ContractDto(
    val contractId: String,
    val version: Int,
    val fields: List<ContractField>,
    val active: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
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
    val responseBody: Map<String, Any>? = null
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
    val updatedAt: LocalDateTime
)
