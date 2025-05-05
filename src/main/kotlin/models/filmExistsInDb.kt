package models

import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

fun filmExistsInDb(title: String): Boolean {
    return transaction {
        Films.select { Films.title eq title }.count() > 0
    }
}
