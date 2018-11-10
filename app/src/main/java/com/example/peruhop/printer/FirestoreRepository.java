package com.example.peruhop.printer;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

public class FirestoreRepository {

    FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
            .setTimestampsInSnapshotsEnabled(true)
            .build();

    private FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public FirebaseFirestore getFirestore() {
        firestore.setFirestoreSettings(settings);
        return firestore;
    }

}
