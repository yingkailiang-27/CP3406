package au.edu.jcu.assessment.utilityapp.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import java.time.LocalDateTime
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import au.edu.jcu.assessment.utilityapp.model.StudyCourse
import au.edu.jcu.assessment.utilityapp.model.dayLabel
import au.edu.jcu.assessment.utilityapp.model.minuteLabel
import au.edu.jcu.assessment.utilityapp.model.validateCourse
import java.util.UUID

@Composable
fun AddTaskDialog(saving: Boolean, onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    val context = LocalContext.current
    val initial = remember { LocalDateTime.now().withHour(23).withMinute(59) }
    var date by rememberSaveable { mutableStateOf(initial.toLocalDate().toString()) }
    var minute by rememberSaveable { mutableIntStateOf(initial.hour * 60 + initial.minute) }
    var title by rememberSaveable { mutableStateOf("") }
    var attempted by rememberSaveable { mutableStateOf(false) }
    val error = when {
        title.isBlank() -> "Enter a task title."
        title.trim().length > 120 -> "Use at most 120 characters."
        else -> null
    }
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Add a task") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it }, label = { Text("Task title") },
                    enabled = !saving, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    isError = attempted && error != null,
                    supportingText = { Text(if (attempted && error != null) error else "${title.trim().length}/120") }
                )
                OutlinedButton(enabled = !saving, onClick = {
                    val selected = java.time.LocalDate.parse(date)
                    DatePickerDialog(context, { _, year, month, day ->
                        date = java.time.LocalDate.of(year, month + 1, day).toString()
                    }, selected.year, selected.monthValue - 1, selected.dayOfMonth).show()
                }) { Text("Due date: $date") }
                OutlinedButton(enabled = !saving, onClick = {
                    TimePickerDialog(context, { _, hour, selectedMinute ->
                        minute = hour * 60 + selectedMinute
                    }, minute / 60, minute % 60, true).show()
                }) { Text("Due time: ${minuteLabel(minute)}") }
                Text("Deadline uses your device's local time.",
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(enabled = !saving, onClick = {
                attempted = true
                if (error == null) onAdd(title.trim(), "${date}T${minuteLabel(minute)}")
            }) { Text(if (saving) "Saving…" else "Add task") }
        },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionField(
    label: String, value: String, options: List<String>, enabled: Boolean = true,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = {
        if (enabled) expanded = !expanded
    }) {
        OutlinedTextField(
            value = value, onValueChange = {}, readOnly = true, enabled = enabled,
            label = { Text(label) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = {
                    onSelect(option); expanded = false
                })
            }
        }
    }
}

@Composable
fun AddCourseDialog(
    existing: List<StudyCourse>, saving: Boolean,
    onDismiss: () -> Unit, onAdd: (StudyCourse) -> Unit
) {
    val context = LocalContext.current
    var title by rememberSaveable { mutableStateOf("") }
    var room by rememberSaveable { mutableStateOf("") }
    var weekday by rememberSaveable { mutableIntStateOf(1) }
    var start by rememberSaveable { mutableIntStateOf(9 * 60) }
    var end by rememberSaveable { mutableIntStateOf(10 * 60) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    val id = rememberSaveable { UUID.randomUUID().toString() }
    val course = StudyCourse(id, title.trim(), weekday, start, end, room.trim())
    val error = validateCourse(course, existing)
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Add a weekly class") },
        text = {
            Column(
                Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it }, label = { Text("Course name") },
                    enabled = !saving, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = room, onValueChange = { room = it }, label = { Text("Room (optional)") },
                    enabled = !saving, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                SelectionField(
                    "Day", dayLabel(weekday), (1..7).map(::dayLabel), !saving
                ) { label -> weekday = (1..7).first { dayLabel(it) == label } }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        enabled = !saving,
                        onClick = {
                            TimePickerDialog(context, { _, hour, minute -> start = hour * 60 + minute },
                                start / 60, start % 60, true).show()
                        }, modifier = Modifier.weight(1f)
                    ) { Text("Start ${minuteLabel(start)}") }
                    OutlinedButton(
                        enabled = !saving,
                        onClick = {
                            TimePickerDialog(context, { _, hour, minute -> end = hour * 60 + minute },
                                end / 60, end % 60, true).show()
                        }, modifier = Modifier.weight(1f)
                    ) { Text("End ${minuteLabel(end)}") }
                }
                Text("Repeats every week. Times follow your device timezone.",
                    style = MaterialTheme.typography.bodySmall)
                if (attempted && error != null) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(enabled = !saving, onClick = {
                attempted = true
                if (error == null) onAdd(course)
            }) { Text(if (saving) "Saving…" else "Add class") }
        },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } }
    )
}
