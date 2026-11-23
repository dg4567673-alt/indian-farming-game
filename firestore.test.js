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

function validFarmPayload(uid, createdDate, updatedDate) {
  return {
    userId: uid,
    farmerName: "Shubham",
    coins: 5000,
    level: 1,
    xp: 0,
    dayCount: 1,
    activeVehicleId: "tractor",
    unlockedVehicles: ["tractor", "trolley", "harvester", "thresher", "jcb", "bulldozer", "truck", "pickup"],
    fieldStates: ["WHEAT:RIPE:100", "RICE:GROWING:55", "SUGARCANE:PLOWED:0", "MUSTARD:UNTILLED:0", "EMPTY:ROUGH:0", "EMPTY:ROUGH:0"],
    cowCount: 2,
    buffaloCount: 2,
    goatCount: 3,
    henCount: 4,
    fodderCount: 12,
    milkCount: 4,
    wheatSeeds: 10,
    riceSeeds: 8,
    sugarcaneSeeds: 6,
    mustardSeeds: 6,
    wheatStock: 5,
    riceStock: 2,
    sugarcaneStock: 0,
    mustardStock: 0,
    fuelReserve: 85,
    missionsCompleted: 0,
    totalHarvested: 0,
    createdAt: createdDate,
    updatedAt: updatedDate,
  };
}

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

test("Unauthenticated user cannot read or write farm saves or market logs", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("farms").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("farms").doc("main_farm").set(
      validFarmPayload(ALICE_UID, new Date(Date.now() - 2000), new Date(Date.now() - 1000))
    )
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("market_logs").doc("log_1").set({
      userId: ALICE_UID,
      title: "Sold Wheat",
      category: "CROP_SALE",
      amountCoins: 1200,
      createdAt: new Date(Date.now() - 1000),
    })
  );
});

test("Authenticated owner: valid FarmSave create, partial sync update, and rename succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("farms").doc("main_farm").set(
      validFarmPayload(ALICE_UID, created, created)
    )
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("farms").doc("main_farm").update({
      coins: 6500,
      xp: 120,
      milkCount: 6,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("farms").doc("main_farm").update({
      farmerName: "Shubham Kisan",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("farms").get()
  );
});

test("Reject shadow fields, @DocumentId id field, immutable createdAt change, and negative coins", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  const withIdField = validFarmPayload(ALICE_UID, created, created);
  withIdField.id = "main_farm";
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("farms").doc("main_farm_2").set(withIdField)
  );

  const withGhost = validFarmPayload(ALICE_UID, created, created);
  withGhost.isAdmin = true;
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("farms").doc("main_farm_3").set(withGhost)
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("farms").doc("main_farm").set(
      validFarmPayload(ALICE_UID, created, created)
    )
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("farms").doc("main_farm").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("farms").doc("main_farm").update({
      coins: -10,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("farms").doc("main_farm").update({
      coins: 5500,
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Cross-user isolation and MarketLog rules work properly", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).collection("farms").doc("main_farm").set(
      validFarmPayload(BOB_UID, created, created)
    )
  );

  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("farms").doc("main_farm").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("farms").get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("farms").doc("main_farm").update({
      coins: 0,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("market_logs").doc("log_1").set({
      userId: ALICE_UID,
      title: "गेहूँ मंडी में बेचा",
      category: "CROP_SALE",
      amountCoins: 1800,
      createdAt: new Date(Date.now() - 1000),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("market_logs").doc("log_1").update({
      amountCoins: 99999,
    })
  );
});
