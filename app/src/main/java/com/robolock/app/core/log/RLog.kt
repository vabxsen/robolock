package com.robolock.app.core.log

import android.util.Log
import com.robolock.app.BuildConfig

/**
 * Logging with a single tag so `adb logcat -s Robolock:V` shows the whole app.
 *
 * Verbose and debug output is compiled against the debug flag rather than merely filtered, so a
 * release build cannot leak which apps someone uses into the system log.
 */
object RLog {
    private const val TAG = "Robolock"

    fun v(message: String) {
        if (BuildConfig.DEBUG) Log.v(TAG, message)
    }

    fun d(message: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message)
    }

    fun i(message: String) = Unit.also { Log.i(TAG, message) }

    fun w(message: String, error: Throwable? = null) {
        if (error != null) Log.w(TAG, message, error) else Log.w(TAG, message)
    }

    fun e(message: String, error: Throwable? = null) {
        if (error != null) Log.e(TAG, message, error) else Log.e(TAG, message)
    }
}
