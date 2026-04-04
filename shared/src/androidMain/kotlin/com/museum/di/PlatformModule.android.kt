package com.museum.di

import com.museum.data.local.DatabaseDriverFactory
import com.whitelabel.core.AppConfig
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val appPlatformModule = module {
    // DatabaseDriverFactory - Android implementation
    single { DatabaseDriverFactory(androidContext(), "heritage_sites.db") }
    // App configuration
    single { AppConfig(enableMap = true, enableCategories = true) }
}
