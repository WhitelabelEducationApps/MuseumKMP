package com.museum.utils

import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleManager {
    fun setAppLocale(context: Context, languageCode: String?) {
        val locale = if (languageCode != null) {
            Locale(languageCode)
        } else {
            Locale.getDefault()
        }

        Locale.setDefault(locale)

        val localeList = LocaleListCompat.create(locale)
        AppCompatDelegate.setApplicationLocales(localeList)

        val config = context.resources.configuration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    fun getAppLocale(context: Context): String {
        val locale = ConfigurationCompat.getLocales(context.resources.configuration)
        return if (locale.size() > 0) {
            locale[0]?.language ?: Locale.getDefault().language
        } else {
            Locale.getDefault().language
        }
    }
}
