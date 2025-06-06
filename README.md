# Eagle Bank API

This project provides a foundational API for a banking application, built with Kotlin and Ktor. It demonstrates core backend functionalities, including user management with persistence, request validation, and JWT-based authentication.

## Table of Contents

- [Features](#features)
- [Technologies Used](#technologies-used)
- [Installation](#installation)
- [Running the Application](#running-the-application)
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