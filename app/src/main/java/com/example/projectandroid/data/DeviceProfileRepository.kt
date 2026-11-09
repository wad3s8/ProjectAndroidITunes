package com.example.projectandroid.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.example.projectandroid.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class DeviceProfileRepository(private val context: Context) : ProfileRepository {
    private val prefs = context.getSharedPreferences("profile", Context.MODE_PRIVATE)
    private val authority = "${context.packageName}.profile.files"
    private val client = OkHttpClient.Builder().callTimeout(90, TimeUnit.SECONDS).build()

    override fun read() = Profile(
        prefs.getString("name", "").orEmpty(),
        prefs.getString("avatar", "").orEmpty(),
        prefs.getString("resume", "").orEmpty(),
    )

    override suspend fun save(profile: Profile) = withContext(Dispatchers.IO) {
        if (!prefs.edit().putString("name", profile.name).putString("avatar", profile.avatar)
                .putString("resume", profile.resume).commit()) throw IOException("Profile write failed")
    }

    override fun newCameraPhoto(): String {
        val folder = File(context.filesDir, "avatars").apply { mkdirs() }
        return FileProvider.getUriForFile(context, authority, File.createTempFile("photo-", ".jpg", folder)).toString()
    }

    override fun retainGalleryPhoto(uri: String) {
        context.contentResolver.takePersistableUriPermission(Uri.parse(uri), Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    override fun releasePhoto(uri: String) {
        if (uri.isEmpty() || uri == read().avatar) return
        val photo = Uri.parse(uri)
        if (photo.authority == authority) context.contentResolver.delete(photo, null, null)
        else try {
            context.contentResolver.releasePersistableUriPermission(photo, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) { /* The provider already revoked this grant. */ }
    }

    override suspend fun download(url: String): LocalDocument = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body ?: throw IOException("Empty response")
            val types = MimeTypeMap.getSingleton()
            val extension = MimeTypeMap.getFileExtensionFromUrl(response.request.url.encodedPath).lowercase()
            val mime = body.contentType()?.toString()?.substringBefore(';')
                ?.takeUnless { it == "application/octet-stream" }
                ?: types.getMimeTypeFromExtension(extension) ?: "application/octet-stream"
            val folder = File(context.cacheDir, "resumes").apply { mkdirs() }
            val suffix = types.getExtensionFromMimeType(mime) ?: extension.takeIf { it.matches(Regex("[a-z0-9]{1,10}")) } ?: "bin"
            val file = File.createTempFile("resume-", ".$suffix", folder)
            try {
                file.outputStream().use { if (body.byteStream().copyTo(it) == 0L) throw IOException("Empty file") }
                LocalDocument(FileProvider.getUriForFile(context, authority, file).toString(), mime)
            } catch (error: Exception) {
                file.delete()
                throw error
            }
        }
    }
}
