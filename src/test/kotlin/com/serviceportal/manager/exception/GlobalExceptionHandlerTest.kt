package com.serviceportal.manager.exception

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    @Test @DisplayName("FlowNotFoundException -> 404 + payload")
    fun notFound() {
        val resp = handler.handleNotFound(FlowNotFoundException("nope"))
        assertThat(resp.statusCode.value()).isEqualTo(404)
        assertThat(resp.body).containsEntry("error", "FLOW_NOT_FOUND")
        assertThat(resp.body).containsEntry("message", "nope")
    }

    @Test @DisplayName("InvalidFlowDefinitionException -> 400")
    fun invalid() {
        val resp = handler.handleInvalid(InvalidFlowDefinitionException("yaml ruim"))
        assertThat(resp.statusCode.value()).isEqualTo(400)
        assertThat(resp.body).containsEntry("error", "INVALID_FLOW")
    }

    @Test @DisplayName("FlowAlreadyExistsException -> 409")
    fun conflict() {
        val resp = handler.handleConflict(FlowAlreadyExistsException("já existe"))
        assertThat(resp.statusCode.value()).isEqualTo(409)
        assertThat(resp.body).containsEntry("error", "FLOW_ALREADY_EXISTS")
    }
}
