package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.Employee
import com.example.data.model.EmployeeStatus
import com.example.data.model.EntryType
import com.example.data.model.UserProfile
import com.example.data.model.WorkMode
import com.example.util.DateTimeUtils
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AttendanceRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth: FirebaseAuth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Pengguna harus masuk dengan Google terlebih dahulu.")
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        require(userId.isNotBlank()) { "userId tidak boleh kosong" }
        val path = "users/$userId"
        db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.GET, path)
                }
                throw error
            }
            .collect { emit(it) }
    }

    fun observeEmployees(userId: String): Flow<List<Employee>> = flow {
        require(userId.isNotBlank()) { "userId tidak boleh kosong" }
        val path = "users/$userId/employees"
        db.collection("users").document(userId).collection("employees")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Employee::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.fullName.lowercase() }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
            .collect { emit(it) }
    }

    fun observeAttendance(userId: String): Flow<List<AttendanceRecord>> = flow {
        require(userId.isNotBlank()) { "userId tidak boleh kosong" }
        val path = "users/$userId/attendance"
        db.collection("users").document(userId).collection("attendance")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(AttendanceRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(
                        compareByDescending<AttendanceRecord> { it.dateKey }
                            .thenByDescending { it.checkInTime }
                    )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
            .collect { emit(it) }
    }

    suspend fun saveUserProfile(
        employeeCode: String,
        fullName: String,
        email: String,
        department: String,
        position: String,
        phone: String,
        workMode: WorkMode,
        shiftStart: String,
        shiftEnd: String
    ): Result<Unit> {
        var docPath = "users/unauthenticated"
        return try {
            val uid = requireUserId()
            val cleanCode = employeeCode.trim().take(32)
            val cleanName = fullName.trim().take(80)
            val cleanEmail = email.trim().take(120)
            val cleanDept = department.trim().take(60)
            val cleanPos = position.trim().take(60)
            val cleanPhone = phone.trim().take(24)
            val cleanStart = shiftStart.trim().take(10)
            val cleanEnd = shiftEnd.trim().take(10)

            require(cleanCode.length in 2..32) { "Kode karyawan harus 2-32 karakter" }
            require(cleanName.length in 2..80) { "Nama lengkap harus 2-80 karakter" }
            require(cleanEmail.length in 3..120) { "Email harus 3-120 karakter" }
            require(cleanDept.length in 2..60) { "Divisi harus 2-60 karakter" }
            require(cleanPos.length in 2..60) { "Jabatan harus 2-60 karakter" }
            require(cleanStart.length in 4..10) { "Format jam mulai shift tidak valid" }
            require(cleanEnd.length in 4..10) { "Format jam selesai shift tidak valid" }

            val userRef = db.collection("users").document(uid)
            docPath = userRef.path
            val existing = userRef.get().await()

            if (existing.exists()) {
                val updatePayload = mapOf(
                    "employeeCode" to cleanCode,
                    "fullName" to cleanName,
                    "email" to cleanEmail,
                    "department" to cleanDept,
                    "position" to cleanPos,
                    "phone" to cleanPhone,
                    "workMode" to workMode.name,
                    "shiftStart" to cleanStart,
                    "shiftEnd" to cleanEnd,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userRef.update(updatePayload).await()
            } else {
                val createPayload = mapOf(
                    "userId" to uid,
                    "employeeCode" to cleanCode,
                    "fullName" to cleanName,
                    "email" to cleanEmail,
                    "department" to cleanDept,
                    "position" to cleanPos,
                    "phone" to cleanPhone,
                    "workMode" to workMode.name,
                    "shiftStart" to cleanStart,
                    "shiftEnd" to cleanEnd,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userRef.set(createPayload).await()
            }

            // Keep self employee directory record in sync
            val selfEmpRef = db.collection("users").document(uid)
                .collection("employees").document("self_$uid")
            val selfEmpSnap = selfEmpRef.get().await()
            if (selfEmpSnap.exists()) {
                selfEmpRef.update(
                    mapOf(
                        "employeeCode" to cleanCode,
                        "fullName" to cleanName,
                        "email" to cleanEmail,
                        "phone" to cleanPhone,
                        "department" to cleanDept,
                        "position" to cleanPos,
                        "workMode" to workMode.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                selfEmpRef.set(
                    mapOf(
                        "userId" to uid,
                        "employeeCode" to cleanCode,
                        "fullName" to cleanName,
                        "email" to cleanEmail,
                        "phone" to cleanPhone,
                        "department" to cleanDept,
                        "position" to cleanPos,
                        "workMode" to workMode.name,
                        "status" to EmployeeStatus.AKTIF.name,
                        "joinDate" to DateTimeUtils.todayDateKey(),
                        "notes" to "Akun utama pemilik workspace",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun registerEmployee(
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
    ): Result<Unit> {
        var docPath = "users/unauthenticated/employees"
        return try {
            val uid = requireUserId()
            val cleanCode = employeeCode.trim().take(32)
            val cleanName = fullName.trim().take(80)
            val cleanEmail = email.trim().take(120)
            val cleanPhone = phone.trim().take(24)
            val cleanDept = department.trim().take(60)
            val cleanPos = position.trim().take(60)
            val cleanJoinDate = joinDate.trim().ifBlank { DateTimeUtils.todayDateKey() }.take(20)
            val cleanNotes = notes.trim().take(240)

            require(cleanCode.length in 2..32) { "Kode karyawan harus 2-32 karakter" }
            require(cleanName.length in 2..80) { "Nama karyawan harus 2-80 karakter" }
            require(cleanDept.length in 2..60) { "Divisi harus 2-60 karakter" }
            require(cleanPos.length in 2..60) { "Jabatan harus 2-60 karakter" }
            require(cleanJoinDate.length in 4..20) { "Tanggal bergabung tidak valid" }

            val docRef = db.collection("users").document(uid).collection("employees").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "employeeCode" to cleanCode,
                "fullName" to cleanName,
                "email" to cleanEmail,
                "phone" to cleanPhone,
                "department" to cleanDept,
                "position" to cleanPos,
                "workMode" to workMode.name,
                "status" to status.name,
                "joinDate" to cleanJoinDate,
                "notes" to cleanNotes,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateEmployee(
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
        notes: String
    ): Result<Unit> {
        var docPath = "users/unauthenticated/employees/$employeeId"
        return try {
            val uid = requireUserId()
            require(employeeId.isNotBlank()) { "ID karyawan tidak boleh kosong" }
            val cleanCode = employeeCode.trim().take(32)
            val cleanName = fullName.trim().take(80)
            val cleanEmail = email.trim().take(120)
            val cleanPhone = phone.trim().take(24)
            val cleanDept = department.trim().take(60)
            val cleanPos = position.trim().take(60)
            val cleanJoinDate = joinDate.trim().ifBlank { DateTimeUtils.todayDateKey() }.take(20)
            val cleanNotes = notes.trim().take(240)

            require(cleanCode.length in 2..32) { "Kode karyawan harus 2-32 karakter" }
            require(cleanName.length in 2..80) { "Nama karyawan harus 2-80 karakter" }
            require(cleanDept.length in 2..60) { "Divisi harus 2-60 karakter" }
            require(cleanPos.length in 2..60) { "Jabatan harus 2-60 karakter" }

            val docRef = db.collection("users").document(uid).collection("employees").document(employeeId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "employeeCode" to cleanCode,
                    "fullName" to cleanName,
                    "email" to cleanEmail,
                    "phone" to cleanPhone,
                    "department" to cleanDept,
                    "position" to cleanPos,
                    "workMode" to workMode.name,
                    "status" to status.name,
                    "joinDate" to cleanJoinDate,
                    "notes" to cleanNotes,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteEmployee(employeeId: String): Result<Unit> {
        var docPath = "users/unauthenticated/employees/$employeeId"
        return try {
            val uid = requireUserId()
            require(employeeId.isNotBlank()) { "ID karyawan tidak boleh kosong" }
            val docRef = db.collection("users").document(uid).collection("employees").document(employeeId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun createAttendance(
        employeeId: String,
        employeeName: String,
        employeeCode: String,
        department: String,
        dateKey: String,
        checkInTime: String,
        checkOutTime: String?,
        workMode: WorkMode,
        status: AttendanceStatus,
        entryType: EntryType,
        notes: String
    ): Result<Unit> {
        var docPath = "users/unauthenticated/attendance"
        return try {
            val uid = requireUserId()
            val cleanEmpId = employeeId.trim().take(128)
            val cleanName = employeeName.trim().take(80)
            val cleanCode = employeeCode.trim().take(32)
            val cleanDept = department.trim().take(60)
            val cleanDate = dateKey.trim().take(12)
            val cleanIn = checkInTime.trim().take(10)
            val cleanOut = checkOutTime?.trim()?.takeIf { it.length in 4..10 }
            val cleanNotes = notes.trim().take(240)

            require(cleanEmpId.isNotBlank()) { "ID karyawan wajib diisi" }
            require(cleanName.length in 2..80) { "Nama karyawan harus 2-80 karakter" }
            require(cleanCode.length in 2..32) { "Kode karyawan harus 2-32 karakter" }
            require(cleanDept.length in 2..60) { "Divisi harus 2-60 karakter" }
            require(cleanDate.length in 8..12) { "Tanggal tidak valid" }
            require(cleanIn.length in 4..10) { "Jam masuk tidak valid" }

            val docRef = db.collection("users").document(uid).collection("attendance").document()
            docPath = docRef.path

            @Suppress("UNCHECKED_CAST")
            val payload = mapOf(
                "userId" to uid,
                "employeeId" to cleanEmpId,
                "employeeName" to cleanName,
                "employeeCode" to cleanCode,
                "department" to cleanDept,
                "dateKey" to cleanDate,
                "checkInTime" to cleanIn,
                "checkOutTime" to cleanOut,
                "workMode" to workMode.name,
                "status" to status.name,
                "entryType" to entryType.name,
                "notes" to cleanNotes,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun checkOutAttendance(attendanceId: String, checkOutTime: String): Result<Unit> {
        var docPath = "users/unauthenticated/attendance/$attendanceId"
        return try {
            val uid = requireUserId()
            require(attendanceId.isNotBlank()) { "ID absensi tidak boleh kosong" }
            val cleanOut = checkOutTime.trim().take(10)
            require(cleanOut.length in 4..10) { "Jam keluar tidak valid" }

            val docRef = db.collection("users").document(uid).collection("attendance").document(attendanceId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "checkOutTime" to cleanOut,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteAttendance(attendanceId: String): Result<Unit> {
        var docPath = "users/unauthenticated/attendance/$attendanceId"
        return try {
            val uid = requireUserId()
            require(attendanceId.isNotBlank()) { "ID absensi tidak boleh kosong" }
            val docRef = db.collection("users").document(uid).collection("attendance").document(attendanceId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
