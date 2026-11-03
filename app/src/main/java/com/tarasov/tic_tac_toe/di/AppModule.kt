package com.tarasov.tic_tac_toe.di

import com.tarasov.tic_tac_toe.MainViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    viewModelOf(::MainViewModel)
   // singleOf(::exampleClass)
}