const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const DATABASE_ID = "ai-studio-android-vibe-39c27958-ab61-4ac1-8bd1-0ca8ac6d2b0e";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const CHARLIE_UID = "charlie_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function dbFor(uid) {
  return testEnv.authenticatedContext(uid).firestore({ databaseId: DATABASE_ID });
}

function unauthDb() {
  return testEnv.unauthenticatedContext().firestore({ databaseId: DATABASE_ID });
}

function pastDate() {
  return new Date(Date.now() - 1000);
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

test("Unauthenticated caller cannot read public_profiles, posts, or conversations", async () => {
  const db = unauthDb();
  await assertFails(db.collection("public_profiles").where("isDiscoverable", "==", true).get());
  await assertFails(db.collection("posts").where("visibility", "==", "public").get());
  await assertFails(db.collection("conversations").where("participantIds", "array-contains", ALICE_UID).get());
});

test("Public profile: owner can create and edit, another user cannot spoof or inject ghost fields", async () => {
  const aliceDb = dbFor(ALICE_UID);
  const bobDb = dbFor(BOB_UID);
  const ts = pastDate();

  await assertSucceeds(
    aliceDb.collection("public_profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      handle: "alice_vibes",
      displayName: "Alice",
      bio: "Film & neon",
      avatarData: "",
      isDiscoverable: true,
      followersCount: 0,
      followingCount: 0,
      postsCount: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Shadow update with ghost field must fail
  await assertFails(
    aliceDb.collection("public_profiles").doc(ALICE_UID).update({
      isVerified: true,
      updatedAt: ts,
    })
  );

  // Bob cannot edit Alice's bio
  await assertFails(
    bobDb.collection("public_profiles").doc(ALICE_UID).update({
      bio: "Hacked",
      updatedAt: ts,
    })
  );

  // Bob cannot list without matching filter (rules are not filters)
  await assertFails(bobDb.collection("public_profiles").get());

  // Bob can discover with matching filter
  await assertSucceeds(bobDb.collection("public_profiles").where("isDiscoverable", "==", true).get());
});

test("PII Isolation: Bob cannot read Alice's private user doc or moodboard collections", async () => {
  const aliceDb = dbFor(ALICE_UID);
  const bobDb = dbFor(BOB_UID);
  const ts = pastDate();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("collections").doc("inspo").set({
      userId: ALICE_UID,
      name: "Inspo",
      postIds: ["post_1"],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("collections").get());
});

test("Posts, likes, comments, and polls: full lifecycle and security isolation", async () => {
  const aliceDb = dbFor(ALICE_UID);
  const bobDb = dbFor(BOB_UID);
  const ts = pastDate();

  await assertSucceeds(
    aliceDb.collection("posts").doc("post_1").set({
      authorId: ALICE_UID,
      authorHandle: "alice_vibes",
      authorAvatar: "",
      visibility: "public",
      mediaType: "poll",
      mediaPayloads: ["data:image/jpeg;base64,abc"],
      caption: "Sunset or midnight?",
      hashtags: ["#vibe"],
      pollQuestion: "Best shoot hour?",
      pollOptions: ["Golden Hour", "Blue Hour"],
      pollCounts: [0, 0],
      likesCount: 0,
      commentsCount: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Unfiltered query must fail
  await assertFails(bobDb.collection("posts").get());
  // Matching visibility query succeeds
  await assertSucceeds(bobDb.collection("posts").where("visibility", "==", "public").get());

  // Bob cannot delete Alice's post or edit caption
  await assertFails(bobDb.collection("posts").doc("post_1").delete());
  await assertFails(
    bobDb.collection("posts").doc("post_1").update({
      caption: "Hijacked",
      updatedAt: ts,
    })
  );

  // Bob can like, comment, and vote in poll
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").collection("likes").doc(BOB_UID).set({
      userId: BOB_UID,
      createdAt: ts,
    })
  );
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").update({
      likesCount: 1,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").collection("comments").doc("c_1").set({
      authorId: BOB_UID,
      authorHandle: "bob_shot",
      visibility: "public",
      text: "Golden Hour!",
      createdAt: ts,
    })
  );
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").collection("pollVotes").doc(BOB_UID).set({
      userId: BOB_UID,
      optionIndex: 0,
      createdAt: ts,
    })
  );
});

test("Stories and Direct Messages: strict participant isolation", async () => {
  const aliceDb = dbFor(ALICE_UID);
  const bobDb = dbFor(BOB_UID);
  const charlieDb = dbFor(CHARLIE_UID);
  const ts = pastDate();
  const future = new Date(Date.now() + 86400000);

  await assertSucceeds(
    aliceDb.collection("stories").doc("story_1").set({
      authorId: ALICE_UID,
      authorHandle: "alice_vibes",
      visibility: "public",
      mediaPayload: "data:image/jpeg;base64,story",
      captionOverlay: "Studio check",
      moodTag: "Creative",
      createdAt: ts,
      expiresAt: future,
    })
  );
  await assertFails(bobDb.collection("stories").doc("story_1").delete());

  // Conversation between Alice and Bob
  await assertSucceeds(
    aliceDb.collection("conversations").doc("conv_ab").set({
      participantIds: [ALICE_UID, BOB_UID],
      participantHandles: ["alice_vibes", "bob_shot"],
      lastMessage: "Hey!",
      lastSenderId: ALICE_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("conversations").doc("conv_ab").collection("messages").doc("msg_1").set({
      participantIds: [ALICE_UID, BOB_UID],
      senderId: ALICE_UID,
      senderHandle: "alice_vibes",
      text: "Hey Bob!",
      createdAt: ts,
    })
  );

  // Charlie cannot read conversation or send a message into it
  await assertFails(charlieDb.collection("conversations").doc("conv_ab").get());
  await assertFails(
    charlieDb.collection("conversations").doc("conv_ab").collection("messages").doc("msg_2").set({
      participantIds: [ALICE_UID, BOB_UID, CHARLIE_UID],
      senderId: CHARLIE_UID,
      senderHandle: "charlie",
      text: "Intruding",
      createdAt: ts,
    })
  );
});
