# User System Implementation

## Overview

This document outlines the implementation of the user management system for Eagle Bank API. The system allows for the
creation, retrieval, updating, and deletion of user accounts, with role-based access control to ensure that only
authorized users can perform certain operations.

## Design Decisions

### User Model

The user system is built around a comprehensive user model that includes:

- **id**: Unique identifier for the user (format: "usr-" followed by random alphanumeric characters)
- **name**: The user's full name
- **address**: A structured address with the following components:
    - **line1**: First line of the address
    - **line2**: Second line of the address (optional)
    - **line3**: Third line of the address (optional)
    - **town**: Town or city
    - **county**: County or state
    - **postcode**: Postal code
- **phoneNumber**: The user's phone number
- **email**: The user's email address
- **keycloakId**: Reference to the user's ID in Keycloak (optional)
- **createdTimestamp**: When the user was created
- **updatedTimestamp**: When the user was last updated

This design ensures that all necessary user information is captured and can be used for various banking operations.

### Persistence Strategy

Users are stored in a dedicated database table with the following schema:

```
users
- id (VARCHAR, PRIMARY KEY)
- name (VARCHAR)
- address_line1 (VARCHAR)
- address_line2 (VARCHAR, NULLABLE)
- address_line3 (VARCHAR, NULLABLE)
- town (VARCHAR)
- county (VARCHAR)
- postcode (VARCHAR)
- phone_number (VARCHAR)
- email (VARCHAR)
- keycloak_id (VARCHAR, NULLABLE)
- created_timestamp (VARCHAR)
- updated_timestamp (VARCHAR)
```

The user's address is flattened into the user table for simplicity and performance.

### Role-Based Access Control

The user system implements role-based access control to restrict access to user operations:

- **Admin Role**: Users with the "admin" role can create, read, update, and delete any user
- **Account Manager Role**: Users with the "account-manager" role can read and update any user
- **Regular Users**: Regular users can only read and update their own user information

This ensures that sensitive user operations are only performed by authorized personnel.

## API Endpoints

The user system exposes the following RESTful endpoints:

### Create a User

```
POST /v1/users
```

This endpoint allows administrators to create a new user. The request body must include:

- `name`: The user's full name
- `address`: The user's address (line1, line2, line3, town, county, postcode)
- `phoneNumber`: The user's phone number
- `email`: The user's email address

The endpoint validates:

- The user has the "admin" role
- All required fields are provided
- The email is in a valid format
- The phone number is in a valid format

### Get a User

```
GET /v1/users/{id}
```

This endpoint returns the details of a specific user. The user must be:

- The user themselves (accessing their own information)
- An administrator
- An account manager

### Update a User

```
PUT /v1/users/{id}
```

This endpoint allows updating a user's information. The user must be:

- The user themselves (updating their own information)
- An administrator
- An account manager

The request body can include any of the user's fields that need to be updated.

### Delete a User

```
DELETE /v1/users/{id}
```

This endpoint allows administrators to delete a user. Only users with the "admin" role can access this endpoint.

### List All Users

```
GET /v1/users
```

This endpoint returns a list of all users. It is available to all authenticated users.

## Implementation Details

### Components

The user system consists of the following components:

1. **Domain Model**: `User` and `Address` classes representing users in the domain model
2. **Repository Interface**: `IUserRepository` defining the contract for user operations
3. **Database Table**: `UserTable` defining the schema for storing users
4. **Entity Class**: `UserEntity` for mapping between the database and domain model
5. **Repository Implementation**: `UserRepository` implementing the repository interface
6. **Mapper**: `UserMapper` for converting between domain models and DTOs
7. **Validation Service**: `UserRequestValidationService` for validating user requests
8. **Route Handler**: `UsersRoute` for handling user endpoints

### Error Handling

The system handles various error scenarios:

- **Bad Request (400)**: When the request is missing required fields or contains invalid values
- **Unauthorized (401)**: When the user is not authenticated
- **Forbidden (403)**: When the user attempts to access or modify another user's information without proper permissions
- **Not Found (404)**: When the user does not exist
- **Internal Server Error (500)**: When an unexpected error occurs

### Security

All endpoints are protected with JWT authentication. Role-based access control ensures that users can only perform
operations they are authorized for.

## Testing

The implementation includes comprehensive tests for all endpoints and scenarios:

- Creating users
- Retrieving users
- Updating users
- Deleting users
- Role-based access control
- Validation of request data

## Conclusion

This user system provides a robust, secure, and flexible way to manage user accounts in the Eagle Bank API. The
role-based access control ensures that only authorized users can perform sensitive operations, while the comprehensive
user model captures all necessary information for banking operations.