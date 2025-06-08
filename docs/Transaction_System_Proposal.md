# Transaction System Implementation Proposal

## Overview

This document outlines the implementation of a transaction system for Eagle Bank API. The system allows users to make
deposits and withdrawals from their bank accounts, with all actions being audited in an immutable transaction log.

## Design Decisions

### Transaction Model

The transaction system is built around an immutable transaction log. Each transaction represents a single operation (
deposit or withdrawal) and contains:

- **id**: Unique identifier for the transaction
- **accountNumber**: The account number associated with this transaction
- **userId**: The ID of the user who owns the account
- **amount**: The amount of the transaction (positive for deposits, negative for withdrawals)
- **currency**: The currency of the transaction
- **type**: The type of transaction (DEPOSIT or WITHDRAWAL)
- **reference**: Optional reference for the transaction
- **createdTimestamp**: When the transaction was created

This design ensures that all account balance changes are tracked and can be audited. The current balance of an account
is the sum of all transaction amounts for that account.

### Persistence Strategy

Transactions are stored in a dedicated database table with the following schema:

```
transactions
- id (VARCHAR, PRIMARY KEY)
- account_number (VARCHAR, INDEXED)
- user_id (VARCHAR)
- amount (DOUBLE)
- currency (VARCHAR)
- type (VARCHAR)
- reference (VARCHAR, NULLABLE)
- created_timestamp (VARCHAR)
```

The transaction log is immutable - once a transaction is created, it cannot be modified or deleted. This ensures a
complete audit trail of all account activity.

### Account Balance Updates

When a transaction is created, the account balance is updated atomically in the same database transaction. This ensures
that the account balance always reflects the sum of all transactions.

For deposits, the amount is added to the account balance. For withdrawals, the amount is subtracted from the account
balance. The system validates that the account has sufficient funds before allowing a withdrawal.

## API Endpoints

The transaction system exposes the following RESTful endpoints:

### Create a Transaction

```
POST /v1/accounts/{accountNumber}/transactions
```

This endpoint allows users to create a new transaction (deposit or withdrawal) for their account. The request body must
include:

- `amount`: The amount of the transaction (must be greater than zero)
- `currency`: The currency of the transaction (e.g., GBP)
- `type`: The type of transaction (deposit or withdrawal)
- `reference` (optional): A reference for the transaction

The endpoint validates:

- The account exists
- The user owns the account
- The amount is greater than zero
- For withdrawals, the account has sufficient funds

### List Transactions

```
GET /v1/accounts/{accountNumber}/transactions
```

This endpoint returns a list of all transactions for the specified account. The user must own the account to access this
endpoint.

### Fetch a Transaction

```
GET /v1/accounts/{accountNumber}/transactions/{transactionId}
```

This endpoint returns the details of a specific transaction. The user must own the account to access this endpoint.

## Implementation Details

### Components

The transaction system consists of the following components:

1. **Domain Model**: `Transaction` class representing a transaction in the domain model
2. **Repository Interface**: `ITransactionRepository` defining the contract for transaction operations
3. **Database Table**: `TransactionTable` defining the schema for storing transactions
4. **Entity Class**: `TransactionEntity` for mapping between the database and domain model
5. **Repository Implementation**: `TransactionRepository` implementing the repository interface
6. **Mapper**: `TransactionMapper` for converting between domain models and DTOs
7. **Validation Service**: `TransactionRequestValidationService` for validating transaction requests
8. **Route Handler**: `TransactionsRoute` for handling transaction endpoints

### Error Handling

The system handles various error scenarios:

- **Bad Request (400)**: When the request is missing required fields or contains invalid values
- **Unauthorized (401)**: When the user is not authenticated
- **Forbidden (403)**: When the user attempts to access another user's account
- **Not Found (404)**: When the account or transaction does not exist
- **Unprocessable Entity (422)**: When the account has insufficient funds for a withdrawal
- **Internal Server Error (500)**: When an unexpected error occurs

### Security

All endpoints are protected with JWT authentication. Users can only access their own accounts and transactions.

## Testing

The implementation includes comprehensive tests for all endpoints and scenarios:

- Creating transactions (deposits and withdrawals)
- Handling insufficient funds
- Validating request data
- Listing transactions
- Fetching specific transactions
- Security checks (preventing access to other users' accounts)

## Conclusion

This transaction system provides a robust, secure, and auditable way to track account balance changes. The immutable
transaction log ensures that all actions are recorded and can be reviewed at any time. The current balance of an account
is always the sum of all transaction amounts, providing a clear and consistent view of the account's financial state.