## Installation

1. Clone the repository
2. Setup the gradle wrapper: ```gradle wrapper```
2. Run the setup script to create the configuration:
   ```bash
   ./scripts/setup.sh
   ```
   This will:
    - Create the configuration directory in `/etc/eagle-bank/`
    - Generate a secure random JWT secret
    - Set appropriate file permissions

3. Start the application:
   ```bash
   ./gradlew run
   ```

## Configuration

The application looks for its configuration file in `/etc/eagle-bank/application.conf`.
This file contains sensitive information and should never be committed to version control.

A template configuration file is provided at `config/application.conf.template` for reference.
