package com.example.peruhop.myapplication2;

import com.google.firebase.firestore.FirebaseFirestore;

public class FirestoreRepository {

    private FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public FirebaseFirestore getFirestore() {
        return firestore;
    }

}
