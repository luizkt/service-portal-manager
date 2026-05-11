package com.serviceportal.manager.service

import com.serviceportal.manager.domain.FlowDocument
import com.serviceportal.manager.exception.FlowAlreadyExistsException
import com.serviceportal.manager.exception.FlowNotFoundException
import com.serviceportal.manager.exception.InvalidFlowDefinitionException
import com.serviceportal.manager.repository.FlowDocumentRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

class FlowDocumentServiceTest {

    private val repo: FlowDocumentRepository = mockk()
    private val validator = YamlValidationService()
    private val service = FlowDocumentService(repo, validator)

    private val yaml = """
        flow:
          id: "create-order-v1"
          version: "1.0.0"
          description: "X"
          active: true
          contract:
            fields: []
          integrations:
            - id: int1
              order: 1
              type: HTTP
        """.trimIndent()

    private fun stored(active: Boolean = true, yamlContent: String? = yaml) = FlowDocument(
        mongoId = "abc",
        flowId = "create-order-v1",
        version = "1.0.0",
        description = "X",
        active = active,
        yamlContent = yamlContent,
        createdAt = LocalDateTime.of(2026, 1, 1, 0, 0),
        updatedAt = LocalDateTime.of(2026, 1, 1, 0, 0)
    )

    @Test @DisplayName("create persists new document")
    fun createPersists() {
        every { repo.existsByFlowIdAndVersion("create-order-v1", "1.0.0") } returns false
        val saveSlot = slot<FlowDocument>()
        every { repo.save(capture(saveSlot)) } answers { saveSlot.captured.also { it.mongoId = "id1" } }

        val dto = service.create(yaml)

        assertThat(dto.flowId).isEqualTo("create-order-v1")
        assertThat(dto.version).isEqualTo("1.0.0")
        assertThat(saveSlot.captured.yamlContent).isEqualTo(yaml)
        assertThat(saveSlot.captured.createdAt).isNotNull()
    }

    @Test @DisplayName("create throws 409 when already exists")
    fun createConflict() {
        every { repo.existsByFlowIdAndVersion(any(), any()) } returns true
        assertThatThrownBy { service.create(yaml) }
            .isInstanceOf(FlowAlreadyExistsException::class.java)
            .hasMessageContaining("create-order-v1")
    }

    @Test @DisplayName("update uses findByFlowIdAndVersionWithYaml to preserve yamlContent on save")
    fun updateUsesWithYaml() {
        val existing = stored()
        every { repo.findByFlowIdAndVersionWithYaml("create-order-v1", "1.0.0") } returns existing
        every { repo.save(any()) } answers { firstArg() }

        val dto = service.update("create-order-v1", "1.0.0", yaml)

        assertThat(dto.flowId).isEqualTo("create-order-v1")
        assertThat(existing.updatedAt).isAfter(LocalDateTime.of(2026, 1, 1, 0, 0))
        verify(exactly = 1) { repo.findByFlowIdAndVersionWithYaml("create-order-v1", "1.0.0") }
        verify(exactly = 0) { repo.findByFlowIdAndVersion(any(), any()) }
        verify(exactly = 1) { repo.save(existing) }
    }

    @Test @DisplayName("update throws 400 when path id/version diverge from YAML")
    fun updateDivergentPath() {
        assertThatThrownBy { service.update("other-id", "1.0.0", yaml) }
            .isInstanceOf(InvalidFlowDefinitionException::class.java)
            .hasMessageContaining("does not match")
    }

    @Test @DisplayName("update throws 404 when missing")
    fun updateMissing() {
        every { repo.findByFlowIdAndVersionWithYaml(any(), any()) } returns null
        assertThatThrownBy { service.update("create-order-v1", "1.0.0", yaml) }
            .isInstanceOf(FlowNotFoundException::class.java)
    }

