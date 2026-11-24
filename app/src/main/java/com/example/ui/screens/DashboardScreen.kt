package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.Employee
import com.example.data.model.EntryType
import com.example.data.model.UserProfile
import com.example.data.model.WorkMode
import com.example.ui.components.AttendanceStatusBadge
import com.example.ui.components.AvatarInitials
import com.example.ui.components.EmptyStateView
import com.example.ui.components.EntryTypeBadge
import com.example.ui.components.MonoCodeChip
import com.example.ui.components.StatMetricCard
import com.example.ui.components.WorkModeBadge
import com.example.ui.theme.HeroGradientEnd
import com.example.ui.theme.HeroGradientStart
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StatusLateBg
import com.example.ui.theme.StatusLateFg
import com.example.ui.theme.StatusPermitBg
import com.example.ui.theme.StatusPermitFg
import com.example.ui.theme.StatusPresentBg
import com.example.ui.theme.StatusPresentFg
import com.example.util.DateTimeUtils
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen(
    profile: UserProfile,
    employees: List<Employee>,
    attendanceRecords: List<AttendanceRecord>,
    isSubmitting: Boolean,
    onCheckIn: (WorkMode) -> Unit,
    onCheckOut: (String) -> Unit,
    onOpenManualAttendance: () -> Unit,
    onOpenRegisterEmployee: () -> Unit,
    modifier: Modifier = Modifier
) {
    var liveClock by remember { mutableStateOf(DateTimeUtils.currentTimeClock()) }
    var selectedWorkMode by remember(profile.workMode) {
        mutableStateOf(WorkMode.fromRaw(profile.workMode))
    }

    LaunchedEffect(Unit) {
        while (true) {
            liveClock = DateTimeUtils.currentTimeClock()
            delay(1000L)
        }
    }

    val todayKey = DateTimeUtils.todayDateKey()
    val todayRecords = remember(attendanceRecords, todayKey) {
        attendanceRecords.filter { it.dateKey == todayKey }
    }
    val myTodayRecord = remember(todayRecords, profile.userId) {
        todayRecords.firstOrNull { it.employeeId == "self_${profile.userId}" }
    }

    val presentCount = remember(todayRecords) {
        todayRecords.count { it.status == AttendanceStatus.HADIR.name }
    }
    val lateCount = remember(todayRecords) {
        todayRecords.count { it.status == AttendanceStatus.TERLAMBAT.name }
    }
    val leaveCount = remember(todayRecords) {
        todayRecords.count {
            it.status == AttendanceStatus.IZIN.name || it.status == AttendanceStatus.SAKIT.name
        }
    }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Greeting Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        AvatarInitials(fullName = profile.fullName, size = 48.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Halo, ${profile.fullName}",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                MonoCodeChip(code = profile.employeeCode)
                                Text(
                                    text = "${profile.department} • ${profile.position}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // 2. Hero Live Clock & Check-In/Out Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(HeroGradientStart, HeroGradientEnd)
                            )
                        )
                        .padding(22.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = DateTimeUtils.formattedIndonesianDate(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = liveClock,
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 36.sp,
                                    color = Color.White,
                                    letterSpacing = (-0.5).sp
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = "Shift ${profile.shiftStart}–${profile.shiftEnd}",
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        // Today's Status Pill
                        val statusText = when {
                            myTodayRecord == null -> "Belum melakukan check-in hari ini"
                            myTodayRecord.checkOutTime.isNullOrBlank() ->
                                "Sedang Bekerja • Masuk ${myTodayRecord.checkInTime} (${WorkMode.fromRaw(myTodayRecord.workMode).label})"
                            else ->
                                "Selesai • ${myTodayRecord.checkInTime} – ${myTodayRecord.checkOutTime}"
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.14f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 9.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Schedule,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Work mode selector when not checked in yet
                        if (myTodayRecord == null) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                WorkMode.entries.forEach { mode ->
                                    val selected = selectedWorkMode == mode
                                    FilterChip(
                                        selected = selected,
                                        onClick = { selectedWorkMode = mode },
                                        label = { Text(mode.label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color.White,
                                            selectedLabelColor = HeroGradientEnd,
                                            containerColor = Color.White.copy(alpha = 0.14f),
                                            labelColor = Color.White
                                        ),
                                        border = null
                                    )
                                }
                            }
                        }

                        // Primary Action Button
                        AnimatedContent(targetState = myTodayRecord, label = "check_in_out_cta") { record ->
                            when {
                                record == null -> {
                                    Button(
                                        onClick = { onCheckIn(selectedWorkMode) },
                                        enabled = !isSubmitting,
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White,
                                            contentColor = HeroGradientEnd
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .testTag("check_in_button")
                                    ) {
                                        if (isSubmitting) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = HeroGradientEnd,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(Icons.AutoMirrored.Rounded.Login, null, Modifier.size(20.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = stringResource(R.string.action_check_in),
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                    }
                                }

                                record.checkOutTime.isNullOrBlank() -> {
                                    Button(
                                        onClick = { onCheckOut(record.id) },
                                        enabled = !isSubmitting,
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = StatusLateBg,
                                            contentColor = StatusLateFg
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .testTag("check_out_button")
                                    ) {
                                        if (isSubmitting) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = StatusLateFg,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(Icons.AutoMirrored.Rounded.Logout, null, Modifier.size(20.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = stringResource(R.string.action_check_out),
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                    }
                                }

                                else -> {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color.White.copy(alpha = 0.22f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(vertical = 14.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = "Absensi Hari Ini Selesai",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Quick Manual Shortcuts
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenManualAttendance,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("quick_manual_attendance_button")
                    ) {
                        Icon(Icons.Rounded.EditNote, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_manual_attendance),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenRegisterEmployee,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("quick_register_employee_button")
                    ) {
                        Icon(Icons.Rounded.PersonAdd, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Registrasi Staf",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 4. Summary Metrics Grid (2x2)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Ringkasan Hari Ini",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatMetricCard(
                            title = "Hadir Tepat Waktu",
                            value = presentCount.toString(),
                            icon = Icons.Rounded.CheckCircle,
                            tint = StatusPresentFg,
                            containerTint = StatusPresentBg,
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricCard(
                            title = "Terlambat",
                            value = lateCount.toString(),
                            icon = Icons.Rounded.Schedule,
                            tint = StatusLateFg,
                            containerTint = StatusLateBg,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatMetricCard(
                            title = "Izin / Sakit",
                            value = leaveCount.toString(),
                            icon = Icons.Rounded.EventBusy,
                            tint = StatusPermitFg,
                            containerTint = StatusPermitBg,
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricCard(
                            title = "Total Karyawan",
                            value = employees.size.toString(),
                            icon = Icons.Rounded.Groups,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            containerTint = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 5. Today's Activity Feed
            item {
                Text(
                    text = "Aktivitas Absensi Hari Ini",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (todayRecords.isEmpty()) {
                item {
                    EmptyStateView(
                        title = stringResource(R.string.empty_attendance_title),
                        description = stringResource(R.string.empty_attendance_desc),
                        actionLabel = stringResource(R.string.action_manual_attendance),
                        onAction = onOpenManualAttendance
                    )
                }
            } else {
                items(todayRecords, key = { it.id }) { record ->
                    TodayAttendanceRow(record = record)
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun TodayAttendanceRow(record: AttendanceRecord) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarInitials(fullName = record.employeeName, size = 42.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.employeeName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${record.employeeCode} • ${record.department}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WorkModeBadge(workMode = WorkMode.fromRaw(record.workMode))
                    EntryTypeBadge(entryType = EntryType.fromRaw(record.entryType))
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                AttendanceStatusBadge(status = AttendanceStatus.fromRaw(record.status))
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (record.checkOutTime.isNullOrBlank()) {
                        "${record.checkInTime} – Aktif"
                    } else {
                        "${record.checkInTime} – ${record.checkOutTime}"
                    },
                    fontFamily = JetBrainsMonoFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
