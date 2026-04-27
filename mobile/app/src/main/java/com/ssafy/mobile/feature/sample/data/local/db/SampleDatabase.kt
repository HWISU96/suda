package com.ssafy.mobile.feature.sample.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Sample 피처 전용 Room 데이터베이스.
 * 본 개발 시 실제 DB는 각 피처 모듈에 독립적으로 정의합니다.
 */
@Database(
    entities = [SampleTodoEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class SampleDatabase : RoomDatabase() {
    abstract fun sampleTodoDao(): SampleTodoDao

    companion object {
        private const val DATABASE_NAME = "sample.db"

        @Volatile
        private var instance: SampleDatabase? = null

        fun getInstance(context: Context): SampleDatabase =
            instance ?: synchronized(this) {
                instance ?: Room
                    .databaseBuilder(
                        context = context.applicationContext,
                        klass = SampleDatabase::class.java,
                        name = DATABASE_NAME,
                    ).build()
                    .also { instance = it }
            }
    }
}
