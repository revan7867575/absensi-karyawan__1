package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.attemptAutoSignIn
import com.example.auth.authStateFlow
import com.example.auth.signOut
import com.example.data.model.AttendanceRecord
import com.example.data.model.Employee
import com.example.data.model.PendingRegistrationDraft
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.repository.AttendanceRepository
import com.example.ui.components.AbsenManualSheet
import com.example.ui.components.EditProfilSheet
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.RegistrasiManualKaryawanSheet
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EmployeesScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ManualRegistrationScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.AbsensiKuTheme
import com.example.ui.viewmodel.AbsensiViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private enum class AppTab(
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME(R.string.tab_home, Icons.Rounded.Home, Icons.Outlined.Home),
    HISTORY(R.string.tab_history, Icons.Rounded.History, Icons.Outlined.History),
    EMPLOYEES(R.string.tab_employees, Icons.Rounded.Groups, Icons.Outlined.Groups),
    PROFILE(R.string.tab_profile, Icons.Rounded.Badge, Icons.Outlined.Badge)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AbsensiKuTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var pendingDraft by remember { mutableStateOf<PendingRegistrationDraft?>(null) }

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = {},
            onRegisterDraftReady = { draft -> pendingDraft = draft }
        )
    } else {
        key(user.uid) {
            AuthenticatedWorkspace(
                currentUserId = user.uid,
                googleDisplayName = user.displayName.orEmpty(),
                googleEmail = user.email.orEmpty(),
                pendingDraft = pendingDraft,
                onConsumeDraft = { pendingDraft = null },
                onSignOut = {
                    pendingDraft = null
                    signOut(credentialManager = credentialManager, scope = scope)
                }
            )
        }
    }
}

@Composable
private fun AuthenticatedWorkspace(
    currentUserId: String,
    googleDisplayName: String,
    googleEmail: String,
    pendingDraft: PendingRegistrationDraft?,
    onConsumeDraft: () -> Unit,
    onSignOut: () -> Unit
) {
    val viewModel: AbsensiViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                AbsensiViewModel(AttendanceRepository(db), currentUserId)
            }
        }
    )

    val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val employeesState by viewModel.employeesState.collectAsStateWithLifecycle()
    val attendanceState by viewModel.attendanceState.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(feedbackMessage) {
        val msg = feedbackMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearFeedback()
    }

    LaunchedEffect(profileState, pendingDraft) {
        val draft = pendingDraft ?: return@LaunchedEffect
        val successState = profileState as? UiState.Success ?: return@LaunchedEffect
        if (successState.data == null) {
            viewModel.saveUserProfile(
                employeeCode = draft.employeeCode,
                fullName = draft.fullName,
                email = draft.email.ifBlank { googleEmail.ifBlank { "karyawan@perusahaan.id" } },
                department = draft.department,
                position = draft.position,
                phone = draft.phone,
                workMode = draft.workMode,
                shiftStart = draft.shiftStart,
                shiftEnd = draft.shiftEnd,
                onSuccess = onConsumeDraft
            )
        } else {
            onConsumeDraft()
        }
    }

    when (val pState = profileState) {
        UiState.Loading -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                CircularProgressIndicator()
            }
        }

        is UiState.Error -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                ErrorBannerCard(message = pState.message)
            }
        }

        is UiState.Success -> {
            val profile = pState.data
            if (profile == null) {
                ManualRegistrationScreen(
                    initialName = googleDisplayName,
                    initialEmail = googleEmail,
                    isSubmitting = isSubmitting,
                    onSubmit = { code, name, email, dept, pos, phone, mode, start, end ->
                        viewModel.saveUserProfile(
                            employeeCode = code,
                            fullName = name,
                            email = email,
                            department = dept,
                            position = pos,
                            phone = phone,
                            workMode = mode,
                            shiftStart = start,
                            shiftEnd = end
                        )
                    },
                    onSignOutClick = onSignOut
                )
            } else {
                val employees = (employeesState as? UiState.Success)?.data.orEmpty()
                val attendanceRecords = (attendanceState as? UiState.Success)?.data.orEmpty()

                MainNavigationShell(
                    profile = profile,
                    employees = employees,
                    attendanceRecords = attendanceRecords,
                    isSubmitting = isSubmitting,
                    snackbarHostState = snackbarHostState,
                    viewModel = viewModel,
                    onSignOut = onSignOut
                )
            }
        }
    }
}

