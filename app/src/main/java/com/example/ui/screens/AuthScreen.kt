package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.auth.GoogleSignInButton
import com.example.data.model.DefaultDepartments
import com.example.data.model.PendingRegistrationDraft
import com.example.data.model.WorkMode
import com.example.ui.components.ErrorBannerCard
import com.example.util.DateTimeUtils

private enum class AuthTabMode(val label: String) {
    LOGIN("Masuk"),
    REGISTER("Registrasi")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onRegisterDraftReady: (PendingRegistrationDraft) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(AuthTabMode.LOGIN) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Registration form state
    var fullName by remember { mutableStateOf("") }
    var employeeCode by remember { mutableStateOf("EMP-${(100..999).random()}") }
    var email by remember { mutableStateOf("") }
    var department by remember { mutableStateOf(DefaultDepartments.first()) }
    var position by remember { mutableStateOf("Staff") }
    var phone by remember { mutableStateOf("") }
    var workMode by remember { mutableStateOf(WorkMode.WFO) }
    var shiftStart by remember { mutableStateOf("08:00") }
    var shiftEnd by remember { mutableStateOf("17:00") }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 540.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Brand Pill & Mode Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Badge,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.auth_badge),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                AuthTabMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = selectedTab == mode,
                        onClick = {
                            selectedTab = mode
                            errorMessage = null
                        },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = AuthTabMode.entries.size
                        ),
                        modifier = Modifier.testTag(
                            if (mode == AuthTabMode.LOGIN) "auth_tab_login" else "auth_tab_register"
                        )
                    ) {
                        Text(text = mode.label)
                    }
                }
            }

            AnimatedContent(targetState = selectedTab, label = "auth_mode_content") { mode ->
                if (mode == AuthTabMode.LOGIN) {
                    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .clip(MaterialTheme.shapes.large)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_hero_office),
                                contentDescription = stringResource(R.string.auth_title),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                            )
                                        )
                                    )
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = stringResource(R.string.auth_title),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = stringResource(R.string.auth_subtitle),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                FeatureRow(
                                    icon = Icons.Rounded.Fingerprint,
                                    text = stringResource(R.string.auth_feature_checkin)
                                )
                                FeatureRow(
                                    icon = Icons.Rounded.PersonAdd,
                                    text = stringResource(R.string.auth_feature_manual)
                                )
                                FeatureRow(
                                    icon = Icons.Rounded.CloudDone,
                                    text = stringResource(R.string.auth_feature_cloud)
                                )
                            }
                        }

                        errorMessage?.let { msg ->
                            ErrorBannerCard(message = msg)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            GoogleSignInButton(
                                onAuthSuccess = {
                                    errorMessage = null
                                    onAuthSuccess()
                                },
                                onAuthError = { msg -> errorMessage = msg }
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            TextButton(
                                onClick = {
                                    selectedTab = AuthTabMode.REGISTER
                                    errorMessage = null
                                },
                                modifier = Modifier.testTag("switch_to_register_button")
                            ) {
                                Text("Belum terdaftar? Registrasi Karyawan Baru")
                            }

                            Text(
                                text = stringResource(R.string.auth_footer_note),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Registrasi Karyawan Baru",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Isi data karyawan secara manual lalu verifikasi dengan akun Google Anda.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = fullName,
                                    onValueChange = { fullName = it },
                                    label = { Text("Nama Lengkap *") },
                                    leadingIcon = { Icon(Icons.Rounded.Person, null) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_reg_fullname")
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
                                        leadingIcon = { Icon(Icons.Rounded.Badge, null) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("auth_reg_code")
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
                                    value = email,
                                    onValueChange = { email = it },
                                    label = { Text("Email Kerja (Opsional)") },
                                    placeholder = { Text("Otomatis dari Google jika dikosongkan") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Text(
                                    text = "Divisi / Departemen *",
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
                                    value = position,
                                    onValueChange = { position = it },
                                    label = { Text("Jabatan / Posisi *") },
                                    leadingIcon = { Icon(Icons.Rounded.WorkOutline, null) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_reg_position")
                                )

                                OutlinedTextField(
                                    value = phone,
                                    onValueChange = { phone = it },
                                    label = { Text("Nomor HP (Opsional)") },
                                    leadingIcon = { Icon(Icons.Rounded.Phone, null) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Text(
                                    text = "Mode Kerja Default",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    WorkMode.entries.forEach { wm ->
                                        FilterChip(
                                            selected = workMode == wm,
                                            onClick = { workMode = wm },
                                            label = { Text(wm.label) }
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
                                        leadingIcon = { Icon(Icons.Rounded.Schedule, null) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = shiftEnd,
                                        onValueChange = { shiftEnd = it },
                                        label = { Text("Jam Pulang") },
                                        leadingIcon = { Icon(Icons.Rounded.Schedule, null) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        errorMessage?.let { msg ->
                            ErrorBannerCard(message = msg)
                        }

                        GoogleSignInButton(
                            buttonText = "Daftar & Verifikasi Google",
                            onBeforeClick = {
                                when {
                                    fullName.trim().length < 2 -> {
                                        errorMessage = "Nama lengkap minimal 2 karakter."
                                        false
                                    }
                                    employeeCode.trim().length < 2 -> {
                                        errorMessage = "NIK / kode karyawan minimal 2 karakter."
                                        false
                                    }
                                    department.trim().length < 2 -> {
                                        errorMessage = "Divisi wajib diisi."
                                        false
                                    }
                                    position.trim().length < 2 -> {
                                        errorMessage = "Jabatan wajib diisi."
                                        false
                                    }
                                    !DateTimeUtils.isValidTimeHHmm(shiftStart) ||
                                        !DateTimeUtils.isValidTimeHHmm(shiftEnd) -> {
                                        errorMessage = "Format jam shift harus HH:mm (contoh 08:00)."
                                        false
                                    }
                                    else -> {
                                        errorMessage = null
                                        onRegisterDraftReady(
                                            PendingRegistrationDraft(
                                                employeeCode = employeeCode.trim(),
                                                fullName = fullName.trim(),
                                                email = email.trim(),
                                                department = department.trim(),
                                                position = position.trim(),
                                                phone = phone.trim(),
                                                workMode = workMode,
                                                shiftStart = shiftStart.trim(),
                                                shiftEnd = shiftEnd.trim()
                                            )
                                        )
                                        true
                                    }
                                }
                            },
                            onAuthSuccess = {
                                errorMessage = null
                                onAuthSuccess()
                            },
                            onAuthError = { msg -> errorMessage = msg }
                        )

                        TextButton(
                            onClick = {
                                selectedTab = AuthTabMode.LOGIN
                                errorMessage = null
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("Sudah punya akun? Masuk di sini")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
