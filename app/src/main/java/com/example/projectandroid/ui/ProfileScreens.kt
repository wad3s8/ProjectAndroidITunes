package com.example.projectandroid.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.projectandroid.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(model: ProfileViewModel) {
    val context = LocalContext.current
    val editing = model.editing
    var sources by rememberSaveable { mutableStateOf(false) }
    val gallery = rememberLauncherForActivityResult(PickVisualMedia()) { uri -> uri?.let { model.gallery(it.toString()) } }
    val camera = rememberLauncherForActivityResult(TakePicture(), model::cameraResult)
    BackHandler(editing) { if (!model.busy) model.cancel() }

    LaunchedEffect(model.document) {
        val file = model.document ?: return@LaunchedEffect
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(file.uri), file.mime)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        } catch (_: ActivityNotFoundException) {
            model.failed("Установите приложение для просмотра этого типа файла")
        } finally { model.opened() }
    }

    Column {
        TopAppBar(title = { Text(if (editing) "Редактирование" else "Профиль") },
            navigationIcon = {
                if (editing) IconButton(onClick = model::cancel, enabled = !model.busy) {
                    Icon(painterResource(R.drawable.ic_back), "Назад")
                }
            }, actions = {
                IconButton(onClick = if (editing) model::save else model::edit, enabled = !model.busy) {
                    Icon(painterResource(if (editing) R.drawable.ic_done else R.drawable.ic_edit),
                        if (editing) "Готово" else "Редактировать профиль")
                }
            })
        val data = if (editing) model.draft else model.profile
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(16.dp),
            horizontalAlignment = if (editing) Alignment.Start else Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Avatar(data.avatar, if (editing) Modifier.clip(CircleShape).clickable(enabled = !model.busy) { sources = true } else Modifier)
            if (editing) {
                OutlinedTextField(data.name, model::name, Modifier.fillMaxWidth(), enabled = !model.busy,
                    label = { Text("ФИО") }, singleLine = true)
                OutlinedTextField(data.resume, model::resume, Modifier.fillMaxWidth(), enabled = !model.busy,
                    label = { Text("Ссылка на резюме") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
            } else {
                if (data.name.isNotEmpty()) Text(data.name, style = MaterialTheme.typography.headlineMedium)
                Button(onClick = model::download, enabled = data.resume.isNotEmpty() && !model.busy) { Text("Резюме") }
            }
            if (model.busy) LinearProgressIndicator()
            model.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }

    if (sources && editing) AlertDialog(onDismissRequest = { sources = false },
        title = { Text("Фото профиля") }, text = {
            Column {
                TextButton(onClick = {
                    sources = false
                    try { gallery.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) }
                    catch (_: ActivityNotFoundException) { model.failed("Галерея недоступна") }
                }) { Text("Галерея") }
                TextButton(onClick = {
                    sources = false
                    try { camera.launch(Uri.parse(model.prepareCamera())) }
                    catch (_: Exception) { model.cameraResult(false); model.failed("Не удалось открыть камеру") }
                }) { Text("Камера") }
            }
        }, confirmButton = { TextButton(onClick = { sources = false }) { Text("Отмена") } })
}

@Composable
private fun Avatar(value: String, modifier: Modifier) {
    val resolver = LocalContext.current.contentResolver
    val bitmap by produceState<Bitmap?>(null, value) {
        this.value = null
        if (value.isNotEmpty()) this.value = withContext(Dispatchers.IO) {
            val uri = Uri.parse(value)
            try {
                if (Build.VERSION.SDK_INT >= 28) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
                        decoder.setTargetSampleSize((maxOf(info.size.width, info.size.height) / 512).coerceAtLeast(1))
                    }
                } else {
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                    options.inJustDecodeBounds = false
                    options.inSampleSize = Integer.highestOneBit((maxOf(options.outWidth, options.outHeight) / 512).coerceAtLeast(1))
                    val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                    val orientation = try {
                        resolver.openInputStream(uri)?.use {
                            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                        }
                    } catch (_: IOException) { null }
                    decoded?.oriented(orientation)
                }
            } catch (_: IOException) { null } catch (_: SecurityException) { null }
        }
    }
    Box(modifier.size(112.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center) {
        val image = bitmap
        if (image == null) Icon(painterResource(R.drawable.ic_profile), "Фото профиля", Modifier.size(56.dp))
        else Image(image.asImageBitmap(), "Фото профиля", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

private fun Bitmap.oriented(orientation: Int?): Bitmap {
    val transform = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> transform.setScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> transform.setScale(1f, -1f)
        ExifInterface.ORIENTATION_ROTATE_90 -> transform.setRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> transform.setRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> transform.setRotate(270f)
        ExifInterface.ORIENTATION_TRANSPOSE -> { transform.setRotate(90f); transform.postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_TRANSVERSE -> { transform.setRotate(270f); transform.postScale(-1f, 1f) }
        else -> return this
    }
    return Bitmap.createBitmap(this, 0, 0, width, height, transform, true)
}
