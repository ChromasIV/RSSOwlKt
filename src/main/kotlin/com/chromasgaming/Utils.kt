package com.chromasgaming

import com.prof18.rssparser.model.RssItem
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

class Utils {
    companion object {

        suspend fun sendMessage(webhookUrl: String, items: List<RssItem>, name: String, db: Database) {
            val previousResponse = getPreviousResponse(name, db, webhookUrl)
            var eligibleItems: List<RssItem> = emptyList()

            if(previousResponse == null) {
                eligibleItems = listOf(items.first())
            } else {
                // Convert LocalDateTime to Date for comparison
                val previousResponseDate = Date.from(previousResponse.atZone(ZoneId.systemDefault()).toInstant())

                eligibleItems = items.filter {
                    val itemTimestamp = parseDateWithMultipleFormats(it.pubDate ?: return@filter false)
                    itemTimestamp != null && itemTimestamp.after(previousResponseDate)
                }.take(5).reversed()
            }

            for (item in eligibleItems) {
                sendMessageToWebhook(webhookUrl, item)
            }

            eligibleItems.maxByOrNull { it.pubDate!! }?.pubDate?.let { mostRecentDate ->
                val itemTimestamp = parseDateWithMultipleFormats(mostRecentDate)
                storePreviousResponse(webhookUrl, itemTimestamp!!, name, db)
            }
        }

        private suspend fun sendMessageToWebhook(webhookUrl: String, item: RssItem) {
            //println(Json.encodeToString(Embeds(listOf(EmbedsObject(item.title!!, description = item.description ?: "No Description", url = item.link!!)))))
            // Code to send the message to the Discord webhook
            val client = HttpClient(CIO)
            client.request(webhookUrl) {
                method = HttpMethod.Post
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(Content(item.link ?: item.title ?: ""))) // Assuming item.description is the message you want to send
                //setBody(Json.encodeToString(Embeds(listOf(EmbedsObject(item.title!!, description = item.description ?: "", url = item.link!!)))))
            }
        }

        private fun storePreviousResponse(webhookUrl: String, itemTimestamp: Date, filename: String, db: Database) {
            transaction(db) {
                FeedTable.update (  {FeedTable.discordWebhookUrl eq webhookUrl and(FeedTable.platformName eq filename) })  {
                    it[previousResponse] = LocalDateTime.ofInstant(itemTimestamp.toInstant(), ZoneId.systemDefault())
                }
            }
        }

        private fun getPreviousResponse(filename: String, db: Database, webhookUrl: String): LocalDateTime? {
            return transaction(db) {
                FeedTable.select {
                    (FeedTable.discordWebhookUrl eq webhookUrl and (FeedTable.platformName eq filename))
                }.singleOrNull()?.get(FeedTable.previousResponse)
            }
        }

        fun parseDateWithMultipleFormats(dateString: String): Date? {
            val dateFormats = listOf(
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "EEE, dd MMM yyyy HH:mm:ss z",
                "yyyy-MM-dd HH:mm:ss",
                // add more formats as needed
                )
            dateFormats.forEach { format ->
                try {
                    val dateFormat = SimpleDateFormat(format)
                    return dateFormat.parse(dateString)
                } catch (e: Exception) {
                // Ignore the exception and try the next format
                }
            }
            return null // or handle this case as needed
        }
    }
}