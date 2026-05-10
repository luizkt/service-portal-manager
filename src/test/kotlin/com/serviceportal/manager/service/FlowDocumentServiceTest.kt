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
        fluxo:
          id: "criar-pedido-v1"
          versao: "1.0.0"
          descricao: "X"
          ativo: true
          contrato:
            campos: []
          integracoes:
            - id: int1
              ordem: 1
              tipo: HTTP
        """.trimIndent()

    private fun stored(ativo: Boolean = true, yamlContent: String? = yaml) = FlowDocument(
        mongoId = "abc",
        flowId = "criar-pedido-v1",
        versao = "1.0.0",
        descricao = "X",
        ativo = ativo,
        yamlContent = yamlContent,
        criadoEm = LocalDateTime.of(2026, 1, 1, 0, 0),
        atualizadoEm = LocalDateTime.of(2026, 1, 1, 0, 0)
    )

    @Test @DisplayName("create persiste novo documento")
    fun createPersiste() {
        every { repo.existsByFlowIdAndVersao("criar-pedido-v1", "1.0.0") } returns false
        val saveSlot = slot<FlowDocument>()
        every { repo.save(capture(saveSlot)) } answers { saveSlot.captured.also { it.mongoId = "id1" } }

        val dto = service.create(yaml)

        assertThat(dto.flowId).isEqualTo("criar-pedido-v1")
        assertThat(dto.versao).isEqualTo("1.0.0")
        assertThat(saveSlot.captured.yamlContent).isEqualTo(yaml)
        assertThat(saveSlot.captured.criadoEm).isNotNull()
    }

    @Test @DisplayName("create lança 409 se já existe")
    fun createConflito() {
        every { repo.existsByFlowIdAndVersao(any(), any()) } returns true
        assertThatThrownBy { service.create(yaml) }
            .isInstanceOf(FlowAlreadyExistsException::class.java)
            .hasMessageContaining("criar-pedido-v1")
    }

    @Test @DisplayName("update usa findByFlowIdAndVersaoWithYaml para preservar yamlContent ao re-salvar")
    fun updateUsaWithYaml() {
        val existing = stored()
        every { repo.findByFlowIdAndVersaoWithYaml("criar-pedido-v1", "1.0.0") } returns existing
        every { repo.save(any()) } answers { firstArg() }

        val dto = service.update("criar-pedido-v1", "1.0.0", yaml)

        assertThat(dto.flowId).isEqualTo("criar-pedido-v1")
        assertThat(existing.atualizadoEm).isAfter(LocalDateTime.of(2026, 1, 1, 0, 0))
        // Crítico: garante que NÃO usamos a versão lightweight no caminho de save
        verify(exactly = 1) { repo.findByFlowIdAndVersaoWithYaml("criar-pedido-v1", "1.0.0") }
        verify(exactly = 0) { repo.findByFlowIdAndVersao(any(), any()) }
        verify(exactly = 1) { repo.save(existing) }
    }

    @Test @DisplayName("update lança 400 quando id/versão do path divergem do YAML")
    fun updatePathDivergente() {
        assertThatThrownBy { service.update("outro-id", "1.0.0", yaml) }
            .isInstanceOf(InvalidFlowDefinitionException::class.java)
            .hasMessageContaining("não bate")
    }

    @Test @DisplayName("update lança 404 quando não existe")
    fun updateNaoExiste() {
        every { repo.findByFlowIdAndVersaoWithYaml(any(), any()) } returns null
        assertThatThrownBy { service.update("criar-pedido-v1", "1.0.0", yaml) }
            .isInstanceOf(FlowNotFoundException::class.java)
    }

    @Test @DisplayName("get usa a versão leve (sem yamlContent)")
    fun getUsaLightweight() {
        every { repo.findByFlowIdAndVersao("criar-pedido-v1", "1.0.0") } returns stored(yamlContent = null)
        val dto = service.get("criar-pedido-v1", "1.0.0")
        assertThat(dto.flowId).isEqualTo("criar-pedido-v1")
        verify(exactly = 1) { repo.findByFlowIdAndVersao("criar-pedido-v1", "1.0.0") }
        verify(exactly = 0) { repo.findByFlowIdAndVersaoWithYaml(any(), any()) }
    }

    @Test @DisplayName("get lança 404 quando não existe")
    fun getNaoExiste() {
        every { repo.findByFlowIdAndVersao(any(), any()) } returns null
        assertThatThrownBy { service.get("x", "1.0.0") }
            .isInstanceOf(FlowNotFoundException::class.java)
    }

    @Test @DisplayName("listAll usa Pageable e devolve Page<FlowSummaryDto>")
    fun listAllPaginado() {
        val pageable = PageRequest.of(0, 20)
        val page = PageImpl(listOf(stored(yamlContent = null)), pageable, 1)
        every { repo.findAll(pageable) } returns page

        val result = service.listAll(pageable)

        assertThat(result.totalElements).isEqualTo(1)
        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].flowId).isEqualTo("criar-pedido-v1")
    }

    @Test @DisplayName("listActive filtra ativo=true (sem yamlContent)")
    fun listActive() {
        every { repo.findByAtivoTrue() } returns listOf(stored(yamlContent = null))
        val ativos = service.listActive()
        assertThat(ativos).hasSize(1)
        assertThat(ativos[0].ativo).isTrue()
    }

    @Test @DisplayName("deactivate usa WithYaml e seta ativo=false ao salvar")
    fun deactivateUsaWithYaml() {
        val doc = stored()
        every { repo.findByFlowIdAndVersaoWithYaml(any(), any()) } returns doc
        every { repo.save(any()) } answers { firstArg() }

        service.deactivate("criar-pedido-v1", "1.0.0")

        assertThat(doc.ativo).isFalse()
        // Garante que o doc completo foi carregado para evitar zerar yamlContent
        assertThat(doc.yamlContent).isNotNull()
        verify(exactly = 1) { repo.findByFlowIdAndVersaoWithYaml("criar-pedido-v1", "1.0.0") }
        verify(exactly = 1) { repo.save(doc) }
    }

    @Test @DisplayName("deactivate é idempotente — já desativado não salva")
    fun deactivateIdempotente() {
        val doc = stored(ativo = false)
        every { repo.findByFlowIdAndVersaoWithYaml(any(), any()) } returns doc

        service.deactivate("criar-pedido-v1", "1.0.0")

        verify(exactly = 0) { repo.save(any()) }
    }

    @Test @DisplayName("deactivate lança 404 quando não existe")
    fun deactivateNaoExiste() {
        every { repo.findByFlowIdAndVersaoWithYaml(any(), any()) } returns null
        assertThatThrownBy { service.deactivate("x", "1.0.0") }
            .isInstanceOf(FlowNotFoundException::class.java)
    }

    @Test @DisplayName("getYaml usa WithYaml e devolve conteúdo")
    fun getYamlUsaWithYaml() {
        every { repo.findByFlowIdAndVersaoWithYaml("criar-pedido-v1", "1.0.0") } returns stored()
        assertThat(service.getYaml("criar-pedido-v1", "1.0.0")).isEqualTo(yaml)
        verify(exactly = 1) { repo.findByFlowIdAndVersaoWithYaml("criar-pedido-v1", "1.0.0") }
    }

    @Test @DisplayName("getYaml lança 404 quando documento não tem yamlContent (legacy)")
    fun getYamlSemConteudo() {
        every { repo.findByFlowIdAndVersaoWithYaml(any(), any()) } returns stored(yamlContent = null)
        assertThatThrownBy { service.getYaml("criar-pedido-v1", "1.0.0") }
            .isInstanceOf(FlowNotFoundException::class.java)
            .hasMessageContaining("yamlContent")
    }

    @Test @DisplayName("getYaml lança 404 quando não existe")
    fun getYamlNaoExiste() {
        every { repo.findByFlowIdAndVersaoWithYaml(any(), any()) } returns null
        assertThatThrownBy { service.getYaml("x", "1.0.0") }
            .isInstanceOf(FlowNotFoundException::class.java)
    }
}
