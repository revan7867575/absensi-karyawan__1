const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: denied access to users, employees, and attendance", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("employees").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("attendance").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("employees").doc("emp_1").set({
      userId: ALICE_UID,
      employeeCode: "EMP-001",
      fullName: "Budi Santoso",
      department: "Engineering",
      position: "Developer",
      workMode: "WFO",
      status: "AKTIF",
      joinDate: "2026-11-23",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("UserProfile: owner can create and update profile; rejects ghost fields and cross-user access", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      employeeCode: "EMP-001",
      fullName: "Alice Wijaya",
      email: "alice@perusahaan.id",
      department: "Engineering",
      position: "Tech Lead",
      phone: "081234567890",
      workMode: "HYBRID",
      shiftStart: "09:00",
      shiftEnd: "18:00",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      position: "VP Engineering",
      workMode: "WFO",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Immutable createdAt mutation fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Ghost field / @DocumentId id field fails
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      id: BOB_UID,
      userId: BOB_UID,
      employeeCode: "EMP-002",
      fullName: "Bob",
      email: "bob@perusahaan.id",
      department: "HRD",
      position: "Staff",
      workMode: "WFO",
      shiftStart: "08:00",
      shiftEnd: "17:00",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Cross-user read & write denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      fullName: "Hijacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Employee directory: owner can register manual employee, list, and update; cross-user denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  // Empty collection list succeeds for owner
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("employees").get());

  // Create manual employee
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("employees").doc("emp_01").set({
      userId: ALICE_UID,
      employeeCode: "EMP-101",
      fullName: "Siti Rahmawati",
      email: "siti@perusahaan.id",
      phone: "08129876543",
      department: "Keuangan",
      position: "Senior Accountant",
      workMode: "WFO",
      status: "AKTIF",
      joinDate: "2026-11-23",
      notes: "Registrasi manual oleh HR",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Update employee status & position
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("employees").doc("emp_01").update({
      status: "CUTI",
      notes: "Cuti tahunan",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Invalid enum rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("employees").doc("emp_02").set({
      userId: ALICE_UID,
      employeeCode: "EMP-102",
      fullName: "Invalid Status",
      department: "HRD",
      position: "Staff",
      workMode: "WFO",
      status: "INVALID_STATUS",
      joinDate: "2026-11-23",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Future timestamp rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("employees").doc("emp_03").set({
      userId: ALICE_UID,
      employeeCode: "EMP-103",
      fullName: "Future Time",
      department: "HRD",
      position: "Staff",
      workMode: "WFO",
      status: "AKTIF",
      joinDate: "2026-11-23",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Bob cannot list or read Alice's employees
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("employees").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("employees").doc("emp_01").get());
});

test("Attendance records: live check-in/out and manual attendance entry succeed for owner; cross-user denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  // Live check-in (checkOutTime omitted)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("attendance").doc("att_live_1").set({
      userId: ALICE_UID,
      employeeId: "emp_01",
      employeeName: "Siti Rahmawati",
      employeeCode: "EMP-101",
      department: "Keuangan",
      dateKey: "2026-11-23",
      checkInTime: "08:45",
      workMode: "WFO",
      status: "HADIR",
      entryType: "LIVE",
      notes: "",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Live check-out partial update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("attendance").doc("att_live_1").update({
      checkOutTime: "17:15",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Manual attendance entry with checkOutTime included
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("attendance").doc("att_manual_1").set({
      userId: ALICE_UID,
      employeeId: "emp_01",
      employeeName: "Siti Rahmawati",
      employeeCode: "EMP-101",
      department: "Keuangan",
      dateKey: "2026-11-22",
      checkInTime: "09:15",
      checkOutTime: "18:00",
      workMode: "WFH",
      status: "TERLAMBAT",
      entryType: "MANUAL",
      notes: "Input manual karena kendala sinyal",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Query alignment test
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("attendance").get()
  );

  // Cross-user read & write denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("attendance").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("attendance").doc("att_live_1").delete()
  );
});
