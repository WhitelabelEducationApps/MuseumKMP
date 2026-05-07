package com.museum.di

import com.whitelabel.platform.data.local.DatabaseDriverFactory
import com.whitelabel.core.AppConfig
import com.whitelabel.core.presentation.home.ItemGrouper
import com.whitelabel.platform.data.models.CatalogItem
import com.museum.presentation.CountryItemGrouper
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val appPlatformModule = module {
    single { DatabaseDriverFactory(androidContext(), "heritage_sites.db") }
    single { AppConfig(enableMap = true, enableCategories = false, enableLocationFilter = false) }
    single<ItemGrouper<CatalogItem>> { CountryItemGrouper() }
}
