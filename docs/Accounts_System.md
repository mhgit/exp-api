# Accounts System Implementation

## Overview

This document outlines the implementation of the accounts system for Eagle Bank API. The system allows users to create,
retrieve, update, and delete bank accounts, as well as manage their account balances through transactions.

## Design Decisions

### Bank Account Model

The accounts system is built around a comprehensive bank account model that includes:

- **id**: Unique identifier for the bank account
- **userId**: ID of the user who owns the account
- **accountNumber**: The account number (unique identifier for external references)
- **sortCode**: The sort code for the bank account
- **name**: The name of the account
- **accountType**: The type of account (e.g., personal)
- **balance**: The current balance of the account
- **currency**: The currency of the account
- **createdTimestamp**: When the account was created
- **updatedTimestamp**: When the account was last updated

This design ensures that all necessary account information is captured and can be used for various banking operations.

### Persistence Strategy

Bank accounts are stored in a dedicated database table with the following schema:

```
bank_accounts
- id (VARCHAR, PRIMARY KEY)
- user_id (VARCHAR)
- account_number (VARCHAR, UNIQUE INDEX)
- sort_code (VARCHAR)
- name (VARCHAR)
- account_type (VARCHAR)
- balance (DOUBLE)
- currency (VARCHAR)
- created_timestamp (VARCHAR)
- updated_timestamp (VARCHAR)
```

The account_number field has a unique index to ensure that each account number is unique across the system.

### Account Balance Management

Account balances are updated through transactions. When a transaction is created, the account balance is updated
atomically in the same database transaction. This ensures that the account balance always reflects the sum of all
transactions.

For deposits, the amount is added to the account balance. For withdrawals, the amount is subtracted from the account
balance. The system validates that the account has sufficient funds before allowing a withdrawal.

## API Endpoints

The accounts system exposes the following RESTful endpoints:

### Create a Bank Account

```
POST /v1/accounts
```

This endpoint allows users to create a new bank account. The request body must include:

- `name`: The name of the account
- `accountType`: The type of account (e.g., personal)

The endpoint validates:

- All required fields are provided
- The user is authenticated

The system automatically generates a unique account number and sort code for the new account.

### List Bank Accounts

```
GET /v1/accounts
```

This endpoint returns a list of all bank accounts owned by the authenticated user.

### Get a Bank Account

```
GET /v1/accounts/{accountNumber}
```

This endpoint returns the details of a specific bank account. The user must be:

- The owner of the account
- An administrator

### Update a Bank Account

```
PATCH /v1/accounts/{accountNumber}
```

This endpoint allows users to update their bank account information. The user must be the owner of the account.

The request body can include:

- `name`: The new name for the account
- `accountType`: The new type for the account

### Delete a Bank Account

```
DELETE /v1/accounts/{accountNumber}
```

This endpoint allows users to delete their bank account. The user must be the owner of the account.

## Implementation Details

### Components

The accounts system consists of the following components:

1. **Domain Model**: `BankAccount` class representing a bank account in the domain model
2. **Repository Interface**: `IBankAccountRepository` defining the contract for bank account operations
3. **Database Table**: `BankAccountTable` defining the schema for storing bank accounts
4. **Entity Class**: `BankAccountEntity` for mapping between the database and domain model
5. **Repository Implementation**: `BankAccountRepository` implementing the repository interface
6. **Mapper**: `BankAccountMapper` for converting between domain models and DTOs
7. **Validation Service**: `BankAccountRequestValidationService` for validating bank account requests
8. **Route Handler**: `BankAccountsRoute` for handling bank account endpoints

### Error Handling

The system handles various error scenarios:

- **Bad Request (400)**: When the request is missing required fields or contains invalid values
- **Unauthorized (401)**: When the user is not authenticated
- **Forbidden (403)**: When the user attempts to access or modify another user's account
- **Not Found (404)**: When the account does not exist
- **Internal Server Error (500)**: When an unexpected error occurs

### Security

All endpoints are protected with JWT authentication. Users can only access and modify their own accounts, with the
exception of administrators who can view any account.

## Integration with Transaction System

The accounts system integrates closely with the transaction system:

1. When a transaction is created, the account balance is updated atomically
2. The account balance is always the sum of all transaction amounts
3. Withdrawals are only allowed if the account has sufficient funds
4. The transaction system validates that the user owns the account before allowing transactions

## Testing

The implementation includes comprehensive tests for all endpoints and scenarios:

- Creating bank accounts
- Retrieving bank accounts
- Updating bank accounts
- Deleting bank accounts
- Validation of request data
- Security checks (preventing access to other users' accounts)

## Conclusion

This accounts system provides a robust, secure, and flexible way to manage bank accounts in the Eagle Bank API. The
integration with the transaction system ensures that account balances are always accurate and up-to-date, while the
role-based access control ensures that only authorized users can access and modify accounts.