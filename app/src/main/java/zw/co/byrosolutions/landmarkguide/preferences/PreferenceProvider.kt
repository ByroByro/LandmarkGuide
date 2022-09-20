package zw.co.byrosolutions.landmarkguide.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager


private const val REMEMBER_ME = "is_logged"
private const val USER_ID = "user_id"
private const val EMAIL = "email"
private const val METRIC = "metric"
private const val LANDMARK = "landmark"

class PreferenceProvider(context: Context) {

    // get the application context so that even if we pass the
    // activity or fragment context we will grab the app context
    private val appContext = context.applicationContext

    private val preference: SharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(appContext)

    fun saveRememberMe(rem: Boolean) {
        preference.edit().putBoolean(REMEMBER_ME, rem).apply()
    }

    fun getRememberMe(): Boolean? {
        return preference.getBoolean(REMEMBER_ME, false)
    }

    // user id
    fun saveUserId(id: String) {
        preference.edit().putString(USER_ID, id).apply()
    }

    fun getUserId(): String? {
        return preference.getString(USER_ID, "")
    }

    // user email id
    fun saveEmailId(id: String) {
        preference.edit().putString(EMAIL, id).apply()
    }

    fun getEmailId(): String? {
        return preference.getString(EMAIL, "")
    }

    fun getLandmark(): String? {
        return preference.getString(LANDMARK, "")
    }

    fun getMetric(): String? {
        return preference.getString(METRIC, "")
    }
}