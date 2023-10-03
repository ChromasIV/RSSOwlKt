package com.chromasgaming

import kotlinx.serialization.Serializable

@Serializable
data class Content(val content: String)

@Serializable
class FeedConfig(
    val rssChannelUrl: String,
    val discordWebhookUrl: String,
    val name: String,
    val delayMs: Long
)