package com.bcw.rsm.data

import android.util.Log
import androidx.core.text.isDigitsOnly
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

data class User(val name: String, val age: Int)

interface UserManager {
    fun getUser(): Flow<User>
    suspend fun setName(name: String)
    suspend fun setAge(age: Int)


    //=========================================================
    suspend fun checkName(name: String): Boolean
}


class UserManagerImpl : UserManager {

    private val tag = "UserManagerImpl"

    private val userFlow = MutableStateFlow(User(name = "John", age = 10))

    override fun getUser(): Flow<User> {
        return userFlow
    }

    override suspend fun setName(name: String) {
        Log.i(tag, "setName $name")
        delay(1500)
        ensureName(name)
        userFlow.update { old -> old.copy(name = name) }
    }

    override suspend fun setAge(age: Int) {
        delay(1500)
        userFlow.update { old -> old.copy(age = age) }
    }

    override suspend fun checkName(name: String): Boolean {
        delay(1500)
        ensureName(name)
        return userFlow.value.name != name
    }

    private fun ensureName(name: String) {
        if (name.isBlank()) {
            throw IllegalArgumentException("name=$name")
        }
        if (name.isDigitsOnly()) {
            throw IllegalArgumentException("isDigitsOnly name=$name")
        }
    }

}

