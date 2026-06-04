package com.serviceportal.manager.service

import com.serviceportal.manager.domain.ContractDocument
import com.serviceportal.manager.domain.ContractField
import com.serviceportal.manager.dto.ContractRequest
import com.serviceportal.manager.exception.ContractAlreadyExistsException
import com.serviceportal.manager.exception.ContractNotFoundException
import com.serviceportal.manager.exception.InvalidContractException
import com.serviceportal.manager.repository.ContractDocumentRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.LocalDateTime

class ContractServiceTest {

    private val repository = mockk<ContractDocumentRepository>(relaxUnitFun = true)
    private val versioning = SequentialVersioningService()
    private val service = ContractService(repository, versioning)

    private val now = LocalDateTime.now()
    private val field = ContractField(name = "clientId", type = "STRING", required = true)
    private val doc = ContractDocument(
        mongoId = "id1", contractId = "ctr-1", version = 1,
        fields = listOf(field), active = true, createdAt = now, updatedAt = now
    )
    private val request = ContractRequest(contractId = "ctr-1", fields = listOf(field))

    @Test fun `create saves with version 1`() {
        every { repository.existsByContractIdAndVersion("ctr-1", 1) } returns false
        every { repository.save(any()) } returns doc
        val result = service.create(request)
        assertThat(result.contractId).isEqualTo("ctr-1")
        assertThat(result.version).isEqualTo(1)
    }

    @Test fun `create throws conflict when version 1 already exists`() {
        every { repository.existsByContractIdAndVersion("ctr-1", 1) } returns true
        assertThatThrownBy { service.create(request) }
            .isInstanceOf(ContractAlreadyExistsException::class.java)
    }

    @Test fun `update creates next version`() {
        val v2doc = doc.copy(version = 2)
        every { repository.findByContractIdAndVersion("ctr-1", 1) } returns doc
        every { repository.existsByContractIdAndVersion("ctr-1", 2) } returns false
        every { repository.save(any()) } returns v2doc
        val result = service.update("ctr-1", 1, request)
        assertThat(result.version).isEqualTo(2)
    }

    @Test fun `update throws not found when contract missing`() {
        every { repository.findByContractIdAndVersion("ctr-1", 1) } returns null
        assertThatThrownBy { service.update("ctr-1", 1, request) }
            .isInstanceOf(ContractNotFoundException::class.java)
    }

    @Test fun `update throws invalid when id mismatch`() {
        val mismatch = request.copy(contractId = "other")
        assertThatThrownBy { service.update("ctr-1", 1, mismatch) }
            .isInstanceOf(InvalidContractException::class.java)
    }

    @Test fun `update throws conflict when next version exists`() {
        every { repository.findByContractIdAndVersion("ctr-1", 1) } returns doc
        every { repository.existsByContractIdAndVersion("ctr-1", 2) } returns true
        assertThatThrownBy { service.update("ctr-1", 1, request) }
            .isInstanceOf(ContractAlreadyExistsException::class.java)
    }

    @Test fun `get returns dto when found`() {
        every { repository.findByContractIdAndVersion("ctr-1", 1) } returns doc
        assertThat(service.get("ctr-1", 1).contractId).isEqualTo("ctr-1")
    }

    @Test fun `get throws not found when missing`() {
        every { repository.findByContractIdAndVersion("ctr-1", 1) } returns null
        assertThatThrownBy { service.get("ctr-1", 1) }
            .isInstanceOf(ContractNotFoundException::class.java)
    }

    @Test fun `listAll returns active page`() {
        val pageable = PageRequest.of(0, 20)
        every { repository.findAllByActiveTrue(pageable) } returns PageImpl(listOf(doc))
        assertThat(service.listAll(pageable).content).hasSize(1)
    }

    @Test fun `listActive returns active list`() {
        every { repository.findAllByActiveTrue() } returns listOf(doc)
        assertThat(service.listActive()).hasSize(1)
    }

    @Test fun `listVersions returns all versions`() {
        val sort = Sort.by(Sort.Direction.ASC, "version")
        every { repository.findAllByContractId("ctr-1", sort) } returns listOf(doc)
        assertThat(service.listVersions("ctr-1", null)).hasSize(1)
    }

    @Test fun `listVersions active filters`() {
        val sort = Sort.by(Sort.Direction.ASC, "version")
        every { repository.findAllByContractIdAndActive("ctr-1", true, sort) } returns listOf(doc)
        assertThat(service.listVersions("ctr-1", "active")).hasSize(1)
    }

    @Test fun `listVersions inactive filters`() {
        val sort = Sort.by(Sort.Direction.ASC, "version")
        every { repository.findAllByContractIdAndActive("ctr-1", false, sort) } returns emptyList()
        assertThat(service.listVersions("ctr-1", "inactive")).isEmpty()
    }

    @Test fun `deactivate sets active false`() {
        every { repository.findByContractIdAndVersion("ctr-1", 1) } returns doc.copy(active = true)
        every { repository.save(any()) } returns doc.copy(active = false)
        service.deactivate("ctr-1", 1)
        verify { repository.save(match { !it.active }) }
    }

    @Test fun `deactivate is idempotent when already inactive`() {
        every { repository.findByContractIdAndVersion("ctr-1", 1) } returns doc.copy(active = false)
        service.deactivate("ctr-1", 1)
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test fun `deactivate throws not found when missing`() {
        every { repository.findByContractIdAndVersion("ctr-1", 1) } returns null
        assertThatThrownBy { service.deactivate("ctr-1", 1) }
            .isInstanceOf(ContractNotFoundException::class.java)
    }
}
