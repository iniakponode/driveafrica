package com.uoa.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.database.Cursor
import java.nio.ByteBuffer
import java.util.UUID

val MIGRATION_40_41 = object : Migration(40, 41) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE trip_data ADD COLUMN alcoholProbability REAL")
    }
}

val MIGRATION_41_42 = object : Migration(41, 42) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE trip_data ADD COLUMN userAlcoholResponse TEXT")
    }
}

val MIGRATION_42_43 = object : Migration(42, 43) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE trip_data ADD COLUMN syncState TEXT")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS trip_summary (
                tripId BLOB NOT NULL,
                driverId BLOB NOT NULL,
                startTime INTEGER NOT NULL,
                endTime INTEGER NOT NULL,
                startDate TEXT NOT NULL,
                endDate TEXT NOT NULL,
                distanceMeters REAL NOT NULL,
                durationSeconds INTEGER NOT NULL,
                harshBrakingEvents INTEGER NOT NULL,
                harshAccelerationEvents INTEGER NOT NULL,
                speedingEvents INTEGER NOT NULL,
                swervingEvents INTEGER NOT NULL,
                aggressiveTurnEvents INTEGER NOT NULL DEFAULT 0,
                aggressiveStopGoEvents INTEGER NOT NULL DEFAULT 0,
                phoneHandlingEvents INTEGER NOT NULL DEFAULT 0,
                fatigueEvents INTEGER NOT NULL DEFAULT 0,
                roughRoadSpeedingEvents INTEGER NOT NULL DEFAULT 0,
                crashEvents INTEGER NOT NULL DEFAULT 0,
                classificationLabel TEXT NOT NULL,
                alcoholProbability REAL,
                PRIMARY KEY(tripId)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_trip_summary_driverId ON trip_summary(driverId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_trip_summary_startDate ON trip_summary(startDate)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_trip_summary_endDate ON trip_summary(endDate)")
    }
}

