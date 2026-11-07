package com.tarasov.tic_tac_toe.di

import com.tarasov.tic_tac_toe.MainViewModel
import com.tarasov.tic_tac_toe.data.LobbyRepository
import com.tarasov.tic_tac_toe.data.LobbyRepositoryImpl
import com.tarasov.tic_tac_toe.data.AuthRepository
import com.tarasov.tic_tac_toe.data.AuthRepositoryImpl
import com.tarasov.tic_tac_toe.screens.LobbyViewModel
import com.tarasov.tic_tac_toe.screens.LoginViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val appModule = module {
    includes(networkModule)

    viewModelOf(::MainViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::LobbyViewModel)
    singleOf(::AuthRepositoryImpl) bind AuthRepository::class
    singleOf(::LobbyRepositoryImpl) bind LobbyRepository::class
}