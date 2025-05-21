package com.example.mediavault

data class Summary(
    val fileName : String,
    val summary : String,
    val type : Type = Type.INTERNAL
)
