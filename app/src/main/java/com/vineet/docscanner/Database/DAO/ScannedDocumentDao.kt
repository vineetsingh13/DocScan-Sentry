package com.vineet.docscanner.Database.DAO

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.vineet.docscanner.Database.Entity.ScannedDocument

@Dao
interface ScannedDocumentDao {
    @Insert
    suspend fun insert(scannedDocument: ScannedDocument)

    @Query("SELECT * FROM scanned_documents")
    suspend fun getAllDocuments(): List<ScannedDocument>

    @Delete
    suspend fun delete(scannedDocument: ScannedDocument)
}