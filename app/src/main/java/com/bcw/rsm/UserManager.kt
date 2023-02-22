package com.bcw.rsm

import kotlinx.coroutines.flow.Flow

data class User(val name: String, val age: Int)

interface UserManager {
    fun getUser(): Flow<User>
    suspend fun setName(name: String)
    suspend fun setAge(age: Int)



    //=========================================================
    suspend fun checkName(name: String): Boolean
}

