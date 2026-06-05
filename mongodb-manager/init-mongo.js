db = db.getSiblingDB('service-portal-manager');

// --- Collections e índices ---

db.createCollection('workflows');
db.createCollection('integrations');
db.createCollection('contracts');
db.createCollection('validations');

db.workflows.createIndex({ "flowId": 1, "version": 1 }, { unique: true, sparse: true });
db.integrations.createIndex({ "integrationId": 1, "version": 1 }, { unique: true, sparse: true });
db.contracts.createIndex({ "contractId": 1, "version": 1 }, { unique: true, sparse: true });
db.validations.createIndex({ "validationId": 1, "version": 1 }, { unique: true, sparse: true });

// --- Dados de exemplo ---

var now = new Date();

// Contract: create-order
db.contracts.insertOne({
    contractId: "create-order",
    version: 1,
    active: true,
    fields: [
        {
            name: "clientId",
            type: "STRING",
            required: true,
            validations: [
                { type: "NOT_BLANK" },
                { type: "PATTERN", value: "^[A-Z0-9]{6,20}$", message: "Invalid clientId" }
            ]
        },
        {
            name: "amount",
            type: "DECIMAL",
            required: true,
            validations: [
                { type: "POSITIVE" }
            ]
        }
    ],
    createdAt: now,
    updatedAt: now,
    _class: "com.serviceportal.manager.domain.ContractDocument"
});

// Integration: validate-client
db.integrations.insertOne({
    integrationId: "validate-client",
    version: 1,
    active: true,
    type: "HTTP",
    url: "http://api.exemplo.com/clients/{{contract.clientId}}",
    method: "GET",
    headers: { "Content-Type": "application/json" },
    timeout: 5000,
    bodyTemplate: null,
    responseBody: {
        clientId: "{{request.pathSegments.[1]}}",
        name: "WireMock Simulated Client",
        document: "12345678910",
        documentType: "CPF",
        active: true,
        createdAt: "2026-01-01T10:00:00Z"
    },
    createdAt: now,
    updatedAt: now,
    _class: "com.serviceportal.manager.domain.IntegrationDocument"
});

// Integration: save-order
db.integrations.insertOne({
    integrationId: "save-order",
    version: 1,
    active: true,
    type: "HTTP",
    url: "http://api.exemplo.com/orders",
    method: "POST",
    headers: { "Content-Type": "application/json" },
    timeout: 5000,
    bodyTemplate: '{"clientId":"{{contract.clientId}}","amount":"{{contract.amount}}","status":"CREATED"}',
    responseBody: {
        id: "ORD-001",
        clientId: "ABC123",
        amount: 150.00,
        status: "CREATED"
    },
    createdAt: now,
    updatedAt: now,
    _class: "com.serviceportal.manager.domain.IntegrationDocument"
});

// Validation: check-credit-limit
db.validations.insertOne({
    validationId: "check-credit-limit",
    version: 1,
    active: true,
    type: "HTTP",
    url: "http://api.exemplo.com/clients/{{contract.clientId}}/credit",
    method: "GET",
    headers: { "Content-Type": "application/json" },
    timeout: 5000,
    bodyTemplate: null,
    responseBody: {
        clientId: "{{request.pathSegments.[1]}}",
        creditLimit: 5000.00,
        available: 3200.00,
        currency: "BRL"
    },
    createdAt: now,
    updatedAt: now,
    _class: "com.serviceportal.manager.domain.ValidationDocument"
});

// Workflow: create-order-v1
var yamlContent = [
    'flow:',
    '  id: "create-order-v1"',
    '  description: "Order creation flow"',
    '  version: "1.0.0"',
    '  active: true',
    '',
    '  contract:',
    '    fields:',
    '      - name: "clientId"',
    '        type: STRING',
    '        required: true',
    '        validations:',
    '          - type: NOT_BLANK',
    '          - type: PATTERN',
    '            value: "^[A-Z0-9]{6,20}$"',
    '            message: "Invalid clientId"',
    '      - name: "amount"',
    '        type: DECIMAL',
    '        required: true',
    '        validations:',
    '          - type: POSITIVE',
    '',
    '  integrations:',
    '    - id: "validate-client"',
    '      order: 1',
    '      type: HTTP',
    '      continueOnError: false',
    '      http:',
    '        url: "http://api.exemplo.com/clients/{{contract.clientId}}"',
    '        method: GET',
    '        headers:',
    '          Content-Type: "application/json"',
    '        timeout: 5000',
    '',
    '    - id: "save-order"',
    '      order: 2',
    '      type: HTTP',
    '      continueOnError: false',
    '      http:',
    '        url: "http://api.exemplo.com/orders"',
    '        method: POST',
    '        headers:',
    '          Content-Type: "application/json"',
    '        bodyTemplate: |',
    '          {"clientId":"{{contract.clientId}}","amount":"{{contract.amount}}","status":"CREATED"}',
    '        timeout: 5000',
    '        responseMapping:',
    '          targetField: "orderId"',
    '          sourceField: "id"',
    '',
    '    - id: "rabbitmq-notifier"',
    '      order: 3',
    '      type: QUEUE',
    '      provider: RABBITMQ',
    '      continueOnError: true',
    '      queue:',
    '        exchange: "orders.exchange"',
    '        routingKey: "order.created"',
    '        messageTemplate: |',
    '          {"event":"ORDER_CREATED","orderId":"{{integrations.save-order.orderId}}"}',
    '        persistent: true',
    '',
    '  validations:',
    '    - id: "check-credit-limit"',
    '      order: 1',
    '      type: HTTP',
    '      continueOnError: false',
    '      http:',
    '        url: "http://api.exemplo.com/clients/{{contract.clientId}}/credit"',
    '        method: GET',
    '        headers:',
    '          Content-Type: "application/json"',
    '        timeout: 5000'
].join('\n');

db.workflows.insertOne({
    flowId: "create-order-v1",
    version: "1.0.0",
    description: "Order creation flow",
    active: true,
    yamlContent: yamlContent,
    // NOTE: ResourceRef.id mapeia para `_id` no MongoDB (propriedade chamada `id`
    // é tratada como identificador pelo Spring Data) — usar `_id` nos refs aninhados.
    contract: { _id: "create-order", version: 1 },
    integrationRefs: [
        { _id: "validate-client", version: 1 },
        { _id: "save-order", version: 1 }
    ],
    validationRefs: [
        { _id: "check-credit-limit", version: 1 }
    ],
    createdAt: now,
    updatedAt: now,
    _class: "com.serviceportal.manager.domain.FlowDocument"
});

print('[init-mongo] Database service-portal-manager initialized with collections, indexes, and example data.');
