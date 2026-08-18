package com.goalnudge.app.ui.goal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goalnudge.app.data.repository.GoalRepository
import com.goalnudge.app.data.repository.SaveGoalResult
import com.goalnudge.app.domain.model.Goal
import com.goalnudge.app.domain.model.GoalType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class GoalFormState(
    val id: Long = 0L,
    val title: String = "",
    val why: String = "",
    val targetDate: LocalDate = LocalDate.now().plusMonths(1),
    val type: GoalType = GoalType.STREAK,
    val milestoneTotal: Int = 10,
    val photoUri: String? = null,
    val error: String? = null
) {
    val isValid: Boolean
        get() = title.isNotBlank() && why.isNotBlank() && targetDate.isAfter(LocalDate.now())
}

@HiltViewModel
class GoalViewModel @Inject constructor(private val goalRepository: GoalRepository) : ViewModel() {

    val activeGoals: StateFlow<List<Goal>> = goalRepository.observeActiveGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun checkIn(goalId: Long) {
        viewModelScope.launch { goalRepository.checkIn(goalId) }
    }

    fun archive(goal: Goal) {
        viewModelScope.launch { goalRepository.archiveGoal(goal) }
    }

    fun delete(goal: Goal) {
        viewModelScope.launch { goalRepository.deleteGoal(goal) }
    }

    suspend fun loadForEdit(goalId: Long): Goal? =
        if (goalId == 0L) null else goalRepository.getGoalOnce(goalId)

    suspend fun saveGoal(form: GoalFormState): SaveGoalResult {
        val goal = Goal(
            id = form.id,
            title = form.title.trim(),
            why = form.why.trim(),
            targetDate = form.targetDate,
            type = form.type,
            photoUri = form.photoUri,
            milestoneTotal = if (form.type == GoalType.MILESTONE) form.milestoneTotal else 0
        )
        return goalRepository.saveGoal(goal)
    }
}
