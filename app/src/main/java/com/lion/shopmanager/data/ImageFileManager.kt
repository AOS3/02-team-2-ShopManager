package com.lion.shopmanager.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.bumptech.glide.Glide
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

object ImageFileManager {
    private const val FILE_NAME = "product_img.dat"

    fun saveImage(context: Context, imageId: String, bitmap: Bitmap) {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) {
            file.createNewFile()
        }

        RandomAccessFile(file, "rw").use { raf ->
            raf.seek(file.length())

            val idBytes = imageId.toByteArray()
            raf.writeInt(idBytes.size)
            raf.write(idBytes)

            val imageData = bitmapToByteArray(bitmap)
            raf.writeInt(imageData.size)
            raf.write(imageData)
        }
    }

    fun saveImageByModify(context: Context, imageId: String, bitmap: Bitmap): Boolean {
        return try {
            val file = File(context.filesDir, "$imageId.jpg") // 고유 파일 경로 생성
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream) // 이미지를 JPEG로 저장
            }
            true // 저장 성공
        } catch (e: Exception) {
            e.printStackTrace()
            false // 저장 실패
        }
    }

    // 파일에서 이미지 읽기
    fun loadImage(context: Context, imageId: String): Bitmap? {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return null // 파일이 없으면 null 반환

        RandomAccessFile(file, "r").use { raf ->
            while (raf.filePointer < raf.length()) {
                // ID 읽기
                val idLength = raf.readInt()
                val idBytes = ByteArray(idLength)
                raf.read(idBytes)
                val currentId = String(idBytes)

                // 이미지 데이터 읽기
                val imageLength = raf.readInt()
                val imageData = ByteArray(imageLength)
                raf.read(imageData)

                if (currentId == imageId) {
                    // ID가 일치하면 Bitmap으로 변환 후 반환
                    return BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                }
            }
        }
        return null // 이미지 ID가 없으면 null 반환
    }

    fun deleteImage(context: Context, imageId: String): Boolean {
        return try {
            val file = File(context.filesDir, "$imageId.jpg")
            file.delete() // 파일 삭제
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Bitmap -> ByteArray 변환
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        return stream.toByteArray()
    }
}