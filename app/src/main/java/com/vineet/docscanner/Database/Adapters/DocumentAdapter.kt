package com.vineet.docscanner.Database.Adapters

import android.app.AlertDialog
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
import com.vineet.docscanner.Database.DAO.ScannedDocumentDao
import com.vineet.docscanner.Database.Entity.ScannedDocument
import com.vineet.docscanner.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class DocumentAdapter(
    private var documents: MutableList<ScannedDocument>,
    private val scannedDocumentDao: ScannedDocumentDao
) : RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder>() {

    private var filteredDocuments: MutableList<ScannedDocument> = documents

    inner class DocumentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fileNameView: TextView = itemView.findViewById(R.id.file_name)
        //val filePathView: TextView = itemView.findViewById(R.id.file_path)
        val downloadButton=itemView.findViewById<Button>(R.id.download_icon)
        val shareButton=itemView.findViewById<Button>(R.id.share_icon)
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


        holder.downloadButton.setOnClickListener {
            downloadDocument(document.filePath,document.fileName,holder.itemView.context)
        }

        holder.shareButton.setOnClickListener {

            val externalUri = FileProvider.getUriForFile(holder.itemView.context, holder.itemView.context.packageName + ".provider", File(document.filePath))

            val shareIntent =
                Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_STREAM, externalUri)
                    type = "application/pdf"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            holder.itemView.context.startActivity(Intent.createChooser(shareIntent, "share pdf"))
        }

        holder.itemView.setOnLongClickListener {
            deleteDocument(position,document,holder.itemView.context)
            true
        }

        //animated the itemview
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

    fun updateDocuments(newDocuments: MutableList<ScannedDocument>) {
        documents = newDocuments
        filteredDocuments = newDocuments
        notifyDataSetChanged()
    }

    //function for displaying the filtered list
    fun filterDocuments(query: String) {
        filteredDocuments = if (query.isEmpty()) {
            documents // Show all documents if query is empty
        } else {
            documents.filter { it.fileName.contains(query, ignoreCase = true) }.toMutableList() // Filter documents by fileName containing query
        }
        notifyDataSetChanged()
    }


    //function to display the document
    fun displayDocument(path:String,context: Context){
        val file = File(path)
        val contentUri = FileProvider.getUriForFile(context, context.packageName + ".provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }


    //function to download the document to the device
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

    private fun deleteDocument(position: Int,doc:ScannedDocument, context: Context) {
        val document = filteredDocuments[position]

        val builder = AlertDialog.Builder(context)
        builder.setMessage("Are you sure you want to delete")
            .setCancelable(false)
            .setPositiveButton("Yes"){dialog,id->
                CoroutineScope(Dispatchers.IO).launch {
                    scannedDocumentDao.delete(doc)
                    CoroutineScope(Dispatchers.Main).launch {
                        filteredDocuments.removeAt(position)
                        documents.remove(document)
                        notifyItemRemoved(position)
                        notifyItemRangeChanged(position, itemCount)

                        Toast.makeText(context, "Document deleted", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("No"){ dialog,id->
                dialog.cancel()
            }

        val alert = builder.create()
        alert.setCanceledOnTouchOutside(false)
        alert.show()
    }
}
