exports.cleanupExpiredStatuses = functions.pubsub
    .schedule("every 60 minutes")
    .onRun(async (context) => {
        const now = Date.now();
        const expired = await admin.firestore()
            .collection("statuses")
            .where("expiresAt", "<", now)
            .get();
        const batch = admin.firestore().batch();
        expired.forEach((doc) => batch.delete(doc.ref));
        await batch.commit();
        return null;
    });
