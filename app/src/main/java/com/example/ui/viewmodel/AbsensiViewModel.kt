package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.Employee
import com.example.data.model.EmployeeStatus
import com.example.data.model.EntryType
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.WorkMode
import com.example.data.repository.AttendanceRepository
import com.example.util.DateTimeUtils
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AbsensiViewModel(
    private val repository: AttendanceRepository,
    val currentUserId: String
) : ViewModel() {

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId)
            .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing profile", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Gagal memuat profil karyawan"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val employeesState: StateFlow<UiState<List<Employee>>> =
        repository.observeEmployees(currentUserId)
            .map<List<Employee>, UiState<List<Employee>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing employees", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Gagal memuat data karyawan"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val attendanceState: StateFlow<UiState<List<AttendanceRecord>>> =
        repository.observeAttendance(currentUserId)
            .map<List<AttendanceRecord>, UiState<List<AttendanceRecord>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing attendance", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Gagal memuat riwayat absensi"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun saveUserProfile(
        employeeCode: String,
        fullName: String,
        email: String,
        department: String,
        position: String,
        phone: String,
        workMode: WorkMode,
        shiftStart: String,
        shiftEnd: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.saveUserProfile(
                employeeCode = employeeCode,
                fullName = fullName,
                email = email,
                department = department,
                position = position,
                phone = phone,
                workMode = workMode,
                shiftStart = shiftStart,
                shiftEnd = shiftEnd
            )
            _isSubmitting.value = false
            result.onSuccess {
                _feedbackMessage.value = "Profil karyawan berhasil disimpan"
                onSuccess()
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Gagal menyimpan profil"
            }
        }
    }

    fun checkInToday(profile: UserProfile, selectedWorkMode: WorkMode) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val nowTime = DateTimeUtils.currentTimeHHmm()
            val today = DateTimeUtils.todayDateKey()
            val status = if (DateTimeUtils.isLateCheckIn(nowTime, profile.shiftStart)) {
                AttendanceStatus.TERLAMBAT
            } else {
                AttendanceStatus.HADIR
            }
            val result = repository.createAttendance(
                employeeId = "self_${profile.userId}",
                employeeName = profile.fullName,
                employeeCode = profile.employeeCode,
                department = profile.department,
                dateKey = today,
                checkInTime = nowTime,
                checkOutTime = null,
                workMode = selectedWorkMode,
                status = status,
                entryType = EntryType.LIVE,
                notes = if (status == AttendanceStatus.TERLAMBAT) "Melewati batas toleransi shift (${profile.shiftStart})" else ""
            )
            _isSubmitting.value = false
            result.onSuccess {
                _feedbackMessage.value = "Check-In berhasil pukul $nowTime"
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Gagal melakukan Check-In"
            }
        }
    }

    fun checkOutToday(attendanceId: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val nowTime = DateTimeUtils.currentTimeHHmm()
            val result = repository.checkOutAttendance(attendanceId, nowTime)
            _isSubmitting.value = false
            result.onSuccess {
                _feedbackMessage.value = "Check-Out berhasil pukul $nowTime"
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Gagal melakukan Check-Out"
            }
        }
    }

    fun registerEmployee(
        employeeCode: String,
        fullName: String,
        email: String,
        phone: String,
        department: String,
        position: String,
        workMode: WorkMode,
        status: EmployeeStatus,
        joinDate: String,
        notes: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.registerEmployee(
                employeeCode = employeeCode,
                fullName = fullName,
                email = email,
                phone = phone,
                department = department,
                position = position,
                workMode = workMode,
                status = status,
                joinDate = joinDate,
                notes = notes
            )
            _isSubmitting.value = false
            result.onSuccess {
                _feedbackMessage.value = "$fullName berhasil didaftarkan"
                onSuccess()
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Gagal mendaftarkan karyawan"
            }
        }
    }

    fun updateEmployee(
        employeeId: String,
        employeeCode: String,
        fullName: String,
        email: String,
        phone: String,
        department: String,
        position: String,
        workMode: WorkMode,
        status: EmployeeStatus,
        joinDate: String,
        notes: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.updateEmployee(
                employeeId = employeeId,
                employeeCode = employeeCode,
                fullName = fullName,
                email = email,
                phone = phone,
                department = department,
                position = position,
                workMode = workMode,
                status = status,
                joinDate = joinDate,
                notes = notes
            )
            _isSubmitting.value = false
            result.onSuccess {
                _feedbackMessage.value = "Data $fullName diperbarui"
                onSuccess()
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Gagal memperbarui data karyawan"
            }
        }
    }

    fun deleteEmployee(employee: Employee) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.deleteEmployee(employee.id)
            _isSubmitting.value = false
            result.onSuccess {
                _feedbackMessage.value = "${employee.fullName} dihapus dari direktori"
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Gagal menghapus karyawan"
            }
        }
    }

    fun submitManualAttendance(
        employee: Employee,
        dateKey: String,
        checkInTime: String,
        checkOutTime: String?,
        workMode: WorkMode,
        status: AttendanceStatus,
        notes: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.createAttendance(
                employeeId = employee.id,
                employeeName = employee.fullName,
                employeeCode = employee.employeeCode,
                department = employee.department,
                dateKey = dateKey,
                checkInTime = checkInTime,
                checkOutTime = checkOutTime,
                workMode = workMode,
                status = status,
                entryType = EntryType.MANUAL,
                notes = notes
            )
            _isSubmitting.value = false
            result.onSuccess {
                _feedbackMessage.value = "Absen manual ${employee.fullName} tersimpan"
                onSuccess()
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Gagal menyimpan absen manual"
            }
        }
    }

    fun deleteAttendance(record: AttendanceRecord) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.deleteAttendance(record.id)
            _isSubmitting.value = false
            result.onSuccess {
                _feedbackMessage.value = "Catatan absensi dihapus"
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Gagal menghapus catatan"
            }
        }
    }

    private companion object {
        const val TAG = "AbsensiVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
