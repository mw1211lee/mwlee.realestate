package com.study.mwlee.realestate.network

import com.google.gson.annotations.SerializedName


class GeocodingResponse {
    @SerializedName("status")
    var status: String? = null

    @SerializedName("meta")
    var meta: Meta? = null

    @SerializedName("addresses")
    var addresses: List<Address>? = null

    @SerializedName("errorMessage")
    var errorMessage: String? = null
}

class Address {
    @SerializedName("roadAddress")
    var roadAddress: String? = null

    @SerializedName("jibunAddress")
    var jibunAddress: String? = null

    @SerializedName("englishAddress")
    var englishAddress: String? = null

    @SerializedName("addressElements")
    var addressElements: List<AddressElement>? = null

    @SerializedName("x")
    var x: String? = null

    @SerializedName("y")
    var y: String? = null

    @SerializedName("distance")
    var distance: Double? = null
}

class AddressElement {
    @SerializedName("types")
    var types: List<String>? = null

    @SerializedName("longName")
    var longName: String? = null

    @SerializedName("shortName")
    var shortName: String? = null

    @SerializedName("code")
    var code: String? = null
}

class Meta {
    @SerializedName("totalCount")
    var totalCount: Int? = null

    @SerializedName("page")
    var page: Int? = null

    @SerializedName("count")
    var count: Int? = null
}