package com.jostein.maratontrener;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class SecurityUtils {

    private static final String SHARED_PREFS_NAME = "EncryptedMyPrefs";

    public static SharedPreferences getEncryptedPrefs(Context context) {
        return getEncryptedPrefs(context, "EncryptedMyPrefs");
    }

    public static SharedPreferences getEncryptedPrefs(Context context, String prefsName) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            return EncryptedSharedPreferences.create(
                    context,
                    prefsName,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            // Fallback to regular prefs if encryption fails
            return context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        }
    }

    public static void setupCommaToDotWatcher(android.widget.EditText editText) {
        editText.addTextChangedListener(new android.text.TextWatcher() {
            private boolean isUpdating = false;
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                if (isUpdating) return;
                String original = s.toString();
                if (original.contains(",")) {
                    String replaced = original.replace(",", ".");
                    isUpdating = true;
                    editText.setText(replaced);
                    editText.setSelection(replaced.length());
                    isUpdating = false;
                }
            }
        });
    }
}
