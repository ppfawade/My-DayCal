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

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

function validTaskPayload(userId, overrides = {}) {
  const now = new Date();
  return {
    userId,
    dateKey: "2026-11-05",
    title: "Deep Work Architecture",
    notes: "Review timeline rendering performance",
    startMinute: 540,
    durationMinutes: 90,
    category: "focus",
    colorIndex: 0,
    completed: false,
    reminderMinutes: 10,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validJournalPayload(userId, dateKey = "2026-11-05", overrides = {}) {
  const now = new Date();
  return {
    userId,
    dateKey,
    headline: "Productive focus day",
    content: "Today felt productive and calm. Completed all morning blocks.",
    moodScore: 5,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

test("Unauthenticated user: cannot read or create tasks or journals", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("tasks")
      .where("userId", "==", ALICE_UID)
      .get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("tasks")
      .doc("task_1")
      .set(validTaskPayload(ALICE_UID))
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("journals")
      .doc("2026-11-05")
      .get()
  );
});

test("Authenticated user: can create, get, list, update, and delete own task (including break category and reminderMinutes)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const taskRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("tasks")
    .doc("task_1");

  const payload = validTaskPayload(ALICE_UID, { category: "break", colorIndex: 4, reminderMinutes: 15 });
  await assertSucceeds(taskRef.set(payload));
  await assertSucceeds(taskRef.get());
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("tasks")
      .where("userId", "==", ALICE_UID)
      .get()
  );

  // Valid update preserving createdAt and userId
  await assertSucceeds(
    taskRef.set({
      ...payload,
      completed: true,
      title: "Updated Break Block",
      reminderMinutes: -1,
      updatedAt: new Date(),
    })
  );

  await assertSucceeds(taskRef.delete());
});

test("Authenticated user: cannot access another user's tasks or journals", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(BOB_UID)
      .collection("tasks")
      .doc("bob_task")
      .set(validTaskPayload(BOB_UID));
    await context
      .firestore()
      .collection("users")
      .doc(BOB_UID)
      .collection("journals")
      .doc("2026-11-05")
      .set(validJournalPayload(BOB_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("tasks")
      .doc("bob_task")
      .get()
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("journals")
      .doc("2026-11-05")
      .get()
  );
});

test("Red Team Shadow Update & Value Poisoning: rejects ghost fields and invalid bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const taskRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("tasks")
    .doc("task_shadow");

  // Reject ghost field on create
  await assertFails(
    taskRef.set(validTaskPayload(ALICE_UID, { isVerified: true }))
  );

  // Reject invalid startMinute (> 1439)
  await assertFails(
    taskRef.set(validTaskPayload(ALICE_UID, { startMinute: 1500 }))
  );

  // Reject invalid reminderMinutes (> 1440)
  await assertFails(
    taskRef.set(validTaskPayload(ALICE_UID, { reminderMinutes: 2000 }))
  );

  // Reject identity spoofing (userId != auth.uid)
  await assertFails(
    taskRef.set(validTaskPayload(BOB_UID))
  );
});

test("Authenticated user: can create and update own journal entry with valid dateKey", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const journalRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("journals")
    .doc("2026-11-05");

  const payload = validJournalPayload(ALICE_UID, "2026-11-05");
  await assertSucceeds(journalRef.set(payload));
  await assertSucceeds(
    journalRef.set({
      ...payload,
      content: "Updated reflection for the evening.",
      moodScore: 4,
      updatedAt: new Date(),
    })
  );

  // Reject mismatched dateKey between path and body
  const badJournalRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("journals")
    .doc("2026-11-06");
  await assertFails(badJournalRef.set(payload));
});
