# Security System Implementation

## Overview

This document outlines the implementation of the security system for Eagle Bank API. The system provides authentication
and authorization mechanisms to protect API endpoints, ensuring that only authorized users can access sensitive data and
perform operations.

## Design Decisions

### Authentication with Keycloak

The security system uses Keycloak, an open-source identity and access management solution, for authentication. This
provides several benefits:

- **Centralized Identity Management**: Single source of truth for user identities across multiple applications
- **Industry-Standard Security**: Implements OAuth 2.0 and OpenID Connect protocols
- **Rich Feature Set**: User federation, social login, multi-factor authentication, and more
- **Reduced Development Effort**: No need to implement complex security features from scratch
- **Delegation of Security Concerns**: Security experts maintain Keycloak, reducing the risk of security vulnerabilities

### JWT-Based Authentication

The system uses JSON Web Tokens (JWT) for authentication:

- **Stateless**: No need to store session information on the server
- **Portable**: Can be used across different services
- **Secure**: Tokens are signed to prevent tampering
- **Expirable**: Tokens have an expiration time
- **Claims-Based**: Tokens contain claims about the user, including roles

### Role-Based Access Control

The system implements role-based access control (RBAC) to restrict access to API endpoints:

- **Role Assignment**: Users are assigned roles in Keycloak
- **Role Verification**: API endpoints verify that users have the required roles
- **Granular Permissions**: Different endpoints can require different roles
- **Flexible Authorization**: Routes can require a specific role or any of several roles

## Implementation Details

### Components

The security system consists of the following components:

1. **Security Configuration**: `Security.kt` configures JWT authentication with Keycloak
2. **Role-Based Authorization**: `RoleBasedAuthorization.kt` provides utilities for role-based access control
3. **Authentication Models**: `AuthTokenModels.kt` defines data classes for authentication requests and responses

### Authentication Flow

The authentication flow works as follows:

1. User authenticates with Keycloak and receives a JWT token
2. User includes the token in the Authorization header of API requests
3. The application verifies the token's signature using Keycloak's public key
4. The application validates the token's claims (issuer, expiration, etc.)
5. If the token is valid, the user is authenticated and can access protected endpoints

### Authorization Flow

The authorization flow works as follows:

1. The application extracts the user's roles from the JWT token
2. When the user accesses a protected endpoint, the application checks if the user has the required role(s)
3. If the user has the required role(s), the request is processed
4. If the user doesn't have the required role(s), the application returns a 403 Forbidden response

### Preventing IDOR Attacks

The system prevents Insecure Direct Object References (IDOR) attacks by:

1. Extracting the user ID from the JWT token
2. Verifying that the user can only access their own resources
3. Using role-based access control for administrative functions
4. Logging access attempts for security auditing

## API Endpoints

The security system doesn't expose any API endpoints directly, but it protects all API endpoints that require
authentication.

### Protected Endpoints

All endpoints under the following routes are protected with JWT authentication:

- `/v1/users`: User management endpoints
- `/v1/accounts`: Account management endpoints
- `/v1/accounts/{accountNumber}/transactions`: Transaction endpoints

### Admin-Only Endpoints

Some endpoints are restricted to users with the `admin` role:

- `GET /v1/users`: List all users

## Error Handling

The system handles various error scenarios:

- **Unauthorized (401)**: When the user is not authenticated or the token is invalid
- **Forbidden (403)**: When the user doesn't have the required role(s)

## Testing

The implementation includes tests for:

- JWT token validation
- Role-based access control
- Access to protected endpoints

## Conclusion

This security system provides a robust, standards-based approach to authentication and authorization. By leveraging
Keycloak and implementing role-based access control, it ensures that only authorized users can access sensitive data and
perform operations in the Eagle Bank API.