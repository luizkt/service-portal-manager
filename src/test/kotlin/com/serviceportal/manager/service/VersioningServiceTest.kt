package com.serviceportal.manager.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class VersioningServiceTest {

    private val service = VersioningService()

    // YAML base — contrato com um campo, integração int1, descrição "Old"
    private val yamlBase = """
        flow:
          id: "create-order"
          version: "1.0.0"
          description: "Old description"
          active: true
          contract:
            fields:
              - name: id
          integrations:
            - id: int1
              order: 1
              type: HTTP
    """.trimIndent()

    // Somente description mudou → PATCH
    private val yamlDescriptionChanged = """
        flow:
          id: "create-order"
          version: "1.0.0"
          description: "New description"
          active: true
          contract:
            fields:
              - name: id
          integrations:
            - id: int1
              order: 1
              type: HTTP
    """.trimIndent()

    // integrations mudou (novo id de integração) → MINOR
    private val yamlIntegrationsChanged = """
        flow:
          id: "create-order"
          version: "1.0.0"
          description: "Old description"
          active: true
          contract:
            fields:
              - name: id
          integrations:
            - id: int2
              order: 1
              type: HTTP
    """.trimIndent()

    // contract mudou (novo campo) → MAJOR
    private val yamlContractChanged = """
        flow:
          id: "create-order"
          version: "1.0.0"
          description: "Old description"
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

    // contract E integrations mudaram → MAJOR (maior precedência)
    private val yamlBothChanged = """
        flow:
          id: "create-order"
          version: "1.0.0"
          description: "Old description"
          active: true
          contract:
            fields:
              - name: id
              - name: email
          integrations:
            - id: int2
              order: 1
              type: HTTP
    """.trimIndent()

    @Test @DisplayName("contract change → MAJOR")
    fun detectsMajor() {
        assertThat(service.detectChangeType(yamlBase, yamlContractChanged))
            .isEqualTo(VersioningService.ChangeType.MAJOR)
    }

    @Test @DisplayName("integrations change → MINOR")
    fun detectsMinor() {
        assertThat(service.detectChangeType(yamlBase, yamlIntegrationsChanged))
            .isEqualTo(VersioningService.ChangeType.MINOR)
    }

    @Test @DisplayName("only description change → PATCH")
    fun detectsPatch() {
        assertThat(service.detectChangeType(yamlBase, yamlDescriptionChanged))
            .isEqualTo(VersioningService.ChangeType.PATCH)
    }

    @Test @DisplayName("no change → PATCH")
    fun detectsPatchWhenNothingChanged() {
        assertThat(service.detectChangeType(yamlBase, yamlBase))
            .isEqualTo(VersioningService.ChangeType.PATCH)
    }

    @Test @DisplayName("contract + integrations change → MAJOR (highest precedence wins)")
    fun majorPrecedenceOverMinor() {
        assertThat(service.detectChangeType(yamlBase, yamlBothChanged))
            .isEqualTo(VersioningService.ChangeType.MAJOR)
    }

    @Test @DisplayName("MAJOR bump resets minor and patch")
    fun calculateMajor() {
        assertThat(service.calculateNextVersion("1.2.3", VersioningService.ChangeType.MAJOR)).isEqualTo("2.0.0")
    }

    @Test @DisplayName("MINOR bump resets patch")
    fun calculateMinor() {
        assertThat(service.calculateNextVersion("1.2.3", VersioningService.ChangeType.MINOR)).isEqualTo("1.3.0")
    }

    @Test @DisplayName("PATCH bump increments patch only")
    fun calculatePatch() {
        assertThat(service.calculateNextVersion("1.2.3", VersioningService.ChangeType.PATCH)).isEqualTo("1.2.4")
    }

    @Test @DisplayName("starts from 1.0.0 correctly")
    fun calculateFromInitial() {
        assertThat(service.calculateNextVersion("1.0.0", VersioningService.ChangeType.MAJOR)).isEqualTo("2.0.0")
        assertThat(service.calculateNextVersion("1.0.0", VersioningService.ChangeType.MINOR)).isEqualTo("1.1.0")
        assertThat(service.calculateNextVersion("1.0.0", VersioningService.ChangeType.PATCH)).isEqualTo("1.0.1")
    }

    @Test @DisplayName("updateVersionInYaml replaces flow.version field")
    fun updatesVersionInYaml() {
        val updated = service.updateVersionInYaml(yamlBase, "2.0.0")
        assertThat(updated).contains("2.0.0")
        assertThat(updated).doesNotContain("\"1.0.0\"")
    }
}
