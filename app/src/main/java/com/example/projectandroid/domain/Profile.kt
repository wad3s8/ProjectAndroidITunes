package com.example.projectandroid.domain

data class Profile(val name: String = "", val avatar: String = "", val resume: String = "")
data class LocalDocument(val uri: String, val mime: String)

interface ProfileRepository {
    fun read(): Profile
    suspend fun save(profile: Profile)
    fun newCameraPhoto(): String
    fun retainGalleryPhoto(uri: String)
    fun releasePhoto(uri: String)
    suspend fun download(url: String): LocalDocument
}
