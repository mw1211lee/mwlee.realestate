package com.study.mwlee.realestate.preference

import android.content.Context
import android.content.SharedPreferences
import com.naver.maps.geometry.LatLng
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

    fun saveLastLocation(data: LatLng) {
        prefs["lastLatitude"] = data.latitude
        prefs["lastLongitude"] = data.longitude
    }

    fun getLastLocation(): LatLng {
        return LatLng(prefs["lastLatitude", 0.0], prefs["lastLongitude", 0.0])
    }
}
