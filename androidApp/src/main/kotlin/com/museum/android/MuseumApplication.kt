package com.museum.android

import android.app.Application
import android.os.StrictMode
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import coil3.util.DebugLogger
import com.museum.di.initKoin
import com.whitelabel.platform.utils.LanguagePreferences
import com.whitelabel.platform.utils.DataStoreLanguagePersistence
import com.whitelabel.platform.utils.LocationFilterPreferences
import com.whitelabel.platform.utils.DataStoreLocationFilterPersistence
import com.whitelabel.platform.utils.OnboardingPreferences
import com.whitelabel.platform.utils.DataStoreOnboardingPersistence
import com.museum.utils.LocaleManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level

class MuseumApplication : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
        }

        LanguagePreferences.initPersistence(DataStoreLanguagePersistence(this))
        LocationFilterPreferences.initPersistence(DataStoreLocationFilterPersistence(this))
        OnboardingPreferences.initPersistence(DataStoreOnboardingPersistence(this))

        val savedLanguage = LanguagePreferences.getEffectiveLanguage()
        LocaleManager.setAppLocale(this, if (savedLanguage == LocaleManager.getAppLocale(this)) null else savedLanguage)

        CoroutineScope(Dispatchers.Main).launch {
            LanguagePreferences.selectedLanguage.collect { language ->
                LocaleManager.setAppLocale(this@MuseumApplication, language?.code)
            }
        }

        initKoin {
            androidLogger(Level.ERROR)
            androidContext(this@MuseumApplication)
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory())
            }
            .memoryCache {
                coil3.memory.MemoryCache.Builder()
                    .maxSizePercent(context, 0.30)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .crossfade(true)
            .logger(DebugLogger())
            .build()
    }
}
