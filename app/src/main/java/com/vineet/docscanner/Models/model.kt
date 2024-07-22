package com.vineet.docscanner.Models

import android.app.Activity
import android.content.Context
import android.content.IntentSender
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

data class DocumentScanResult(
    val result: GmsDocumentScanningResult?
)

class model {
    fun getStartScanIntent(
        context: Context,
        options: GmsDocumentScannerOptions
    ): Task<IntentSender> {
        return GmsDocumentScanning.getClient(options).getStartScanIntent(context as Activity)
    }
}