@Composable
private fun MainNavigationShell(
    profile: UserProfile,
    employees: List<Employee>,
    attendanceRecords: List<AttendanceRecord>,
    isSubmitting: Boolean,
    snackbarHostState: SnackbarHostState,
    viewModel: AbsensiViewModel,
    onSignOut: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(AppTab.HOME) }
    var showEmployeeSheet by remember { mutableStateOf(false) }
    var editingEmployee by remember { mutableStateOf<Employee?>(null) }
    var showManualAttendanceSheet by remember { mutableStateOf(false) }
    var preselectedEmployeeForAttendance by remember { mutableStateOf<Employee?>(null) }
    var showEditProfileSheet by remember { mutableStateOf(false) }

    // Return to Home tab on back press when on secondary tabs
    BackHandler(enabled = selectedTab != AppTab.HOME) {
        selectedTab = AppTab.HOME
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.statusBars,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        AppTab.entries.forEach { tab ->
                            val selected = selectedTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { selectedTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                when (selectedTab) {
                    AppTab.HISTORY -> {
                        ExtendedFloatingActionButton(
                            onClick = {
                                preselectedEmployeeForAttendance = employees.firstOrNull {
                                    it.id == "self_${profile.userId}"
                                } ?: employees.firstOrNull()
                                showManualAttendanceSheet = true
                            },
                            icon = { Icon(Icons.Rounded.EditNote, contentDescription = null) },
                            text = { Text(stringResource(R.string.action_manual_attendance)) },
                            modifier = Modifier.testTag("fab_manual_attendance")
                        )
                    }
                    AppTab.EMPLOYEES -> {
                        ExtendedFloatingActionButton(
                            onClick = {
                                editingEmployee = null
                                showEmployeeSheet = true
                            },
                            icon = { Icon(Icons.Rounded.PersonAdd, contentDescription = null) },
                            text = { Text(stringResource(R.string.action_register_employee)) },
                            modifier = Modifier.testTag("fab_register_employee")
                        )
                    }
                    else -> {}
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        AppTab.entries.forEach { tab ->
                            val selected = selectedTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { selectedTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        AppTab.HOME -> {
                            DashboardScreen(
                                profile = profile,
                                employees = employees,
                                attendanceRecords = attendanceRecords,
                                isSubmitting = isSubmitting,
                                onCheckIn = { mode -> viewModel.checkInToday(profile, mode) },
                                onCheckOut = { id -> viewModel.checkOutToday(id) },
                                onOpenManualAttendance = {
                                    preselectedEmployeeForAttendance = employees.firstOrNull {
                                        it.id == "self_${profile.userId}"
                                    } ?: employees.firstOrNull()
                                    showManualAttendanceSheet = true
                                },
                                onOpenRegisterEmployee = {
                                    editingEmployee = null
                                    showEmployeeSheet = true
                                }
                            )
                        }

                        AppTab.HISTORY -> {
                            HistoryScreen(
                                records = attendanceRecords,
                                onCheckOutRecord = { id -> viewModel.checkOutToday(id) },
                                onDeleteRecord = { rec -> viewModel.deleteAttendance(rec) },
                                onOpenManualAttendance = {
                                    preselectedEmployeeForAttendance = employees.firstOrNull()
                                    showManualAttendanceSheet = true
                                }
                            )
                        }

                        AppTab.EMPLOYEES -> {
                            EmployeesScreen(
                                employees = employees,
                                onRegisterNewEmployee = {
                                    editingEmployee = null
                                    showEmployeeSheet = true
                                },
                                onEditEmployee = { emp ->
                                    editingEmployee = emp
                                    showEmployeeSheet = true
                                },
                                onDeleteEmployee = { emp -> viewModel.deleteEmployee(emp) },
                                onManualAttendanceForEmployee = { emp ->
                                    preselectedEmployeeForAttendance = emp
                                    showManualAttendanceSheet = true
                                }
                            )
                        }

                        AppTab.PROFILE -> {
                            ProfileScreen(
                                profile = profile,
                                attendanceRecords = attendanceRecords,
                                onEditProfile = { showEditProfileSheet = true },
                                onSignOut = onSignOut
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEmployeeSheet) {
        RegistrasiManualKaryawanSheet(
            existingEmployee = editingEmployee,
            isSubmitting = isSubmitting,
            onDismiss = { showEmployeeSheet = false },
            onSubmit = { code, name, email, phone, dept, pos, mode, status, joinDate, notes ->
                val target = editingEmployee
                if (target == null) {
                    viewModel.registerEmployee(
                        employeeCode = code,
                        fullName = name,
                        email = email,
                        phone = phone,
                        department = dept,
                        position = pos,
                        workMode = mode,
                        status = status,
                        joinDate = joinDate,
                        notes = notes,
                        onSuccess = { showEmployeeSheet = false }
                    )
                } else {
                    viewModel.updateEmployee(
                        employeeId = target.id,
                        employeeCode = code,
                        fullName = name,
                        email = email,
                        phone = phone,
                        department = dept,
                        position = pos,
                        workMode = mode,
                        status = status,
                        joinDate = joinDate,
                        notes = notes,
                        onSuccess = { showEmployeeSheet = false }
                    )
                }
            }
        )
    }

    if (showManualAttendanceSheet) {
        val fallbackSelf = remember(profile) {
            Employee(
                id = "self_${profile.userId}",
                userId = profile.userId,
                employeeCode = profile.employeeCode,
                fullName = profile.fullName,
                email = profile.email,
                phone = profile.phone,
                department = profile.department,
                position = profile.position,
                workMode = profile.workMode
            )
        }
        val availableEmployees = employees.ifEmpty { listOf(fallbackSelf) }

        AbsenManualSheet(
            employees = availableEmployees,
            preselectedEmployee = preselectedEmployeeForAttendance ?: availableEmployees.firstOrNull(),
            isSubmitting = isSubmitting,
            onDismiss = { showManualAttendanceSheet = false },
            onSubmit = { employee, dateKey, checkIn, checkOut, mode, status, notes ->
                viewModel.submitManualAttendance(
                    employee = employee,
                    dateKey = dateKey,
                    checkInTime = checkIn,
                    checkOutTime = checkOut,
                    workMode = mode,
                    status = status,
                    notes = notes,
                    onSuccess = { showManualAttendanceSheet = false }
                )
            }
        )
    }

    if (showEditProfileSheet) {
        EditProfilSheet(
            profile = profile,
            isSubmitting = isSubmitting,
            onDismiss = { showEditProfileSheet = false },
            onSubmit = { code, name, email, dept, pos, phone, mode, start, end ->
                viewModel.saveUserProfile(
                    employeeCode = code,
                    fullName = name,
                    email = email,
                    department = dept,
                    position = pos,
                    phone = phone,
                    workMode = mode,
                    shiftStart = start,
                    shiftEnd = end,
                    onSuccess = { showEditProfileSheet = false }
                )
            }
        )
    }
}
