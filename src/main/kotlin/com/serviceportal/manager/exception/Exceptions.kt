package com.serviceportal.manager.exception

class FlowNotFoundException(message: String) : RuntimeException(message)
class InvalidFlowDefinitionException(message: String) : RuntimeException(message)
class FlowAlreadyExistsException(message: String) : RuntimeException(message)

class IntegrationNotFoundException(message: String) : RuntimeException(message)
class InvalidIntegrationException(message: String) : RuntimeException(message)
class IntegrationAlreadyExistsException(message: String) : RuntimeException(message)

class ContractNotFoundException(message: String) : RuntimeException(message)
class InvalidContractException(message: String) : RuntimeException(message)
class ContractAlreadyExistsException(message: String) : RuntimeException(message)

class ValidationNotFoundException(message: String) : RuntimeException(message)
class InvalidValidationException(message: String) : RuntimeException(message)
class ValidationAlreadyExistsException(message: String) : RuntimeException(message)
