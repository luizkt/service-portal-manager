package com.serviceportal.manager.repository

import com.serviceportal.manager.domain.ContractDocument
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ContractDocumentRepository : MongoRepository<ContractDocument, String> {

    fun findAllByActiveTrue(pageable: Pageable): Page<ContractDocument>

    fun findAllByActiveTrue(): List<ContractDocument>

    fun findByContractIdAndVersion(contractId: String, version: Int): ContractDocument?

    fun existsByContractIdAndVersion(contractId: String, version: Int): Boolean

    fun findAllByContractId(contractId: String, sort: Sort): List<ContractDocument>

    fun findAllByContractIdAndActive(contractId: String, active: Boolean, sort: Sort): List<ContractDocument>
}
