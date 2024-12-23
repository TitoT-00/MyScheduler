package com.ttdeveloper.scheduler

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.ttdeveloper.scheduler.services.*
import org.joda.time.DateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var scheduleService: ScheduleService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scheduleService = ScheduleService(this)
        
        // Start notification service in background
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                scheduleService.startNotificationService()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        setContent {
            MaterialTheme {
                SchedulerApp(scheduleService)
            }
        }
    }
}

@Composable
fun SchedulerApp(scheduleService: ScheduleService) {
    var schedules by remember { mutableStateOf(emptyList<ScheduleRow>()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedSchedule by remember { mutableStateOf<ScheduleRow?>(null) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                schedules = scheduleService.readSchedules()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scheduler") },
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = MaterialTheme.colors.onPrimary
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                backgroundColor = MaterialTheme.colors.secondary
            ) {
                Icon(Icons.Default.Add, "Add Schedule")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(schedules) { schedule ->
                    ScheduleCard(
                        schedule = schedule,
                        onEdit = { selectedSchedule = schedule },
                        onDelete = {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    scheduleService.deleteSchedule(schedule.id)
                                    schedules = scheduleService.readSchedules()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    )
                }
            }
        }
        
        if (showAddDialog) {
            ScheduleDialog(
                schedule = null,
                onDismiss = { showAddDialog = false },
                onSave = { title, description, startTime, endTime ->
                    scope.launch(Dispatchers.IO) {
                        try {
                            scheduleService.createSchedule(title, description, startTime, endTime)
                            schedules = scheduleService.readSchedules()
                            withContext(Dispatchers.Main) {
                                showAddDialog = false
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            )
        }
        
        selectedSchedule?.let { schedule ->
            ScheduleDialog(
                schedule = schedule,
                onDismiss = { selectedSchedule = null },
                onSave = { title, description, startTime, endTime ->
                    scope.launch(Dispatchers.IO) {
                        try {
                            scheduleService.updateSchedule(schedule.id, title, description, startTime, endTime)
                            schedules = scheduleService.readSchedules()
                            withContext(Dispatchers.Main) {
                                selectedSchedule = null
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun ScheduleCard(
    schedule: ScheduleRow,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = schedule.title,
                    style = MaterialTheme.typography.h6
                )
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, "Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, "Delete")
                    }
                }
            }
            schedule.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.body1
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Start: ${schedule.startTime}",
                style = MaterialTheme.typography.body2
            )
            Text(
                text = "End: ${schedule.endTime}",
                style = MaterialTheme.typography.body2
            )
        }
    }
}

@Composable
fun ScheduleDialog(
    schedule: ScheduleRow?,
    onDismiss: () -> Unit,
    onSave: (String, String, DateTime, DateTime) -> Unit
) {
    var title by remember { mutableStateOf(schedule?.title ?: "") }
    var description by remember { mutableStateOf(schedule?.description ?: "") }
    var startTime by remember { mutableStateOf(schedule?.startTime ?: DateTime.now()) }
    var endTime by remember { mutableStateOf(schedule?.endTime ?: DateTime.now().plusHours(1)) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (schedule == null) "Add Schedule" else "Edit Schedule") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Start Time: ${startTime}")
                Text("End Time: ${endTime}")
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(title, description, startTime, endTime)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}