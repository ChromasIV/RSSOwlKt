package com.chromasgaming

import com.chromasgaming.database.DatabaseConnectionPool
import com.prof18.rssparser.RssParser
import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

private val dataSource = HikariDataSource(DatabaseConnectionPool("jdbc:mysql://127.0.0.1:3306/rssowl",  System.getenv("username"), System.getenv("password")))

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
fun main() {
    val db = Database.connect(dataSource)

    val rssParser = RssParser()
    //val feeds = Utils.readFeedConfigsFromFile("feeds.json")
    transaction(db) {
        SchemaUtils.create(FeedTable)
        //Do stuff
    }
    runBlocking {
        val activeFeeds = ConcurrentHashMap.newKeySet<String>()
        val feedCoroutines = ConcurrentHashMap<String, Job>()

        launch {
            while (true) {
                val currentFeeds = transaction(db) {
                    FeedTable.selectAll().map { row ->
                        FeedConfig(
                            rssChannelUrl = row[FeedTable.rssChannelUrl],
                            discordWebhookUrl = row[FeedTable.discordWebhookUrl],
                            name = row[FeedTable.platformName],
                            delayMs = row[FeedTable.delayMs],
                            previousResponse = row[FeedTable.previousResponse],
                            discordId = row[FeedTable.discordId]
                        )
                    }
                }.associateBy { it.name }

                // Cancel coroutines for removed feeds
                activeFeeds.forEach { feedName ->
                    if (feedName !in currentFeeds) {
                        feedCoroutines[feedName]?.cancel()
                        feedCoroutines.remove(feedName)
                        activeFeeds.remove(feedName)
                    }
                }

                currentFeeds.forEach { (feedName, feedConfig)  ->
                    if (activeFeeds.add(feedName)) {
                        val job =  launch(newSingleThreadContext(feedName)) {
                            try {
                                while (true) {
                                    val rssChannel = rssParser.getRssChannel(feedConfig.rssChannelUrl)
                                    val firstItem = rssChannel.items.firstOrNull()
                                    val linkOrFallback = firstItem?.link ?: firstItem?.description ?: firstItem?.title

                                    Utils.sendMessage(feedConfig.discordWebhookUrl, linkOrFallback!!, feedName, db)
                                    delay(feedConfig.delayMs)
                                }
                            } catch (e: Exception) {
                                logger.error { e.printStackTrace() }
                            } finally {
                                activeFeeds.remove(feedConfig.name)
                            }
                        }
                        feedCoroutines[feedName] = job
                    }
                }

                delay(30 * 60 * 1000) // Replace with desired interval to update feed list.
            }
        }

        // Wait for all the coroutines to finish.
//        coroutines.forEach { it.join() }
        logger.info { "Application has completed." }
    }
}
