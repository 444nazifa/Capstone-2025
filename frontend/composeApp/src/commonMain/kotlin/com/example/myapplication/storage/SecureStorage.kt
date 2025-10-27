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

expect fun createSecureStorage(): SecureStorage
