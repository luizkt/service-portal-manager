package com.serviceportal.manager.controller

import com.serviceportal.manager.dto.FlowSummaryDto
import com.serviceportal.manager.service.FlowDocumentService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

class FlowControllerTest {

    private val service: FlowDocumentService = mockk(relaxUnitFun = true)
    private val controller = FlowController(service)

    private val summary = FlowSummaryDto(
        flowId = "x", versao = "1", descricao = "d", ativo = true,
        criadoEm = LocalDateTime.now(), atualizadoEm = LocalDateTime.now()
    )

    @Test @DisplayName("POST /manager/flows -> 201")
    fun createOk() {
        every { service.create("yaml") } returns summary
        val resp = controller.create("yaml")
        assertThat(resp.statusCode.value()).isEqualTo(201)
        assertThat(resp.body).isEqualTo(summary)
    }

    @Test @DisplayName("GET /manager/flows -> Page<FlowSummaryDto> com Pageable")
    fun listAllPaginado() {
        val pageable = PageRequest.of(0, 20)
        val page = PageImpl(listOf(summary), pageable, 1)
        every { service.listAll(pageable) } returns page

        val resp = controller.listAll(pageable)

        assertThat(resp.statusCode.value()).isEqualTo(200)
        assertThat(resp.body!!.content).containsExactly(summary)
        assertThat(resp.body!!.totalElements).isEqualTo(1)
    }

    @Test @DisplayName("GET /manager/flows/{id}/{versao}")
    fun getOne() {
        every { service.get("x", "1") } returns summary
        assertThat(controller.get("x", "1").body).isEqualTo(summary)
    }

    @Test @DisplayName("PUT /manager/flows/{id}/{versao}")
    fun update() {
        every { service.update("x", "1", "yaml") } returns summary
        assertThat(controller.update("x", "1", "yaml").body).isEqualTo(summary)
    }

    @Test @DisplayName("DELETE /manager/flows/{id}/{versao} -> 204")
    fun delete() {
        val resp = controller.delete("x", "1")
        assertThat(resp.statusCode.value()).isEqualTo(204)
        verify(exactly = 1) { service.deactivate("x", "1") }
    }
}
