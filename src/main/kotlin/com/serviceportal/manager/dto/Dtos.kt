package com.serviceportal.manager.dto

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
