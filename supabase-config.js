// Supabase Public Configuration
const SUPABASE_URL = "https://sqvoleulthpjrfxgdnlt.supabase.co";
const SUPABASE_ANON_KEY = "sb_publishable_2rtHI4SlRlgta8SuCWEBeA_N7T5Rs17";

// Initialize Supabase Client if SDK is loaded
let supabaseClient = null;

if (typeof supabase !== 'undefined' && SUPABASE_URL !== "YOUR_SUPABASE_URL_HERE") {
    supabaseClient = supabase.createClient(SUPABASE_URL, SUPABASE_ANON_KEY);
    console.log("Supabase Client initialized successfully.");
} else {
    console.warn("Supabase Client pending configuration or SDK load.");
}
