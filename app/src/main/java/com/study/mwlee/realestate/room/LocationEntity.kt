package com.study.mwlee.realestate.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location")
class LocationEntity(
    /* Common */
    @PrimaryKey
    val address: String,
    val latitude: String?,
    val longitude: String?
)