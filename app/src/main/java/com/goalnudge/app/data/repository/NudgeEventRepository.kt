package com.goalnudge.app.data.repository

import com.goalnudge.app.data.local.dao.NudgeEventDao
import com.goalnudge.app.data.local.entity.NudgeEventEntity
import com.goalnudge.app.data.local.entity.NudgeOutcome
import com.goalnudge.app.domain.model.NudgeWindow
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NudgeEventRepository @Inject constructor(private val dao: NudgeEventDao) {

    suspend fun record(goalId: Long?, window: NudgeWindow?, outcome: NudgeOutcome, atMillis: Long) {
        dao.insert(
            NudgeEventEntity(
                goalId = goalId,
                timestampMillis = atMillis,
                window = window?.name,
                outcome = outcome
            )
        )
    }

    suspend fun countShownSince(sinceMillis: Long): Int = dao.countShownSince(sinceMillis)

    suspend fun lastShownAtMillis(): Long? = dao.lastShown()?.timestampMillis

    suspend fun wasWindowAlreadyUsed(window: NudgeWindow, sinceMillis: Long): Boolean =
        dao.countForWindowSince(window.name, sinceMillis) > 0

    fun observeAll(): Flow<List<NudgeEventEntity>> = dao.observeAll()
}
