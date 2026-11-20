package com.example.medicare.model

data class Specialty(
    val id: String,
    val name: String,
    val description: String,
    val isActive: Boolean = true,
    val iconKey: String = "",
)
