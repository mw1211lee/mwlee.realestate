package com.study.mwlee.realestate.preference

import android.content.Context
import android.content.SharedPreferences
import com.study.mwlee.realestate.preference.PreferenceHelper.set
import com.study.mwlee.realestate.preference.PreferenceHelper.get

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = PreferenceHelper.defaultPrefs(context)

    fun saveLastUpdateDate(date: String) {
        prefs["lastUpdateDate"] = date
    }

    fun getLastUpdateDate(): String {
        return prefs["lastUpdateDate", ""]
    }
}