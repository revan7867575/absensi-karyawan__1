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
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.DeleteOutline
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.EntryType
import com.example.data.model.WorkMode
import com.example.ui.components.AttendanceStatusBadge
import com.example.ui.components.EmptyStateView
import com.example.ui.components.EntryTypeBadge
import com.example.ui.components.MonoCodeChip
import com.example.ui.components.WorkModeBadge
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.util.DateTimeUtils

@Composable
fun HistoryScreen(
    records: List<AttendanceRecord>,
    onCheckOutRecord: (String) -> Unit,
    onDeleteRecord: (AttendanceRecord) -> Unit,
    onOpenManualAttendance: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf<AttendanceStatus?>(null) }

    val filtered = remember(records, searchQuery, selectedStatus) {
        val q = searchQuery.trim().lowercase()
        records.filter { rec ->
            val matchesStatus = selectedStatus == null || rec.status == selectedStatus?.name
            val matchesQuery = q.isEmpty() ||
                rec.employeeName.lowercase().contains(q) ||
                rec.employeeCode.lowercase().contains(q) ||
                rec.department.lowercase().contains(q) ||
                rec.dateKey.contains(q)
            matchesStatus && matchesQuery
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
                    text = "Riwayat Absensi",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari nama, NIK, atau tanggal...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_search_input")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedStatus == null,
                        onClick = { selectedStatus = null },
                        label = { Text("Semua (${records.size})") }
                    )
                    AttendanceStatus.entries.forEach { st ->
                        val count = records.count { it.status == st.name }
                        FilterChip(
                            selected = selectedStatus == st,
                            onClick = { selectedStatus = if (selectedStatus == st) null else st },
                            label = { Text("${st.label} ($count)") }
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                EmptyStateView(
                    title = stringResource(R.string.empty_attendance_title),
                    description = stringResource(R.string.empty_attendance_desc),
                    actionLabel = stringResource(R.string.action_manual_attendance),
                    onAction = onOpenManualAttendance
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { record ->
                        AttendanceHistoryCard(
                            record = record,
                            onCheckOut = { onCheckOutRecord(record.id) },
                            onDelete = { onDeleteRecord(record) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceHistoryCard(
    record: AttendanceRecord,
    onCheckOut: () -> Unit,
    onDelete: () -> Unit
) {
    val duration = DateTimeUtils.computeDurationLabel(record.checkInTime, record.checkOutTime)
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.employeeName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MonoCodeChip(code = record.employeeCode)
                        Text(
                            text = record.department,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                AttendanceStatusBadge(status = AttendanceStatus.fromRaw(record.status))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = DateTimeUtils.formatDateKeyReadable(record.dateKey),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (record.checkOutTime.isNullOrBlank()) {
                                "Masuk ${record.checkInTime} • Belum Check-Out"
                            } else {
                                "${record.checkInTime} – ${record.checkOutTime}"
                            },
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (duration != null) {
                            Text(
                                text = "($duration)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WorkModeBadge(workMode = WorkMode.fromRaw(record.workMode))
                    EntryTypeBadge(entryType = EntryType.fromRaw(record.entryType))
                }
            }

            if (record.notes.isNotBlank()) {
                Text(
                    text = "Catatan: ${record.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (record.checkOutTime.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = onCheckOut,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Check-Out", style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(Modifier.width(8.dp))
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(40.dp)
                ) {
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
