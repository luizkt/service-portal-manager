package com.serviceportal.manager.controller

import com.serviceportal.manager.dto.FlowSummaryDto
import com.serviceportal.manager.service.FlowDocumentService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

class FlowControllerTest {

    private val service: FlowDocumentService = mockk(relaxUnitFun = true)
    private val controller = FlowController(service)

    private val summary = FlowSummaryDto(
        flowId = "x", version = "1", description = "d", active = true,
        createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now()
    )

    @Test @DisplayName("POST /manager/flows -> 201")
    fun createOk() {
        every { service.create("yaml") } returns summary
        val resp = controller.create("yaml")
        assertThat(resp.statusCode.value()).isEqualTo(201)
        assertThat(resp.body).isEqualTo(summary)
    }

    @Test @DisplayName("GET /manager/flows (paginated) -> Page<FlowSummaryDto>")
    fun listPaginated() {
        val pageable = PageRequest.of(0, 20)
        val page = PageImpl(listOf(summary), pageable, 1)
        every { service.listAll(pageable) } returns page

        val resp = controller.list(pageable, null)

        assertThat(resp.statusCode.value()).isEqualTo(200)
        @Suppress("UNCHECKED_CAST")
        val body = resp.body as Page<FlowSummaryDto>
        assertThat(body.content).containsExactly(summary)
        assertThat(body.totalElements).isEqualTo(1)
        verify(exactly = 1) { service.listAll(pageable) }
    }

    @Test @DisplayName("GET /manager/flows?status=active -> List<FlowSummaryDto> (no Page envelope)")
    fun listActive() {
        every { service.listActive() } returns listOf(summary)
        val resp = controller.list(PageRequest.of(0, 20), "active")

        assertThat(resp.statusCode.value()).isEqualTo(200)
        @Suppress("UNCHECKED_CAST")
        val body = resp.body as List<FlowSummaryDto>
        assertThat(body).containsExactly(summary)
        verify(exactly = 1) { service.listActive() }
    }

    @Test @DisplayName("GET /manager/flows/{id}/versions/{v}")
    fun getOne() {
        every { service.get("x", "1") } returns summary
        assertThat(controller.get("x", "1").body).isEqualTo(summary)
    }

    @Test @DisplayName("GET /manager/flows/{id}/versions/{v}/yaml -> application/x-yaml")
    fun getYaml() {
        every { service.getYaml("x", "1") } returns "flow:\n  id: x"
        val resp = controller.getYaml("x", "1")
        assertThat(resp.statusCode.value()).isEqualTo(200)
        assertThat(resp.body).isEqualTo("flow:\n  id: x")
        assertThat(resp.headers.getFirst("Content-Type")).isEqualTo("application/x-yaml")
    }

    @Test @DisplayName("PUT /manager/flows/{id}/versions/{v}")
    fun update() {
        every { service.update("x", "1", "yaml") } returns summary
        assertThat(controller.update("x", "1", "yaml").body).isEqualTo(summary)
    }

    @Test @DisplayName("DELETE /manager/flows/{id}/versions/{v} -> 204")
    fun delete() {
        val resp = controller.delete("x", "1")
        assertThat(resp.statusCode.value()).isEqualTo(204)
        verify(exactly = 1) { service.deactivate("x", "1") }
    }
}
