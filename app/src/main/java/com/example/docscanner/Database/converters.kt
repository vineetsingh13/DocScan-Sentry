package com.example.docscanner.Database

import androidx.room.TypeConverter

class converters {

    @TypeConverter
    fun fromByteArray(byteArray: ByteArray?): String {
        return byteArray?.let { String(it) } ?: ""
    }

    @TypeConverter
    fun toByteArray(byteString: String): ByteArray {
        return byteString.toByteArray()
    }
}