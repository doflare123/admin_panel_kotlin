package models

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import models.User
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq


data class User(val id: Int, val name: String)

object UserRepository {
    fun getAllUsers(): List<User> = transaction {
        Users.selectAll().map { User(it[Users.id].value, it[Users.name]) }
    }

    fun addUser(name: String) = transaction {
        Users.insert {
            it[Users.name] = name
        }
    }

    fun updateUser(id: Int, newName: String) = transaction {
        Users.update({ Users.id eq id }) {
            it[name] = newName
        }
    }

    fun deleteUser(id: Int) = transaction {
        Users.deleteWhere { Users.id eq id }
    }
}
