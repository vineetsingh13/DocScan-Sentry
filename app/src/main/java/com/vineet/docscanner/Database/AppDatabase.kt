package com.vineet.docscanner.Database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.vineet.docscanner.Database.DAO.ScannedDocumentDao
import com.vineet.docscanner.Database.Entity.ScannedDocument
import javax.inject.Singleton

@Singleton
@Database(entities = [ScannedDocument::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scannedDocumentDao(): ScannedDocumentDao
}