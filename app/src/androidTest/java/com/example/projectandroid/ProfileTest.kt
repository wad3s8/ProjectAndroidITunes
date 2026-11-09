package com.example.projectandroid

import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.platform.app.InstrumentationRegistry
import com.example.projectandroid.data.DeviceProfileRepository
import com.example.projectandroid.ui.ProfileScreen
import com.example.projectandroid.ui.ProfileViewModel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ProfileTest {
    @get:Rule val compose = createComposeRule()
    private val context = object : ContextWrapper(InstrumentationRegistry.getInstrumentation().targetContext) {
        override fun getSharedPreferences(name: String, mode: Int) = baseContext.getSharedPreferences("test_$name", mode)
    }

    @Test fun savesFieldsTogetherAndDiscardsNextEdit() {
        context.getSharedPreferences("profile", Context.MODE_PRIVATE).edit().clear().commit()
        val repository = DeviceProfileRepository(context)
        val model = ProfileViewModel(repository, SavedStateHandle())
        compose.setContent { ProfileScreen(model) }
        compose.onNodeWithContentDescription("Редактировать профиль").performClick()
        compose.onNodeWithText("ФИО").performTextInput("Captain Smollett")
        compose.onNodeWithText("Ссылка на резюме").performTextInput("example.org/cv.pdf")
        compose.onNodeWithContentDescription("Готово").performClick()
        compose.waitUntil { !model.editing }
        compose.onNodeWithText("Captain Smollett").assertIsDisplayed()
        compose.onNodeWithText("Резюме").assertIsEnabled()
        compose.onNodeWithContentDescription("Редактировать профиль").performClick()
        compose.onNodeWithText("ФИО").performTextReplacement("Draft")
        compose.onNodeWithContentDescription("Назад").performClick()
        val restored = DeviceProfileRepository(context).read()
        assertEquals("Captain Smollett", restored.name)
        assertEquals("https://example.org/cv.pdf", restored.resume)
    }

    @Test fun cancelledCaptureAfterRecreationReleasesOnlyPendingPhoto() {
        val repository = DeviceProfileRepository(context)
        val state = SavedStateHandle()
        val model = ProfileViewModel(repository, state)
        model.edit()
        val pending = Uri.parse(model.prepareCamera())
        context.contentResolver.openInputStream(pending)!!.close()
        val restored = ProfileViewModel(repository, state)
        restored.cameraResult(false)
        assertEquals(repository.read().avatar, restored.draft.avatar)
        assertThrows(java.io.FileNotFoundException::class.java) {
            context.contentResolver.openInputStream(pending)?.close()
        }
    }
}
