package com.robolock.app.core.time

import android.os.SystemClock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Every reading of "now" the app makes.
 *
 * Three separate notions of time, because they behave differently and the difference is
 * load-bearing:
 *  - [nowUtcMillis] is the wall clock. It survives reboots and moves when the user changes it.
 *  - [elapsedRealtime] is monotonic since boot. It cannot be tampered with but resets on reboot.
 *  - [zone] decides where a local calendar day starts, which is what daily budgets roll over on.
 *
 * Injected as an interface so schedule and expiry logic can be tested at any instant without
 * waiting for one.
 */
interface Clock {
    fun nowUtcMillis(): Long
    fun elapsedRealtime(): Long
    fun zone(): ZoneId

    /** Identifies the current boot session, so monotonic deadlines can be invalidated by a reboot. */
    fun bootId(): String

    fun localNow(): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(nowUtcMillis()), zone())

    fun today(): LocalDate = localNow().toLocalDate()

    /** The storage key for the current local day. */
    fun todayKey(): String = today().format(DATE_KEY)

    fun dateKeyFor(utcMillis: Long): String =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(utcMillis), zone()).toLocalDate().format(DATE_KEY)

    companion object {
        val DATE_KEY: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }
}

class SystemClockSource : Clock {
    override fun nowUtcMillis(): Long = System.currentTimeMillis()

    override fun elapsedRealtime(): Long = SystemClock.elapsedRealtime()

    override fun zone(): ZoneId = ZoneId.systemDefault()

    /**
     * Derived rather than stored: boot time is "now minus time since boot", which stays constant
     * across a session and changes on every restart. Rounded to the second so ordinary jitter in
     * the two readings does not make it look like the device rebooted.
     */
    override fun bootId(): String =
        ((System.currentTimeMillis() - SystemClock.elapsedRealtime()) / 1000L).toString()
}
