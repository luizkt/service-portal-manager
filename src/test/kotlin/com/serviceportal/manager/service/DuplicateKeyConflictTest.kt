/*
 * MIT License
 *
 * Copyright (c) 2026 Luiz
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.serviceportal.manager.service

import com.serviceportal.manager.client.OrchestratorCacheClient
import com.serviceportal.manager.domain.ContractDocument
import com.serviceportal.manager.domain.FlowDocument
import com.serviceportal.manager.domain.IntegrationDocument
import com.serviceportal.manager.domain.ValidationDocument
import com.serviceportal.manager.dto.ContractRequest
import com.serviceportal.manager.dto.IntegrationRequest
import com.serviceportal.manager.dto.ValidationRequest
import com.serviceportal.manager.exception.ContractAlreadyExistsException
import com.serviceportal.manager.exception.FlowAlreadyExistsException
import com.serviceportal.manager.exception.IntegrationAlreadyExistsException
import com.serviceportal.manager.exception.ValidationAlreadyExistsException
import com.serviceportal.manager.repository.ContractDocumentRepository
import com.serviceportal.manager.repository.FlowDocumentRepository
import com.serviceportal.manager.repository.IntegrationDocumentRepository
import com.serviceportal.manager.repository.ValidationDocumentRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.dao.DuplicateKeyException
import java.time.LocalDateTime

/**
 * Corrida existsBy + save: o índice único (id, version) do Mongo lança
 * DuplicateKeyException, que deve virar a exceção de domínio "já existe" (409).
 */
class DuplicateKeyConflictTest {
    private val dup = DuplicateKeyException("E11000 duplicate key error collection: secret.db index: x")
    private val versioning = SequentialVersioningService()
    private val now = LocalDateTime.now()

    private fun assertNoMongoLeak(ex: Throwable?) {
        assertThat(ex?.message).doesNotContain("E11000").doesNotContain("secret")
    }

    @Test
    @DisplayName("contract create/update: DuplicateKeyException vira ContractAlreadyExistsException")
    fun contract() {
        val repo = mockk<ContractDocumentRepository>()
        val service = ContractService(repo, versioning)
        every { repo.existsByContractIdAndVersion(any(), any()) } returns false
        every { repo.findByContractIdAndVersion("c", 1) } returns mockk<ContractDocument>()
        every { repo.save(any()) } throws dup
        val request = ContractRequest(contractId = "c")

        val create = assertThatThrownBy { service.create(request) }
        create.isInstanceOf(ContractAlreadyExistsException::class.java)
        assertThatThrownBy { service.update("c", 1, request) }
            .isInstanceOf(ContractAlreadyExistsException::class.java)
            .hasMessageContaining("version 2")
            .satisfies({ assertNoMongoLeak(it) })
    }

    @Test
    @DisplayName("integration create/update: DuplicateKeyException vira IntegrationAlreadyExistsException")
    fun integration() {
        val repo = mockk<IntegrationDocumentRepository>()
        val service = IntegrationService(repo, versioning)
        every { repo.existsByIntegrationIdAndVersion(any(), any()) } returns false
        every { repo.findByIntegrationIdAndVersion("i", 1) } returns mockk<IntegrationDocument>()
        every { repo.save(any()) } throws dup
        val request = IntegrationRequest(integrationId = "i", url = "http://x", method = "GET")

        assertThatThrownBy { service.create(request) }
            .isInstanceOf(IntegrationAlreadyExistsException::class.java)
        assertThatThrownBy { service.update("i", 1, request) }
            .isInstanceOf(IntegrationAlreadyExistsException::class.java)
            .satisfies({ assertNoMongoLeak(it) })
    }

    @Test
    @DisplayName("validation create/update: DuplicateKeyException vira ValidationAlreadyExistsException")
    fun validation() {
        val repo = mockk<ValidationDocumentRepository>()
        val service = ValidationService(repo, versioning)
        every { repo.existsByValidationIdAndVersion(any(), any()) } returns false
        every { repo.findByValidationIdAndVersion("v", 1) } returns mockk<ValidationDocument>()
        every { repo.save(any()) } throws dup
        val request = ValidationRequest(validationId = "v", url = "http://x", method = "GET")

        assertThatThrownBy { service.create(request) }
            .isInstanceOf(ValidationAlreadyExistsException::class.java)
        assertThatThrownBy { service.update("v", 1, request) }
            .isInstanceOf(ValidationAlreadyExistsException::class.java)
            .satisfies({ assertNoMongoLeak(it) })
    }

    @Test
    @DisplayName("flow create/update: DuplicateKeyException vira FlowAlreadyExistsException e não invalida cache")
    fun flow() {
        val repo = mockk<FlowDocumentRepository>()
        val cache = mockk<OrchestratorCacheClient>(relaxed = true)
        val service = FlowDocumentService(repo, YamlValidationService(), VersioningService(), cache)
        val yaml =
            """
            flow:
              id: "f"
              version: "1.0.0"
              description: "X"
              contract:
                fields:
                  - name: id
              integrations:
                - id: int1
                  order: 1
                  type: HTTP
            """.trimIndent()
        val existing =
            FlowDocument(
                flowId = "f",
                version = "1.0.0",
                description = "X",
                active = true,
                yamlContent = yaml,
                createdAt = now,
                updatedAt = now,
            )
        every { repo.existsByFlowIdAndVersion(any(), any()) } returns false
        every { repo.findByFlowIdAndVersionWithYaml("f", "1.0.0") } returns existing
        every { repo.save(any()) } throws dup

        assertThatThrownBy { service.create(yaml) }
            .isInstanceOf(FlowAlreadyExistsException::class.java)
        assertThatThrownBy { service.update("f", "1.0.0", yaml.replace("\"X\"", "\"Y\"")) }
            .isInstanceOf(FlowAlreadyExistsException::class.java)
            .satisfies({ assertNoMongoLeak(it) })
        verify(exactly = 0) { cache.evictWorkflow(any(), any()) }
    }
}
