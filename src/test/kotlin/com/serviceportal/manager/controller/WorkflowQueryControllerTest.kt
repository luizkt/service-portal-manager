package com.serviceportal.manager.controller

import com.serviceportal.manager.dto.FlowSummaryDto
import com.serviceportal.manager.service.FlowDocumentService
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class WorkflowQueryControllerTest {

    private val service: FlowDocumentService = mockk()
    private val controller = WorkflowQueryController(service)

    @Test @DisplayName("GET /active devolve lista de ativos")
    fun listActive() {
        val ativo = FlowSummaryDto("x", "1", null, true, LocalDateTime.now(), LocalDateTime.now())
        every { service.listActive() } returns listOf(ativo)
        val resp = controller.listActive()
        assertThat(resp.statusCode.value()).isEqualTo(200)
        assertThat(resp.body).containsExactly(ativo)
    }

    @Test @DisplayName("GET /yaml devolve YAML cru com Content-Type application/x-yaml")
    fun getYaml() {
        every { service.getYaml("x", "1") } returns "fluxo:\n  id: x"
        val resp = controller.getYaml("x", "1")
        assertThat(resp.statusCode.value()).isEqualTo(200)
        assertThat(resp.body).isEqualTo("fluxo:\n  id: x")
        assertThat(resp.headers.getFirst("Content-Type")).isEqualTo("application/x-yaml")
    }
}
