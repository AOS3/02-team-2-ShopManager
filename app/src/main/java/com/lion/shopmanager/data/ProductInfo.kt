package com.lion.shopmanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Product")
data class ProductInfo(
    @PrimaryKey(autoGenerate = true) val idx: Int = 0,
    val productName: String,
    val productPrice: Int,
    val productLabel: String,
    val productPath: List<String>,
    val productSale: Boolean = true
)