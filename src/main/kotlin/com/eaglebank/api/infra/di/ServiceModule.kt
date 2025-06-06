
package com.eaglebank.api.infra.di

import com.eaglebank.api.infra.validation.SimpleUserRequestValidationService
import org.koin.dsl.module

val serviceModule = module {
    single { SimpleUserRequestValidationService() }
}