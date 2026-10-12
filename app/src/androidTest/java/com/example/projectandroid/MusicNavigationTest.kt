package com.example.projectandroid

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MusicNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun tab(label: String) =
        compose.onNode(hasText(label) and hasClickAction())

    @Test fun listDetailsTabsAndBackPreservePosition() {
        tab("Музыка").assertIsSelected()
        compose.onNodeWithTag("track_list").performScrollToNode(hasText("Harder, Better, Faster, Stronger"))
        compose.onNodeWithTag("track_2003").performClick()
        compose.onNodeWithTag("track_details").assertIsDisplayed()
        compose.onNodeWithText("Harder, Better, Faster, Stronger").assertIsDisplayed()
        compose.onNodeWithText("Жанр").performScrollTo().assertIsDisplayed()

        tab("Избранное").performClick()
        tab("Избранное").assertIsSelected()
        compose.onNodeWithText("Здесь появятся любимые треки. Раздел будет доступен в следующей практике.")
            .assertIsDisplayed()
        tab("Настройки").performClick()
        tab("Настройки").assertIsSelected()
        compose.onNodeWithText("Здесь появятся настройки приложения. Раздел будет доступен в следующей практике.")
            .assertIsDisplayed()
        tab("Музыка").performClick()
        tab("Музыка").assertIsSelected()
        compose.onNodeWithTag("track_details").assertIsDisplayed()
        compose.onNodeWithContentDescription("Назад").performClick()
        compose.onNodeWithTag("track_2003").assertIsDisplayed()

        compose.onNodeWithTag("track_2003").performClick()
        Espresso.pressBack()
        compose.onNodeWithTag("track_2003").assertIsDisplayed()
    }

    @Test fun detailIdSurvivesActivityRecreation() {
        compose.onNodeWithTag("track_1000").performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("track_details").assertIsDisplayed()
        compose.onNodeWithText("Give Life Back to Music").assertIsDisplayed()
        compose.onNodeWithContentDescription("Назад").performClick()
        compose.onNodeWithTag("track_list").assertIsDisplayed()
    }
}


