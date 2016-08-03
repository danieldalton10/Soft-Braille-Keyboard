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

import java.util.Locale;

import android.Manifest;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.inputmethodservice.Keyboard;
import android.support.v4.content.ContextCompat;
import android.view.inputmethod.InputMethodManager;

import com.dalton.braillekeyboard.EditingUtilities.Word;
import com.dalton.braillekeyboard.Options.KeyboardEcho;
import com.dalton.braillekeyboard.Options.KeyboardFeedback;
import com.dalton.braillekeyboard.Pad.Swipe;
import com.dalton.braillekeyboard.SpellChecker.SpellingSuggestionsReadyListener;
import com.dalton.braillekeyboard.SpellChecker.Suggestion;

/**
 * ActionHandler handles actions from a View or other interface and performs the
 * appropriate logic before returning the results back to the calling View in
 * the form of a callback. This performs higher level logic than the
 * InputService itself and communicates directly with the InputService to
 * collaborate and solve the requests from the View.
 * 
 * A View should instantiate this class once upon initialisation and call it's
 * handleSwipe or handleCharacter methods to perform actions. Before such
 * activity the view shall set the IME listener by calling
 * setKeyboardListener(KeyboardListener listener) and also set the callback by
 * calling setCallback(OnActionListener callback). You should always call the
 * shutdown() method when you are done.
 * 
 * The callback is how the results are sent back to the View. The View should
 * implement the respective callbacks and implement them appropriate to their
 * View and user interface. See ActionHandler.OnActionListener for details.
 */
public class ActionHandler {
    // The maximum time between two identical swipe patterns which constitutes a
    // double swipe.
    private static final long DOUBLE_TOUCH_THRESHOLD = 1300;

    /**
     * Listener for handling the results of requests to the input methods. You
     * should implement these callbacks in your View and display the results to
     * the user.
     */
    public interface OnActionListener {
        /**
         * Deliver a string of text to the view as output of a certain action
         * that was performed. This might be some sort of message, a key name to
         * echo or some other text. Your UI should communicate this to the user
         * somehow in the form of audible or visual representation whatever is
         * appropriate for the use case.
         * 
         * @param format
         *            Format string to be used to display the message.
         * @param text
         *            Any text of the message.
         * @param isPasswordField
         *            True if it should be displayed with the same rules of
         *            showing passwords.
         */
        void onText(String format, String text, boolean isPasswordField);

        void onText(String format, String text, boolean isPasswordField,
                int mode);

        /**
         * Called when a notification should be delivered to the user.
         * 
         * @param vibrate
         *            true if the device should be vibrated for this
         *            notification.
         * @param playSound
         *            true if a sound should be played for this notification.
         */
        void onNotify(boolean vibrate, boolean playSound);

        /**
         * Called when dots 7 and 8 should be set in the View.
         * 
         * @param dot7
         *            Whether dot7 is pressed.
         * @param dot8
         *            Whether dot8 is pressed.
         */
        void onSetDots(boolean dot7, boolean dot8);

        /**
         * Called when the View should update it's Locale. This is generally
         * called when the Braille table is changed because there is the
         * possibility for language change.
         */
        void onSetLocale(Locale locale);

        /**
         * Called when the View should shrink itself.
         */
        void onShrink();

        /**
         * Called when the View should update the state of it's privacy mode.
         */
        void onPrivacy();

        void onShutup();
    }

    /**
     * Representation of varying textual granularities from the smallest level
     * character up until the entire text.
     */
    private enum Granularity {
        CHARACTER, WORD, LINE, ALL;
    }

    /**
     * Representation of the currently available edit actions that can be
     * performed on a selection of text.
     */
    private enum EditAction {
        SELECT_ALL(R.string.select_all), COPY(android.R.string.copy), CUT(
                android.R.string.cut), PASTE(android.R.string.paste), SPEAK(
                R.string.speak_selection), DELETE(R.string.delete_selection);

        // The Android resource for the text UI string for this action.
        public final int resource;

        EditAction(int resource) {
            this.resource = resource;
        }

