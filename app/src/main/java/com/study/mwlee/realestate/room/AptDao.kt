package com.study.mwlee.realestate.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AptDao {

    @Query("SELECT * FROM apt")
    suspend fun getAptAllData(): List<AptEntity>

    @Query("SELECT COUNT(*) FROM apt WHERE dealYear = :year AND dealMonth = :month AND regionalCode = :regionalCode")
    suspend fun getAptMonthData(year: Int, month: Int, regionalCode: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(aptEntity: AptEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(aptEntityList: List<AptEntity>)

//    @Query("UPDATE book set name = :name, author = :author, kind = :kind, price = :price WHERE idx = :idx")
//    fun update(idx : Int, name : String, author : String, kind : String, price : Int)

//    @Query("DELETE FROM book WHERE idx = :idx")
//    fun delete(idx : Int)
}