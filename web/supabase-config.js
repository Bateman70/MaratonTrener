// Supabase Public Configuration & Auth Helpers
const SUPABASE_URL = "https://sqvoleulthpjrfxgdnlt.supabase.co";
const SUPABASE_ANON_KEY = "sb_publishable_2rtHI4SlRlgta8SuCWEBeA_N7T5Rs17";

let supabaseClient = null;

if (typeof supabase !== 'undefined' && SUPABASE_URL !== "YOUR_SUPABASE_URL_HERE") {
    supabaseClient = supabase.createClient(SUPABASE_URL, SUPABASE_ANON_KEY);
    console.log("Supabase Client initialized successfully.");
} else {
    console.warn("Supabase Client pending configuration or SDK load.");
}

// ----------------------------------------------------
// AUTHENTICATION HELPERS
// ----------------------------------------------------

async function supabaseSignInWithEmail(email) {
    if (!supabaseClient) throw new Error("Supabase client not initialized.");
    const { error } = await supabaseClient.auth.signInWithOtp({
        email: email,
        options: {
            emailRedirectTo: window.location.origin
        }
    });
    if (error) throw error;
    return true;
}

async function supabaseSignInWithOAuth(provider) {
    if (!supabaseClient) throw new Error("Supabase client not initialized.");
    const { error } = await supabaseClient.auth.signInWithOAuth({
        provider: provider,
        options: {
            redirectTo: window.location.origin
        }
    });
    if (error) throw error;
}

async function supabaseSignOut() {
    if (!supabaseClient) return;
    const { error } = await supabaseClient.auth.signOut();
    if (error) console.error("Sign out error:", error.message);
    window.location.reload();
}

async function getSupabaseSession() {
    if (!supabaseClient) return null;
    const { data, error } = await supabaseClient.auth.getSession();
    if (error) return null;
    return data.session;
}
