package com.serviceportal.manager.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(FlowNotFoundException::class)
    fun handleNotFound(e: FlowNotFoundException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(mapOf("error" to "FLOW_NOT_FOUND", "message" to e.message))

    @ExceptionHandler(InvalidFlowDefinitionException::class)
    fun handleInvalid(e: InvalidFlowDefinitionException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(mapOf("error" to "INVALID_FLOW", "message" to e.message))

    @ExceptionHandler(FlowAlreadyExistsException::class)
    fun handleConflict(e: FlowAlreadyExistsException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(mapOf("error" to "FLOW_ALREADY_EXISTS", "message" to e.message))
}