val MIGRATION_43_44 = object : Migration(43, 44) {
    override fun migrate(db: SupportSQLiteDatabase) {
        if (!tableExists(db, "trip_summary")) {
            createLegacyTripSummaryTable(db, "trip_summary")
            return
        }

        db.execSQL("DROP TABLE IF EXISTS trip_summary_new")
        createLegacyTripSummaryTable(db, "trip_summary_new")

        val cursor = db.query(
            """
            SELECT
                tripId,
                driverId,
                startTime,
                endTime,
                startDate,
                endDate,
                distanceMeters,
                durationSeconds,
                harshBrakingEvents,
                harshAccelerationEvents,
                speedingEvents,
                classificationLabel,
                alcoholProbability
            FROM trip_summary
            """.trimIndent()
        )

        try {
            val insert = db.compileStatement(
                """
                INSERT INTO trip_summary_new (
                    tripId,
                    driverId,
                    startTime,
                    endTime,
                    startDate,
                    endDate,
                    distanceMeters,
                    durationSeconds,
                    harshBrakingEvents,
                    harshAccelerationEvents,
                    speedingEvents,
                    swervingEvents,
                    aggressiveTurnEvents,
                    aggressiveStopGoEvents,
                    phoneHandlingEvents,
                    fatigueEvents,
                    roughRoadSpeedingEvents,
                    crashEvents,
                    classificationLabel,
                    alcoholProbability
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            )

            val tripIdIndex = cursor.getColumnIndex("tripId")
            val driverIdIndex = cursor.getColumnIndex("driverId")
            val startTimeIndex = cursor.getColumnIndex("startTime")
            val endTimeIndex = cursor.getColumnIndex("endTime")
            val startDateIndex = cursor.getColumnIndex("startDate")
            val endDateIndex = cursor.getColumnIndex("endDate")
            val distanceMetersIndex = cursor.getColumnIndex("distanceMeters")
            val durationSecondsIndex = cursor.getColumnIndex("durationSeconds")
            val harshBrakingEventsIndex = cursor.getColumnIndex("harshBrakingEvents")
            val harshAccelerationEventsIndex = cursor.getColumnIndex("harshAccelerationEvents")
            val speedingEventsIndex = cursor.getColumnIndex("speedingEvents")
            val classificationLabelIndex = cursor.getColumnIndex("classificationLabel")
            val alcoholProbabilityIndex = cursor.getColumnIndex("alcoholProbability")

            while (cursor.moveToNext()) {
                insert.bindBlob(1, readUuidBytes(cursor, tripIdIndex))
                insert.bindBlob(2, readUuidBytes(cursor, driverIdIndex))
                insert.bindLong(3, cursor.getLong(startTimeIndex))
                insert.bindLong(4, cursor.getLong(endTimeIndex))
                bindStringOrNull(insert, 5, cursor, startDateIndex)
                bindStringOrNull(insert, 6, cursor, endDateIndex)
                insert.bindDouble(7, cursor.getDouble(distanceMetersIndex))
                insert.bindLong(8, cursor.getLong(durationSecondsIndex))
                insert.bindLong(9, cursor.getLong(harshBrakingEventsIndex))
                insert.bindLong(10, cursor.getLong(harshAccelerationEventsIndex))
                insert.bindLong(11, cursor.getLong(speedingEventsIndex))
                insert.bindLong(12, 0L)
                insert.bindLong(13, 0L)
                insert.bindLong(14, 0L)
                insert.bindLong(15, 0L)
                insert.bindLong(16, 0L)
                insert.bindLong(17, 0L)
                insert.bindLong(18, 0L)
                bindStringOrNull(insert, 19, cursor, classificationLabelIndex)
                bindDoubleOrNull(insert, 20, cursor, alcoholProbabilityIndex)
                insert.executeInsert()
                insert.clearBindings()
            }
        } finally {
            cursor.close()
        }

        db.execSQL("DROP TABLE trip_summary")
        db.execSQL("ALTER TABLE trip_summary_new RENAME TO trip_summary")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_trip_summary_driverId ON trip_summary(driverId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_trip_summary_startDate ON trip_summary(startDate)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_trip_summary_endDate ON trip_summary(endDate)")
    }
}

val MIGRATION_44_45 = object : Migration(44, 45) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS trip_feature_state (
                tripId BLOB NOT NULL,
                driverProfileId BLOB,
                accelCount INTEGER NOT NULL,
                accelMean REAL NOT NULL,
                speedCount INTEGER NOT NULL,
                speedMean REAL NOT NULL,
                speedM2 REAL NOT NULL,
                courseCount INTEGER NOT NULL,
                courseMean REAL NOT NULL,
                courseM2 REAL NOT NULL,
                lastLocationId BLOB,
                lastLatitude REAL,
                lastLongitude REAL,
                lastLocationTimestamp INTEGER,
                lastSensorTimestamp INTEGER,
                PRIMARY KEY(tripId)
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_trip_feature_state_tripId " +
                "ON trip_feature_state(tripId)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_trip_feature_state_driverProfileId " +
                "ON trip_feature_state(driverProfileId)"
        )
    }
}

val MIGRATION_45_46 = object : Migration(45, 46) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE trip_summary ADD COLUMN swervingEvents INTEGER NOT NULL DEFAULT 0"
        )
    }
}

val MIGRATION_46_47 = object : Migration(46, 47) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_trip_feature_state_tripId " +
                "ON trip_feature_state(tripId)"
        )
    }
}

val MIGRATION_47_48 = object : Migration(47, 48) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE location ADD COLUMN accuracy REAL")
    }
}

val MIGRATION_48_49 = object : Migration(48, 49) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE trip_summary ADD COLUMN aggressiveTurnEvents INTEGER NOT NULL DEFAULT 0"
        )
    }
}

val MIGRATION_49_50 = object : Migration(49, 50) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE trip_summary ADD COLUMN aggressiveStopGoEvents INTEGER NOT NULL DEFAULT 0"
        )
        db.execSQL(
            "ALTER TABLE trip_summary ADD COLUMN phoneHandlingEvents INTEGER NOT NULL DEFAULT 0"
        )
        db.execSQL(
            "ALTER TABLE trip_summary ADD COLUMN fatigueEvents INTEGER NOT NULL DEFAULT 0"
        )
        db.execSQL(
            "ALTER TABLE trip_summary ADD COLUMN roughRoadSpeedingEvents INTEGER NOT NULL DEFAULT 0"
        )
        db.execSQL(
            "ALTER TABLE trip_summary ADD COLUMN crashEvents INTEGER NOT NULL DEFAULT 0"
        )
    }
}

val MIGRATION_50_51 = object : Migration(50, 51) {
    override fun migrate(db: SupportSQLiteDatabase) {
        createTripSummaryBehaviourTable(db)

        if (tableExists(db, "trip_summary")) {
            val cursor = db.query(
                """
                SELECT
                    tripId,
                    harshBrakingEvents,
                    harshAccelerationEvents,
                    speedingEvents,
                    swervingEvents,
                    aggressiveTurnEvents,
                    aggressiveStopGoEvents,
                    phoneHandlingEvents,
                    fatigueEvents,
                    roughRoadSpeedingEvents,
                    crashEvents
                FROM trip_summary
                """.trimIndent()
            )

            try {
                val insert = db.compileStatement(
                    """
                    INSERT OR REPLACE INTO trip_summary_behaviour (
                        tripId,
                        behaviourType,
                        count
                    ) VALUES (?, ?, ?)
                    """.trimIndent()
                )

                val tripIdIndex = cursor.getColumnIndex("tripId")
                val harshBrakingEventsIndex = cursor.getColumnIndex("harshBrakingEvents")
                val harshAccelerationEventsIndex = cursor.getColumnIndex("harshAccelerationEvents")
                val speedingEventsIndex = cursor.getColumnIndex("speedingEvents")
                val swervingEventsIndex = cursor.getColumnIndex("swervingEvents")
                val aggressiveTurnEventsIndex = cursor.getColumnIndex("aggressiveTurnEvents")
                val aggressiveStopGoEventsIndex = cursor.getColumnIndex("aggressiveStopGoEvents")
                val phoneHandlingEventsIndex = cursor.getColumnIndex("phoneHandlingEvents")
                val fatigueEventsIndex = cursor.getColumnIndex("fatigueEvents")
                val roughRoadSpeedingEventsIndex = cursor.getColumnIndex("roughRoadSpeedingEvents")
                val crashEventsIndex = cursor.getColumnIndex("crashEvents")

                while (cursor.moveToNext()) {
                    val tripIdBytes = readUuidBytes(cursor, tripIdIndex)
                    val values = listOf(
                        "Harsh Braking" to cursor.getInt(harshBrakingEventsIndex),
                        "Harsh Acceleration" to cursor.getInt(harshAccelerationEventsIndex),
                        "Speeding" to cursor.getInt(speedingEventsIndex),
                        "Swerving" to cursor.getInt(swervingEventsIndex),
                        "Aggressive Turn" to cursor.getInt(aggressiveTurnEventsIndex),
                        "Aggressive Stop-and-Go" to cursor.getInt(aggressiveStopGoEventsIndex),
                        "Phone Handling" to cursor.getInt(phoneHandlingEventsIndex),
                        "Fatigue" to cursor.getInt(fatigueEventsIndex),
                        "Rough Road Speeding" to cursor.getInt(roughRoadSpeedingEventsIndex),
                        "Crash Detected" to cursor.getInt(crashEventsIndex)
                    )

                    values.filter { it.second > 0 }.forEach { (type, count) ->
                        insert.bindBlob(1, tripIdBytes)
                        insert.bindString(2, type)
                        insert.bindLong(3, count.toLong())
                        insert.executeInsert()
                        insert.clearBindings()
                    }
                }
            } finally {
                cursor.close()
            }
        }

        db.execSQL("DROP TABLE IF EXISTS trip_summary_new")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS trip_summary_new (
                tripId BLOB NOT NULL,
                driverId BLOB NOT NULL,
                startTime INTEGER NOT NULL,
                endTime INTEGER NOT NULL,
                startDate TEXT NOT NULL,
                endDate TEXT NOT NULL,
                distanceMeters REAL NOT NULL,
                durationSeconds INTEGER NOT NULL,
                classificationLabel TEXT NOT NULL,
                alcoholProbability REAL,
                PRIMARY KEY(tripId)
            )
            """.trimIndent()
        )

        val summaryCursor = db.query(
            """
            SELECT
                tripId,
                driverId,
                startTime,
                endTime,
                startDate,
                endDate,
                distanceMeters,
                durationSeconds,
                classificationLabel,
                alcoholProbability
            FROM trip_summary
            """.trimIndent()
        )

        try {
            val insert = db.compileStatement(
                """
                INSERT INTO trip_summary_new (
                    tripId,
                    driverId,
                    startTime,
                    endTime,
                    startDate,
                    endDate,
                    distanceMeters,
                    durationSeconds,
                    classificationLabel,
                    alcoholProbability
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            )

            val tripIdIndex = summaryCursor.getColumnIndex("tripId")
            val driverIdIndex = summaryCursor.getColumnIndex("driverId")
            val startTimeIndex = summaryCursor.getColumnIndex("startTime")
            val endTimeIndex = summaryCursor.getColumnIndex("endTime")
            val startDateIndex = summaryCursor.getColumnIndex("startDate")
            val endDateIndex = summaryCursor.getColumnIndex("endDate")
            val distanceMetersIndex = summaryCursor.getColumnIndex("distanceMeters")
            val durationSecondsIndex = summaryCursor.getColumnIndex("durationSeconds")
            val classificationLabelIndex = summaryCursor.getColumnIndex("classificationLabel")
            val alcoholProbabilityIndex = summaryCursor.getColumnIndex("alcoholProbability")

            while (summaryCursor.moveToNext()) {
                insert.bindBlob(1, readUuidBytes(summaryCursor, tripIdIndex))
                insert.bindBlob(2, readUuidBytes(summaryCursor, driverIdIndex))
                insert.bindLong(3, summaryCursor.getLong(startTimeIndex))
                insert.bindLong(4, summaryCursor.getLong(endTimeIndex))
                bindStringOrNull(insert, 5, summaryCursor, startDateIndex)
                bindStringOrNull(insert, 6, summaryCursor, endDateIndex)
                insert.bindDouble(7, summaryCursor.getDouble(distanceMetersIndex))
                insert.bindLong(8, summaryCursor.getLong(durationSecondsIndex))
                bindStringOrNull(insert, 9, summaryCursor, classificationLabelIndex)
                bindDoubleOrNull(insert, 10, summaryCursor, alcoholProbabilityIndex)
                insert.executeInsert()
                insert.clearBindings()
            }
        } finally {
            summaryCursor.close()
        }

        db.execSQL("DROP TABLE trip_summary")
        db.execSQL("ALTER TABLE trip_summary_new RENAME TO trip_summary")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_trip_summary_driverId ON trip_summary(driverId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_trip_summary_startDate ON trip_summary(startDate)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_trip_summary_endDate ON trip_summary(endDate)")
    }
}

val MIGRATION_51_52 = object : Migration(51, 52) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE trip_summary ADD COLUMN sync INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_52_53 = object : Migration(52, 53) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE trip_feature_state ADD COLUMN sync INTEGER NOT NULL DEFAULT 0")
    }
}

private fun createTripSummaryTable(db: SupportSQLiteDatabase, tableName: String) {
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS $tableName (
            tripId BLOB NOT NULL,
            driverId BLOB NOT NULL,
            startTime INTEGER NOT NULL,
            endTime INTEGER NOT NULL,
            startDate TEXT NOT NULL,
            endDate TEXT NOT NULL,
            distanceMeters REAL NOT NULL,
            durationSeconds INTEGER NOT NULL,
            classificationLabel TEXT NOT NULL,
            alcoholProbability REAL,
            sync INTEGER NOT NULL DEFAULT 0,
            PRIMARY KEY(tripId)
        )
        """.trimIndent()
    )
    db.execSQL("CREATE INDEX IF NOT EXISTS index_trip_summary_driverId ON $tableName(driverId)")
    db.execSQL("CREATE INDEX IF NOT EXISTS index_trip_summary_startDate ON $tableName(startDate)")
    db.execSQL("CREATE INDEX IF NOT EXISTS index_trip_summary_endDate ON $tableName(endDate)")
}

private fun createLegacyTripSummaryTable(db: SupportSQLiteDatabase, tableName: String) {
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS $tableName (
            tripId BLOB NOT NULL,
            driverId BLOB NOT NULL,
            startTime INTEGER NOT NULL,
            endTime INTEGER NOT NULL,
            startDate TEXT NOT NULL,
            endDate TEXT NOT NULL,
            distanceMeters REAL NOT NULL,
            durationSeconds INTEGER NOT NULL,
            harshBrakingEvents INTEGER NOT NULL,
            harshAccelerationEvents INTEGER NOT NULL,
            speedingEvents INTEGER NOT NULL,
            swervingEvents INTEGER NOT NULL,
            aggressiveTurnEvents INTEGER NOT NULL DEFAULT 0,
            aggressiveStopGoEvents INTEGER NOT NULL DEFAULT 0,
            phoneHandlingEvents INTEGER NOT NULL DEFAULT 0,
            fatigueEvents INTEGER NOT NULL DEFAULT 0,
            roughRoadSpeedingEvents INTEGER NOT NULL DEFAULT 0,
            crashEvents INTEGER NOT NULL DEFAULT 0,
            classificationLabel TEXT NOT NULL,
            alcoholProbability REAL,
            PRIMARY KEY(tripId)
        )
        """.trimIndent()
    )
    db.execSQL("CREATE INDEX IF NOT EXISTS index_trip_summary_driverId ON $tableName(driverId)")
    db.execSQL("CREATE INDEX IF NOT EXISTS index_trip_summary_startDate ON $tableName(startDate)")
    db.execSQL("CREATE INDEX IF NOT EXISTS index_trip_summary_endDate ON $tableName(endDate)")
}

private fun createTripSummaryBehaviourTable(db: SupportSQLiteDatabase) {
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS trip_summary_behaviour (
            tripId BLOB NOT NULL,
            behaviourType TEXT NOT NULL,
            count INTEGER NOT NULL,
            PRIMARY KEY(tripId, behaviourType)
        )
        """.trimIndent()
    )
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS index_trip_summary_behaviour_tripId " +
            "ON trip_summary_behaviour(tripId)"
    )
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS index_trip_summary_behaviour_behaviourType " +
            "ON trip_summary_behaviour(behaviourType)"
    )
}

