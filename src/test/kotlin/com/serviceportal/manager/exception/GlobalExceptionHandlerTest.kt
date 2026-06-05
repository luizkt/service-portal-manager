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
package com.serviceportal.manager.exception

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class GlobalExceptionHandlerTest {
    private val handler = GlobalExceptionHandler()

    @Test
    @DisplayName("FlowNotFoundException -> 404 + payload")
    fun notFound() {
        val resp = handler.handleNotFound(FlowNotFoundException("nope"))
        assertThat(resp.statusCode.value()).isEqualTo(404)
        assertThat(resp.body).containsEntry("error", "FLOW_NOT_FOUND")
        assertThat(resp.body).containsEntry("message", "nope")
    }

    @Test
    @DisplayName("InvalidFlowDefinitionException -> 400")
    fun invalid() {
        val resp = handler.handleInvalid(InvalidFlowDefinitionException("yaml ruim"))
        assertThat(resp.statusCode.value()).isEqualTo(400)
        assertThat(resp.body).containsEntry("error", "INVALID_FLOW")
    }

    @Test
    @DisplayName("FlowAlreadyExistsException -> 409")
    fun conflict() {
        val resp = handler.handleConflict(FlowAlreadyExistsException("já existe"))
        assertThat(resp.statusCode.value()).isEqualTo(409)
        assertThat(resp.body).containsEntry("error", "FLOW_ALREADY_EXISTS")
    }
}
