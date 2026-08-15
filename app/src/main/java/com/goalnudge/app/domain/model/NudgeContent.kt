package com.goalnudge.app.domain.model

data class NudgeContent(
    val goalId: Long,
    val title: String,
    val body: String,
    val why: String,
    val checkInLine: String
)
