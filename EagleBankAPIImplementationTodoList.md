
# Eagle Bank API Implementation TODO List

## Authentication & Security
### Login System
- [ ] Update User entity to include hashed password field
- [ ] Integrate `PasswordService` with User management
- [ ] Modify login flow to verify passwords using BCrypt
- [ ] Add password strength validation
- [ ] Implement proper error handling for authentication failures

### Token Management
- [ ] Complete refresh token implementation
- [ ] Add token revocation mechanism
- [ ] Implement token rotation security
- [ ] Add rate limiting for authentication endpoints

## User Management
### Data Model
- [ ] Add password hash field to User entity
- [ ] Add password salt storage (if required)
- [ ] Add password reset functionality
- [ ] Add email verification system

### API Endpoints
- [ ] Update user creation to include password hashing
- [ ] Add password change endpoint
- [ ] Add password reset request endpoint
- [ ] Add email verification endpoint

## Account Management
### Account Creation (/v1/accounts POST)
- [ ] Implement account creation endpoint
- [ ] Add validation for CreateBankAccountRequest
- [ ] Implement account number generation
- [ ] Add initial balance handling

### Account Retrieval
- [ ] Implement GET /v1/accounts endpoint
- [ ] Implement GET /v1/accounts/{accountNumber}
- [ ] Add pagination for account listing
- [ ] Implement filtering and sorting options

### Account Updates
- [ ] Implement PATCH /v1/accounts/{accountNumber}
- [ ] Add validation for UpdateBankAccountRequest
- [ ] Implement partial updates
- [ ] Add modification history tracking

### Account Deletion
- [ ] Implement DELETE /v1/accounts/{accountNumber}
- [ ] Add soft delete functionality
- [ ] Implement account closure process
- [ ] Add deletion verification steps

## Validation & Security
- [ ] Implement account number pattern validation (^01\d{6}$)
- [ ] Add input sanitization
- [ ] Implement request rate limiting
- [ ] Add comprehensive error handling

## Testing
- [ ] Add unit tests for password hashing
- [ ] Add integration tests for authentication flow
- [ ] Add tests for account management
- [ ] Add security testing suite

## Documentation
- [ ] Update API documentation with security details
- [ ] Add password policy documentation
- [ ] Document account management processes
- [ ] Add deployment security guidelines

## Future Considerations
- [ ] Implement audit logging
- [ ] Add transaction management
- [ ] Consider multi-factor authentication
- [ ] Plan for scalability improvements
- [ ] Hosting arrangement
- [ ] LB
- [ ] ALB
- [ ] WAF
- [ ] DB choice
- [ ] transaction store
- [ ] Deployment