        // Find the position of this action in the enum values() array.
        private int getIndexOfThis() {
            EditAction[] values = EditAction.values();
            int i;
            for (i = 0; i < values.length; i++) {
                if (this == values[i]) {
                    break;
                }
            }
            return i;
        }

        /**
         * Move in order to the next EditAction in the enum list. If we are at
         * the end of the list it will wrap.
         * 
         * @return The new EditAction instance.
         */
        public EditAction next(ClipboardManager clipboard) {
            int i = getIndexOfThis();

            if (++i == values().length) {
                i = 0; // point at first item in list.
            }

            if (values()[i] == PASTE) {
                boolean canPaste;
                // Check that there is text on the clipboard so it makes sense
                // showing paste.
                if (!(clipboard.hasPrimaryClip())) {
                    canPaste = false;
                } else {
                    // This enables the paste menu item, since the clipboard
                    // contains plain text.
                    canPaste = true;
                }
                if (!canPaste) {
                    if (++i == values().length) {
                        i = 0; // Point at start if we exceed the end
                    }
                }
            }

            return values()[i];
        }
    }

    private final ClipboardManager clipboard;
    private final InputMethodManager inputManager;
    private final SpellChecker spellChecker;
    private final VoiceInput voiceInput = new VoiceInput();

    private EditAction editAction = EditAction.COPY;
    private long lastTouchTime = 0; // Time screen was last touched.
    private Swipe lastSwipe = Swipe.NONE; // Type of last gesture.
    private KeyboardListener listener;
    private OnActionListener callback;
    private int directionThroughSuggestionList;
    private SpellChecker.Direction spellingDirection;
    private Suggestion spellingSuggestion;

    /**
     * Create a new ActionHandler for the given context.
     * 
     * @param context
     *            The application context.
     */
    public ActionHandler(Context context) {
        inputManager = (InputMethodManager) context
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        clipboard = (ClipboardManager) context
                .getSystemService(Context.CLIPBOARD_SERVICE);
        spellChecker = new SpellChecker(context);
    }

    /**
     * Set the callback to deliver results to the View interracting with this
     * instance.
     * 
     * @param callback
     *            The View's implementation of ActionHandler.OnActionListener
     *            listening for updates.
     */
    public void setCallback(OnActionListener callback) {
        this.callback = callback;
    }

    /**
     * Set the listener so that the ActionHandler can communicate with the
     * underlying IME.
     * 
     * @param listener
     *            The KeyboardListener for the current input session.
     */
    public void setKeyboardListener(KeyboardListener listener) {
        this.listener = listener;
    }

    /**
     * Releases system resources. This should be called just before the
     * interracting View gets destroyed.
     */
    public void shutdown() {
        voiceInput.destroy();
        spellChecker.destroy();
    }

