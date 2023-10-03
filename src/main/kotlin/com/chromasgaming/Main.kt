package com.chromasgaming

import com.prof18.rssparser.RssParser
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File


fun main(args: Array<String>) {
    val rssParser: RssParser = RssParser()
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


