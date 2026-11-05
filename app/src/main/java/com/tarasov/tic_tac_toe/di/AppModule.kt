package com.tarasov.tic_tac_toe.di

import com.tarasov.tic_tac_toe.MainViewModel
import com.tarasov.tic_tac_toe.data.LoginRepository
import com.tarasov.tic_tac_toe.data.LoginRepositoryImpl
import com.tarasov.tic_tac_toe.screens.LoginViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val appModule = module {
    includes(networkModule)

    viewModelOf(::MainViewModel)
    viewModelOf(::LoginViewModel)
    singleOf(::LoginRepositoryImpl) bind LoginRepository::class
}