package com.bcw.rsm

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

    private val userFlow = MutableStateFlow(User(name = "John", age = 10))

    override fun getUser(): Flow<User> {
        return userFlow
    }

    override suspend fun setName(name: String) {
        userFlow.update { old -> old.copy(name = name) }
    }

    override suspend fun setAge(age: Int) {
        userFlow.update { old -> old.copy(age = age) }
    }

    override suspend fun checkName(name: String): Boolean {
        delay(1000)
        return userFlow.value.name != name
    }

}

