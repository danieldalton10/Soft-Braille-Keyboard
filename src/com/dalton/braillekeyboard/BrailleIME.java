/*
 * Copyright (C) 2016 Daniel Dalton
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.support.v4.content.ContextCompat;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;

import com.googlecode.eyesfree.braille.translate.TableInfo;

/**
 * Implementation of an Input method service for Android.
 * 
 * Specifically, this IME service implements the capabilities to support Braille
 * input from a BrailleView and several editing capabilities.
 * 
 * You should not instantiate this class directly rather it will create it's own
 * View with the onCreateView method and set this service in that View to
 * facilitate communication between the View and the IME. You should communicate
 * according to the KeyboardListener interface and consult that for further
 * documentation.
 * 
 */
public class BrailleIME extends InputMethodService implements KeyboardListener {
    // Quick and dirty way to lock down apks for trial
    private static final String EXPIREY_DATE = "March 31, 2016";
    private static boolean isTrial = false;
    private static long endDate = getEndDate();

    private final List<Byte> cells = new ArrayList<Byte>();
    private final StringBuilder composingText = new StringBuilder();

    private BrailleParser brailleParser;
    private BrailleView brailleView = null;
    private int caps;
    private int cursor = -1;
    private int mark = -1;
    private boolean predictionOn;
    private boolean selectAll = false;

    private static long getEndDate() {
        try {
            return (new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH))
                    .parse(EXPIREY_DATE).getTime();
        } catch (ParseException PE) {
            // Never get here because the data is static.
            // If we mess up though, set the end date to 0 so the app can't be
            // used if in doubt.
            return 0;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (brailleParser == null) {
            brailleParser = new BrailleParser(this,
                    new BrailleParser.BrailleParserListener() {

                        @Override
                        public void onTranslatorReady(int status) {
                            brailleParserReady(status);
                        }
                    });
        }
    }

