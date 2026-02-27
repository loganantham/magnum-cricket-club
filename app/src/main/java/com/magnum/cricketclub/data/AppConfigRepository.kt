package com.magnum.cricketclub.data

class AppConfigRepository(private val appConfigDao: AppConfigDao) {
    suspend fun getConfig(key: String): AppConfig? = appConfigDao.getConfig(key)
    suspend fun getConfigValue(key: String): String? = appConfigDao.getConfigValue(key)
    suspend fun setConfig(key: String, value: String) {
        appConfigDao.insertOrUpdateConfig(AppConfig(key, value))
    }
    suspend fun deleteConfig(key: String) = appConfigDao.deleteConfig(key)
    
    companion object {
        const val KEY_WHATSAPP_GROUP_ID = "whatsapp_group_id"
        const val KEY_WHATSAPP_ENABLED = "whatsapp_enabled"
        const val KEY_TEAM_NAME = "team_name"
    }
}
