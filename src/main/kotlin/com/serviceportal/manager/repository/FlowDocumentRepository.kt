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
package com.serviceportal.manager.repository

import com.serviceportal.manager.domain.FlowDocument
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository

/**
 * Acesso à collection `workflows`.
 *
 * Contrato de projeção:
 *   - `findAll(Pageable)` e `findByActiveTrue()` retornam documentos **sem** o campo
 *     `yamlContent` — economiza banda quando o caller só precisa de metadados
 *     (CRUD admin, lista de ativos para o orquestrador).
 *   - `findByFlowIdAndVersion(...)` também é leve (sem `yamlContent`) — usado pelo
 *     `GET /manager/flows/{id}/versions/{v}` que devolve só o summary.
 *   - `findByFlowIdAndVersionWithYaml(...)` traz o documento completo — usado pelo
 *     endpoint `GET /manager/flows/{id}/versions/{v}/yaml` e pelas mutações
 *     (update/deactivate) que precisam preservar o `yamlContent` ao re-salvar.
 *
 * Importante: nas queries que projetam fora `yamlContent`, o objeto retornado
 * tem o campo `null`. NÃO chame `repository.save(doc)` em cima dele — a gravação
 * sobrescreve o documento inteiro e zera o `yamlContent` no banco. Use sempre o
 * `WithYaml` quando for fazer save após mutação.
 */
@Repository
interface FlowDocumentRepository : MongoRepository<FlowDocument, String> {
    /** Lista paginada — exclui yamlContent. */
    @Query(value = "{}", fields = "{ 'yamlContent': 0 }")
    override fun findAll(pageable: Pageable): Page<FlowDocument>

    /** Lista de fluxos ativos — exclui yamlContent. Usado pelo orquestrador (sem paginação). */
    @Query(value = "{ 'active': true }", fields = "{ 'yamlContent': 0 }")
    fun findByActiveTrue(): List<FlowDocument>

    /** Lista paginada de fluxos ativos — exclui yamlContent. Usado pelo endpoint de listagem. */
    @Query(value = "{ 'active': true }", fields = "{ 'yamlContent': 0 }")
    fun findAllByActiveTrue(pageable: Pageable): Page<FlowDocument>

    /** Get leve (sem yamlContent) — usado pelo `GET /manager/flows/{id}/versions/{v}`. */
    @Query(value = "{ 'flowId': ?0, 'version': ?1 }", fields = "{ 'yamlContent': 0 }")
    fun findByFlowIdAndVersion(
        flowId: String,
        version: String,
    ): FlowDocument?

    /** Get completo — usado pelo `GET /yaml` e por mutações que precisam preservar yamlContent. */
    @Query(value = "{ 'flowId': ?0, 'version': ?1 }")
    fun findByFlowIdAndVersionWithYaml(
        flowId: String,
        version: String,
    ): FlowDocument?

    fun existsByFlowIdAndVersion(
        flowId: String,
        version: String,
    ): Boolean

    /** Todas as versões de um flow (ativas + inativas) — exclui yamlContent. */
    @Query(value = "{ 'flowId': ?0 }", fields = "{ 'yamlContent': 0 }")
    fun findAllByFlowId(
        flowId: String,
        sort: Sort,
    ): List<FlowDocument>

    /** Versões de um flow filtradas por active — exclui yamlContent. */
    @Query(value = "{ 'flowId': ?0, 'active': ?1 }", fields = "{ 'yamlContent': 0 }")
    fun findAllByFlowIdAndActive(
        flowId: String,
        active: Boolean,
        sort: Sort,
    ): List<FlowDocument>
}
