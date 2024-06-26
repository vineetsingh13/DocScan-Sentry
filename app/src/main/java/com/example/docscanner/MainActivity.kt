package com.example.docscanner

import android.app.Activity
import android.app.AlertDialog
import android.content.IntentSender
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.SearchView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.bumptech.glide.Glide
import com.example.docscanner.Database.Adapters.DocumentAdapter
import com.example.docscanner.Database.AppDatabase
import com.example.docscanner.Database.Entity.ScannedDocument
import com.example.docscanner.databinding.ActivityMainBinding
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: DocumentAdapter
    private var documents: MutableList<ScannedDocument> = mutableListOf()
    private lateinit var db: AppDatabase

    private lateinit var fileNameTextBox: EditText
    private lateinit var scannerLauncher: ActivityResultLauncher<IntentSenderRequest>
    private lateinit var model: model
    private var enableGalleryImport = true
    private val FULL_MODE = "FULL"
    private val BASE_MODE = "BASE"
    private val BASE_MODE_WITH_FILTER = "BASE_WITH_FILTER"
    private var selectedMode = FULL_MODE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "DocScanner"
        ).build()

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = DocumentAdapter(documents)
        binding.recyclerView.adapter = adapter
        model = model()

        scannerLauncher = registerForActivityResult(StartIntentSenderForResult()) { result ->
            handleActivityResult(result)
        }

        populateModeSelector()

        lifecycleScope.launch {
            documents = db.scannedDocumentDao().getAllDocuments().toMutableList()
            adapter.updateDocuments(documents)
        }

        binding.searchBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    adapter.filterDocuments(it) // Filter documents on text change
                }
                return true
            }
        })

    }

    fun onEnableGalleryImportCheckboxClicked(view: View) {
        enableGalleryImport = (view as CheckBox).isChecked
    }

    @Suppress("UNUSED_PARAMETER")
    fun onScanButtonClicked(unused: View) {
        //binding.resultInfo.text = null
        //Glide.with(this).clear(binding.firstPageView)

        val options = GmsDocumentScannerOptions.Builder()
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_BASE)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
            .setGalleryImportAllowed(enableGalleryImport)

        when (selectedMode) {
            FULL_MODE -> options.setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            BASE_MODE -> options.setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_BASE)
            BASE_MODE_WITH_FILTER -> options.setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_BASE_WITH_FILTER)
            else -> Log.e(TAG, "Unknown selectedMode: $selectedMode")
        }

        val pageLimitInputText = binding.pageLimitInput.text.toString()
        if (pageLimitInputText.isNotEmpty()) {
            try {
                val pageLimit = pageLimitInputText.toInt()
                options.setPageLimit(pageLimit)
            } catch (e: Throwable) {
                //binding.resultInfo.text = e.message
                return
            }
        }

        model.getStartScanIntent(this, options.build())
            .addOnSuccessListener { intentSender: IntentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener { e: Exception ->
                //binding.resultInfo.text = getString(R.string.error_default_message, e.message)
            }
    }

    private fun populateModeSelector() {
        val featureSpinner = findViewById<Spinner>(R.id.mode_selector)
        val options: MutableList<String> = ArrayList()
        options.add(FULL_MODE)
        options.add(BASE_MODE)
        options.add(BASE_MODE_WITH_FILTER)

        val dataAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        featureSpinner.adapter = dataAdapter
        featureSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, selectedItemView: View?, pos: Int, id: Long) {
                selectedMode = parentView.getItemAtPosition(pos).toString()
            }

            override fun onNothingSelected(arg0: AdapterView<*>?) {}
        }
    }

    private fun handleActivityResult(activityResult: ActivityResult) {
        val resultCode = activityResult.resultCode
        val result = GmsDocumentScanningResult.fromActivityResultIntent(activityResult.data)
        if (resultCode == Activity.RESULT_OK && result != null) {
            updateUIWithScanResult(result)
        } else if (resultCode == Activity.RESULT_CANCELED) {
            //binding.resultInfo.text = getString(R.string.error_scanner_cancelled)
        } else {
            //binding.resultInfo.text = getString(R.string.error_default_message)
        }
    }

    private fun updateUIWithScanResult(result: GmsDocumentScanningResult) {
        //binding.resultInfo.text = getString(R.string.scan_result, result)

//        val pages = result.pages
//        if (pages != null && pages.isNotEmpty()) {
//            Glide.with(this).load(pages[0].imageUri).into(binding.firstPageView)
//        }

        val dialogView = layoutInflater.inflate(R.layout.alert_dialog_layout, null)
        val builder = AlertDialog.Builder(this)

        fileNameTextBox = dialogView.findViewById(R.id.password)
        builder.setView(dialogView)
            .setPositiveButton("Save") { dialog, id ->
                var fname = fileNameTextBox.text.toString().trim()
                if (!fname.endsWith(".pdf")) {
                    fname += ".pdf"
                }
                result.pdf?.uri?.path?.let { path ->
                    //saveFileToDownloads(path, fname.trim())
                    saveDocumentToDatabase(fname, path)
                }
            }
            .setNegativeButton("Cancel") { dialog, id ->
                dialog.cancel()
            }

        val alert = builder.create()
        alert.show()
    }

    private fun saveFileToDownloads(path: String, fileName: String) {
        val externalUri = FileProvider.getUriForFile(this, packageName + ".provider", File(path))
        val contentResolver = applicationContext.contentResolver
        val downloadDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        contentResolver.openInputStream(externalUri)?.use { inputStream ->
            val destinationFile = File(downloadDirectory, fileName)
            FileOutputStream(destinationFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            Toast.makeText(this, "file saved in downloads", Toast.LENGTH_SHORT).show()
        }
    }


    private fun saveDocumentToDatabase(fileName: String, filePath: String) {
        val scannedDocument = ScannedDocument(fileName = fileName, filePath = filePath)
        lifecycleScope.launch {
            db.scannedDocumentDao().insert(scannedDocument)
            documents = db.scannedDocumentDao().getAllDocuments().toMutableList()
            adapter.updateDocuments(documents)

        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}



//                val shareIntent =
//                    Intent(Intent.ACTION_SEND).apply {
//                        putExtra(Intent.EXTRA_STREAM, externalUri)
//                        type = "application/pdf"
//                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//                    }
//                startActivity(Intent.createChooser(shareIntent, "share pdf"))