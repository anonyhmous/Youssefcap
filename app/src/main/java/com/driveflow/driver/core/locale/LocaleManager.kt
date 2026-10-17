package com.driveflow.driver.core.locale

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.compose.ui.unit.LayoutDirection
import java.util.Locale

object AppLocaleManager {
    fun applyLocale(context: Context, langCode: String): Context {
        val targetLocale = if (langCode == "en") Locale.ENGLISH else Locale("ar")
        Locale.setDefault(targetLocale)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val systemLocaleManager = context.getSystemService(LocaleManager::class.java)
            systemLocaleManager?.applicationLocales = LocaleList(targetLocale)
        }

        val config = Configuration(context.resources.configuration)
        config.setLocale(targetLocale)
        config.setLayoutDirection(targetLocale)
        return context.createConfigurationContext(config)
    }

    fun getLayoutDirection(langCode: String): LayoutDirection {
        return if (langCode == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
    }

    fun isRtl(langCode: String): Boolean = langCode == "ar"
}
