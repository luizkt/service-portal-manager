package com.serviceportal.manager.controller

import com.serviceportal.manager.dto.ValidationDto
import com.serviceportal.manager.dto.ValidationRequest
import com.serviceportal.manager.service.ValidationService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

class ValidationControllerTest {

    private val service: ValidationService = mockk(relaxUnitFun = true)
    private val controller = ValidationController(service)

    private val dto = ValidationDto(
        validationId = "val-1", version = 1, type = "HTTP",
        url = "http://api/credit", method = "GET", headers = emptyMap(),
        timeout = 5000, bodyTemplate = null, responseBody = null,
        active = true, createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now()
    )
    private val request = ValidationRequest(validationId = "val-1", url = "http://api/credit", method = "GET")

    @Test fun `POST creates and returns 201`() {
        every { service.create(request) } returns dto
        val resp = controller.create(request)
        assertThat(resp.statusCode.value()).isEqualTo(201)
        assertThat(resp.body).isEqualTo(dto)
    }

    @Test fun `GET list (paginated) returns 200`() {
        val pageable = PageRequest.of(0, 20)
        every { service.listAll(pageable) } returns PageImpl(listOf(dto))
        assertThat(controller.list(pageable, null).statusCode.value()).isEqualTo(200)
    }

    @Test fun `GET list status=active returns list`() {
        every { service.listActive() } returns listOf(dto)
        val resp = controller.list(PageRequest.of(0, 20), "active")
        assertThat(resp.statusCode.value()).isEqualTo(200)
        @Suppress("UNCHECKED_CAST")
        assertThat(resp.body as List<ValidationDto>).hasSize(1)
    }

    @Test fun `GET versions returns list`() {
        every { service.listVersions("val-1", null) } returns listOf(dto)
        val resp = controller.listVersions("val-1", null)
        assertThat(resp.body).hasSize(1)
    }

    @Test fun `GET by id and version returns 200`() {
        every { service.get("val-1", 1) } returns dto
        assertThat(controller.get("val-1", 1).statusCode.value()).isEqualTo(200)
    }

    @Test fun `PUT returns 201 with new version and Location`() {
        val v2dto = dto.copy(version = 2)
        every { service.update("val-1", 1, request) } returns v2dto
        val resp = controller.update("val-1", 1, request)
        assertThat(resp.statusCode.value()).isEqualTo(201)
        assertThat(resp.body?.version).isEqualTo(2)
        assertThat(resp.headers.getFirst("Location")).isEqualTo("/manager/validations/val-1/versions/2")
    }

    @Test fun `DELETE returns 204`() {
        val resp = controller.delete("val-1", 1)
        assertThat(resp.statusCode.value()).isEqualTo(204)
        verify(exactly = 1) { service.deactivate("val-1", 1) }
    }
}
