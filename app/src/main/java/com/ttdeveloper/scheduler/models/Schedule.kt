package com.ttdeveloper.scheduler.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.jodatime.datetime

object Schedule : Table() {
    val id = integer("id").autoIncrement()
    val title = varchar("title",225)
    val description = text("description").nullable()
    val startTime = datetime("start_time")
    val endTime = datetime("end_time")
    val notified = bool("notified").default(false)

    override val primaryKey = PrimaryKey(id)

}