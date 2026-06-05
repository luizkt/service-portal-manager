package com.serviceportal.manager.repository

import com.serviceportal.manager.domain.IntegrationDocument
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface IntegrationDocumentRepository : MongoRepository<IntegrationDocument, String> {

    fun findAllByActiveTrue(pageable: Pageable): Page<IntegrationDocument>

    fun findAllByActiveTrue(): List<IntegrationDocument>

    fun findByIntegrationIdAndVersion(integrationId: String, version: Int): IntegrationDocument?

    fun existsByIntegrationIdAndVersion(integrationId: String, version: Int): Boolean

    fun findAllByIntegrationId(integrationId: String, sort: Sort): List<IntegrationDocument>

    fun findAllByIntegrationIdAndActive(integrationId: String, active: Boolean, sort: Sort): List<IntegrationDocument>
}
