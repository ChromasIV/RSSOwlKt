package com.chromasgaming

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class Content(val content: String)

@Serializable
class FeedConfig(
    val rssChannelUrl: String,
    val discordWebhookUrl: String,
    val name: String,
    val delayMs: Long,
    @Contextual val previousResponse: LocalDateTime?,
    val discordId: String
)
