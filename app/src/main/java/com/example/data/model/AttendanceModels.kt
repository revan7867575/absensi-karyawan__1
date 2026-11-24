package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class WorkMode(val label: String) {
    WFO("WFO"),
    WFH("WFH"),
    HYBRID("Hybrid");

    companion object {
        fun fromRaw(raw: String?): WorkMode =
            entries.firstOrNull { it.name == raw } ?: WFO
    }
}

enum class EmployeeStatus(val label: String) {
    AKTIF("Aktif"),
    CUTI("Cuti"),
    NONAKTIF("Nonaktif");

    companion object {
        fun fromRaw(raw: String?): EmployeeStatus =
            entries.firstOrNull { it.name == raw } ?: AKTIF
    }
}

enum class AttendanceStatus(val label: String) {
    HADIR("Hadir"),
    TERLAMBAT("Terlambat"),
    IZIN("Izin"),
    SAKIT("Sakit");

    companion object {
        fun fromRaw(raw: String?): AttendanceStatus =
            entries.firstOrNull { it.name == raw } ?: HADIR
    }
}

enum class EntryType(val label: String) {
    LIVE("Live"),
    MANUAL("Manual");

    companion object {
        fun fromRaw(raw: String?): EntryType =
            entries.firstOrNull { it.name == raw } ?: MANUAL
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val employeeCode: String = "",
    val fullName: String = "",
    val email: String = "",
    val department: String = "",
    val position: String = "",
    val phone: String = "",
    val workMode: String = WorkMode.WFO.name,
    val shiftStart: String = "08:00",
    val shiftEnd: String = "17:00",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Employee(
    @DocumentId val id: String = "",
    val userId: String = "",
    val employeeCode: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val department: String = "",
    val position: String = "",
    val workMode: String = WorkMode.WFO.name,
    val status: String = EmployeeStatus.AKTIF.name,
    val joinDate: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AttendanceRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val employeeId: String = "",
    val employeeName: String = "",
    val employeeCode: String = "",
    val department: String = "",
    val dateKey: String = "",
    val checkInTime: String = "",
    val checkOutTime: String? = null,
    val workMode: String = WorkMode.WFO.name,
    val status: String = AttendanceStatus.HADIR.name,
    val entryType: String = EntryType.LIVE.name,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

val DefaultDepartments = listOf(
    "Engineering",
    "Operasional",
    "HR & People",
    "Keuangan",
    "Marketing",
    "Produk"
)

data class PendingRegistrationDraft(
    val employeeCode: String,
    val fullName: String,
    val email: String,
    val department: String,
    val position: String,
    val phone: String,
    val workMode: WorkMode,
    val shiftStart: String,
    val shiftEnd: String
)

