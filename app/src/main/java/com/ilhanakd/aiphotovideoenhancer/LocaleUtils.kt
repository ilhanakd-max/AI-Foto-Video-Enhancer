package com.ilhanakd.aiphotovideoenhancer

import android.content.Context
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object LocaleUtils {
    fun updateLocale(context: Context, code: String?): Context {
        val locale = if (code.isNullOrEmpty()) Locale.getDefault() else Locale(code)
        Locale.setDefault(locale)
        val resources = context.resources
        val config = resources.configuration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        } else {
            config.setLocale(locale)
        }
        return context.createConfigurationContext(config)
    }
}
