#!/bin/bash

# Create configuration directory
mkdir -p ./etc/eagle-bank

# Generate a random JWT secret
JWT_SECRET=$(openssl rand -base64 32)

# Create the configuration file
tee ./etc/eagle-bank/application-dev.conf << EOF
ktor {
    deployment {
        port = 8080
        port = \${?PORT}
    }
    application {
        modules = [ com.eaglebank.api.ApplicationKt.module ]
    }
}

jwt {
    secret = "$JWT_SECRET"
    issuer = "http://0.0.0.0:8080/"
    audience = "jwt-audience"
    realm = "Eagle Bank API"
    accessToken {
        expiration = 900000  # 15 minutes in milliseconds
    }
    refreshToken {
        expiration = 2592000000  # 30 days in milliseconds
    }
}
EOF

# Set proper permissions
chmod 644 ./etc/eagle-bank/application-dev.conf