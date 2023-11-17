package com.chromasgaming

import org.jetbrains.exposed.sql.*

object FeedTable : Table() {
    val id = integer("id").autoIncrement()
    val rssChannelUrl = varchar("rssChannelUrl", 255)
    val discordWebhookUrl = varchar("discordWebhookUrl", 255)
    val platformName = varchar("platformName", 255)
    val delayMs = long("delayMs")
    val previousResponse = varchar("previousResponse", 255)
    val discordId = varchar("discordId", 45)

    override val primaryKey = PrimaryKey(id, name = "PK_FeedTable_id")
}
