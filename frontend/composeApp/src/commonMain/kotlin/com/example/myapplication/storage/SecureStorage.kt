package com.example.myapplication.storage

/**
 * Platform-agnostic interface for secure storage
 */
interface SecureStorage {
    fun saveString(key: String, value: String)
    fun getString(key: String): String?
    fun remove(key: String)
    fun clear()
}

fun SecureStorage.saveToken(token: String) {
    saveString("auth_token", token)
}

fun SecureStorage.getToken(): String? {
    return getString("auth_token")
}

fun SecureStorage.removeToken() {
    remove("auth_token")
}

expect fun createSecureStorage(): SecureStorage
