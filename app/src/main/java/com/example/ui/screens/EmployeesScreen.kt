package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Employee
import com.example.data.model.EmployeeStatus
import com.example.data.model.WorkMode
import com.example.ui.components.AvatarInitials
import com.example.ui.components.EmployeeStatusBadge
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MonoCodeChip
import com.example.ui.components.WorkModeBadge

@Composable
fun EmployeesScreen(
    employees: List<Employee>,
    onRegisterNewEmployee: () -> Unit,
    onEditEmployee: (Employee) -> Unit,
    onDeleteEmployee: (Employee) -> Unit,
    onManualAttendanceForEmployee: (Employee) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedDepartment by remember { mutableStateOf<String?>(null) }

    val departments = remember(employees) {
        employees.map { it.department }.distinct().sorted()
    }

    val filtered = remember(employees, searchQuery, selectedDepartment) {
        val q = searchQuery.trim().lowercase()
        employees.filter { emp ->
            val matchesDept = selectedDepartment == null || emp.department == selectedDepartment
            val matchesQuery = q.isEmpty() ||
                emp.fullName.lowercase().contains(q) ||
                emp.employeeCode.lowercase().contains(q) ||
                emp.position.lowercase().contains(q) ||
                emp.department.lowercase().contains(q)
            matchesDept && matchesQuery
        }
    }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Direktori Karyawan",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari nama, NIK, jabatan...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("employees_search_input")
                )

                if (departments.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedDepartment == null,
                            onClick = { selectedDepartment = null },
                            label = { Text("Semua (${employees.size})") }
                        )
                        departments.forEach { dept ->
                            FilterChip(
                                selected = selectedDepartment == dept,
                                onClick = {
                                    selectedDepartment = if (selectedDepartment == dept) null else dept
                                },
                                label = { Text(dept) }
                            )
                        }
                    }
                }
            }

            if (filtered.isEmpty()) {
                EmptyStateView(
                    title = stringResource(R.string.empty_employees_title),
                    description = stringResource(R.string.empty_employees_desc),
                    actionLabel = stringResource(R.string.action_register_employee),
                    onAction = onRegisterNewEmployee
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { emp ->
                        EmployeeDirectoryCard(
                            employee = emp,
                            onManualAttendance = { onManualAttendanceForEmployee(emp) },
                            onEdit = { onEditEmployee(emp) },
                            onDelete = { onDeleteEmployee(emp) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmployeeDirectoryCard(
    employee: Employee,
    onManualAttendance: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarInitials(fullName = employee.fullName, size = 46.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = employee.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${employee.position} • ${employee.department}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MonoCodeChip(code = employee.employeeCode)
                        WorkModeBadge(workMode = WorkMode.fromRaw(employee.workMode))
                    }
                }
                EmployeeStatusBadge(status = EmployeeStatus.fromRaw(employee.status))
            }

            if (employee.email.isNotBlank() || employee.phone.isNotBlank()) {
                val contact = listOf(employee.email, employee.phone)
                    .filter { it.isNotBlank() }
                    .joinToString(" • ")
                Text(
                    text = contact,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onManualAttendance,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(Icons.Rounded.EditNote, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Absen Manual", style = MaterialTheme.typography.labelMedium)
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit karyawan",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!employee.id.startsWith("self_")) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = stringResource(R.string.action_delete),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
