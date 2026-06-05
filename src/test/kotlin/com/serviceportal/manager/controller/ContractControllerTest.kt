package com.serviceportal.manager.controller

import com.serviceportal.manager.domain.ContractField
import com.serviceportal.manager.dto.ContractDto
import com.serviceportal.manager.dto.ContractRequest
import com.serviceportal.manager.service.ContractService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

class ContractControllerTest {

    private val service: ContractService = mockk(relaxUnitFun = true)
    private val controller = ContractController(service)

    private val field = ContractField(name = "clientId", type = "STRING", required = true)
    private val dto = ContractDto(
        contractId = "ctr-1", version = 1, fields = listOf(field),
        active = true, createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now()
    )
    private val request = ContractRequest(contractId = "ctr-1", fields = listOf(field))

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
        assertThat(resp.body as List<ContractDto>).hasSize(1)
    }

    @Test fun `GET versions returns list`() {
        every { service.listVersions("ctr-1", null) } returns listOf(dto)
        val resp = controller.listVersions("ctr-1", null)
        assertThat(resp.body).hasSize(1)
    }

    @Test fun `GET by id and version returns 200`() {
        every { service.get("ctr-1", 1) } returns dto
        assertThat(controller.get("ctr-1", 1).statusCode.value()).isEqualTo(200)
    }

    @Test fun `PUT returns 201 with new version and Location`() {
        val v2dto = dto.copy(version = 2)
        every { service.update("ctr-1", 1, request) } returns v2dto
        val resp = controller.update("ctr-1", 1, request)
        assertThat(resp.statusCode.value()).isEqualTo(201)
        assertThat(resp.body?.version).isEqualTo(2)
        assertThat(resp.headers.getFirst("Location")).isEqualTo("/manager/contracts/ctr-1/versions/2")
    }

    @Test fun `DELETE returns 204`() {
        val resp = controller.delete("ctr-1", 1)
        assertThat(resp.statusCode.value()).isEqualTo(204)
        verify(exactly = 1) { service.deactivate("ctr-1", 1) }
    }
}
