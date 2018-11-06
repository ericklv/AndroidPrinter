package com.example.peruhop.printer;

import com.google.firebase.firestore.FirebaseFirestore;

public class FirestoreRepository {

    private FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public FirebaseFirestore getFirestore() {
        return firestore;
    }

}
