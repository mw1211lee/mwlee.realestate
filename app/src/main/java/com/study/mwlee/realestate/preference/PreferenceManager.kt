package com.study.mwlee.realestate.preference

import android.content.Context
import android.content.SharedPreferences
import com.naver.maps.geometry.LatLng
import com.study.mwlee.realestate.preference.PreferenceHelper.set
import com.study.mwlee.realestate.preference.PreferenceHelper.get

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = PreferenceHelper.defaultPrefs(context)

    fun saveTradeLastUpdateDate(date: String) {
        prefs["tradeLastUpdateDate"] = date
    }

    fun getTradeLastUpdateDate(): String {
        return prefs["tradeLastUpdateDate", ""]
    }

    fun saveRentLastUpdateDate(date: String) {
        prefs["rentLastUpdateDate"] = date
    }

    fun getRentLastUpdateDate(): String {
        return prefs["rentLastUpdateDate", ""]
    }

    fun saveLastLocation(data: LatLng) {
        prefs["lastLatitude"] = data.latitude
        prefs["lastLongitude"] = data.longitude
    }

    fun getLastLocation(): LatLng {
        return LatLng(prefs["lastLatitude", 0.0], prefs["lastLongitude", 0.0])
    }
}
