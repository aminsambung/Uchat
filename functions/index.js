const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

/**
 * Cloud Function: Kirim notifikasi Ping ke penerima.
 * Trigger: setiap ada dokumen baru di koleksi `pings`.
 */
exports.sendPingNotification = functions.firestore
    .document("pings/{pingId}")
    .onCreate(async (snap, context) => {
        const pingData = snap.data();
        const receiverEmail = pingData.receiver;
        const senderEmail = pingData.sender;

        console.log(`Ping dari ${senderEmail} untuk ${receiverEmail}`);

        // 1. Cari user penerima berdasarkan email
        const usersRef = admin.firestore().collection("users");
        const snapshot = await usersRef.where("email", "==", receiverEmail).get();

        if (snapshot.empty) {
            console.log("Penerima tidak ditemukan:", receiverEmail);
            return null;
        }

        let fcmToken = null;
        snapshot.forEach((doc) => {
            fcmToken = doc.data().fcmToken;
        });

        if (!fcmToken) {
            console.log("Token FCM tidak ada untuk:", receiverEmail);
            return null;
        }

        // 2. Susun payload
        const payload = {
            data: {
                type: "ping",
                sender: senderEmail,
            },
        };

        // 3. Kirim FCM
        try {
            await admin.messaging().sendToDevice(fcmToken, payload);
            console.log("Ping terkirim ke:", receiverEmail);
        } catch (error) {
            console.error("Gagal kirim ping:", error);
        }

        // 4. Hapus dokumen ping dari database
        return snap.ref.delete();
    });

/**
 * Cloud Function: Kirim notifikasi pesan baru.
 * Trigger: setiap ada pesan baru di chats/{chatId}/messages.
 */
exports.sendMessageNotification = functions.firestore
    .document("chats/{chatId}/messages/{messageId}")
    .onCreate(async (snap, context) => {
        const messageData = snap.data();
        const chatId = context.params.chatId;

        // 1. Ambil data chat untuk tahu siapa penerimanya
        const chatDoc = await admin.firestore().collection("chats").doc(chatId).get();
        if (!chatDoc.exists) return null;

        const chatData = chatDoc.data();
        const members = chatData.members || [];
        const senderEmail = messageData.sender;

        // 2. Cari user penerima (bukan pengirim)
        const usersRef = admin.firestore().collection("users");
        const senderSnap = await usersRef.where("email", "==", senderEmail).get();
        let senderUid = null;
        senderSnap.forEach((doc) => {
            senderUid = doc.id;
        });

        const receiverUid = members.find((uid) => uid !== senderUid);
        if (!receiverUid) return null;

        // 3. Ambil FCM token penerima
        const receiverDoc = await usersRef.doc(receiverUid).get();
        const fcmToken = receiverDoc.data()?.fcmToken;

        if (!fcmToken) return null;

        // 4. Susun payload
        const messageText =
            messageData.type === "image" ? "📷 Foto" : messageData.text;
        const senderName = senderSnap.docs[0]?.data()?.displayName || "Pesan baru";

        const payload = {
            data: {
                type: "message",
                sender: senderName,
                text: messageText,
            },
        };

        // 5. Kirim
        try {
            await admin.messaging().sendToDevice(fcmToken, payload);
            console.log("Notifikasi pesan terkirim ke:", receiverUid);
        } catch (error) {
            console.error("Gagal kirim notifikasi pesan:", error);
        }

        return null;
    });

/**
 * Cloud Function Terjadwal: Hapus status kadaluarsa lebih cepat.
 * (Backup untuk TTL policy — dijalankan setiap jam).
 */
exports.cleanupExpiredStatuses = functions.pubsub
    .schedule("every 60 minutes")
    .onRun(async (context) => {
        const now = Date.now();
        const expired = await admin.firestore()
            .collection("statuses")
            .where("expiresAt", "<", now)
            .get();

        if (expired.empty) return null;

        const batch = admin.firestore().batch();
        expired.forEach((doc) => batch.delete(doc.ref));
        await batch.commit();

        console.log(`Dihapus ${expired.size} status kadaluarsa.`);
        return null;
    });
