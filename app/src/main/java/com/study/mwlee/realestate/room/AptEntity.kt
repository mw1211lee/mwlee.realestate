package com.study.mwlee.realestate.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.study.mwlee.realestate.network.Item

@Entity(tableName = "apt")
class AptEntity(
    @PrimaryKey(autoGenerate = true)
    val key: Int,

    /* Common */
    val isTrade: Boolean,
    val buildYear: Int,
    val apartmentName: String,
    val dong: String,
    val dealYear: Int,
    val dealMonth: Int,
    val dealDay: Int,
    val areaForExclusiveUse: Double,
    val jibun: String,
    val regionalCode: Int,
    val floor: Int,

    /* Trade */
    val dealAmount: String,
    val cancelDealType: String,
    val cancelDealDay: String,

    /* Rent */
    val deposit: String,
    val monthlyRent: String
) {
    constructor(it: Item) : this(
        0, it.거래금액?.isNotEmpty() ?: false,
        it.건축년도 ?: 0,
        it.아파트 ?: "",
        it.법정동 ?: "",
        it.년 ?: 0,
        it.월 ?: 0,
        it.일 ?: 0,
        it.전용면적 ?: 0.0,
        it.지번 ?: "",
        it.지역코드 ?: 0,
        it.층 ?: 0,
        it.거래금액 ?: "",
        it.해제여부 ?: "",
        it.해제사유발생일 ?: "",
        it.보증금액 ?: "",
        it.월세금액 ?: ""
    )
}