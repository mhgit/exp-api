## Eagle Bank API

This project provides a foundational API for a banking application, built with Kotlin and Ktor. It demonstrates core backend functionalities, including user management with persistence, request validation, and JWT-based authentication.

## Table of Contents

- [Features](#features)
- [Technologies Used](#technologies-used)
- [Installation](#installation)
- [Running the Application](#running_the_application)
- [API Endpoints](#api-endpoints)
- [Testing](#testing)
- [Configuration](#configuration)
- [Future Considerations](#future-considerations)
- [Contributing](#contributing)
- [License](#license)

## Features

-   **User Management:** CRUD (Create, Read, Update, Delete) operations for user entities.
-   **Request Validation:** Basic validation for incoming API requests to ensure data integrity.
-   **JWT Authentication:** Secure access to protected API endpoints using JSON Web Tokens.
-   **Database Integration:** Uses an in-memory H2 database for development and testing, with a flexible design for other SQL databases.
-   **Ktor Framework:** Leverages Ktor's asynchronous and lightweight framework for building web applications.
-   **Dependency Injection:** Managed with Koin for easy service resolution and testability.
-   **Swagger/OpenAPI:** Automatic API documentation generation for easy exploration and testing of endpoints.

## Technologies Used

*   **Kotlin:** A modern, concise, and safe programming language for JVM.
*   **Ktor:** A flexible and asynchronous web framework for Kotlin.
*   **Koin:** A pragmatic lightweight dependency injection framework for Kotlin developers.
*   **H2 Database:** An in-memory relational database for development and testing.
*   **Gradle:** Build automation tool.
*   **JUnit 5:** Testing framework.
*   **MockK:** Mocking library for Kotlin.

## Installation

To get started with the project, follow these steps:

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/eagle-bank-api.git
   cd eagle-bank-api
   ```

2. Setup the Gradle Wrapper (if not already present):
   ```bash
   gradle wrapper
   ```

3. Start the application:
   ```bash
   ./gradlew run
   ```

## Configuration

The application uses HOCON configuration format with the following hierarchy:

1. Default configuration is embedded in `src/main/resources/application.conf`
2. Environment-specific configuration should be placed in:
   - Development: `./config/application-dev.conf`
   - Production: `./config/application-prod.conf`

A template configuration file is provided at `./config/application.conf.template`. Copy this file
to create your environment-specific configuration:

Key configuration parameters include:
- Database connection settings
- JWT authentication configuration
- Server port and host settings
- Logging configuration

⚠️ **Important**: Environment-specific configuration files contain sensitive information and should never be committed to version control.
A full discussion around password vaults etc will be required before this example is deployed.

Note: For containerized deployments, configuration can be provided through environment variables that override the corresponding HOCON settings.

## Keycloak Setup and Usage

### Authentication Server Management

The project uses Keycloak as the authentication server. Three scripts are provided to manage the Keycloak instance:

1. `./scripts/start-keycloak.sh` - Downloads (if needed) and starts the Keycloak server
   ```bash
   ./scripts/start-keycloak.sh
   ```
   This script will:
   - Download Keycloak 22.0.5 if not present (stored in `./infrastructure/keycloak`)
   - Extract it to `./infrastructure/keycloak/keycloak-22.0.5`
   - Start Keycloak on port 8082 (HTTP) and 9002 (management)

2. `./scripts/setup-keycloak-realm.sh` - Configures the Keycloak realm and resources
   ```bash
   ./scripts/setup-keycloak-realm.sh
   ```
   This script will:
   - Create the `eagle-bank` realm if it doesn't exist
   - Set up the `eagle-bank-api` client
   - Create required roles (`user`, `admin`, `account-manager`)
   - Create a test user with username: `test-user` and password: `test123`

3. `./scripts/stop-keycloak-server.sh` - Stops the Keycloak server
   ```bash
   ./scripts/stop-keycloak-server.sh
   ```

### Test User Login

To access the test user account:
1. Go to: http://localhost:8082/auth/realms/eagle-bank/account
2. Login with:
   - Username: `test-user`
   - Password: `test123`

Note: Make sure to use the specific URL above, as it directs to the correct realm (`eagle-bank`). 
If you try to log in through the admin console, it will attempt to authenticate against the master realm instead.

### Development Authentication

To obtain an authentication token for development/testing:
```bash curl -X POST "[http://localhost:8082/auth/realms/eagle-bank/protocol/openid-connect/token](http://localhost:8082/auth/realms/eagle-bank/protocol/openid-connect/token)"
-H "Content-Type: application/x-www-form-urlencoded"
-d "client_id=eagle-bank-api"
-d "username=test-user"
-d "password=test123"
-d "grant_type=password"
```

The response will include an access token that can be used in subsequent API calls:

```bash 
curl -H "Authorization: Bearer <your-token>" [http://localhost:8080/api/v1/your-endpoint](http://localhost:8080/api/v1/your-endpoint)
```

### Keycloak Admin Console

The Keycloak admin console can be accessed at:
- URL: http://localhost:8082/auth/admin
- Username: admin
- Password: admin

Remember to select the "eagle-bank" realm (top-left dropdown) when managing resources for this project.

## Architecture

The application follows a clean architecture pattern with distinct layers:
he application follows a clean architecture pattern with distinct layers:

```mermaid
graph TD
    R[Routes]
    DTO[DTOs]
    M[Mappers]
    DM[Domain Models]
    RI[Repository Interfaces]
    RE[Repository Implementations]
    E[Entities]
    DB[(Database)]

    R --> DTO
    DTO --> M
    M --> DM
    DM --> RI
    RI --> RE
    RE --> E
    E --> DB

    subgraph "Presentation Layer"
        R
        DTO
        M
    end
    
    subgraph "Domain Layer"
        DM
        RI
    end
    
    subgraph "Infrastructure Layer"
        RE
        E
        DB
    end

    style "Presentation Layer" fill:#f9f,stroke:#333,stroke-width:4px
    style "Domain Layer" fill:#bbf,stroke:#333,stroke-width:4px
    style "Infrastructure Layer" fill:#bfb,stroke:#333,stroke-width:4px
```


### Layer Responsibilities

- **Presentation Layer**
  - Routes: Handle HTTP requests and responses
  - DTOs: Data Transfer Objects for API requests/responses
  - Mappers: Convert between DTOs and Domain Models

- **Domain Layer**
  - Domain Models: Core business entities
  - Repository Interfaces: Define data access contracts

- **Infrastructure Layer**
  - Repository Implementations: Concrete data access logic
  - Entities: Database model representations
  - Database: Actual data storage

### Data Flow

1. HTTP Request → Route
2. Route receives DTO
3. Mapper converts DTO to Domain Model
4. Domain Layer processes business logic
5. Repository Interface defines data access
6. Repository Implementation handles persistence
7. Entity maps to database structure
8. Response flows back through the layers

This architecture ensures:
- Separation of concerns
- Domain logic isolation
- Infrastructure independence
- Testability
- Maintainability