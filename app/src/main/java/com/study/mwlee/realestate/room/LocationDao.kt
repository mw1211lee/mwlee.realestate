package com.study.mwlee.realestate.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LocationDao {

    @Query("SELECT * FROM location")
    suspend fun getLocationAllData(): List<LocationEntity>

    @Query("SELECT * FROM location WHERE address in (:address)")
    suspend fun getLocationListData(address: List<String>): List<LocationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(locationEntity: LocationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(locationEntityList: List<LocationEntity>)

//    @Query("UPDATE book set name = :name, author = :author, kind = :kind, price = :price WHERE idx = :idx")
//    fun update(idx : Int, name : String, author : String, kind : String, price : Int)

//    @Query("DELETE FROM book WHERE idx = :idx")
//    fun delete(idx : Int)
}