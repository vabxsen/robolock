package com.robolock.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        InterventionEventEntity::class,
        SessionEntity::class,
        DailyUsageEntity::class,
        BypassGrantEntity::class,
        AppRuleEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class RobolockDatabase : RoomDatabase() {

    abstract fun interventionEvents(): InterventionEventDao
    abstract fun sessions(): SessionDao
    abstract fun dailyUsage(): DailyUsageDao
    abstract fun bypassGrants(): BypassGrantDao
    abstract fun appRules(): AppRuleDao

    companion object {
        fun build(context: Context): RobolockDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                RobolockDatabase::class.java,
                "robolock.db",
            ).build()
    }
}
