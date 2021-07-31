package com.study.mwlee.realestate.network

import com.tickaroo.tikxml.annotation.Element
import com.tickaroo.tikxml.annotation.PropertyElement
import com.tickaroo.tikxml.annotation.Xml


@Xml(name = "response")
class AptResponse(
    @Element
    val header: Header,
    @Element
    val body: Body,
)

@Xml(name = "header")
class Header(
    @PropertyElement(name = "resultCode")
    val resultCode: Int,
    @PropertyElement(name = "resultMsg")
    val resultMsg: String
)

@Xml(name = "body")
class Body(
    @Element
    val items: Items,

    @PropertyElement(name = "numOfRows") val numOfRows: Int,
    @PropertyElement(name = "pageNo") val pageNo: Int,
    @PropertyElement(name = "totalCount") val totalCount: Int,
)

@Xml(name = "items")
class Items(
    @Element
    val item: List<Item>?
)

@Suppress("NonAsciiCharacters")
@Xml(name = "item")
class Item(
    /* Common */
    @PropertyElement(name = "건축년도") var 건축년도: Int?,
    @PropertyElement(name = "년") var 년: Int?,
    @PropertyElement(name = "법정동") var 법정동: String?,
    @PropertyElement(name = "아파트") var 아파트: String?,
    @PropertyElement(name = "월") var 월: Int?,
    @PropertyElement(name = "일") var 일: Int?,
    @PropertyElement(name = "전용면적") var 전용면적: Double?,
    @PropertyElement(name = "지번") var 지번: String?,
    @PropertyElement(name = "지역코드") var 지역코드: Int?,
    @PropertyElement(name = "층") var 층: Int?,

    /* Trade */
    @PropertyElement(name = "거래금액") var 거래금액: String?,
    @PropertyElement(name = "해제여부") var 해제여부: String?,
    @PropertyElement(name = "해제사유발생일") var 해제사유발생일: String?,

    /* Rent */
    @PropertyElement(name = "보증금액") var 보증금액: String?,
    @PropertyElement(name = "월세금액") var 월세금액: String?,
)