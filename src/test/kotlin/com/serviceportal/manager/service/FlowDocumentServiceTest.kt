package com.serviceportal.manager.service

import com.serviceportal.manager.client.OrchestratorCacheClient
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
    private val versioning = VersioningService()
    private val cacheClient: OrchestratorCacheClient = mockk(relaxed = true)
    private val service = FlowDocumentService(repo, validator, versioning, cacheClient)

    private val baseYaml = """
        flow:
          id: "create-order-v1"
          version: "1.0.0"
          description: "X"
          active: true
          contract:
            fields:
              - name: id
          integrations:
            - id: int1
              order: 1
              type: HTTP
        """.trimIndent()

    /** YAML com `description` diferente — provoca PATCH bump. */
    private val patchYaml = """
        flow:
          id: "create-order-v1"
          version: "1.0.0"
          description: "Updated description"
          active: true
          contract:
            fields:
              - name: id
          integrations:
            - id: int1
              order: 1
              type: HTTP
        """.trimIndent()

    /** YAML com `contract` diferente — provoca MAJOR bump. */
    private val majorYaml = """
        flow:
          id: "create-order-v1"
          version: "1.0.0"
          description: "X"
          active: true
          contract:
            fields:
              - name: id
              - name: email
          integrations:
            - id: int1
              order: 1
              type: HTTP
        """.trimIndent()

    private fun stored(active: Boolean = true, yamlContent: String? = baseYaml) = FlowDocument(
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

        val dto = service.create(baseYaml)

        assertThat(dto.flowId).isEqualTo("create-order-v1")
        assertThat(dto.version).isEqualTo("1.0.0")
        assertThat(saveSlot.captured.yamlContent).isEqualTo(baseYaml)
        assertThat(saveSlot.captured.createdAt).isNotNull()
    }

    @Test @DisplayName("create throws 409 when already exists")
    fun createConflict() {
        every { repo.existsByFlowIdAndVersion(any(), any()) } returns true
        assertThatThrownBy { service.create(baseYaml) }
            .isInstanceOf(FlowAlreadyExistsException::class.java)
            .hasMessageContaining("create-order-v1")
    }

    @Test @DisplayName("update (PATCH bump): keeps old version active and creates new 1.0.1")
    fun updatePatchBump() {
        val existing = stored()
        every { repo.findByFlowIdAndVersionWithYaml("create-order-v1", "1.0.0") } returns existing
        every { repo.existsByFlowIdAndVersion("create-order-v1", "1.0.1") } returns false
        val savedSlot = slot<FlowDocument>()
        every { repo.save(capture(savedSlot)) } answers { firstArg<FlowDocument>().also { it.mongoId = "new" } }

        val dto = service.update("create-order-v1", "1.0.0", patchYaml)

        assertThat(dto.flowId).isEqualTo("create-order-v1")
        assertThat(dto.version).isEqualTo("1.0.1")
        assertThat(dto.active).isTrue()

        // apenas um save: a nova versão — a antiga permanece ativa sem modificação
        verify(exactly = 1) { repo.save(any()) }
        assertThat(savedSlot.captured.version).isEqualTo("1.0.1")
        assertThat(savedSlot.captured.active).isTrue()
        assertThat(savedSlot.captured.yamlContent).contains("1.0.1")
        // versão antiga não foi tocada
        assertThat(existing.active).isTrue()
        // cache do orquestrador invalidado para a versão alvo do PUT
        verify(exactly = 1) { cacheClient.evictWorkflow("create-order-v1", "1.0.0") }
    }

    @Test @DisplayName("update (MAJOR bump): keeps old version active and creates new 2.0.0")
    fun updateMajorBump() {
        val existing = stored()
        every { repo.findByFlowIdAndVersionWithYaml("create-order-v1", "1.0.0") } returns existing
        every { repo.existsByFlowIdAndVersion("create-order-v1", "2.0.0") } returns false
        val savedSlot = slot<FlowDocument>()
        every { repo.save(capture(savedSlot)) } answers { firstArg<FlowDocument>().also { it.mongoId = "new" } }

        val dto = service.update("create-order-v1", "1.0.0", majorYaml)

        assertThat(dto.version).isEqualTo("2.0.0")
        verify(exactly = 1) { repo.save(any()) }
        assertThat(savedSlot.captured.version).isEqualTo("2.0.0")
        assertThat(existing.active).isTrue()
    }

    @Test @DisplayName("update throws 400 when path flowId diverges from YAML flow.id")
    fun updateDivergentFlowId() {
        assertThatThrownBy { service.update("other-id", "1.0.0", baseYaml) }
            .isInstanceOf(InvalidFlowDefinitionException::class.java)
            .hasMessageContaining("does not match")
    }

    @Test @DisplayName("update throws 404 when existing version not found")
    fun updateMissing() {
        every { repo.findByFlowIdAndVersionWithYaml(any(), any()) } returns null
        assertThatThrownBy { service.update("create-order-v1", "1.0.0", baseYaml) }
            .isInstanceOf(FlowNotFoundException::class.java)
    }

    @Test @DisplayName("update throws 404 when existing doc has no yamlContent")
    fun updateNoYamlContent() {
        every { repo.findByFlowIdAndVersionWithYaml("create-order-v1", "1.0.0") } returns stored(yamlContent = null)
        assertThatThrownBy { service.update("create-order-v1", "1.0.0", baseYaml) }
            .isInstanceOf(FlowNotFoundException::class.java)
            .hasMessageContaining("yamlContent")
    }

    @Test @DisplayName("update throws 409 when calculated next version already exists")
    fun updateCalculatedVersionConflict() {
        val existing = stored()
        every { repo.findByFlowIdAndVersionWithYaml("create-order-v1", "1.0.0") } returns existing
        every { repo.existsByFlowIdAndVersion("create-order-v1", "1.0.1") } returns true

        assertThatThrownBy { service.update("create-order-v1", "1.0.0", patchYaml) }
            .isInstanceOf(FlowAlreadyExistsException::class.java)
            .hasMessageContaining("1.0.1")
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

    @Test @DisplayName("listAll returns only active workflows (paginated)")
    fun listAllOnlyActive() {
        val pageable = PageRequest.of(0, 20)
        val page = PageImpl(listOf(stored(yamlContent = null)), pageable, 1)
        every { repo.findAllByActiveTrue(pageable) } returns page

        val result = service.listAll(pageable)

        assertThat(result.totalElements).isEqualTo(1)
        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].active).isTrue()
        verify(exactly = 1) { repo.findAllByActiveTrue(pageable) }
        verify(exactly = 0) { repo.findAll(pageable) }
    }

    @Test @DisplayName("listVersions without status returns all versions sorted by version ASC")
    fun listVersionsAll() {
        val sort = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "version")
        every { repo.findAllByFlowId("create-order-v1", sort) } returns listOf(
            stored(active = false),
            stored(active = true)
        )
        val result = service.listVersions("create-order-v1", null)
        assertThat(result).hasSize(2)
        verify(exactly = 1) { repo.findAllByFlowId("create-order-v1", sort) }
        verify(exactly = 0) { repo.findAllByFlowIdAndActive(any(), any(), any()) }
    }

    @Test @DisplayName("listVersions status=inactive returns only inactive versions")
    fun listVersionsInactive() {
        val sort = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "version")
        every { repo.findAllByFlowIdAndActive("create-order-v1", false, sort) } returns listOf(stored(active = false))
        val result = service.listVersions("create-order-v1", "inactive")
        assertThat(result).hasSize(1)
        assertThat(result[0].active).isFalse()
        verify(exactly = 1) { repo.findAllByFlowIdAndActive("create-order-v1", false, sort) }
    }

    @Test @DisplayName("listVersions status=active returns only active versions")
    fun listVersionsActive() {
        val sort = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "version")
        every { repo.findAllByFlowIdAndActive("create-order-v1", true, sort) } returns listOf(stored())
        val result = service.listVersions("create-order-v1", "active")
        assertThat(result).hasSize(1)
        assertThat(result[0].active).isTrue()
    }

    @Test @DisplayName("listVersions returns empty list when flowId has no versions")
    fun listVersionsEmpty() {
        val sort = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "version")
        every { repo.findAllByFlowId("unknown-flow", sort) } returns emptyList()
        assertThat(service.listVersions("unknown-flow", null)).isEmpty()
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
        assertThat(service.getYaml("create-order-v1", "1.0.0")).isEqualTo(baseYaml)
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
