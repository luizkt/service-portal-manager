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

    @ExceptionHandler(IntegrationNotFoundException::class)
    fun handleIntegrationNotFound(e: IntegrationNotFoundException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(mapOf("error" to "INTEGRATION_NOT_FOUND", "message" to e.message))

    @ExceptionHandler(InvalidIntegrationException::class)
    fun handleInvalidIntegration(e: InvalidIntegrationException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(mapOf("error" to "INVALID_INTEGRATION", "message" to e.message))

    @ExceptionHandler(IntegrationAlreadyExistsException::class)
    fun handleIntegrationConflict(e: IntegrationAlreadyExistsException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(mapOf("error" to "INTEGRATION_ALREADY_EXISTS", "message" to e.message))

    @ExceptionHandler(ContractNotFoundException::class)
    fun handleContractNotFound(e: ContractNotFoundException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(mapOf("error" to "CONTRACT_NOT_FOUND", "message" to e.message))

    @ExceptionHandler(InvalidContractException::class)
    fun handleInvalidContract(e: InvalidContractException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(mapOf("error" to "INVALID_CONTRACT", "message" to e.message))

    @ExceptionHandler(ContractAlreadyExistsException::class)
    fun handleContractConflict(e: ContractAlreadyExistsException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(mapOf("error" to "CONTRACT_ALREADY_EXISTS", "message" to e.message))

    @ExceptionHandler(ValidationNotFoundException::class)
    fun handleValidationNotFound(e: ValidationNotFoundException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(mapOf("error" to "VALIDATION_NOT_FOUND", "message" to e.message))

    @ExceptionHandler(InvalidValidationException::class)
    fun handleInvalidValidation(e: InvalidValidationException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(mapOf("error" to "INVALID_VALIDATION", "message" to e.message))

    @ExceptionHandler(ValidationAlreadyExistsException::class)
    fun handleValidationConflict(e: ValidationAlreadyExistsException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(mapOf("error" to "VALIDATION_ALREADY_EXISTS", "message" to e.message))
}
