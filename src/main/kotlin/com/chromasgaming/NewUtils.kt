package com.chromasgaming

import com.prof18.rssparser.RssParser
import com.prof18.rssparser.model.RssItem
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

class NewUtils {
    companion object {

        suspend fun getFeedItems(db: Database, feedId: Int, feedUrl: String) {
            val rssParser = RssParser()
            val rssChannel = rssParser.getRssChannel(feedUrl)
            val itemList: List<RssItem> = rssChannel.items.take(3).reversed()

            itemList.forEach { item ->
                transaction(db) {
                    val itemExist = FeedItems.select {
                        (FeedItems.title eq item.title.toString()) and
                                (FeedItems.link eq item.link.toString())
                    }.empty().not()

                    if (!itemExist) {
                        FeedItems.insert {
                            it[FeedItems.feedId] = feedId
                            it[title] = item.title.toString()
                            it[link] = item.link.toString()
                            it[publicationDate] =
                                Utils.parseDateWithMultipleFormats(item.pubDate!!)
                                    ?.toInstant()?.atZone(
                                        ZoneId.systemDefault()
                                    )?.toLocalDateTime()!!
                        }
                    }
                }
            }
        }

        suspend fun sendMessageToWebhook(webhookUrl: String, link: String?, title: String?) {
            //println(Json.encodeToString(Embeds(listOf(EmbedsObject(item.title!!, description = item.description ?: "No Description", url = item.link!!)))))
            // Code to send the message to the Discord webhook
            val client = HttpClient(CIO)
            client.request(webhookUrl) {
                method = HttpMethod.Post
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(Content(link ?: title ?: ""))) // Assuming item.description is the message you want to send
                //setBody(Json.encodeToString(Embeds(listOf(EmbedsObject(item.title!!, description = item.description ?: "", url = item.link!!)))))
            }
        }
    }
}