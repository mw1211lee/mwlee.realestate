package com.study.mwlee.realestate.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AptDao {

    @Query("SELECT * FROM apt")
    suspend fun getAptAllData(): List<AptEntity>

    @Query("SELECT * FROM apt WHERE isTrade = :isTrade AND apartmentName = :aptName ORDER BY dealYear DESC, dealMonth DESC, dealDay DESC")
    suspend fun getAptData(isTrade: Boolean, aptName: String): List<AptEntity>

    @Query("SELECT COUNT(*) FROM apt WHERE isTrade = :isTrade AND buildYear = :buildYear AND apartmentName = :apartmentName AND dong = :dong AND dealYear = :dealYear AND dealMonth = :dealMonth AND dealDay = :dealDay AND areaForExclusiveUse = :areaForExclusiveUse AND jibun = :jibun AND regionalCode = :regionalCode AND floor = :floor AND dealAmount = :dealAmount AND cancelDealType = :cancelDealType AND cancelDealDay = :cancelDealDay AND deposit = :deposit AND monthlyRent = :monthlyRent AND dongPlusJibun = :dongPlusJibun")
    suspend fun getAptData(
        isTrade: Boolean,
        buildYear: Int,
        apartmentName: String,
        dong: String,
        dealYear: Int,
        dealMonth: Int,
        dealDay: Int,
        areaForExclusiveUse: Double,
        jibun: String,
        regionalCode: Int,
        floor: Int,
        dealAmount: String,
        cancelDealType: String,
        cancelDealDay: String,
        deposit: String,
        monthlyRent: String,
        dongPlusJibun: String
    ): Int

    @Query("SELECT DISTINCT areaForExclusiveUse FROM apt WHERE apartmentName = :aptName")
    suspend fun getAptAreaData(aptName: String): List<Double>

    @Query("SELECT COUNT(*) FROM apt WHERE dealYear = :year AND dealMonth = :month AND regionalCode = :regionalCode AND isTrade = :isTrade")
    suspend fun getAptMonthData(year: Int, month: Int, regionalCode: Int, isTrade: Boolean): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(aptEntity: AptEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(aptEntityList: List<AptEntity>)

//    @Query("UPDATE book set name = :name, author = :author, kind = :kind, price = :price WHERE idx = :idx")
//    fun update(idx : Int, name : String, author : String, kind : String, price : Int)

//    @Query("DELETE FROM book WHERE idx = :idx")
//    fun delete(idx : Int)
}