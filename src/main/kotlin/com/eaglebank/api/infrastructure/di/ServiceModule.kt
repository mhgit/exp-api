
package com.eaglebank.api.infrastructure.di

import com.eaglebank.api.infrastructure.validation.ValidationService
import org.koin.dsl.module

val serviceModule = module {
    single { ValidationService() }
}