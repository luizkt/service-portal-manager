package com.serviceportal.manager.dto

import java.time.LocalDateTime

/** Resposta resumida de um fluxo — usada nos GETs e na lista de ativos. */
data class FlowSummaryDto(
    val flowId: String,
    val versao: String,
    val descricao: String?,
    val ativo: Boolean,
    val criadoEm: LocalDateTime,
    val atualizadoEm: LocalDateTime
)

/** Login JWT (compatível com o esquema do generic-orchestrator). */
data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val expiresIn: Long
)
