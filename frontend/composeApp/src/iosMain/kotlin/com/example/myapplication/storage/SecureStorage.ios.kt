package com.example.myapplication.storage

import platform.Foundation.NSUserDefaults

class IOSSecureStorage : SecureStorage {
    private val userDefaults = NSUserDefaults.standardUserDefaults

    override fun saveString(key: String, value: String) {
        userDefaults.setObject(value, forKey = key)
        userDefaults.synchronize()
    }

    override fun getString(key: String): String? {
        return userDefaults.stringForKey(key)
    }

    override fun remove(key: String) {
        userDefaults.removeObjectForKey(key)
        userDefaults.synchronize()
    }

    override fun clear() {
        val domain = userDefaults.dictionaryRepresentation().keys
        domain.forEach { key ->
            if (key is String) {
                userDefaults.removeObjectForKey(key)
            }
        }
        userDefaults.synchronize()
    }
}

actual fun createSecureStorage(): SecureStorage {
    return IOSSecureStorage()
}
