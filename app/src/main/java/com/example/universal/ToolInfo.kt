package com.example.universal

data class ToolInfo(
    val name: String,
    val description: String,
    val syntax: String,
    val example: String,
    val tip: String = "",
    val enabled: Boolean = true
)