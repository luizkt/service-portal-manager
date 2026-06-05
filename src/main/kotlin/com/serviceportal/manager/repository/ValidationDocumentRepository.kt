package com.serviceportal.manager.repository

import com.serviceportal.manager.domain.ValidationDocument
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ValidationDocumentRepository : MongoRepository<ValidationDocument, String> {

    fun findAllByActiveTrue(pageable: Pageable): Page<ValidationDocument>

    fun findAllByActiveTrue(): List<ValidationDocument>

    fun findByValidationIdAndVersion(validationId: String, version: Int): ValidationDocument?

    fun existsByValidationIdAndVersion(validationId: String, version: Int): Boolean

    fun findAllByValidationId(validationId: String, sort: Sort): List<ValidationDocument>

    fun findAllByValidationIdAndActive(validationId: String, active: Boolean, sort: Sort): List<ValidationDocument>
}
