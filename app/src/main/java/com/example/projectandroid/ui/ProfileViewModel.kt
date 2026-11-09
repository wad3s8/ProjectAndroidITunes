package com.example.projectandroid.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectandroid.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class ProfileViewModel(private val repo: ProfileRepository, private val state: SavedStateHandle) : ViewModel() {
    var profile by mutableStateOf(repo.read())
        private set
    var draft by mutableStateOf(Profile(state["name"] ?: profile.name, state["avatar"] ?: profile.avatar,
        state["resume"] ?: profile.resume))
        private set
    var editing by mutableStateOf(state["editing"] ?: false)
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var document by mutableStateOf<LocalDocument?>(null)
        private set

    private fun update(value: Profile) {
        draft = value
        state["name"] = value.name
        state["avatar"] = value.avatar
        state["resume"] = value.resume
        error = null
    }

    private fun editMode(value: Boolean) {
        editing = value
        state["editing"] = value
    }

    fun edit() {
        update(profile)
        editMode(true)
    }

    fun name(value: String) = update(draft.copy(name = value))
    fun resume(value: String) = update(draft.copy(resume = value))
    fun failed(message: String) { error = message }

    private fun replacePhoto(uri: String) {
        if (draft.avatar != uri) repo.releasePhoto(draft.avatar)
        update(draft.copy(avatar = uri))
    }

    fun gallery(uri: String) {
        try {
            repo.retainGalleryPhoto(uri)
            replacePhoto(uri)
        } catch (_: SecurityException) { failed("Не удалось получить доступ к фотографии") }
    }

    fun prepareCamera(): String = repo.newCameraPhoto().also { state["camera"] = it }

    fun cameraResult(success: Boolean) {
        val uri = state.remove<String>("camera") ?: return
        if (success) replacePhoto(uri) else repo.releasePhoto(uri)
    }

    fun cancel() {
        repo.releasePhoto(draft.avatar)
        update(profile)
        editMode(false)
    }

    fun save() {
        if (busy) return
        val entered = draft.resume.trim()
        val url = if (entered.isEmpty()) "" else {
            val candidate = if ("://" in entered) entered else "https://$entered"
            val parsed = candidate.toHttpUrlOrNull()
            if (parsed == null || parsed.scheme != "https") {
                failed("Введите ссылку HTTPS на файл резюме")
                return
            }
            parsed.toString()
        }
        perform("Не удалось сохранить профиль") {
            val previous = profile
            val saved = draft.copy(name = draft.name.trim(), resume = url)
            repo.save(saved)
            profile = saved
            repo.releasePhoto(previous.avatar)
            update(saved)
            editMode(false)
        }
    }

    fun download() = perform("Не удалось скачать резюме. Проверьте ссылку и подключение.") {
        document = repo.download(profile.resume)
    }

    fun opened() { document = null }

    private fun perform(message: String, action: suspend () -> Unit) {
        if (busy) return
        busy = true
        error = null
        viewModelScope.launch {
            try { action() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { failed(message) }
            finally { busy = false }
        }
    }
}
