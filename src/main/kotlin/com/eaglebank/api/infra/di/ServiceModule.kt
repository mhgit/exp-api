package com.eaglebank.api.infra.di

import com.eaglebank.api.domain.repository.IUserRepository
import com.eaglebank.api.infra.persistence.UserRepository
import org.koin.dsl.module

val userModule = module {
    single<IUserRepository> { UserRepository() }
}