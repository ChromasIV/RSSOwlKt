package com.chromasgaming

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class Content(val content: String)

@Serializable
data class Embeds(val embeds: List<EmbedsObject>)
@Serializable
data class EmbedsObject(val title: String, val type: String = "rich", val description: String, val url: String, val image: EmbedImage? = null)

@Serializable
data class EmbedImage(val url:String)

@Serializable
class FeedConfig(
    val rssChannelUrl: String,
    val discordWebhookUrl: String,
    val name: String,
    val delayMs: Long,
    @Contextual val previousResponse: LocalDateTime?,
    val discordId: String
)