    @Override
    public View onCreateInputView() {
        super.onCreateInputView();
        if (!isTrial || System.currentTimeMillis() < endDate) {
            // passed the trial expiry check or it's a full version.
            brailleView = (BrailleView) getLayoutInflater().inflate(
                    R.layout.keyboard, null);
        }

        if (isTrial && System.currentTimeMillis() > endDate) {
            Intent intent = new Intent(this, IntentActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction(getString(R.string.action_expired));
            startActivity(intent);
        } else if (!Options.getBooleanPreference(this,
                R.string.pref_has_asked_record_audio_key, false)) {
            Options.switchBooleanPreference(this,
                    R.string.pref_has_asked_record_audio_key, false);
            // Android 6+ show a permission dialog for record audio dangerous
            // permission.
            // Only do this once on the very first run though.
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(this, IntentActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setAction(getString(R.string.action_record_audio_permission));
                startActivity(intent);
            }
        }
        return brailleView;
    }

    @Override
    public void onStartInput(EditorInfo info, boolean restarting) {
        super.onStartInput(info, restarting);
        // remove any existing selection.
        selectAll = false;
        mark = -1;

        predictionOn = false;
        // We are now going to initialize our state based on the type of
        // text being edited.
        switch (info.inputType & InputType.TYPE_MASK_CLASS) {
        case InputType.TYPE_CLASS_TEXT:
            predictionOn = true;
            // We now look for a few special variations of text that will
            // modify our behavior.
            int variation = info.inputType & InputType.TYPE_MASK_VARIATION;
            if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD
                    || variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    || variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
                    || variation == InputType.TYPE_TEXT_VARIATION_URI) {
                // Do not display predictions / what the user is typing
                // when they are entering a password or uri.
                predictionOn = false;
            }

            if ((info.inputType & InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                predictionOn = false;
            }
            break;
        default:
        }
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        if (!restarting && brailleView != null) {
            // Tell the user the keyboard is ready, but only the first time it
            // starts for this input field, not restarts. That'll be annoying.
            brailleView.onInitialiseForInput(this, this);
        }
        brailleParser.setTranslator(this);
    }

    @Override
    public void onFinishInputView(boolean finishingInput) {
        super.onFinishInputView(finishingInput);
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            finishComposingText(false);
        }

        if (brailleView != null) {
            brailleView.close();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (brailleParser != null) {
            brailleParser.destroy();
            brailleParser = null;
        }
    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        // The view dictates whether we are using the full screen.
        // If the keyboard is being used it will always take up the whole
        // screen.
        // If the keyboard is in the shrink state it will not use the full
        // screen.
        return brailleView != null ? !brailleView.getShrinkKeyboard() : false;
    }

    private void brailleParserReady(int status) {
        if (status == BrailleParser.STATUS_OK) {
            if (brailleView != null) {
                brailleView.setLocale(getLocale());
            }
        }
    }

    @Override
    public ExtractedText getAllText() {
        InputConnection ic = getCurrentInputConnection();
        return ic.getExtractedText(new ExtractedTextRequest(), 0);
    }

    @Override
    public CharSequence getTextBeforeCursor(int n) {
        InputConnection ic = getCurrentInputConnection();
        return ic.getTextBeforeCursor(n, 0);
    }

    @Override
    public CharSequence getTextAfterCursor(int n) {
        InputConnection ic = getCurrentInputConnection();
        return ic.getTextAfterCursor(n, 0);
    }

    @Override
    public CharSequence getSelectedText(int flags) {
        InputConnection ic = getCurrentInputConnection();
        return ic.getSelectedText(flags);
    }

    @Override
    public boolean setSelection() {
        int cursor = 0;
        if (!selectAll) {
            cursor = getCursor();
        }

        int[] positions = getSelectionBoundaries(cursor);
        return mark >= 0 && positions != null ? setSelection(positions[0],
                positions[1]) : false;
    }

    @Override
    public boolean setSelection(int cursor) {
        // Disable any selection first.
        if (selectAll) {
            toggleMark();
            selectAll = false;
        }
        finishComposingText();
        this.cursor = cursor;

        // Set the cursor to the new requested position.
        return setSelection(cursor, cursor);
    }

    @Override
    public boolean performContextMenuAction(int id) {
        InputConnection ic = getCurrentInputConnection();
        return ic.performContextMenuAction(id);
    }

    @Override
    public boolean deleteSurroundingText(int before, int after) {
        InputConnection ic = getCurrentInputConnection();
        selectAll = false;
        return ic.deleteSurroundingText(before, after);
    }

    @Override
    public boolean deleteSelection() {
        InputConnection ic = getCurrentInputConnection();
        int cursor = 0;
        if (!selectAll) {
            cursor = getCursor();
        }
        int[] positions = getSelectionBoundaries(cursor);
        setSelection(positions[1], positions[1]);
        return ic.deleteSurroundingText(positions[1] - positions[0], 0);
    }

    @Override
    public boolean toggleMark() {
        int cursor = getCursor();
        if (cursor == mark || selectAll) {
            mark = -1;
            selectAll = false;
        } else {
            mark = cursor;
        }
        return mark != -1 ? true : false;
    }

    @Override
    public int getCursor() {
        ExtractedText extractedText = getAllText();
        if (extractedText != null) {
            if (extractedText.startOffset + extractedText.selectionStart == extractedText.startOffset
                    + extractedText.selectionEnd) {
                cursor = extractedText.startOffset
                        + extractedText.selectionStart;
            }
        } else {
            cursor = -1;
        }
        return cursor;
    }

    @Override
    public boolean deselect() {
        if (!selectAll) {
            int cursor = getCursor();
            return setSelection(cursor, cursor);
        } else { // todo fix this eg. moving the cursor should remove select all
            return false;
        }
    }

    @Override
    public boolean setCursorToStartOfSelection() {
        cursor = Math.min(getCursor(), mark);
        return setSelection(cursor, cursor);
    }

    @Override
    public boolean selectAll() {
        getCursor();
        ExtractedText text = getAllText();
        if (text != null) {
            mark = text.text.length();
            setSelection(0, mark + 1);
            selectAll = true;
        }
        return selectAll;
    }

    @Override
    public boolean isSelectAll() {
        return selectAll;
    }

    @Override
    public Locale getLocale() {
        if (brailleParser != null) {
            TableInfo table = brailleParser.getTable(this);
            return table != null ? table.getLocale() : null;
        }
        return null;
    }

    @Override
    public int getDots() {
        if (brailleParser != null) {
            return brailleParser.getBrailleType(this).dots;
        }
        return -1;
    }

    @Override
    public String handleTypedCharacter(byte dots) {
        if (brailleParser != null) {
            String oldText = composingText.toString();
            setCells(dots);
            String text = brailleParser.backTranslate(this,
                    cells.toArray(new Byte[cells.size()]));
            if (text != null) {
                text = compose(text.subSequence(0, text.length()));
            } else { // unable to translate this byte string
                cells.remove(cells.size() - 1);
                return null;
            }

            // Return the update to the input field to be read to the user.
            return text != null ? stringDifference(oldText, text) : null;
        }
        return null;
    }

    @Override
    public int switchBrailleType() {
        if (brailleParser != null) {
            return brailleParser.switchBrailleType(this).dots;
        }
        return -1;
    }

    @Override
    public String switchTable() {
        if (brailleParser != null) {
            return brailleParser.switchTable(this);
        }
        return null;
    }

    @Override
    public boolean isPasswordField() {
        int inputType = getCurrentInputEditorInfo().inputType;
        return (inputType & InputType.TYPE_TEXT_VARIATION_PASSWORD) != 0;
    }

    private String compose(CharSequence text) {
        if (composingText.length() == 0) {
            updateShiftState(); // auto-caps
        }

        InputConnection ic = getCurrentInputConnection();
        if (selectAll) {
            toggleMark();
            selectAll = false;
        }

        // Braille is context specific and the text previously can change as the
        // user adds more Braille patterns.
        // First make sure the new text gets capitalised in the appropriate way
        // according to auto-capitalisation rules.
        text = capitalise(text);

        if (predictionOn) {
            // we can use composing text capabilities of android to make life
            // easy and efficient here.
            composingText.setLength(0);
            composingText.append(text);
            ic.setComposingText(composingText.toString(),
                    composingText.length());
        } else if (text.length() > 0) {
            // We have something to write to the field.
            // The IME could do strange things with our input here.
            // First clear our last text translation from n-1 Braille cells.
            ic.deleteSurroundingText(composingText.length(), 0);
            composingText.setLength(0);
            composingText.append(text);

            // Now write the text corresponding to n Braille cells.
            // We must write individual characters so that the input field
            // doesn't misbehave.
            // This is the case for some fields that do validation like banking
            // apps for security and auto-completing fields.
            for (int i = 0; i < text.length(); i++) {
                ic.commitText(text.subSequence(i, i + 1), 1);
            }
        }

        // return the new text we wrote if any.
        return text.toString();
    }

    // Capitalise the text if auto-caps is enabled and the IME told us to
    // capitalise this first character.
    private CharSequence capitalise(CharSequence text) {
        if (Options
                .getBooleanPreference(
                        this,
                        R.string.pref_auto_caps_key,
                        Boolean.parseBoolean(getString(R.string.pref_auto_caps_default)))) {
            if (caps != 0 && text != null) {
                if (text.length() > 0) {
                    text = String
                            .valueOf(Character.toUpperCase(text.charAt(0)))
                            + text.subSequence(1, text.length());
                }
            }
        }
        return text;
    }

    @Override
    public void onKey(int keyCode) {
        InputConnection ic = getCurrentInputConnection();
        // disable selection
        if (selectAll) {
            toggleMark();
            selectAll = false;
        }
        finishComposingText();
        switch (keyCode) {
        case Keyboard.KEYCODE_DELETE:
            ic.deleteSurroundingText(1, 0);
            break;
        case Keyboard.KEYCODE_DONE:
        case '\n':
            keyDownUp(ic, KeyEvent.KEYCODE_ENTER);
            break;
        default:
            if (keyCode >= '0' && keyCode <= '9') {
                keyDownUp(ic, keyCode - '0' + KeyEvent.KEYCODE_0);
            } else {
                ic.commitText(String.valueOf((char) keyCode), 1);
            }
            break;
        }
    }

    /**
     * Helper to send a key down / key up pair to the current editor.
     */
    private void keyDownUp(InputConnection ic, int keyEventCode) {
        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
    }

    private boolean setSelection(int start, int end) {
        InputConnection ic = getCurrentInputConnection();
        finishComposingText();
        return ic.setSelection(start, end);
    }

    private int[] getSelectionBoundaries(int cursor) {
        int[] array = null;
        ExtractedText text = getAllText();
        if (text != null) {
            mark = mark > text.text.length() ? text.text.length() : mark;
            array = new int[2];
            array[0] = Math.min(cursor, mark);
            array[1] = Math.max(cursor, mark);
            array[1] = array[1] < text.text.length() ? array[1] + 1 : array[1];
        }
        return array;
    }

    @Override
    public void finishComposingText() {
        finishComposingText(true);
    }

    private void finishComposingText(boolean commit) {
        InputConnection ic = getCurrentInputConnection();
        if (composingText.length() > 0) {
            if (predictionOn && commit) {
                ic.commitText(composingText, 1);
            }
            composingText.setLength(0);
        }
        cells.clear();
    }

    private void setCells(byte dots) {
        if (cells.size() == 0) {
            cells.add((byte) 0);
        }
        cells.add(dots);
    }

    /**
     * Return the difference between to strings so that the user knows what
     * change occurred to the input. If str2 is completely unique to str1 then
     * return the entire string as the input has totally changed. Otherwise
     * return from the point of difference to end of str2 which represents the
     * new text that the user should know about.
     * 
     * @param str1
     *            The old text.
     * @param str2
     *            The new text.
     */
    private static String stringDifference(String str1, String str2) {
        int i = -1;
        while (++i < Math.min(str1.length(), str2.length())
                && Character.toLowerCase(str1.charAt(i)) == Character
                        .toLowerCase(str2.charAt(i))) {
        }
        return i >= str2.length() ? str2 : str2.substring(i, str2.length());
    }

    private void updateShiftState() {
        caps = 0;
        EditorInfo editorInfo = getCurrentInputEditorInfo();
        if (editorInfo != null && editorInfo.inputType != InputType.TYPE_NULL) {
            caps = getCurrentInputConnection().getCursorCapsMode(
                    editorInfo.inputType);
        }
    }

    @Override
    public void commitText(String text, int newCursorPosition) {
        finishComposingText();
        InputConnection ic = getCurrentInputConnection();
        if (selectAll) {
            toggleMark();
            selectAll = false;
        }
        updateShiftState();
        text = capitalise(text.subSequence(0, text.length())).toString();
        ic.commitText(text, newCursorPosition);
    }
}
