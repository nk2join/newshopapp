package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shop_settings")
data class ShopSetting(
    @PrimaryKey
    val key: String,
    val value: String
)
