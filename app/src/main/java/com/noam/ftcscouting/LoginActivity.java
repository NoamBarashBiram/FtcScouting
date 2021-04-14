package com.noam.ftcscouting;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.noam.ftcscouting.utils.StaticSync;
import com.noam.ftcscouting.utils.Toaster;

public class LoginActivity extends AppCompatActivity {
    public static final String LOGGED_IN = "LoggedIn", invalid = "";
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private EditText emailEditText, passwordEditText;
    private boolean isShowingPassword = false;
    private SharedPreferences preferences;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        emailEditText = findViewById(R.id.email_edittext);
        passwordEditText = findViewById(R.id.password_edittext);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String email = preferences.getString(getString(R.string.email_key), invalid);
        if (!invalid.equals(email)) {
            emailEditText.setText(email);
        }
    }

    public void showPass(View view) {
        ImageView img = (ImageView) view;
        if (isShowingPassword) {
            passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            img.setImageResource(R.drawable.eye);
        } else {
            passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            img.setImageResource(R.drawable.eye_disable);
        }
        isShowingPassword = !isShowingPassword;
    }

    public void onBackPressed() {
        finish();
    }

    public void tryLogin(View v) {
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        if (!email.matches("^[a-zA-Z0-9._\\-]+@[a-z]+(\\.[a-z]+)+$")) {
            Toaster.toast(this, getString(R.string.sorry_invalid_email));
            emailEditText.setError(getString(R.string.invalid_email));
        } else if (password.length() <= 6 || !password.matches("[a-zA-Z0-9]+")) {
            Toaster.toast(this, getString(R.string.sorry_invalid_password));
            passwordEditText.setError(getString(R.string.invalid_password));
        } else {
            tryLogin(email, password);
        }
    }

    public void tryLogin(String email, String password) {
        auth.signInWithEmailAndPassword(email, password).addOnSuccessListener(obj -> {
            preferences.edit()
                    .putString(getString(R.string.email_key), email)
                    .apply();
            StaticSync.send(LOGGED_IN);
            finish();
        }).addOnFailureListener(ex -> Toaster.toast(this, getString(R.string.login_failed)));
    }
}