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

import com.serviceportal.manager.domain.IntegrationDocument
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface IntegrationDocumentRepository : MongoRepository<IntegrationDocument, String> {
    fun findAllByActiveTrue(pageable: Pageable): Page<IntegrationDocument>

    fun findAllByActiveTrue(): List<IntegrationDocument>

    fun findByIntegrationIdAndVersion(
        integrationId: String,
        version: Int,
    ): IntegrationDocument?

    fun existsByIntegrationIdAndVersion(
        integrationId: String,
        version: Int,
    ): Boolean

    fun findAllByIntegrationId(
        integrationId: String,
        sort: Sort,
    ): List<IntegrationDocument>

    fun findAllByIntegrationIdAndActive(
        integrationId: String,
        active: Boolean,
        sort: Sort,
    ): List<IntegrationDocument>
}