    /**
     * Handle swipe actions delivered from the interracting View.
     * 
     * Each View should deliver a swipe action as defined by the generic
     * Pad.Swipe type. This method will perform the appropriate action for the
     * received Swipe gesture. If need be the appropriate callbacks will be
     * invoked.
     * 
     * @param context
     *            The application context.
     * @param value
     *            The Swipe value from the View.
     * @return true if the Swipe was handled otherwise false.
     */
    public boolean handleSwipe(Context context, Swipe value) {
        // Disable all swipes while voice input is in progress.
        if (voiceInput.isListening()) {
            return true;
        }

        value = normaliseSwipe(value);
        String message = null;
        boolean notify = true;
        boolean setDots = false;
        boolean considerPassword = false;
        // states for dots 7 and 8
        boolean dots[] = { false, false };
        boolean fastDoubleSwipe = fastDoubleSwipe(value, DOUBLE_TOUCH_THRESHOLD);

        switch (value) {
        case ONE_LEFT:
            moveLeft(context, Granularity.CHARACTER);
            break;
        case ONE_RIGHT:
            moveRight(context, Granularity.CHARACTER);
            break;
        case ONE_DOWN:
            KeyboardFeedback feedback = KeyboardFeedback.valueOf(Integer
                    .parseInt(Options.getStringPreference(context,
                            R.string.pref_keyboard_feedback_key,
                            KeyboardFeedback.ALL.getValue())));
            feedback = KeyboardFeedback.next(feedback);
            Options.writeStringPreference(context,
                    R.string.pref_keyboard_feedback_key, feedback.getValue());
            message = context.getString(feedback.resource);
            break;
        case ONE_UP:
            message = getInput(Granularity.CHARACTER);
            considerPassword = true;
            break;
        case TWO_LEFT:
            moveLeft(context, Granularity.WORD);
            break;
        case TWO_RIGHT:
            moveRight(context, Granularity.WORD);
            break;
        case TWO_UP:
            message = getInput(Granularity.WORD);
            considerPassword = true;
            break;
        case TWO_DOWN:
            KeyboardEcho echo = KeyboardEcho.valueOf(Integer.parseInt(Options
                    .getStringPreference(context,
                            R.string.pref_echo_feedback_key,
                            KeyboardEcho.CHARACTER.getValue())));
            echo = KeyboardEcho.next(echo);
            Options.writeStringPreference(context,
                    R.string.pref_echo_feedback_key, echo.getValue());
            message = context.getString(echo.resource);
            break;
        case THREE_LEFT:
            moveLeft(context, Granularity.LINE);
            break;
        case THREE_RIGHT:
            moveRight(context, Granularity.LINE);
            break;
        case THREE_UP:
            message = getInput(Granularity.LINE);
            considerPassword = true;
            break;
        case THREE_DOWN:
            if (listener.getDots() == 8) {
                setDots = true;
                dots[0] = true;
            } else {
                message = context.getString(R.string.unknown_character);
            }
            break;
        case FOUR_LEFT:
            backspace(context, Granularity.CHARACTER, fastDoubleSwipe);
            break;
        case FOUR_RIGHT:
            if (fastDoubleSwipe) {
                if (handleDoubleSpace(context)) {
                    break;
                }
            }
            typeCharacter(context, (int) ' ', " ");
            break;
        case FOUR_DOWN: // newline / enter
            typeCharacter(context, Keyboard.KEYCODE_DONE,
                    context.getString(R.string.newline));
            break;
        case FOUR_UP:
            Options.switchBooleanPreference(context, R.string.pref_privacy_key,
                    Boolean.parseBoolean(context
                            .getString(R.string.pref_privacy_default)));
            callback.onPrivacy();
            message = Options.getBooleanPreference(context,
                    R.string.pref_privacy_key, Boolean.parseBoolean(context
                            .getString(R.string.pref_privacy_default))) ? context
                    .getString(R.string.privacy_enabled) : context
                    .getString(R.string.privacy_disabled);
            break;
        case FIVE_LEFT:
            backspace(context, Granularity.WORD, fastDoubleSwipe);
            break;
        case FIVE_DOWN:
            if (fastDoubleSwipe) {
                message = context.getString(R.string.show_input_switcher);
                inputManager.showInputMethodPicker();
            } else {
                message = context.getString(R.string.swipe_confirm_input);
            }
            break;
        case FIVE_UP:
            if (fastDoubleSwipe) {
                callback.onSetLocale(Locale.getDefault());
                message = context.getString(R.string.show_settings);
                Intent intent = new Intent(context, PreferenceIME.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } else {
                message = context.getString(R.string.swipe_confirm_settings);
            }
            break;
        case SIX_LEFT:
            backspace(context, Granularity.LINE, fastDoubleSwipe);
            break;
        case SIX_RIGHT:
            nextAction(context);
            break;
        case SIX_UP:
            selectAction(context);
            break;
        case SIX_DOWN:
            if (listener.getDots() == 8) {
                setDots = true;
                dots[1] = true;
            } else {
                message = context.getString(R.string.unknown_character);
            }
            break;
        case HOLD_SIX_LEFT:
            moveLeft(context, Granularity.ALL);
            break;
        case HOLD_SIX_RIGHT:
            moveRight(context, Granularity.ALL);
            break;
        case HOLD_SIX_DOWN:
            boolean echoPassword = Options.switchBooleanPreference(context,
                    R.string.pref_echo_passwords_key, false);
            message = echoPassword ? context
                    .getString(R.string.speak_passwords) : context
                    .getString(R.string.no_password_echo);
            break;
        case HOLD_SIX_UP:
            message = getInput(Granularity.ALL);
            considerPassword = true;
            break;
        case HOLD_THREE_LEFT:
            backspace(context, Granularity.ALL, fastDoubleSwipe);
            break;
        case HOLD_THREE_RIGHT:
            int brailleType = listener.switchBrailleType();
            message = brailleType == 8 ? context
                    .getString(R.string.grade_computer) : context
                    .getString(R.string.grade_literary);
            callback.onSetLocale(listener.getLocale());
            break;
        case HOLD_THREE_DOWN:
            message = listener.switchTable();
            message = message == null ? context
                    .getString(R.string.no_braille_table) : message;
            callback.onSetLocale(listener.getLocale());
            break;
        case HOLD_THREE_UP:
            doVoiceInput(context, fastDoubleSwipe);
            break;
        case HOLD_ONE_RIGHT:
            message = context
                    .getString(listener.toggleMark() ? R.string.set_mark
                            : R.string.unset_mark);
            break;
        case HOLD_ONE_LEFT:
            message = context.getString(R.string.keyboard_shrink);
            callback.onShrink();
            break;
        case HOLD_ONE_DOWN:
            CharSequence text = listener.getAllText().text;
            if (text != null) {
                message = String.format(context.getString(R.string.word_count),
                        EditingUtilities.lineCount(text),
                        EditingUtilities.wordCount(text),
                        EditingUtilities.characterCount(text));
            }
            break;
        case HOLD_ONE_UP:
            Options.switchBooleanPreference(context,
                    R.string.pref_auto_caps_key, Boolean.parseBoolean(context
                            .getString(R.string.pref_auto_caps_default)));
            message = Options.getBooleanPreference(context,
                    R.string.pref_auto_caps_key, Boolean.parseBoolean(context
                            .getString(R.string.pref_auto_caps_default))) ? context
                    .getString(R.string.auto_caps_enabled) : context
                    .getString(R.string.auto_caps_disabled);
            break;
        case HOLD_FOUR_LEFT:
            doSpellCheck(context, SpellChecker.Direction.LEFT, 0,
                    listener.getCursor());
            break;
        case HOLD_FOUR_RIGHT:
            doSpellCheck(context, SpellChecker.Direction.RIGHT, 0,
                    listener.getCursor());
            break;
        case HOLD_FOUR_DOWN:
            nextSpellCheckSuggestion(context);
            break;
        case HOLD_FOUR_UP:
            previousSpellCheckSuggestion(context);
            break;
        case NONE:
            return false;
        default:
            notify = false;
        }

        // Invoke the notification callback
        callback.onNotify(notify, notify);
        if (message != null) {
            // Only invoke onText callback if there is a message to send i.e. it
            // wasn't already handled.
            callback.onText("%s", message,
                    considerPassword ? listener.isPasswordField() : false);
        }

        if (setDots) { // Dots 7 or 8 were triggered
            callback.onSetDots(dots[0], dots[1]);
        }

        lastSwipe = value; // update the last swipe
        return true;
    }

    /**
     * Handle typing a Braille character into the underlying IME. The character
     * is delivered as a byte value representing the dot pattern and will be
     * converted and written as a standard textual character by the IME.
     * 
     * @param context
     *            The application context.
     * @param value
     *            The byte value which represents the dot pattern to type. This
     *            is a bitstring that represents a Braille pattern where dot 8
     *            is represented by the MSB and dot 1 by the LSB. A value of 0
     *            means no dots are present and a value of 0b11111111 means all
     *            8 dots are pressed.
     */
    public void handleCharacter(Context context, byte value) {
        // Can't type while voice input is in progress.
        if (voiceInput.isListening()) {
            return;
        }

        lastSwipe = Swipe.NONE;
        String result;
        if ((result = listener.handleTypedCharacter(value)) == null) {
            // IME couldn't handle the dot pattern propergate the error to the
            // callback.
            callback.onText("%s",
                    context.getString(R.string.unknown_character), false);
        } else {
            callback.onNotify(true, true);

            // Decide what to deliver to the callback such as a key echo or
            // autocompletion string.
            String character = echoCharacter(context, result);
            result = character == null ? "" : character;
            if (!(result = result.trim()).equals("")) {
                callback.onText("%s", result.toString(),
                        listener.isPasswordField());
            }
        }

        // dots 7 and 8 should now be unset
        callback.onSetDots(false, false);
    }

    // Handle prompting user to confirm an action with a double swipe.
    private boolean isConfirmed(Context context, boolean fastDoubleTouch) {
        if (!fastDoubleTouch) {
            callback.onText("%s", context.getString(R.string.swipe_confirm),
                    false);
            return false;
        }
        return true;
    }

    // Perform the currently selected Edit Action on the region.
    private void selectAction(Context context) {
        switch (editAction) {
        case COPY:
            performContextMenuAction(context, true, android.R.id.copy,
                    R.string.copied, R.string.copy_error);
            listener.deselect();
            break;
        case CUT:
            if (performContextMenuAction(context, true, android.R.id.cut,
                    android.R.string.cut, R.string.cut_error)) {
                listener.setCursorToStartOfSelection();
            }
            break;
        case PASTE:
            if (performContextMenuAction(context, false, android.R.id.paste,
                    R.string.pasted, R.string.paste_error)) {
                int cursor = listener.getCursor();
                listener.setSelection(cursor);
            }
            break;
        case SPEAK:
        case DELETE:
            performAction(context, editAction);
            break;
        case SELECT_ALL:
            listener.selectAll();
            callback.onText("%s", context.getString(R.string.selected_all),
                    false);
            break;
        default:
        }
    }

    // Handle these actions by using the inbuilt android context menu action
    private boolean performContextMenuAction(Context context,
            boolean requiresSelection, int code, int successString,
            int errorString) {
        if (!requiresSelection || listener.setSelection()) {
            if (listener.performContextMenuAction(code)) {
                callback.onText("%s", context.getString(successString), false);
                return true;
            } else {
                callback.onText("%s", context.getString(errorString), false);
                return false;
            }
        } else {
            callback.onText("%s", context.getString(R.string.mark_not_set),
                    false);
            return false;
        }
    }

    // These actions aren't implemented by Android so do them ourselves.
    private boolean performAction(Context context, EditAction action) {
        if (listener.setSelection()) {
            CharSequence text = listener.getSelectedText(0);
            listener.deselect();
            switch (action) {
            case SPEAK:
                callback.onText(
                        "%s",
                        text == null ? context.getString(R.string.blank) : text
                                .toString(), listener.isPasswordField());
                break;
            case DELETE:
                if (listener.deleteSelection() && text != null) {
                    callback.onText(context.getString(R.string.deleted),
                            text.toString(), listener.isPasswordField());
                } else {
                    callback.onText("%s",
                            context.getString(R.string.nothing_to_delete),
                            false);
                }
                break;
            default:
                return false;
            }
            return true;
        }
        callback.onText("%s", context.getString(R.string.mark_not_set), false);
        return false;
    }

    private void nextAction(Context context) {
        editAction = editAction.next(clipboard);
        callback.onText("%s", context.getString(editAction.resource), false);
    }

    // Move the cursor left by the appropriate granularity and speak the result.
    private void moveLeft(Context context, Granularity granularity) {
        EditingUtilities.Word word = null;
        listener.finishComposingText();
        if (listener.isSelectAll()) {
            granularity = Granularity.ALL;
            listener.setSelection(0);
        }
        switch (granularity) {
        case CHARACTER:
            word = EditingUtilities.moveToPreviousCharacter(listener);
            break;
        case WORD:
            word = EditingUtilities.moveToPreviousWord(listener);
            break;
        case LINE:
            word = EditingUtilities.moveToPreviousLine(listener);
            break;
        case ALL:
            word = EditingUtilities.moveToHome(listener);
            break;
        default:
        }

        if (word != null) {
            callback.onText("%s",
                    !word.moveLeft ? context.getString(R.string.start_of_text)
                            : word.word,
                    word.moveLeft && listener.isPasswordField());
        }
    }

    // Move the cursor right by the appropriate granularity and speak the
    // result.
    private void moveRight(Context context, Granularity granularity) {
        EditingUtilities.Word word = null;
        listener.finishComposingText();
        if (listener.isSelectAll()) {
            granularity = Granularity.ALL;
            listener.setSelection(0);
        }
        switch (granularity) {
        case CHARACTER:
            word = EditingUtilities.moveToNextCharacter(listener);
            break;
        case WORD:
            word = EditingUtilities.moveToNextWord(listener);
            break;
        case LINE:
            word = EditingUtilities.moveToNextLine(listener);
            break;
        case ALL:
            word = EditingUtilities.moveToEnd(listener);
            break;
        default:
        }

        if (word != null) {
            callback.onText("%s",
                    !word.moveRight ? context.getString(R.string.end_of_text)
                            : word.word,
                    word.moveRight && listener.isPasswordField());
        }
    }

    // Perform backspace by the specified Granularity.
    private boolean backspace(Context context, Granularity granularity,
            boolean fastDoubleTouch) {
        EditingUtilities.Word word = null;
        boolean canDelete = true;
        switch (granularity) {
        case CHARACTER:
            listener.finishComposingText();
            word = EditingUtilities.moveToPreviousCharacter(listener);
            break;
        case WORD:
            listener.finishComposingText();
            Word space = EditingUtilities.skipSepBackwards(listener,
                    EditingUtilities.WORD_SEPARATORS);
            word = EditingUtilities.getWord(listener);
            if (word != null) {
                if (space != null) {
                    word.charsBefore += space.charsBefore;
                }
                if (word.word.length() > word.charsBefore) {
                    word.word = word.word.substring(0, word.charsBefore);
                }
                EditingUtilities.moveToPreviousWord(listener);
            }
            break;
        case LINE:
            canDelete = isConfirmed(context, fastDoubleTouch);
            if (canDelete) {
                listener.finishComposingText();
                int cursor = listener.getCursor();
                word = EditingUtilities.getLine(listener);
                if (word != null && (cursor > 0 || word.charsAfter > 0)) {
                    int moveChars = (cursor - word.charsBefore) > 0 ? 1 : 0;
                    listener.setSelection(cursor - moveChars - word.charsBefore);
                    word.charsBefore = word.charsBefore + moveChars
                            + word.charsAfter;
                }
            }
            break;
        case ALL:
            canDelete = isConfirmed(context, fastDoubleTouch);
            if (canDelete) {
                listener.finishComposingText();
                word = EditingUtilities.moveToHome(listener);
                word.word = EditingUtilities.getAllText(listener);
                word.charsBefore = word.word.length();
            }
            break;
        default:
        }
        return performDelete(context, word, canDelete);
    }

    // Given the text to delete and a canDelete flag do the actual deletion.
    private boolean performDelete(Context context, EditingUtilities.Word word,
            boolean canDelete) {
        if (canDelete && word != null) {
            if (word.charsBefore > 0 || word.charsAfter > 0) {
                // cursor moved back word.charsBefore positions, so delete that
                // many chars ahead of the cursor.
                if (listener.deleteSurroundingText(0, word.charsBefore)) {
                    if (word.word != null) {
                        callback.onText(context.getString(R.string.deleted),
                                word.word, listener.isPasswordField());
                    }
                    return true;
                } else {
                    return false;
                }
            } else {
                callback.onText("%s",
                        context.getString(R.string.nothing_to_delete), false);
                return true;
            }
        }
        return false;
    }

    // Insert a certain character like a ' ' or '\n'
    private void typeCharacter(Context context, int code, String charName) {
        listener.finishComposingText();
        Word word = EditingUtilities.getWord(listener);
        String message = word == null ? null : word.word.substring(0,
                word.charsBefore);
        listener.onKey(code);

        if ((message = echoWord(context, message)) == null) {
            message = echoCharacter(context, charName);
        }
        callback.onText("%s", message, listener.isPasswordField());

        if (Options.getBooleanPreference(context,
                R.string.pref_echo_misspellings_key,
                Boolean.parseBoolean(context
                        .getString(R.string.pref_echo_misspellings_default)))
                && spellChecker.isSpellCheckAvailable()) {
            doSpellCheck(context, SpellChecker.Direction.UNDER_CURSOR, 0,
                    listener.getCursor() - 2);
        }
    }

    // Special logic for double space to insert a period followed by a space.
    private boolean handleDoubleSpace(Context context) {
        if (Options.getBooleanPreference(context,
                R.string.pref_double_space_period_key,
                Boolean.parseBoolean(context
                        .getString(R.string.pref_double_space_period_default)))) {
            CharSequence text = listener.getTextBeforeCursor(2);
            if (text != null) {
                if (text.length() == 2
                        && Character.isWhitespace(text.charAt(1))
                        && Character.isLetterOrDigit(text.charAt(0))) {
                    listener.deleteSurroundingText(1, 0);
                    listener.onKey('.');
                    typeCharacter(context, ' ', " ");
                    return true;
                }
            }
        }
        return false;
    }

    // Return true if the same gesture was typed quickly in succession.
    private boolean fastDoubleSwipe(Swipe swipe, long threshold) {
        if ((lastTouchTime + threshold) > System.currentTimeMillis()
                && lastSwipe == swipe) {
            lastTouchTime = 0;
            return true;
        } else {
            lastSwipe = swipe;
            lastTouchTime = System.currentTimeMillis();
            return false;
        }
    }

    // Get a particular granularity of text from the IME.
    private String getInput(Granularity granularity) {
        String text = "";
        Word word;
        switch (granularity) {
        case CHARACTER:
            text = EditingUtilities.getCharacter(listener);
            break;
        case WORD:
            word = EditingUtilities.getWord(listener);
            text = word == null ? null : word.word;
            break;
        case LINE:
            word = EditingUtilities.getLine(listener);
            text = word == null ? null : word.word;
            break;
        case ALL:
            text = EditingUtilities.getAllText(listener);
            break;
        default:
        }
        return text;
    }

    // Handle voice input.
    public boolean doVoiceInput(final Context context, boolean fastDoubleSwipe) {
        // Check for the "dangerous permission" for Android 6 and higher.
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // Fast double swipe to show the permission dialog so we don't
            // surprise the user having no screen reader or talking keyboard in
            // focus.
            if (!fastDoubleSwipe) {
                callback.onText("%s",
                        context.getString(R.string.voice_input_enable), false);
            } else { // Show permission dialog
                Intent intent = new Intent(context, IntentActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setAction(context
                        .getString(R.string.action_record_audio_permission));
                intent.putExtra(
                        context.getString(R.string.require_record_audio_now),
                        true);
                context.startActivity(intent);
            }
            return false; // We didn't do voice input.
        }

        VoiceInput.TextReadyListener textReadyListener = new VoiceInput.TextReadyListener() {

            @Override
            public void onTextReady(String text) {
                // Write the text and send it back to the callback.
                if (text != null && text.length() > 0) {
                    CharSequence before = listener.getTextBeforeCursor(1);
                    if (before != null && before.length() > 0
                            && !Character.isWhitespace(before.charAt(0))) {
                        listener.onKey(' ');
                    }
                    listener.commitText(text, 1);
                    callback.onText("%s", text, listener.isPasswordField());
                }
            }

            @Override
            public void onError(int error) {
                callback.onText("%s", String.format(
                        context.getString(R.string.voice_input_error), error),
                        false);
            }
        };

        if (voiceInput.start(context, textReadyListener)) {
            callback.onShutup();
        } else {
            callback.onText("%s",
                    context.getString(R.string.voice_input_is_not_available),
                    false);
            return false;
        }
        return true;
    }

    // Rules for echoing character. Return the character if it should be echoed
    // else null.
    private static String echoCharacter(Context context, String character) {
        if ((Integer.parseInt(Options.getStringPreference(context,
                R.string.pref_echo_feedback_key,
                KeyboardEcho.CHARACTER.getValue())) & KeyboardEcho.CHARACTER.value) != 0) {
            return character;
        }
        return null;
    }

    // Rules for echoing word. Return the word if it should be echoed
    // else null.
    private static String echoWord(Context context, String word) {
        if ((Integer.parseInt(Options.getStringPreference(context,
                R.string.pref_echo_feedback_key,
                KeyboardEcho.CHARACTER.getValue())) & KeyboardEcho.WORD.value) != 0) {
            return word;
        }
        return null;
    }

    // Some swipe actions should resolve to the same thing eg. dots 4 and 5
    // swipe right.
    private static Swipe normaliseSwipe(Swipe swipe) {
        if (swipe == Swipe.FIVE_RIGHT) {
            return Swipe.FOUR_RIGHT;
        }
        return swipe;
    }

    private void doSpellCheck(final Context context,
            SpellChecker.Direction direction, int move, int cursor) {
        SpellingSuggestionsReadyListener spellingListener = new SpellingSuggestionsReadyListener() {

            @Override
            public void suggestionsReady(Suggestion result) {
                spellingSuggestion = result;
                if (result != null
                        || spellingDirection == SpellChecker.Direction.UNDER_CURSOR) {
                    handleSpellingSuggestion(context);
                } else {
                    callback.onText("%s",
                            context.getString(R.string.no_more_misspellings),
                            false);
                }
            }
        };

        String text = getInput(Granularity.ALL);
        spellingDirection = direction;
        directionThroughSuggestionList = move;
        if (text != null && text.length() > 0) {
            if (!spellChecker.checkSpelling(spellingListener, text, cursor,
                    direction)) {
                callback.onText("%s",
                        context.getString(R.string.spellcheck_not_supported),
                        false);
            }
        } else {
            callback.onText("%s", context.getString(R.string.blank), false);
        }
    }

    private void handleSpellingSuggestion(Context context) {
        boolean password = false;
        String message = null;

        if (spellingSuggestion == null && directionThroughSuggestionList != 0) {
            message = context.getString(R.string.word_correct);
        } else if (spellingSuggestion != null
                && spellingDirection == SpellChecker.Direction.UNDER_CURSOR) {
            spellingDirection = null;
            if (directionThroughSuggestionList > 0) {
                nextSpellCheckSuggestion(context);
            } else if (directionThroughSuggestionList < 0) {
                previousSpellCheckSuggestion(context);
            } else {
                message = context.getString(R.string.word_misspelled);
            }
        } else if (spellingSuggestion != null) {
            password = true;
            message = spellingSuggestion.isMisspelledWord() ? String.format(
                    context.getString(R.string.word_correction_misspelled),
                    spellingSuggestion.getCurrent()) : spellingSuggestion
                    .getCurrent();
            listener.setSelection(spellingSuggestion.offset);
            listener.deleteSurroundingText(0, spellingSuggestion.getLength());
            listener.commitText(spellingSuggestion.getCurrent(), 1);
            listener.setSelection(spellingSuggestion.offset);
            spellingSuggestion.setLength();
        }

        if (message != null) {
            callback.onText("%s", message, listener.isPasswordField()
                    && password, Speech.QUEUE_ADD);
        }
    }

    private void nextSpellCheckSuggestion(Context context) {
        if (spellingSuggestion != null) {
            if (spellCheckerMatchesWord()) {
                spellingSuggestion.next();
                handleSpellingSuggestion(context);
                return;
            }
        }
        doSpellCheck(context, SpellChecker.Direction.UNDER_CURSOR, 1,
                listener.getCursor());
    }

    private void previousSpellCheckSuggestion(Context context) {
        if (spellingSuggestion != null) {
            if (spellCheckerMatchesWord()) {
                spellingSuggestion.prev();
                handleSpellingSuggestion(context);
                return;
            }
        }
        doSpellCheck(context, SpellChecker.Direction.UNDER_CURSOR, -1,
                listener.getCursor());
    }

    private boolean spellCheckerMatchesWord() {
        String text = getInput(Granularity.ALL);
        if (text != null && spellingSuggestion != null) {
            int offset = spellingSuggestion.offset;
            int length = spellingSuggestion.getLength();
            int cursor = listener.getCursor();
            if (text.length() > 0 && (offset + length) <= text.length()
                    && cursor >= offset && cursor < (offset + length)) {
                return text.substring(offset, offset + length).equals(
                        spellingSuggestion.getCurrent());
            }
        }
        return false;
    }
}
