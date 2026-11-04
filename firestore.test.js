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

test("Unauthenticated user: cannot read items", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("items").get());
});

test("Unauthenticated user: cannot create item", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("items").add({
    ownerId: "someone",
    ownerEmail: "test@campus.edu",
    type: "LOST",
    title: "Keys",
    description: "Brass key on red lanyard",
    location: "Library"
  }));
});

test("Authenticated user: can read items", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("items").get());
});

test("Authenticated user: can create their own valid item", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("items").doc("item_1").set({
    ownerId: ALICE_UID,
    ownerEmail: "alice@campus.edu",
    ownerName: "Alice Smith",
    type: "LOST",
    title: "Calculus Textbook",
    description: "Hardcover Stewart Calculus 9th edition left on bench",
    location: "Science Building 2nd Floor",
    imageUrl: "https://example.com/img.jpg"
  }));
});

test("Authenticated user: can create item with empty email and base64 image", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("items").doc("item_flexible").set({
    ownerId: ALICE_UID,
    ownerEmail: "",
    ownerName: "Campus Student",
    type: "LOST",
    title: "Keys",
    description: "Red lanyard",
    location: "Library",
    imageUrl: "data:image/jpeg;base64," + "A".repeat(5000)
  }));
});



test("Authenticated user: cannot create item for another user", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("items").doc("item_2").set({
    ownerId: BOB_UID,
    ownerEmail: "bob@campus.edu",
    type: "FOUND",
    title: "Water Bottle",
    description: "Silver Hydroflask found near gym",
    location: "Rec Center"
  }));
});

test("Authenticated user: cannot create item with invalid type", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("items").doc("item_3").set({
    ownerId: ALICE_UID,
    ownerEmail: "alice@campus.edu",
    type: "STOLEN", // Invalid enum
    title: "Backpack",
    description: "Black backpack",
    location: "Library"
  }));
});

test("Authenticated user: cannot delete another user's item", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("items").doc("alice_item").set({
      ownerId: ALICE_UID,
      ownerEmail: "alice@campus.edu",
      type: "LOST",
      title: "Calculator",
      description: "TI-84 Plus",
      location: "Math Hall"
    });
  });

  const bobDb = testEnv.authenticatedContext(BOOB_UID = BOB_UID).firestore();
  await assertFails(bobDb.collection("items").doc("alice_item").delete());
});

test("Authenticated user: can delete their own item", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("items").doc("alice_item").set({
      ownerId: ALICE_UID,
      ownerEmail: "alice@campus.edu",
      type: "LOST",
      title: "Calculator",
      description: "TI-84 Plus",
      location: "Math Hall"
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("items").doc("alice_item").delete());
});
