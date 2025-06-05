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

The application looks for its configuration file in `./etc/eagle-bank/application-dev.conf`.
This file contains sensitive information and should never be committed to version control.

A template configuration file is provided at `config/application.conf.template` for reference.

TODO This system would need a revisit when thinking about how the application would be containerised and deployed. 
## Future Questions:
### Security
1. What identity system should be used?  This should facilitate fine grain controls and audit.
2. Only a valid admin should be able to create a user.  For that we would need a fine grain token, with roles packed inside.
3. Deployment model would require protections, WAF, DoS
4. Sharing of the security implimentation.  Does not scale on many levels.

### Other
1. High availability
2. Tracing of the app
3. Deployment options.  Containerisation etc.



