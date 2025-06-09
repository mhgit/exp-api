# Eagle Bank API Implementation TODO List

## Authentication & Security (Keycloak Integration)
### Keycloak Setup
- [x] Configure Keycloak realm for Eagle Bank
- [x] Set up client application in Keycloak
- [ ] Configure user federation if needed
- [x] Set up required user attributes and roles
- [x] Configure password policies in Keycloak
- [ ] Set up email verification flow
- [ ] Configure Keycloak themes for branded experience
- [ ] Review integration methods, introduce Fedorated ID and API gateway.
- [ ] Migrate more concerns away from the microservice.

### API Integration

- [x] Add Keycloak dependencies to the project
- [x] Configure Keycloak adapter in the application
- [x] Implement token validation middleware
- [x] Map Keycloak roles to application permissions
- [x] Add Keycloak user ID to User entity
- [ ] Implement token introspection
- [x] Set up role-based access control (RBAC)

## Infrastructure (AWS Setup)
### Keycloak Deployment
- [ ] Set up ECS Fargate cluster
- [ ] Configure Aurora PostgreSQL for Keycloak
- [ ] Deploy Keycloak container to ECS
- [ ] Set up Application Load Balancer
- [ ] Configure auto-scaling policies
- [ ] Implement health checks
- [ ] Set up container logging to CloudWatch

### Edge Protection
- [ ] Configure CloudFront distribution
- [ ] Set up AWS WAF rules
- [ ] Implement AWS Shield protection
- [ ] Configure SSL/TLS with ACM
- [ ] Set up security groups
- [ ] Configure VPC settings
- [ ] Implement network ACLs

### Monitoring & Operations
- [ ] Set up CloudWatch alarms
- [ ] Configure backup strategy
- [ ] Implement disaster recovery plan
- [ ] Set up monitoring dashboards
- [ ] Configure alerting
- [ ] Implement infrastructure as code (Terraform/CDK)
- [ ] Implement SIEM logging integration
- [ ] Set up OpenTelemetry for distributed tracing
- [ ] Configure comprehensive monitoring strategy

## User Management
### Data Model

- [x] Update User entity to include Keycloak ID
- [ ] Add role mapping fields if needed
- [ ] Update user synchronization logic

### API Endpoints

- [x] Update user creation to integrate with Keycloak
- [x] Modify user update endpoints for Keycloak sync
- [ ] Implement user profile endpoints
- [ ] Add group management endpoints

## Account Management
### Account Creation (/v1/accounts POST)

- [x] Implement account creation endpoint
- [x] Add validation for CreateBankAccountRequest
- [x] Implement account number generation
- [ ] Add initial balance handling

### Account Retrieval

- [x] Implement GET /v1/accounts endpoint
- [x] Implement GET /v1/accounts/{accountNumber}
- [ ] Add pagination for account listing
- [ ] Implement filtering and sorting options

### Account Updates

- [x] Implement PATCH /v1/accounts/{accountNumber}
- [x] Add validation for UpdateBankAccountRequest
- [x] Implement partial updates
- [ ] Add modification history tracking

### Account Deletion

- [x] Implement DELETE /v1/accounts/{accountNumber}
- [ ] Add soft delete functionality
- [ ] Implement account closure process
- [ ] Add deletion verification steps

## Testing
- [ ] Add Keycloak integration tests
- [ ] Test token validation
- [ ] Test role-based access
- [ ] Add security testing suite
- [ ] Test infrastructure deployment
- [ ] Load testing with authentication

## Documentation

- [x] Document Keycloak setup process
- [x] Update API documentation with authentication details
- [ ] Document infrastructure setup
- [ ] Add deployment guidelines
- [ ] Document security practices
- [ ] Create operations runbook

## API Implementation Status

### Implemented APIs

- [x] POST /v1/users - Create a new user
- [x] GET /v1/users/{userId} - Fetch user by ID
- [x] PATCH /v1/users/{userId} - Update user by ID
- [x] DELETE /v1/users/{userId} - Delete user by ID
- [x] GET /v1/users - List all users

### Authentication APIs (Implemented)

- [x] POST /v1/login - Authenticate user and obtain JWT tokens
- [x] POST /v1/refresh-token - Obtain a new access token using a refresh token
- [x] GET /v1/protected - Access a protected resource

### Account APIs (Implemented)

- [x] POST /v1/accounts - Create a new bank account
- [x] GET /v1/accounts - List accounts
- [x] GET /v1/accounts/{accountNumber} - Fetch account by account number
- [x] PATCH /v1/accounts/{accountNumber} - Update account by account number
- [x] DELETE /v1/accounts/{accountNumber} - Delete account by account number

### Transaction APIs (Implemented)

- [x] POST /v1/accounts/{accountNumber}/transactions - Create a transaction
- [x] GET /v1/accounts/{accountNumber}/transactions - List transactions
- [x] GET /v1/accounts/{accountNumber}/transactions/{transactionId} - Fetch transaction by ID

## Future Considerations
- [ ] Implement audit logging with Keycloak events
- [ ] Add transaction management
- [ ] Consider multi-factor authentication via Keycloak
- [ ] Plan for scalability improvements
- [ ] Consider multi-region deployment
- [ ] Evaluate Keycloak clustering options
