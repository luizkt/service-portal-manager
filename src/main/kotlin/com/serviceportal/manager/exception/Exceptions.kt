package com.serviceportal.manager.exception

class FlowNotFoundException(message: String) : RuntimeException(message)

class InvalidFlowDefinitionException(message: String) : RuntimeException(message)

class FlowAlreadyExistsException(message: String) : RuntimeException(message)
