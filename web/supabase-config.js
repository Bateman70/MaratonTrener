// Supabase Public Configuration
// Replace SUPABASE_URL and SUPABASE_ANON_KEY with your values from Supabase Dashboard -> Settings -> API

const SUPABASE_URL = window.ENV_SUPABASE_URL || "YOUR_SUPABASE_URL_HERE";
const SUPABASE_ANON_KEY = window.ENV_SUPABASE_ANON_KEY || "YOUR_SUPABASE_ANON_KEY_HERE";

// Initialize Supabase Client if SDK is loaded
let supabaseClient = null;

if (typeof supabase !== 'undefined' && SUPABASE_URL !== "YOUR_SUPABASE_URL_HERE") {
    supabaseClient = supabase.createClient(SUPABASE_URL, SUPABASE_ANON_KEY);
    console.log("Supabase Client initialized successfully.");
} else {
    console.warn("Supabase Client pending configuration or SDK load.");
}