private fun tableExists(db: SupportSQLiteDatabase, tableName: String): Boolean {
    db.query(
        "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
        arrayOf(tableName)
    ).use { cursor ->
        return cursor.moveToFirst()
    }
}

private fun readUuidBytes(cursor: Cursor, index: Int): ByteArray {
    return when (cursor.getType(index)) {
        Cursor.FIELD_TYPE_BLOB -> {
            val blob = cursor.getBlob(index)
            if (blob != null && blob.size == 16) {
                blob
            } else if (blob != null && blob.size == 36) {
                val text = blob.toString(Charsets.UTF_8)
                uuidToBytes(UUID.fromString(text))
            } else if (blob != null) {
                blob
            } else {
                ByteArray(0)
            }
        }
        Cursor.FIELD_TYPE_STRING -> {
            val text = cursor.getString(index)
            uuidToBytes(UUID.fromString(text))
        }
        else -> {
            val text = cursor.getString(index)
            uuidToBytes(UUID.fromString(text))
        }
    }
}

private fun uuidToBytes(uuid: UUID): ByteArray {
    val buffer = ByteBuffer.allocate(16)
    buffer.putLong(uuid.mostSignificantBits)
    buffer.putLong(uuid.leastSignificantBits)
    return buffer.array()
}

private fun bindStringOrNull(
    statement: androidx.sqlite.db.SupportSQLiteStatement,
    index: Int,
    cursor: Cursor,
    columnIndex: Int
) {
    if (cursor.isNull(columnIndex)) {
        statement.bindNull(index)
    } else {
        statement.bindString(index, cursor.getString(columnIndex))
    }
}

private fun bindDoubleOrNull(
    statement: androidx.sqlite.db.SupportSQLiteStatement,
    index: Int,
    cursor: Cursor,
    columnIndex: Int
) {
    if (cursor.isNull(columnIndex)) {
        statement.bindNull(index)
    } else {
        statement.bindDouble(index, cursor.getDouble(columnIndex))
    }
}
