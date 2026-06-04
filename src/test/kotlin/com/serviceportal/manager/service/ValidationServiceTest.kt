package com.serviceportal.manager.service

import com.serviceportal.manager.domain.ValidationDocument
import com.serviceportal.manager.dto.ValidationRequest
import com.serviceportal.manager.exception.InvalidValidationException
import com.serviceportal.manager.exception.ValidationAlreadyExistsException
import com.serviceportal.manager.exception.ValidationNotFoundException
import com.serviceportal.manager.repository.ValidationDocumentRepository
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

class ValidationServiceTest {

    private val repository = mockk<ValidationDocumentRepository>(relaxUnitFun = true)
    private val versioning = SequentialVersioningService()
    private val service = ValidationService(repository, versioning)

    private val now = LocalDateTime.now()
    private val doc = ValidationDocument(
        mongoId = "id1", validationId = "val-1", version = 1,
        type = "HTTP", url = "http://api/credit", method = "GET",
        active = true, createdAt = now, updatedAt = now
    )
    private val request = ValidationRequest(
        validationId = "val-1", type = "HTTP", url = "http://api/credit", method = "GET"
    )

    @Test fun `create saves with version 1`() {
        every { repository.existsByValidationIdAndVersion("val-1", 1) } returns false
        every { repository.save(any()) } returns doc
        val result = service.create(request)
        assertThat(result.validationId).isEqualTo("val-1")
        assertThat(result.version).isEqualTo(1)
    }

    @Test fun `create throws conflict when version 1 already exists`() {
        every { repository.existsByValidationIdAndVersion("val-1", 1) } returns true
        assertThatThrownBy { service.create(request) }
            .isInstanceOf(ValidationAlreadyExistsException::class.java)
    }

    @Test fun `update creates next version`() {
        val v2doc = doc.copy(version = 2)
        every { repository.findByValidationIdAndVersion("val-1", 1) } returns doc
        every { repository.existsByValidationIdAndVersion("val-1", 2) } returns false
        every { repository.save(any()) } returns v2doc
        val result = service.update("val-1", 1, request)
        assertThat(result.version).isEqualTo(2)
    }

    @Test fun `update throws not found when validation missing`() {
        every { repository.findByValidationIdAndVersion("val-1", 1) } returns null
        assertThatThrownBy { service.update("val-1", 1, request) }
            .isInstanceOf(ValidationNotFoundException::class.java)
    }

    @Test fun `update throws invalid when id mismatch`() {
        val mismatch = request.copy(validationId = "other")
        assertThatThrownBy { service.update("val-1", 1, mismatch) }
            .isInstanceOf(InvalidValidationException::class.java)
    }

    @Test fun `update throws conflict when next version exists`() {
        every { repository.findByValidationIdAndVersion("val-1", 1) } returns doc
        every { repository.existsByValidationIdAndVersion("val-1", 2) } returns true
        assertThatThrownBy { service.update("val-1", 1, request) }
            .isInstanceOf(ValidationAlreadyExistsException::class.java)
    }

    @Test fun `get returns dto when found`() {
        every { repository.findByValidationIdAndVersion("val-1", 1) } returns doc
        assertThat(service.get("val-1", 1).validationId).isEqualTo("val-1")
    }

    @Test fun `get throws not found when missing`() {
        every { repository.findByValidationIdAndVersion("val-1", 1) } returns null
        assertThatThrownBy { service.get("val-1", 1) }
            .isInstanceOf(ValidationNotFoundException::class.java)
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
        every { repository.findAllByValidationId("val-1", sort) } returns listOf(doc)
        assertThat(service.listVersions("val-1", null)).hasSize(1)
    }

    @Test fun `listVersions active filters`() {
        val sort = Sort.by(Sort.Direction.ASC, "version")
        every { repository.findAllByValidationIdAndActive("val-1", true, sort) } returns listOf(doc)
        assertThat(service.listVersions("val-1", "active")).hasSize(1)
    }

    @Test fun `listVersions inactive filters`() {
        val sort = Sort.by(Sort.Direction.ASC, "version")
        every { repository.findAllByValidationIdAndActive("val-1", false, sort) } returns emptyList()
        assertThat(service.listVersions("val-1", "inactive")).isEmpty()
    }

    @Test fun `deactivate sets active false`() {
        every { repository.findByValidationIdAndVersion("val-1", 1) } returns doc.copy(active = true)
        every { repository.save(any()) } returns doc.copy(active = false)
        service.deactivate("val-1", 1)
        verify { repository.save(match { !it.active }) }
    }

    @Test fun `deactivate is idempotent when already inactive`() {
        every { repository.findByValidationIdAndVersion("val-1", 1) } returns doc.copy(active = false)
        service.deactivate("val-1", 1)
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test fun `deactivate throws not found when missing`() {
        every { repository.findByValidationIdAndVersion("val-1", 1) } returns null
        assertThatThrownBy { service.deactivate("val-1", 1) }
            .isInstanceOf(ValidationNotFoundException::class.java)
    }
}
