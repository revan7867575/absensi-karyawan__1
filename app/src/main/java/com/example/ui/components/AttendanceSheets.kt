package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AttendanceStatus
import com.example.data.model.DefaultDepartments
import com.example.data.model.Employee
import com.example.data.model.EmployeeStatus
import com.example.data.model.UserProfile
import com.example.data.model.WorkMode
import com.example.util.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RegistrasiManualKaryawanSheet(
    existingEmployee: Employee?,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (
        employeeCode: String,
        fullName: String,
        email: String,
        phone: String,
        department: String,
        position: String,
        workMode: WorkMode,
        status: EmployeeStatus,
        joinDate: String,
        notes: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var employeeCode by remember(existingEmployee) {
        mutableStateOf(existingEmployee?.employeeCode ?: "EMP-${(100..999).random()}")
    }
    var fullName by remember(existingEmployee) { mutableStateOf(existingEmployee?.fullName ?: "") }
    var email by remember(existingEmployee) { mutableStateOf(existingEmployee?.email ?: "") }
    var phone by remember(existingEmployee) { mutableStateOf(existingEmployee?.phone ?: "") }
    var department by remember(existingEmployee) {
        mutableStateOf(existingEmployee?.department ?: DefaultDepartments.first())
    }
    var position by remember(existingEmployee) { mutableStateOf(existingEmployee?.position ?: "") }
    var workMode by remember(existingEmployee) {
        mutableStateOf(WorkMode.fromRaw(existingEmployee?.workMode))
    }
    var status by remember(existingEmployee) {
        mutableStateOf(EmployeeStatus.fromRaw(existingEmployee?.status))
    }
    var joinDate by remember(existingEmployee) {
        mutableStateOf(existingEmployee?.joinDate?.ifBlank { DateTimeUtils.todayDateKey() } ?: DateTimeUtils.todayDateKey())
    }
    var notes by remember(existingEmployee) { mutableStateOf(existingEmployee?.notes ?: "") }
    var errorText by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (existingEmployee == null) "Registrasi Karyawan Manual" else "Edit Data Karyawan",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Tambahkan data karyawan baru secara manual ke direktori absensi.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = employeeCode,
                    onValueChange = { employeeCode = it.uppercase() },
                    label = { Text("NIK / Kode Karyawan *") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sheet_emp_code_input")
                )
                TextButton(
                    onClick = { employeeCode = "EMP-${(100..999).random()}" }
                ) {
                    Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Acak")
                }
            }

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Nama Lengkap *") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sheet_emp_name_input")
            )

            Text(
                text = "Divisi *",
                style = MaterialTheme.typography.labelLarge
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DefaultDepartments.forEach { dept ->
                    FilterChip(
                        selected = department == dept,
                        onClick = { department = dept },
                        label = { Text(dept) },
                        leadingIcon = if (department == dept) {
                            { Icon(Icons.Rounded.Check, null, Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            OutlinedTextField(
                value = department,
                onValueChange = { department = it },
                label = { Text("Nama Divisi") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = position,
                onValueChange = { position = it },
                label = { Text("Jabatan / Posisi *") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sheet_emp_position_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Kerja") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("No. HP") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            Text(text = "Mode Kerja", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WorkMode.entries.forEach { mode ->
                    FilterChip(
                        selected = workMode == mode,
                        onClick = { workMode = mode },
                        label = { Text(mode.label) }
                    )
                }
            }

            Text(text = "Status Kepegawaian", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EmployeeStatus.entries.forEach { st ->
                    FilterChip(
                        selected = status == st,
                        onClick = { status = st },
                        label = { Text(st.label) }
                    )
                }
            }

            OutlinedTextField(
                value = joinDate,
                onValueChange = { joinDate = it },
                label = { Text("Tanggal Bergabung (YYYY-MM-DD)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Catatan Registrasi (Opsional)") },
                maxLines = 2,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            errorText?.let { err ->
                Text(
                    text = err,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(
                    onClick = {
                        when {
                            employeeCode.trim().length < 2 -> errorText = "Kode karyawan minimal 2 karakter."
                            fullName.trim().length < 2 -> errorText = "Nama lengkap minimal 2 karakter."
                            department.trim().length < 2 -> errorText = "Divisi wajib diisi."
                            position.trim().length < 2 -> errorText = "Jabatan wajib diisi."
                            !DateTimeUtils.isValidDateKey(joinDate) -> errorText = "Format tanggal harus YYYY-MM-DD."
                            else -> {
                                errorText = null
                                onSubmit(
                                    employeeCode.trim(),
                                    fullName.trim(),
                                    email.trim(),
                                    phone.trim(),
                                    department.trim(),
                                    position.trim(),
                                    workMode,
                                    status,
                                    joinDate.trim(),
                                    notes.trim()
                                )
                            }
                        }
                    },
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.4f)
                        .height(50.dp)
                        .testTag("sheet_emp_submit_button")
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(R.string.action_save))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AbsenManualSheet(
    employees: List<Employee>,
    preselectedEmployee: Employee?,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (
        employee: Employee,
        dateKey: String,
        checkInTime: String,
        checkOutTime: String?,
        workMode: WorkMode,
        status: AttendanceStatus,
        notes: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedEmployee by remember(preselectedEmployee, employees) {
        mutableStateOf(preselectedEmployee ?: employees.firstOrNull())
    }
    var employeeMenuExpanded by remember { mutableStateOf(false) }
    var dateKey by remember { mutableStateOf(DateTimeUtils.todayDateKey()) }
    var checkInTime by remember { mutableStateOf(DateTimeUtils.currentTimeHHmm()) }
    var checkOutTime by remember { mutableStateOf("") }
    var workMode by remember(selectedEmployee) {
        mutableStateOf(WorkMode.fromRaw(selectedEmployee?.workMode))
    }
    var status by remember { mutableStateOf(AttendanceStatus.HADIR) }
    var notes by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Input Absen Manual",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Catat kehadiran, keterlambatan, izin, atau sakit secara manual.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ExposedDropdownMenuBox(
                expanded = employeeMenuExpanded,
                onExpandedChange = { employeeMenuExpanded = !employeeMenuExpanded }
            ) {
                OutlinedTextField(
                    value = selectedEmployee?.let { "${it.fullName} (${it.employeeCode})" }
                        ?: "Pilih karyawan",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Karyawan *") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = employeeMenuExpanded)
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = employeeMenuExpanded,
                    onDismissRequest = { employeeMenuExpanded = false }
                ) {
                    employees.forEach { emp ->
                        DropdownMenuItem(
                            text = { Text("${emp.fullName} • ${emp.employeeCode} (${emp.department})") },
                            onClick = {
                                selectedEmployee = emp
                                workMode = WorkMode.fromRaw(emp.workMode)
                                employeeMenuExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = dateKey,
                onValueChange = { dateKey = it },
                label = { Text("Tanggal (YYYY-MM-DD) *") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = checkInTime,
                    onValueChange = { checkInTime = it },
                    label = { Text("Jam Masuk (HH:mm) *") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sheet_att_checkin_input")
                )
                OutlinedTextField(
                    value = checkOutTime,
                    onValueChange = { checkOutTime = it },
                    label = { Text("Jam Keluar (Opsional)") },
                    placeholder = { Text("17:00") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sheet_att_checkout_input")
                )
            }

            Text(text = "Status Kehadiran", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AttendanceStatus.entries.forEach { st ->
                    FilterChip(
                        selected = status == st,
                        onClick = { status = st },
                        label = { Text(st.label) }
                    )
                }
            }

            Text(text = "Mode Kerja", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WorkMode.entries.forEach { mode ->
                    FilterChip(
                        selected = workMode == mode,
                        onClick = { workMode = mode },
                        label = { Text(mode.label) }
                    )
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Catatan / Keterangan (Opsional)") },
                maxLines = 2,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            errorText?.let { err ->
                Text(
                    text = err,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(
                    onClick = {
                        val emp = selectedEmployee
                        val cleanOut = checkOutTime.trim().ifBlank { null }
                        when {
                            emp == null -> errorText = "Pilih karyawan terlebih dahulu."
                            !DateTimeUtils.isValidDateKey(dateKey) -> errorText = "Format tanggal harus YYYY-MM-DD."
                            !DateTimeUtils.isValidTimeHHmm(checkInTime) -> errorText = "Format jam masuk harus HH:mm."
                            cleanOut != null && !DateTimeUtils.isValidTimeHHmm(cleanOut) ->
                                errorText = "Format jam keluar harus HH:mm."
                            else -> {
                                errorText = null
                                onSubmit(
                                    emp,
                                    dateKey.trim(),
                                    checkInTime.trim(),
                                    cleanOut,
                                    workMode,
                                    status,
                                    notes.trim()
                                )
                            }
                        }
                    },
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.4f)
                        .height(50.dp)
                        .testTag("sheet_att_submit_button")
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(R.string.action_save))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditProfilSheet(
    profile: UserProfile,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (
        employeeCode: String,
        fullName: String,
        email: String,
        department: String,
        position: String,
        phone: String,
        workMode: WorkMode,
        shiftStart: String,
        shiftEnd: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var employeeCode by remember(profile) { mutableStateOf(profile.employeeCode) }
    var fullName by remember(profile) { mutableStateOf(profile.fullName) }
    var email by remember(profile) { mutableStateOf(profile.email) }
    var department by remember(profile) { mutableStateOf(profile.department) }
    var position by remember(profile) { mutableStateOf(profile.position) }
    var phone by remember(profile) { mutableStateOf(profile.phone) }
    var workMode by remember(profile) { mutableStateOf(WorkMode.fromRaw(profile.workMode)) }
    var shiftStart by remember(profile) { mutableStateOf(profile.shiftStart) }
    var shiftEnd by remember(profile) { mutableStateOf(profile.shiftEnd) }
    var errorText by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Edit Profil Karyawan",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Nama Lengkap *") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = employeeCode,
                onValueChange = { employeeCode = it.uppercase() },
                label = { Text("NIK / Kode Karyawan *") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DefaultDepartments.forEach { dept ->
                    FilterChip(
                        selected = department == dept,
                        onClick = { department = dept },
                        label = { Text(dept) }
                    )
                }
            }
            OutlinedTextField(
                value = department,
                onValueChange = { department = it },
                label = { Text("Divisi *") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = position,
                onValueChange = { position = it },
                label = { Text("Jabatan *") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("No. HP") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WorkMode.entries.forEach { mode ->
                    FilterChip(
                        selected = workMode == mode,
                        onClick = { workMode = mode },
                        label = { Text(mode.label) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = shiftStart,
                    onValueChange = { shiftStart = it },
                    label = { Text("Jam Masuk") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = shiftEnd,
                    onValueChange = { shiftEnd = it },
                    label = { Text("Jam Pulang") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            errorText?.let { err ->
                Text(
                    text = err,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(
                    onClick = {
                        when {
                            fullName.trim().length < 2 -> errorText = "Nama minimal 2 karakter."
                            employeeCode.trim().length < 2 -> errorText = "Kode minimal 2 karakter."
                            department.trim().length < 2 -> errorText = "Divisi wajib diisi."
                            position.trim().length < 2 -> errorText = "Jabatan wajib diisi."
                            !DateTimeUtils.isValidTimeHHmm(shiftStart) || !DateTimeUtils.isValidTimeHHmm(shiftEnd) ->
                                errorText = "Format shift harus HH:mm."
                            else -> {
                                errorText = null
                                onSubmit(
                                    employeeCode.trim(),
                                    fullName.trim(),
                                    email.trim(),
                                    department.trim(),
                                    position.trim(),
                                    phone.trim(),
                                    workMode,
                                    shiftStart.trim(),
                                    shiftEnd.trim()
                                )
                            }
                        }
                    },
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.4f)
                        .height(50.dp)
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}
