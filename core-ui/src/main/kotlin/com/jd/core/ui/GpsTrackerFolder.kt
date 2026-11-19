package com.jd.core.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts

/**
 * The one folder every file the user saves goes to: `Download/GPS-Tracker` on the shared storage (Nov19.3).
 * From Android 10 the file is written there directly through MediaStore, with no storage permission and no picker.
 * Android 8 and 9 cannot place a file in a named folder without the storage permission, so they keep the picker
 * ([canSaveDirectly] is false there).
 */
object GpsTrackerFolder {
    const val NAME = "GPS-Tracker"
    val RELATIVE_PATH = "${Environment.DIRECTORY_DOWNLOADS}/$NAME"

    val canSaveDirectly: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    /** A new empty file named [displayName] in the folder, or null when it cannot be created (or before Android 10). */
    fun createFile(context: Context, displayName: String, mimeType: String): Uri? {
        if (!canSaveDirectly) return null
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_PATH)
        }
        return runCatching {
            context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        }.getOrNull()
    }

    /** Where the open picker starts: the folder on the primary storage (a hint the picker may ignore if it does not exist yet). */
    val initialUri: Uri = DocumentsContract.buildDocumentUri(
        "com.android.externalstorage.documents", "primary:$RELATIVE_PATH"
    )

    /** The shown path, for the "saved to" message. */
    fun shownPath(displayName: String): String = "$RELATIVE_PATH/$displayName"
}

/** The system open picker, starting in the GPS-Tracker folder. */
class OpenInGpsTrackerFolder : ActivityResultContracts.OpenDocument() {
    override fun createIntent(context: Context, input: Array<String>): Intent =
        super.createIntent(context, input).putExtra(DocumentsContract.EXTRA_INITIAL_URI, GpsTrackerFolder.initialUri)
}

/** The system save picker, starting in the GPS-Tracker folder (used only before Android 10). */
class CreateInGpsTrackerFolder(mimeType: String) : ActivityResultContracts.CreateDocument(mimeType) {
    override fun createIntent(context: Context, input: String): Intent =
        super.createIntent(context, input).putExtra(DocumentsContract.EXTRA_INITIAL_URI, GpsTrackerFolder.initialUri)
}
