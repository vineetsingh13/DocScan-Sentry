package com.example.docscanner.Database.Adapters

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.docscanner.Database.Entity.ScannedDocument
import com.example.docscanner.R
import java.io.File
import java.io.FileOutputStream

class DocumentAdapter(private var documents: List<ScannedDocument>) : RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder>() {

    private var filteredDocuments: List<ScannedDocument> = documents

    inner class DocumentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fileNameView: TextView = itemView.findViewById(R.id.file_name)
        //val filePathView: TextView = itemView.findViewById(R.id.file_path)
        val viewButton=itemView.findViewById<Button>(R.id.view_icon)
        val downloadButton=itemView.findViewById<Button>(R.id.download_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_document, parent, false)
        return DocumentViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        val document = filteredDocuments[position]
        holder.fileNameView.text = document.fileName
        //holder.filePathView.text = document.filePath
        holder.itemView.setOnClickListener {
            displayDocument(document.filePath,holder.itemView.context)
        }

        holder.viewButton.setOnClickListener {
            displayDocument(document.filePath,holder.itemView.context)
        }

        holder.downloadButton.setOnClickListener {
            downloadDocument(document.filePath,document.fileName,holder.itemView.context)
        }

        //
        holder.itemView.apply {
            alpha = 0f
            scaleX = 0.5f
            scaleY = 0.5f
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .start()
        }
    }

    override fun getItemCount(): Int {
        return filteredDocuments.size
    }

    fun updateDocuments(newDocuments: List<ScannedDocument>) {
        documents = newDocuments
        filteredDocuments = newDocuments
        notifyDataSetChanged()
    }

    fun filterDocuments(query: String) {
        filteredDocuments = if (query.isEmpty()) {
            documents // Show all documents if query is empty
        } else {
            documents.filter { it.fileName.contains(query, ignoreCase = true) } // Filter documents by fileName containing query
        }
        notifyDataSetChanged()
    }


    fun displayDocument(path:String,context: Context){
        val file = File(path)
        val contentUri = FileProvider.getUriForFile(context, context.packageName + ".provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }


    fun downloadDocument(path:String, fileName: String, context: Context){
        val externalUri = FileProvider.getUriForFile(context, context.packageName + ".provider", File(path))
        val contentResolver = context.contentResolver
        val downloadDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        contentResolver.openInputStream(externalUri)?.use { inputStream ->
            val destinationFile = File(downloadDirectory, fileName)
            FileOutputStream(destinationFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            Toast.makeText(context, "file saved in downloads", Toast.LENGTH_SHORT).show()
        }
    }
}
