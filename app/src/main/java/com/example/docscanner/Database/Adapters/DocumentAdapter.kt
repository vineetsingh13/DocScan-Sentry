package com.example.docscanner.Database.Adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.docscanner.Database.Entity.ScannedDocument
import com.example.docscanner.R
import java.io.File

class DocumentAdapter(private var documents: List<ScannedDocument>) : RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder>() {

    private var filteredDocuments: List<ScannedDocument> = documents

    inner class DocumentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fileNameView: TextView = itemView.findViewById(R.id.file_name)
        //val filePathView: TextView = itemView.findViewById(R.id.file_path)
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
            val file = File(document.filePath)
            val contentUri = FileProvider.getUriForFile(holder.itemView.context, holder.itemView.context.packageName + ".provider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            holder.itemView.context.startActivity(intent)
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
}
