# Eagle Bank API - Setup Guide

This document provides detailed instructions for setting up and configuring the Eagle Bank API project.

## Installation

To get started with the project, follow these steps:

⚠️This project is just a development setup. No thought yet given to productionisation. Any security
information is just for the purposes of a demonstration.

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

⚠️ **Important**: Environment-specific configuration files contain sensitive information and should never be committed
to version control.
A full discussion around password vaults etc will be required before this example is deployed.

Note: For containerized deployments, configuration can be provided through environment variables that override the
corresponding HOCON settings.

## Keycloak Setup and Usage

### Authentication Server Management

The project uses Keycloak as the authentication server. Three scripts are provided to manage the Keycloak instance:

1. `./scripts/start-keycloak.sh` - Downloads (if needed) and starts the Keycloak server
   ```bash
   ./scripts/start-keycloak.sh
   ```
   This script will:
   - Download Keycloak 26.2.5 if not present (stored in `./dev/keycloak-26.2.5`)
   - Extract it to `./dev/keycloak/keycloak-26.2.5`
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

```bash 
curl -X POST "http://localhost:8082/auth/realms/eagle-bank/protocol/openid-connect/token"
-H "Content-Type: application/x-www-form-urlencoded"
-d "client_id=eagle-bank-api"
-d "username=test-user"
-d "password=test123"
-d "grant_type=password"
```

The response will include an access token that can be used in subsequent API calls:

```bash 
curl -H "Authorization: Bearer <your-token>" http://localhost:8080/api/v1/your-endpoint
```

### Keycloak Admin Console

The Keycloak admin console can be accessed at:

- URL: http://localhost:8082/auth/admin
- Username: admin
- Password: admin

Remember to select the "eagle-bank" realm (top-left dropdown) when managing resources for this project.

### Audience Mapper Configuration

For detailed information on how to find and check the audience mapper in the Keycloak admin UI, please refer to
the [Keycloak Mapper Guide](./README-keycloak-mapper.md).
