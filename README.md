[![Release](https://github.com/ChromasIV/RSSOwlKt/actions/workflows/release.yml/badge.svg)](https://github.com/ChromasIV/RSSOwlKt/actions/workflows/release.yml)

# RSSOwlKt
RSSOwlKt is a Kotlin application that effortlessly routes updates from specified RSS feeds directly to your webhooks. Stay updated with the latest content from your favorite sources without missing a beat. With RSSOwlKt, turning RSS feed updates into webhook notifications is a breeze.

## Features

* Easy to set up and use
* Supports multiple RSS feeds and webhooks
* Configurable delay timer
* Previous response storage to avoid duplicate messages
* Written in Kotlin for performance and scalability

## Installation

1. Clone the repository: ```git clone https://github.com/ChromasIV/RSSOwlKt.git```
2. Create a distribution build for 
   3. local development: ```gradle installDist```
   4. Linux/Windows: ```gradle assembleDist```
5. Create the feeds.json file required for the parser to work.
```
[
  {
    "rssChannelUrl": "https://chromasgaming.com/feed/",
    "discordWebhookUrl": "https://discord.com/api/webhooks/1234567890/abcdefghijklmnopqrstuvwxyz",
    "name": "rssName1",
    "delayMs": 300000
  },
  {
    "rssChannelUrl": "https://www.youtube.com/feeds/videos.xml?channel_id=UCM8vFbFdb_S1n_uSACA6M9Q",
    "discordWebhookUrl": "https://discord.com/api/webhooks/9876543210/zyxwvutsrqponmlkjihgfedcba",
    "name": "rssName2",
    "delayMs": 300000
  }
]
```
4. Navigate to the bin folder and run the script:
```
cd build/install/rssowlkt/bin
./rssowlkt
```

RSSOwlKt will start running in the background and will periodically check for updates to your RSS feeds. When an update is found, RSSOwlKt will send a notification to the specified webhook.

# Dependencies

This project depends on the following open source projects:

* [RSS-Parser](https://github.com/prof18/RSS-Parser) (Apache-2.0 License)

## Contributing

We welcome contributions to RSSOwlKt. If you have any feedback or suggestions, please feel free to open an issue on GitHub.
