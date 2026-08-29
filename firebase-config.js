// Firebase configuration for Maratontrener Dashboard
// These keys were safely extracted directly from your Android configuration.

const firebaseConfig = {
    apiKey: "AIzaSyDGea1aIuyzQakdkEYB1hAqSIU64vbOSzw",
    authDomain: "the-rum-runner.firebaseapp.com",
    databaseURL: "https://the-rum-runner-default-rtdb.europe-west1.firebasedatabase.app",
    projectId: "the-rum-runner",
    storageBucket: "the-rum-runner.firebasestorage.app",
    messagingSenderId: "1082247622225",
    
    // IMPORTANT: Web App ID - Plug in your web app ID here once registered in Firebase Console!
    // Format: "1:1082247622225:web:xxxxxxxxxxxxxxxxx"
    appId: "1:1082247622225:web:13af5b00220e0b8f0f1270"
};

// Initialize Firebase if the SDK is loaded
let db = null;
let firebaseInitError = null;

if (typeof firebase !== 'undefined') {
    try {
        firebase.initializeApp(firebaseConfig);
        db = firebase.database();
        console.log("🔥 Firebase initialized successfully!");
    } catch (e) {
        firebaseInitError = e.message;
        console.error("Firebase initialization failed:", e);
    }
} else {
    firebaseInitError = "SDK failed to load (run a local server)";
    console.warn("Firebase SDK not loaded. Dashboard will run in simulated demo mode.");
}
