package com.chromasgaming

import com.chromasgaming.DBUtil.Companion.sendMessageToWebhook
import com.chromasgaming.database.DatabaseConnectionPool
import com.prof18.rssparser.RssParser
import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

private const val isActive = true

private val dataSource = HikariDataSource(
    DatabaseConnectionPool(
        "jdbc:mysql://127.0.0.1:3306/rssowl",
        System.getenv("username"),
        System.getenv("password")
    )
)

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
fun main() {
    val db = Database.connect(dataSource)

    val rssParser = RssParser()
    //val feeds = Utils.readFeedConfigsFromFile("feeds.json")
    transaction(db) {
        SchemaUtils.create(FeedTable, RssFeeds, FeedItems, Users, UserFeedItems, UserSubscriptions)
        //Do stuff
    }
    runBlocking {
        val activeFeeds = ConcurrentHashMap.newKeySet<String>()
        val feedCoroutines = ConcurrentHashMap<String, Job>()

        launch {
            while (isActive) {

                transaction(db) {
//                    addLogger(StdOutSqlLogger)
                    val feedUrls = (UserSubscriptions innerJoin RssFeeds)
                        .slice(RssFeeds.feedUrl)
                        .selectAll()
                        .groupBy(RssFeeds.feedUrl)
                        .map { it[RssFeeds.feedUrl] }
                        .toSet()

                    val userSubFeeds = (UserSubscriptions innerJoin RssFeeds)
                        .selectAll()
                        .toList()

                    //feedUrls.forEach { it -> println(it) }

                    userSubFeeds.forEach { feed ->
                        val job = launch(Dispatchers.IO) {
                            transaction(db) {
                                //addLogger(StdOutSqlLogger)
                                val newItems = (UserSubscriptions innerJoin RssFeeds innerJoin FeedItems)
                                    .select {
                                        (UserSubscriptions.feedId eq feed[RssFeeds.id]) and
                                                ((UserSubscriptions.lastChecked.isNull() and FeedItems.publicationDate.isNotNull()) or
                                                        (FeedItems.publicationDate greater UserSubscriptions.lastChecked))
                                    }

                                newItems.forEach { row ->
                                    //println(row[FeedItems.title])
                                    transaction(db) {
                                        //addLogger(StdOutSqlLogger)
                                        val alreadySent =
                                            UserFeedItems.select { (UserFeedItems.userId eq row[UserSubscriptions.userId]) and (UserFeedItems.feedItemId eq row[FeedItems.id]) }
                                                .empty().not()

                                        if (!alreadySent) {
                                            //if (activeFeeds.add(feed[RssFeeds.feedUrl])) {
                                            val job = launch(Dispatchers.IO) {
                                                try {
                                                    while (true) {
                                                        //val rssChannel = rssParser.getRssChannel(feedConfig.rssChannelUrl)
                                                        //Utils.sendMessage(item[UserSubscriptions.webhookUrl], rssChannel.items, feedUrl, db)
                                                        sendMessageToWebhook(
                                                            row[UserSubscriptions.webhookUrl],
                                                            row[FeedItems.link],
                                                            row[FeedItems.title]
                                                        )
                                                        delay(row[UserSubscriptions.delayInterval] * 1000L)
                                                    }
                                                } catch (e: Exception) {
                                                    logger.error { e.printStackTrace() }
                                                } finally {
                                                    //activeFeeds.remove(row[RssFeeds.feedUrl])
                                                }
                                            }
                                            feedCoroutines[row[RssFeeds.feedUrl]] = job

                                            UserFeedItems.insert {
                                                it[userId] = row[UserSubscriptions.userId]
                                                it[feedItemId] = row[FeedItems.id]
                                                it[sentDate] = LocalDateTime.now()

                                            }
                                            //}
                                        }
                                    }
                                }

                                UserSubscriptions.update({ UserSubscriptions.subscriptionId eq feed[UserSubscriptions.subscriptionId] }) {
                                    it[lastChecked] = LocalDateTime.now()
                                }
                            }
                        }
                    }
                }


                val newItems = (UserSubscriptions innerJoin RssFeeds innerJoin FeedItems)
                    .select {
                        (UserSubscriptions.feedId eq RssFeeds.id) and
                                (RssFeeds.id eq FeedItems.feedId) and
                                ((UserSubscriptions.lastChecked.isNull() and FeedItems.publicationDate.isNotNull()) or
                                        (FeedItems.publicationDate greater UserSubscriptions.lastChecked))
                    }

                val currentFeeds = getCurrentFeeds(db)

                /*val currentFeeds2 = transaction(db) {
                    RssFeeds.selectAll().map { row ->
                        launch {
                            getFeedItems(db, row[RssFeeds.id].value, row[RssFeeds.feedUrl])
                            transaction(db) {
                                //addLogger(StdOutSqlLogger)
                                UserSubscriptions.selectAll().forEach { subscription ->
                                    val userId = subscription[UserSubscriptions.userId]
                                    val feedId = subscription[UserSubscriptions.feedId]

                                    val newItems = (UserSubscriptions innerJoin RssFeeds innerJoin FeedItems)
                                        .select {
                                            (UserSubscriptions.userId eq userId) and
                                                    (UserSubscriptions.feedId eq RssFeeds.id) and
                                                    (RssFeeds.id eq FeedItems.feedId) and
                                                    ((UserSubscriptions.lastChecked.isNull() and FeedItems.publicationDate.isNotNull()) or
                                                            (FeedItems.publicationDate greater UserSubscriptions.lastChecked))
                                        }

                                    newItems.filter { it[FeedItems.feedId] == feedId }.forEach { item ->
                                        launch {
                                            //println(item)
                                            val feedItemId = item[FeedItems.id].value
                                            val rssChannel = rssParser.getRssChannel(item[RssFeeds.feedUrl])
                                            transaction(db) {
                                                val alreadySent =
                                                    UserFeedItems.select { (UserFeedItems.userId eq userId) and (UserFeedItems.feedItemId eq feedItemId) }
                                                        .empty().not()

                                                if (!alreadySent) {
                                                    val feedUrl = item[RssFeeds.feedUrl]
                                                    if (activeFeeds.add(feedUrl)) {
                                                        val job = launch(newSingleThreadContext(feedUrl)) {
                                                            try {
                                                                while (true) {
                                                                    //val rssChannel = rssParser.getRssChannel(feedConfig.rssChannelUrl)
                                                                    //Utils.sendMessage(item[UserSubscriptions.webhookUrl], rssChannel.items, feedUrl, db)
                                                                    sendMessageToWebhook(item[UserSubscriptions.webhookUrl], item[FeedItems.link], item[FeedItems.title])
                                                                    delay(item[UserSubscriptions.delayInterval] * 1000L)
                                                                }
                                                            } catch (e: Exception) {
                                                                logger.error { e.printStackTrace() }
                                                            } finally {
                                                                activeFeeds.remove(feedUrl)
                                                            }
                                                        }
                                                        feedCoroutines[feedUrl] = job
                                                    }

                                                    UserFeedItems.insert {
                                                        it[UserFeedItems.userId] = userId
                                                        it[UserFeedItems.feedItemId] = feedItemId
                                                        it[UserFeedItems.sentDate] = LocalDateTime.now()

                                                    }
                                                }
                                            }
                                        }
                                    }


                                    UserSubscriptions.update({ UserSubscriptions.subscriptionId eq subscription[UserSubscriptions.subscriptionId] }) {
                                        it[lastChecked] = LocalDateTime.now()
                                    }
                                }
                            }
                        }
                    }
                }*/


//                // Cancel coroutines for removed feeds
//                activeFeeds.forEach { feedName ->
//                    println("$feedName is active")
//                    if (feedName !in currentFeeds) {
//                        feedCoroutines[feedName]?.cancel()
//                        feedCoroutines.remove(feedName)
//                        activeFeeds.remove(feedName)
//                    }
//                }

//                currentFeeds.forEach { (feedName, feedConfig) ->
//                    if (activeFeeds.add(feedName)) {
//                        val job = launch(newSingleThreadContext(feedName)) {
//                            try {
//                                while (true) {
//                                    val rssChannel = rssParser.getRssChannel(feedConfig.rssChannelUrl)
//                                    Utils.sendMessage(feedConfig.discordWebhookUrl, rssChannel.items, feedName, db)
//                                    delay(feedConfig.delayMs)
//                                }
//                            } catch (e: Exception) {
//                                logger.error { e.printStackTrace() }
//                            } finally {
//                                activeFeeds.remove(feedConfig.name)
//                            }
//                        }
//                        feedCoroutines[feedName] = job
//                    }
//                }

                delay(30 * 60 * 1000) // Replace with desired interval to update feed list.
            }
        }

        // Wait for all the coroutines to finish.
//        coroutines.forEach { it.join() }
        logger.info { "Application has completed." }
    }
}

suspend fun getCurrentFeeds(db: Database): Set<String> {
    return withContext(Dispatchers.IO) { // Use withContext for switching to IO dispatcher
        transaction(db) {
            RssFeeds.selectAll().map { it[RssFeeds.feedUrl] }.toSet() // Extracting feed URLs and converting to a Set
        }
    }
}