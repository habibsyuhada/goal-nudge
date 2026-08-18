package com.goalnudge.app.ui.goal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.goalnudge.app.data.repository.SaveGoalResult
import com.goalnudge.app.domain.model.GoalType
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalEditScreen(
    goalId: Long,
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: GoalViewModel = hiltViewModel()
) {
    var form by remember { mutableStateOf(GoalFormState(id = goalId)) }
    var showDatePicker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(goalId) {
        val existing = viewModel.loadForEdit(goalId)
        if (existing != null) {
            form = GoalFormState(
                id = existing.id,
                title = existing.title,
                why = existing.why,
                targetDate = existing.targetDate,
                type = existing.type,
                milestoneTotal = existing.milestoneTotal.takeIf { it > 0 } ?: 10,
                photoUri = existing.photoUri
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (goalId == 0L) "Goal baru" else "Edit goal") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = form.title,
                onValueChange = { form = form.copy(title = it, error = null) },
                label = { Text("Judul goal") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = form.why,
                onValueChange = { form = form.copy(why = it, error = null) },
                label = { Text("Kenapa goal ini penting buatmu? (\"why\")") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = form.targetDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                onValueChange = {},
                readOnly = true,
                label = { Text("Target tanggal") },
                trailingIcon = {
                    TextButton(onClick = { showDatePicker = true }) { Text("Pilih") }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("Tipe progres", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GoalType.entries.forEach { type ->
                    FilterChip(
                        selected = form.type == type,
                        onClick = { form = form.copy(type = type) },
                        label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
            if (form.type == GoalType.MILESTONE) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = form.milestoneTotal.toString(),
                    onValueChange = { value ->
                        form = form.copy(milestoneTotal = value.toIntOrNull()?.coerceIn(1, 999) ?: form.milestoneTotal)
                    },
                    label = { Text("Total milestone") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            form.error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    scope.launch {
                        when (val result = viewModel.saveGoal(form)) {
                            is SaveGoalResult.Success -> onDone()
                            SaveGoalResult.TooManyActiveGoals ->
                                form = form.copy(error = "Maksimal ${com.goalnudge.app.domain.model.Goal.MAX_ACTIVE_GOALS} goal aktif. Arsipkan salah satu dulu.")
                        }
                    }
                },
                enabled = form.isValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Simpan")
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = form.targetDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        form = form.copy(targetDate = date, error = null)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Batal") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
