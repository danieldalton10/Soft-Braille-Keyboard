/*
 * Copyright (C) 2016 The Soft Braille Keyboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dalton.braillekeyboard;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

/**
 * This is the MainActivity of the application.
 * 
 * This activity is shown when the user opens the app from the app screen. Most
 * of the app's logic is handled as part of the IME, but this activity provides
 * the UI for the user to enable this keyboard, practice in a text field,
 * navigate to the Settings screen and to navigate to the user manual.
 */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        updateUIStates();
    }

    // Called when we gain or lose focus.
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // If we have focus update the state of our buttons as system
            // settings might have changed.
            updateUIStates();
        }
    }

    // Triggered when the user clicks the enable keyboard button.
    public void onKeyboardSettings(View view) {
        startActivityForResult(new Intent(
                android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS), 0);
    }

    // Triggered when the button to change default input method is pressed.
    public void onDefaultInputMethod(View view) {
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.showInputMethodPicker();
    }

    // Triggered when the user clicks the button to read the manual.
    // Visit the appropriate url for the documentation for the current Locale in
    // the web browser.
    public void onURL(View view) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(getString(R.string.info_url)));
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, PreferenceIME.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    // Update the state of buttons (clickable) or not and decide whether to show
    // the sample text field.
    private void updateUIStates() {
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        List<InputMethodInfo> list = inputManager.getEnabledInputMethodList();
        Button btnEnable = (Button) findViewById(R.string.btn_id_enable);
        Button btnDefaultKeyboard = (Button) findViewById(R.string.btn_id_default_keyboard);
        EditText text = (EditText) findViewById(R.string.txt_id_practice);
        btnEnable.setEnabled(true);
        btnDefaultKeyboard.setEnabled(false);
        text.setVisibility(View.INVISIBLE);

        for (InputMethodInfo info : list) {
            if (info.getPackageName().equals(getPackageName())) {
                // sbk is enabled as an input method, may or may not be default.
                btnEnable.setEnabled(false);
                btnDefaultKeyboard.setEnabled(true);
                String id = Settings.Secure.getString(getContentResolver(),
                        Settings.Secure.DEFAULT_INPUT_METHOD);
                if (info.getId().equals(id)) {
                    // SBK is default so disable make sbk default button and
                    // show the sample text field.
                    btnDefaultKeyboard.setEnabled(false);
                    text.setVisibility(View.VISIBLE);
                }
                return;
            }
        }
    }
}
