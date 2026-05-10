package com.serviceportal.manager.repository

import com.serviceportal.manager.domain.FlowDocument
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository

/**
 * Acesso à collection `workflows`.
 *
 * Contrato de projeção:
 *   - `findAll(Pageable)` e `findByAtivoTrue()` retornam documentos **sem** o campo
 *     `yamlContent` — economiza banda quando o caller só precisa de metadados
 *     (CRUD admin, lista de ativos para o orquestrador).
 *   - `findByFlowIdAndVersao(...)` também é leve (sem `yamlContent`) — usado pelo
 *     `GET /manager/flows/{id}/{versao}` que devolve só o summary.
 *   - `findByFlowIdAndVersaoWithYaml(...)` traz o documento completo — usado pelo
 *     endpoint `GET /manager/workflows/{id}/{versao}/yaml` e pelas mutações
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

    /** Lista de fluxos ativos — exclui yamlContent. Usado pelo orquestrador. */
    @Query(value = "{ 'ativo': true }", fields = "{ 'yamlContent': 0 }")
    fun findByAtivoTrue(): List<FlowDocument>

    /** Get leve (sem yamlContent) — usado pelo `GET /manager/flows/{id}/{versao}`. */
    @Query(value = "{ 'flowId': ?0, 'versao': ?1 }", fields = "{ 'yamlContent': 0 }")
    fun findByFlowIdAndVersao(flowId: String, versao: String): FlowDocument?

    /** Get completo — usado pelo `GET /yaml` e por mutações que precisam preservar yamlContent. */
    @Query(value = "{ 'flowId': ?0, 'versao': ?1 }")
    fun findByFlowIdAndVersaoWithYaml(flowId: String, versao: String): FlowDocument?

    fun existsByFlowIdAndVersao(flowId: String, versao: String): Boolean
}
