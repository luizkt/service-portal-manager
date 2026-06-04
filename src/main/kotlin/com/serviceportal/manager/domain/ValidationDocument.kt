package com.serviceportal.manager.domain

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document(collection = "validations")
data class ValidationDocument(
    @Id
    var mongoId: String? = null,

    var validationId: String,

    var version: Int,

    var type: String = "HTTP",

    var url: String,

    var method: String,

    var headers: Map<String, String> = emptyMap(),

    var timeout: Int = 5000,

    var bodyTemplate: String? = null,

    var responseBody: Map<String, Any>? = null,

    var active: Boolean = true,

    var createdAt: LocalDateTime = LocalDateTime.now(),

    var updatedAt: LocalDateTime = LocalDateTime.now()
)
