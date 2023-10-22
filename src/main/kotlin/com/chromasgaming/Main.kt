package com.chromasgaming

import com.prof18.rssparser.RssParser
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking

private val logger = KotlinLogging.logger {}

@OptIn(DelicateCoroutinesApi::class)
fun main() {

    val rssParser = RssParser()
    val feeds = Utils.readFeedConfigsFromFile("feeds.json")

    runBlocking {
        // Launch a separate coroutine for each feed.
        val coroutines = feeds.map { feed ->
            launch(newSingleThreadContext(feed.name)) {
                while (true) {
                    try {
                        val rssChannel = rssParser.getRssChannel(feed.rssChannelUrl)
                        val link = rssChannel.items[0].link!!

                        Utils.sendMessage(feed.discordWebhookUrl, link, feed.name)
                    } catch (e: Exception) {
                        logger.error { e.printStackTrace() }
                    }
                    delay(feed.delayMs)
                }
            }
        }

        // Wait for all the coroutines to finish.
        coroutines.forEach { it.join() }
        logger.info { "Application has completed." }
    }
}
