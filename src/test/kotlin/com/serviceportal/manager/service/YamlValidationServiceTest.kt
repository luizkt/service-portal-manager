package com.serviceportal.manager.service

import com.serviceportal.manager.exception.InvalidFlowDefinitionException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class YamlValidationServiceTest {

    private val service = YamlValidationService()

    private val validYaml = """
        flow:
          id: "create-order-v1"
          version: "1.0.0"
          description: "Creates order"
          active: true
          contract:
            fields:
              - name: x
                type: STRING
          integrations:
            - id: int1
              order: 1
              type: HTTP
              http:
                url: http://x
                method: GET
        """.trimIndent()

    @Test @DisplayName("Extracts metadata from a valid YAML")
    fun valid() {
        val meta = service.extractMetadata(validYaml)
        assertThat(meta.flowId).isEqualTo("create-order-v1")
        assertThat(meta.version).isEqualTo("1.0.0")
        assertThat(meta.description).isEqualTo("Creates order")
        assertThat(meta.active).isTrue()
    }

    @Test @DisplayName("active defaults to true when missing")
    fun activeDefault() {
        val yaml = validYaml.replace("active: true", "")
        assertThat(service.extractMetadata(yaml).active).isTrue()
    }

    @Test @DisplayName("description is optional")
    fun descriptionOptional() {
        val yaml = validYaml.replace("description: \"Creates order\"", "")
        assertThat(service.extractMetadata(yaml).description).isNull()
    }

    @Test @DisplayName("Empty YAML is rejected")
    fun empty() {
        assertThatThrownBy { service.extractMetadata("   ") }
            .isInstanceOf(InvalidFlowDefinitionException::class.java)
            .hasMessageContaining("Empty")
    }

    @Test @DisplayName("Malformed YAML is rejected")
    fun malformed() {
        assertThatThrownBy { service.extractMetadata(":\n  - not: [valid") }
            .isInstanceOf(InvalidFlowDefinitionException::class.java)
            .hasMessageContaining("Invalid")
    }

    @Test @DisplayName("Missing 'flow' root key is rejected")
    fun missingFlow() {
        assertThatThrownBy { service.extractMetadata("other:\n  id: x") }
            .isInstanceOf(InvalidFlowDefinitionException::class.java)
            .hasMessageContaining("'flow'")
    }

    @Test @DisplayName("Missing id is rejected")
    fun missingId() {
        val yaml = """
            flow:
              version: "1.0.0"
              contract: {}
              integrations:
                - id: x
            """.trimIndent()
        assertThatThrownBy { service.extractMetadata(yaml) }
            .isInstanceOf(InvalidFlowDefinitionException::class.java)
            .hasMessageContaining("'flow.id'")
    }

    @Test @DisplayName("Missing version is rejected")
    fun missingVersion() {
        val yaml = """
            flow:
              id: "x"
              contract: {}
              integrations:
                - id: y
            """.trimIndent()
        assertThatThrownBy { service.extractMetadata(yaml) }
            .isInstanceOf(InvalidFlowDefinitionException::class.java)
            .hasMessageContaining("'flow.version'")
    }

    @Test @DisplayName("Missing contract is rejected")
    fun missingContract() {
        val yaml = """
            flow:
              id: "x"
              version: "1.0"
              integrations:
                - id: y
            """.trimIndent()
        assertThatThrownBy { service.extractMetadata(yaml) }
            .isInstanceOf(InvalidFlowDefinitionException::class.java)
            .hasMessageContaining("'flow.contract'")
    }

    @Test @DisplayName("Empty integrations is rejected")
    fun emptyIntegrations() {
        val yaml = """
            flow:
              id: "x"
              version: "1.0"
              contract: {}
              integrations: []
            """.trimIndent()
        assertThatThrownBy { service.extractMetadata(yaml) }
            .isInstanceOf(InvalidFlowDefinitionException::class.java)
            .hasMessageContaining("integration")
    }

    @Test @DisplayName("Missing integrations is rejected")
    fun missingIntegrations() {
        val yaml = """
            flow:
              id: "x"
              version: "1.0"
              contract: {}
            """.trimIndent()
        assertThatThrownBy { service.extractMetadata(yaml) }
            .isInstanceOf(InvalidFlowDefinitionException::class.java)
            .hasMessageContaining("integration")
    }

    @Test @DisplayName("Blank id is rejected")
    fun blankId() {
        val yaml = validYaml.replace("\"create-order-v1\"", "\"\"")
        assertThatThrownBy { service.extractMetadata(yaml) }
            .isInstanceOf(InvalidFlowDefinitionException::class.java)
            .hasMessageContaining("'flow.id'")
    }
}
