package com.serviceportal.manager.service

import com.serviceportal.manager.exception.InvalidFlowDefinitionException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class YamlValidationServiceTest {

    private val service = YamlValidationService()

    private val validYaml = """
        fluxo:
          id: "criar-pedido-v1"
          versao: "1.0.0"
          descricao: "Cria pedido"
          ativo: true
          contrato:
            campos:
              - nome: x
                tipo: STRING
          integracoes:
            - id: int1
              ordem: 1
              tipo: HTTP
              http:
                url: http://x
                metodo: GET
        """.trimIndent()

    @Test @DisplayName("Extrai metadados de YAML válido")
    fun valido() {
        val meta = service.extractMetadata(validYaml)
        assertThat(meta.flowId).isEqualTo("criar-pedido-v1")
        assertThat(meta.versao).isEqualTo("1.0.0")
        assertThat(meta.descricao).isEqualTo("Cria pedido")
        assertThat(meta.ativo).isTrue()
    }

    @Test @DisplayName("ativo default = true quando ausente")
    fun ativoDefault() {
        val yaml = validYaml.replace("ativo: true", "")
        assertThat(service.extractMetadata(yaml).ativo).isTrue()
    }

    @Test @DisplayName("descricao opcional")
    fun descricaoOpcional() {
        val yaml = validYaml.replace("descricao: \"Cria pedido\"", "")
        assertThat(service.extractMetadata(yaml).descricao).isNull()
    }

    @Test @DisplayName("YAML vazio é rejeitado")
    fun vazio() {
        assertThatThrownBy { service.extractMetadata("   ") }
            .isInstanceOf(InvalidFlowDefinitionException::class.java)
            .hasMessageContaining("vazio")
    }

    @Test @DisplayName("YAML malformado é rejeitado")
    fun malformado() {
        assertThatThrownBy { service.extractMetadata(":\n  - not: [valid") }
            .isInstanceOf(InvalidFlowDefinitionException::class.java)
            .hasMessageContaining("inválido")
    }

    @Test @DisplayName("Sem chave 'fluxo' é rejeitado")
    fun semFluxo() {
        assertThatThrownBy { service.extractMetadata("outro:\n  id: x") }
            .isInstanceOf(InvalidFlowDefinitionException::class.java)
            .hasMessageContaining("'fluxo'")
    }

    @Test @DisplayName("Sem id é rejeitado")
    fun semId() {
        val yaml = """
            fluxo:
              versao: "1.0.0"
              contrato: {}
              integracoes:
                - id: x
            """.trimIndent()
        assertThatThrownBy { service.extractMetadata(yaml) }
            .isInstanceOf(InvalidFlowDefinitionException::class.java)
            .hasMessageContaining("'fluxo.id'")
    }

    @Test @DisplayName("Sem versao é rejeitado")
    fun semVersao() {
        val yaml = """
            fluxo:
              id: "x"
              contrato: {}
              integracoes:
                - id: y
            """.trimIndent()
        assertThatThrownBy { service.extractMetadata(yaml) }
            .isInstanceOf(InvalidFlowDefinitionException::class.java)
            .hasMessageContaining("'fluxo.versao'")
    }

    @Test @DisplayName("Sem contrato é rejeitado")
    fun semContrato() {
        val yaml = """
            fluxo:
              id: "x"
              versao: "1.0"
              integracoes:
                - id: y
            """.trimIndent()
        assertThatThrownBy { service.extractMetadata(yaml) }
            .isInstanceOf(InvalidFlowDefinitionException::class.java)
            .hasMessageContaining("'fluxo.contrato'")
    }

    @Test @DisplayName("Integrações vazias é rejeitado")
    fun integracoesVazias() {
        val yaml = """
            fluxo:
              id: "x"
              versao: "1.0"
              contrato: {}
              integracoes: []
            """.trimIndent()
        assertThatThrownBy { service.extractMetadata(yaml) }
            .isInstanceOf(InvalidFlowDefinitionException::class.java)
            .hasMessageContaining("integração")
    }

    @Test @DisplayName("Integrações ausentes é rejeitado")
    fun semIntegracoes() {
        val yaml = """
            fluxo:
              id: "x"
              versao: "1.0"
              contrato: {}
            """.trimIndent()
        assertThatThrownBy { service.extractMetadata(yaml) }
            .isInstanceOf(InvalidFlowDefinitionException::class.java)
            .hasMessageContaining("integração")
    }

    @Test @DisplayName("id em branco é rejeitado")
    fun idEmBranco() {
        val yaml = validYaml.replace("\"criar-pedido-v1\"", "\"\"")
        assertThatThrownBy { service.extractMetadata(yaml) }
            .isInstanceOf(InvalidFlowDefinitionException::class.java)
            .hasMessageContaining("'fluxo.id'")
    }
}
