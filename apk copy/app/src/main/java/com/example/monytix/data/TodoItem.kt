package com.example.monytix.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TodoItem(
    val id: Int,
    val name: String
)
