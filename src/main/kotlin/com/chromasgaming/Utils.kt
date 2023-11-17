package com.chromasgaming

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class Utils {
    companion object {

        suspend fun sendMessage(webhookUrl: String, message: String, name: String, db: Database) {
            val previousResponse = getPreviousResponse(name, db, webhookUrl)

            if (previousResponse == message) {
                return
            }
            // Send the message to the Discord webhook

            val client = HttpClient(CIO)
            client.request(webhookUrl) {
                // Configure request parameters exposed by HttpRequestBuilder
                method = HttpMethod.Post
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(Content(message)))
            }

            storePreviousResponse(webhookUrl, message, name, db)
        }

        private fun storePreviousResponse(webhookUrl: String, message: String, filename: String, db: Database) {
            transaction(db) {
                FeedTable.update (  {FeedTable.discordWebhookUrl eq webhookUrl and(FeedTable.platformName eq filename) })  {
                    it[previousResponse] = message
                }
            }
        }

        private fun getPreviousResponse(filename: String, db: Database, webhookUrl: String): String {
            var response = ""
            transaction(db) {
                response = FeedTable.select {
                    (FeedTable.discordWebhookUrl eq webhookUrl and (FeedTable.platformName eq filename))
                }.single()[FeedTable.previousResponse]
            }

            return response
        }
    }
}