package com.example.musicplayer

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var currentSongIndex = -1

    private lateinit var listView: ListView
    private lateinit var btnPlayPause: Button
    private lateinit var btnNext: Button
    private lateinit var btnPrevious: Button

    private var musicFiles: MutableList<Uri> = mutableListOf()
    private lateinit var fileNames: MutableList<String>
    private lateinit var adapter: ArrayAdapter<String>

    private val folderPickerLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    listMusicFilesFromFolder(uri)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnFindFolder = findViewById<Button>(R.id.btnFindFolder)
        listView = findViewById(R.id.listView)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        btnNext = findViewById(R.id.btnNext)
        btnPrevious = findViewById(R.id.btnPrevious)

        btnPlayPause.setCompoundDrawablesWithIntrinsicBounds(R.drawable.play_icon, 0, 0, 0)
        btnNext.setCompoundDrawablesWithIntrinsicBounds(0,0, R.drawable.next, 0)
        btnPrevious.setCompoundDrawablesWithIntrinsicBounds(R.drawable.prev, 0, 0, 0)


        fileNames = mutableListOf()
        adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, fileNames) {
            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                if (position == currentSongIndex) {
                    view.setBackgroundColor(resources.getColor(R.color.green))
                    view.setTextColor(resources.getColor(R.color.white))
                } else {
                    view.setBackgroundColor(resources.getColor(android.R.color.transparent))
                    view.setTextColor(resources.getColor(R.color.black))
                }
                return view
            }
        }
        listView.adapter = adapter

        btnFindFolder.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            folderPickerLauncher.launch(intent)
        }

        listView.divider = ColorDrawable(resources.getColor(R.color.green)) // Set divider color
        listView.dividerHeight = 6 // Set divider height in pixels (optional)
        listView.setBackgroundResource(R.drawable.background)  // Set drawable background programmatically



        listView.setOnItemClickListener { _, _, position, _ ->
            if (musicFiles.isNotEmpty()) {
                currentSongIndex = position
                playMusic(musicFiles[currentSongIndex])
            }
        }

        btnPlayPause.setOnClickListener {
            if (isPlaying) {
                pauseMusic()
                btnPlayPause.setCompoundDrawablesWithIntrinsicBounds(R.drawable.play_icon, 0, 0, 0)
            } else {
                if (currentSongIndex == -1 && musicFiles.isNotEmpty()) {
                    currentSongIndex = 0
                }
                if (currentSongIndex != -1) {
                    playMusic(musicFiles[currentSongIndex])
                    btnPlayPause.setCompoundDrawablesWithIntrinsicBounds(R.drawable.pause_icon, 0, 0, 0)
                }
            }
        }

        btnNext.setOnClickListener {
            playNextSong()
        }

        btnPrevious.setOnClickListener {
            playPreviousSong()
        }
    }

    private fun listMusicFilesFromFolder(uri: Uri) {
        musicFiles.clear()
        fileNames.clear()
        currentSongIndex = -1

        val documentUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
        val cursor = contentResolver.query(documentUri, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, OpenableColumns.DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE), null, null, null)

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameColumn = it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
            val mimeTypeColumn = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)

            while (it.moveToNext()) {
                val documentId = it.getString(idColumn)
                val fileName = it.getString(nameColumn)
                val mimeType = it.getString(mimeTypeColumn)

                if (mimeType.startsWith("audio/")) {
                    val fileUri = DocumentsContract.buildDocumentUriUsingTree(uri, documentId)
                    musicFiles.add(fileUri)
                    fileNames.add(fileName)
                }
            }

            adapter.notifyDataSetChanged()
        }
    }

    private fun playMusic(uri: Uri) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, uri)
        mediaPlayer?.apply {
            setOnCompletionListener {
                playNextSong()
            }
            start()
        }
        btnPlayPause.text = "Stop"
        isPlaying = true
        btnPlayPause.setCompoundDrawablesWithIntrinsicBounds(R.drawable.pause_icon, 0, 0, 0)
        adapter.notifyDataSetChanged()
    }

    private fun pauseMusic() {
        mediaPlayer?.takeIf { it.isPlaying }?.pause()
        btnPlayPause.text = "Play"
        isPlaying = false
    }

    private fun playNextSong() {
        if (currentSongIndex < musicFiles.size - 1) {
            currentSongIndex++
            playMusic(musicFiles[currentSongIndex])
        }
    }

    private fun playPreviousSong() {
        if (currentSongIndex > 0) {
            currentSongIndex--
            playMusic(musicFiles[currentSongIndex])
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
