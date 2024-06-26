package com.example.docscanner.Database.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.docscanner.Database.Entity.ScannedDocument

@Dao
interface ScannedDocumentDao {
    @Insert
    suspend fun insert(scannedDocument: ScannedDocument)

    @Query("SELECT * FROM scanned_documents")
    suspend fun getAllDocuments(): List<ScannedDocument>
}