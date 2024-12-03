package com.lion.shopmanager.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ProductDAO {
    @Insert
    fun insert(productInfo: ProductInfo)

    @Delete
    fun delete(productInfo: ProductInfo)

    @Update
    fun update(productInfo: ProductInfo)

    /* Query */
    @Query("SELECT * FROM product")
    fun getAllProduct(): List<ProductInfo>

    @Query("SELECT * FROM product WHERE idx = :idx")
    fun getProductByIdx(idx: Int): List<ProductInfo>

    @Query("SELECT * FROM product WHERE productSale = :isSale")
    fun getProductSale(isSale: Boolean): List<ProductInfo>

    @Query("DELETE FROM product WHERE idx = :idx")
    fun deleteByIdx(idx: Int)

    @Query(
        "UPDATE product SET productName = :productName," +
                " productPrice = :productPrice, " +
                "productLabel = :productLabel, " +
                "productPath = :productPath WHERE idx = :idx"
    )
    fun updateByIdx(
        productName: String,
        productPrice: Int, productLabel: String, productPath: List<String>, idx: Int
    )

    @Query("UPDATE product SET productSale = :productSale WHERE idx = :idx")
    fun updateSaleByIdx(productSale: Boolean, idx: Int)
}