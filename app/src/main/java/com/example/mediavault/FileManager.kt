package com.example.mediavault

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.example.mediavault.Utils.DIRECTORY
import kotlinx.coroutines.channels.awaitClose
import java.io.File
import java.io.OutputStream
import java.io.OutputStreamWriter
import kotlinx.coroutines.flow.callbackFlow
import androidx.core.net.toUri
import com.example.mediavault.ui.theme.MediaVaultTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class FileManager(private val context: Context, private var uri: Uri? = null) {

    fun setUri(uri: Uri?) {
        this.uri = uri
    }

    suspend fun save(summary: Summary): List<Summary> = withContext(Dispatchers.IO){
        val fileName = summary.fileName.replace(".txt", "").plus(".txt")

        when (summary.type) {
            Type.INTERNAL -> {
                val directory = context.createDirectory()
                val file = File(directory, fileName)
                file.writeText(summary.summary)
            }

            Type.PRIVATE_EXTERNAL -> {
                val directory = context.createPrivateDir()
                val file = File(directory, fileName)
                file.writeText(summary.summary)
            }

            Type.SHARED -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                        put(
                            MediaStore.MediaColumns.RELATIVE_PATH,
                            Environment.DIRECTORY_DOCUMENTS.plus("/$DIRECTORY")
                        )
                    }


                    val contentUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI
                    }else{
                        MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
                    }


                    val uri = context.contentResolver.insert(contentUri, contentValues)

                    uri?.let {
                        context.contentResolver.openOutputStream(it).use {
                            OutputStreamWriter(it).use {
                                it.write(summary.summary)
                            }
                        }
                    }
                } else {
                    val state = Environment.getExternalStorageState()
                    if (state == Environment.MEDIA_MOUNTED) {
                        val directory = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS.plus("/$DIRECTORY")
                        )

                        if (!directory.exists()) directory.mkdirs()



                        val file = File(directory, fileName)
                        try {
                            file.writeText(summary.summary)
                        } catch (e: SecurityException) {
                            e.printStackTrace()
                            // Handle permission error
                            return@withContext emptyList()
                        }                    }
                }
            }
        }

        return@withContext getSummaries()
    }

   suspend fun delete(summary: Summary): List<Summary> = withContext(Dispatchers.IO) {
        when (summary.type) {
            Type.INTERNAL -> {
                val directory = context.createDirectory()
                val file = File(directory, summary.fileName)
                if (file.exists()) file.delete()
            }

            Type.PRIVATE_EXTERNAL -> {
                val directory = context.createPrivateDir()
                val file = File(directory, summary.fileName)
                if (file.exists()) file.delete()
            }

            Type.SHARED -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val contentResolver = context.contentResolver
                    val projection = arrayOf(MediaStore.MediaColumns._ID)
                    val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? "
                    val selectionArgs = arrayOf(summary.fileName)
                    val pathUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)

                    val cursor =
                        contentResolver.query(pathUri, projection, selection, selectionArgs, null)
                    var deleteUri : Uri? = null

                    cursor?.let {
                        while (it.moveToFirst()) {
                            val index = it.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                            val fileId = it.getLong(index)
                            deleteUri = ContentUris.withAppendedId(pathUri, fileId)
                            break
                        }
                        it.close()
                    }
                    deleteUri?.let {

                    contentResolver.delete(it, null, null)
                    }
                } else {
                    File(
                        Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS.plus("/$DIRECTORY")
                        ),
                        summary.fileName
                    ).takeIf { it.exists() }?.delete()
                }
            }
        }
        return@withContext getSummaries()
    }

   suspend fun update(summary: Summary): List<Summary> = withContext(Dispatchers.IO){
        when (summary.type) {
            Type.INTERNAL -> {
                val directory = context.createDirectory()
                val file = File(directory, summary.fileName)
                if (file.exists()) file.writeText(summary.summary)

            }

            Type.PRIVATE_EXTERNAL -> {
                val directory = context.createPrivateDir()
                val file = File(directory, summary.fileName)
                if (file.exists()) file.writeText(summary.summary)

            }

            Type.SHARED -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val contentResolver = context.contentResolver
                    val projection = arrayOf(MediaStore.MediaColumns._ID)
                    val selection = MediaStore.MediaColumns.DISPLAY_NAME + " = ?"
                    val selectionArgs = arrayOf(summary.fileName)
                    val pathUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)

                    val cursor =
                        contentResolver.query(pathUri, projection, selection, selectionArgs, null)
                    var updateUri : Uri? = null

                    cursor?.let {
                        while (it.moveToFirst()) {
                            val index = it.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                            val fileId = it.getLong(index)
                            updateUri = ContentUris.withAppendedId(pathUri, fileId)
                            break
                        }
                        it.close()
                    }
                    updateUri?.let {

                    contentResolver.openOutputStream(it)?.use {outPutStream->
                        OutputStreamWriter(outPutStream).use { writer->
                            writer.write(summary.summary)
                        }
                    }
                    }
                } else {
                    val directory = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS.plus("/$DIRECTORY")
                    )
                    val file = File(directory, summary.fileName)
                    if (file.exists()) file.writeText(summary.summary)
                }
            }
        }
        return@withContext getSummaries()
    }

   suspend fun getSummaries(): List<Summary> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Summary>()

        context.createDirectory().listFiles()?.map {
            Summary(it.name, it.readText(), Type.INTERNAL)
        }?.let {
            list.addAll(it)
        }

        context.createPrivateDir().listFiles()?.map {
            Summary(
                fileName = it.name,
                summary = it.readText(),
                type = Type.PRIVATE_EXTERNAL
            )
        }?.let {
            list.addAll(it)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (uri != null){

            DocumentFile.fromTreeUri(context, uri!!)?.listFiles()?.filter { it.isFile }?.map {
                val fileNameFromDocumentFile = it.name
                val content =
                    context.contentResolver.openInputStream(it.uri)?.bufferedReader()?.use {
                        it.readText()
                    }
                Summary(
                    fileName = fileNameFromDocumentFile!!.toString(),
                    summary = content.toString(),
                    type = Type.SHARED
                )
            }?.let {
                list.addAll(it)
            }
            }else{
                val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns._ID)
                val selection = MediaStore.MediaColumns.RELATIVE_PATH + " = ?"
                val selectionArgs = arrayOf(Environment.DIRECTORY_DOWNLOADS.plus("/$DIRECTORY"))
                val queryUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI

                context.contentResolver.query(queryUri,projection,selection,selectionArgs,null)?.use {cursor->
                    val nameColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                    val idColumn = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                    while (cursor.moveToNext()){
                        val fileName = cursor.getString(nameColumn)
                        val fileId = cursor.getLong(idColumn)

                        val fileUri = ContentUris.withAppendedId(queryUri,fileId)

                        context.contentResolver.openInputStream(fileUri)?.bufferedReader()?.use {bufferReader->
                            val content = bufferReader.readText()
                            list.add(Summary(fileName,content, Type.SHARED))

                        }
                    }
                }
            }
        } else {
            val directory =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS.plus("/$DIRECTORY"))
            directory.listFiles()?.map {
                Summary(it.name, it.readText(), Type.SHARED)
            }?.let {
                list.addAll(it)
            }
        }
        return@withContext list
    }

    fun getSummaryFlow() = callbackFlow<List<Summary>> {
        trySend(getSummaries())
        awaitClose {}
    }

    private fun Context.createDirectory(): File {
        val directory = filesDir
        val file = File(directory, DIRECTORY)
        if (!file.exists()) file.mkdir()
        return file
    }

    fun Context.createPrivateDir(): File {
        val directory = getExternalFilesDir(null)
        val file = File(directory, DIRECTORY)
        if (file.exists().not()) file.mkdir()
        return file
    }

}

