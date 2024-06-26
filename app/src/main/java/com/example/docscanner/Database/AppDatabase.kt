package com.example.docscanner.Database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.docscanner.Database.DAO.ScannedDocumentDao
import com.example.docscanner.Database.Entity.ScannedDocument
import javax.inject.Singleton

@Singleton
@Database(entities = [ScannedDocument::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scannedDocumentDao(): ScannedDocumentDao
}