package com.chromasgaming

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime

object FeedTable : Table() {
    val id = integer("id").autoIncrement()
    val rssChannelUrl = varchar("rssChannelUrl", 255)
    val discordWebhookUrl = varchar("discordWebhookUrl", 255)
    val platformName = varchar("platformName", 255)
    val delayMs = long("delayMs")
    val previousResponse = datetime("previousResponse")
    val discordId = varchar("discordId", 45)

    override val primaryKey = PrimaryKey(id, name = "PK_FeedTable_id")
}


object RssFeeds : IntIdTable() {
    val feedUrl = varchar("feed_url", 2048).uniqueIndex() // Ensuring uniqueness of the URL
    //val feedName = varchar("feed_name", 255).nullable() // Optional feed name
}

object FeedItems : IntIdTable() {
    val feedId = reference("feed_id", RssFeeds)
    val title = varchar("title", 255)
    val link = varchar("link", 2048)
    val publicationDate = datetime("publication_date")
}

object Users : IntIdTable() {
    val userId = varchar("user_id", 255)
}

object UserFeedItems : Table() {
    val userId = reference("user_id", Users)
    val feedItemId = reference("feed_item_id", FeedItems)
    val sentDate = datetime("sent_date")

    // Composite primary key to ensure uniqueness for each user-feed item combination
    override val primaryKey = PrimaryKey(userId, feedItemId)
}
object UserSubscriptions : Table() {
    val subscriptionId = integer("subscription_id").autoIncrement() // Primary key
    val userId = reference("user_id", Users) // Foreign key referencing Users table
    val feedId = reference("feed_id", RssFeeds) // Foreign key referencing RssFeeds table
    val webhookUrl = varchar("webhook_url", 255)
    val delayInterval = integer("delay_interval").default(300) // Delay in minutes
    val lastChecked = datetime("last_checked").nullable() // Timestamp of the last check

    override val primaryKey = PrimaryKey(subscriptionId) // Setting the primary key
}