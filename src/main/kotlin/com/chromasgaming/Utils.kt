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
import java.io.File

class Utils {
    companion object {

        suspend fun sendMessage(webhookUrl: String, message: String, name: String) {
            val previousResponse = getPreviousResponse("$name.json")

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

            storePreviousResponse(message, "$name.json")
        }


        fun readFeedConfigsFromFile(filename: String): List<FeedConfig> {
            val file = File(filename)

            val jsonStr = file.bufferedReader().use { it.readText() }
            val json = Json { ignoreUnknownKeys = true }
            return json.decodeFromString(jsonStr)
        }

        private fun storePreviousResponse(previousResponse: String, filename: String) {
            val json = Json { ignoreUnknownKeys = true }
            val jsonStr = json.encodeToString(previousResponse)

            val file = File(filename)
            file.writeText(jsonStr)
        }

        private fun getPreviousResponse(filename: String): String {
            val file = File(filename)
            if (!file.exists()) {
                return ""
            }

            val jsonStr = file.readText()
            val json = Json { ignoreUnknownKeys = true }
            return json.decodeFromString(jsonStr)
        }
    }
}