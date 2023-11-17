package com.chromasgaming.database

import com.zaxxer.hikari.HikariConfig

class DatabaseConnectionPool(databaseUrl: String, databaseUser: String, databasePassword: String) : HikariConfig() {
    init {
        jdbcUrl = databaseUrl
        username = databaseUser
        password = databasePassword
        maximumPoolSize = 10 // set the maximum number of connections in the pool
    }
}