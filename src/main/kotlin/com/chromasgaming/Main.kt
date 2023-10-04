package com.chromasgaming

import com.prof18.rssparser.RssParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


fun main() {
    val rssParser = RssParser()
    val feeds =  Utils.readFeedConfigsFromFile("feeds.json")

    runBlocking {
        // Launch a separate coroutine for each feed.
        feeds.forEach { feed ->
            launch {
                while (true) {
                    val rssChannel = rssParser.getRssChannel(feed.rssChannelUrl)
                    val link = rssChannel.items[0].link!!

                    Utils.sendMessage(feed.discordWebhookUrl, link, feed.name)

                    delay(feed.delayMs)
                }
            }
        }
    }
}