    @Test @DisplayName("get uses lightweight repo method (no yamlContent)")
    fun getUsesLightweight() {
        every { repo.findByFlowIdAndVersion("create-order-v1", "1.0.0") } returns stored(yamlContent = null)
        val dto = service.get("create-order-v1", "1.0.0")
        assertThat(dto.flowId).isEqualTo("create-order-v1")
        verify(exactly = 1) { repo.findByFlowIdAndVersion("create-order-v1", "1.0.0") }
        verify(exactly = 0) { repo.findByFlowIdAndVersionWithYaml(any(), any()) }
    }

    @Test @DisplayName("get throws 404 when missing")
    fun getMissing() {
        every { repo.findByFlowIdAndVersion(any(), any()) } returns null
        assertThatThrownBy { service.get("x", "1.0.0") }
            .isInstanceOf(FlowNotFoundException::class.java)
    }

    @Test @DisplayName("listAll uses Pageable and returns Page<FlowSummaryDto>")
    fun listAllPaginated() {
        val pageable = PageRequest.of(0, 20)
        val page = PageImpl(listOf(stored(yamlContent = null)), pageable, 1)
        every { repo.findAll(pageable) } returns page

        val result = service.listAll(pageable)

        assertThat(result.totalElements).isEqualTo(1)
        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].flowId).isEqualTo("create-order-v1")
    }

    @Test @DisplayName("listActive filters active=true (without yamlContent)")
    fun listActive() {
        every { repo.findByActiveTrue() } returns listOf(stored(yamlContent = null))
        val active = service.listActive()
        assertThat(active).hasSize(1)
        assertThat(active[0].active).isTrue()
    }

    @Test @DisplayName("deactivate uses WithYaml and sets active=false on save")
    fun deactivateUsesWithYaml() {
        val doc = stored()
        every { repo.findByFlowIdAndVersionWithYaml(any(), any()) } returns doc
        every { repo.save(any()) } answers { firstArg() }

        service.deactivate("create-order-v1", "1.0.0")

        assertThat(doc.active).isFalse()
        assertThat(doc.yamlContent).isNotNull()
        verify(exactly = 1) { repo.findByFlowIdAndVersionWithYaml("create-order-v1", "1.0.0") }
        verify(exactly = 1) { repo.save(doc) }
    }

    @Test @DisplayName("deactivate is idempotent — already inactive does not save")
    fun deactivateIdempotent() {
        val doc = stored(active = false)
        every { repo.findByFlowIdAndVersionWithYaml(any(), any()) } returns doc

        service.deactivate("create-order-v1", "1.0.0")

        verify(exactly = 0) { repo.save(any()) }
    }

    @Test @DisplayName("deactivate throws 404 when missing")
    fun deactivateMissing() {
        every { repo.findByFlowIdAndVersionWithYaml(any(), any()) } returns null
        assertThatThrownBy { service.deactivate("x", "1.0.0") }
            .isInstanceOf(FlowNotFoundException::class.java)
    }

    @Test @DisplayName("getYaml uses WithYaml and returns content")
    fun getYamlUsesWithYaml() {
        every { repo.findByFlowIdAndVersionWithYaml("create-order-v1", "1.0.0") } returns stored()
        assertThat(service.getYaml("create-order-v1", "1.0.0")).isEqualTo(yaml)
        verify(exactly = 1) { repo.findByFlowIdAndVersionWithYaml("create-order-v1", "1.0.0") }
    }

    @Test @DisplayName("getYaml throws 404 when doc has no yamlContent (legacy)")
    fun getYamlNoContent() {
        every { repo.findByFlowIdAndVersionWithYaml(any(), any()) } returns stored(yamlContent = null)
        assertThatThrownBy { service.getYaml("create-order-v1", "1.0.0") }
            .isInstanceOf(FlowNotFoundException::class.java)
            .hasMessageContaining("yamlContent")
    }

    @Test @DisplayName("getYaml throws 404 when missing")
    fun getYamlMissing() {
        every { repo.findByFlowIdAndVersionWithYaml(any(), any()) } returns null
        assertThatThrownBy { service.getYaml("x", "1.0.0") }
            .isInstanceOf(FlowNotFoundException::class.java)
    }
}
