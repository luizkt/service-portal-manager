package com.serviceportal.manager.domain

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document(collection = "contracts")
data class ContractDocument(
    @Id
    var mongoId: String? = null,

    var contractId: String,

    var version: Int,

    var fields: List<ContractField> = emptyList(),

    var active: Boolean = true,

    var createdAt: LocalDateTime = LocalDateTime.now(),

    var updatedAt: LocalDateTime = LocalDateTime.now()
)

data class ContractField(
    val name: String,
    val type: String,
    val required: Boolean,
    val validations: List<FieldValidation> = emptyList()
)

data class FieldValidation(
    val type: String,
    val value: String? = null,
    val message: String? = null
)
