package models

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DbSettings {
    fun connect() {
        Database.connect(
            url = "jdbc:postgresql://localhost:5432/FilmsAndMovies",
            driver = "org.postgresql.Driver",
            user = "postgres",
            password = ""
        )
    }

    fun init() {
        connect()
        transaction {
            SchemaUtils.create(
                Users,
                Films,
                UserExpectations,
                UserImpressions,
                Genres,
                FilmGenres
            )
        }
    }
}
