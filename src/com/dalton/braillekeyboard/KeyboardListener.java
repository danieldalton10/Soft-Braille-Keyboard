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

import java.util.Locale;

import android.view.inputmethod.ExtractedText;

/**
 * Braile IME's should implement this interface. It describes all of the methods
 * required to facilitate communication between a View and an InputService to
 * facilitate Braille Input.
 */
public interface KeyboardListener {

    /**
     * Retrieves the locale for the currently active Braille table in the
     * BrailleParser translator client.
     * 
     * @return The locale of the currently active Braille table in the
     *         translator client. If there is a problem with the client null is
     *         returned.
     */
    Locale getLocale();

    /**
     * Returns the number of dots that the active table uses.
     * 
     * @return 6 for Literary tables or 8 for computer Braille tables.
     */
    int getDots();

    /**
     * Toggle the active BrailleType and return the new BrailleType in use in
     * terms of the number of dots (6 or 8).
     * 
     * @return The number of dots used by the newly active type.
     */
    int switchBrailleType();

    /**
     * Switch to the next "on the fly" Braille table and return a textual String
     * describing the table.
     * 
     * @return A String describing the table suitable to show to the user.
     */
    String switchTable();

    /**
     * This method is once for every Braille cell that is typed. The
     * implementation of this method should handle back translating the Braille
     * pattern to text, and writing it to the input method. It should be smarter
     * than this and implement logic to maximise Braille accuracy.
     * 
     * @param dots
     *            A bitstring representing the pressed dots for the given cell.
     *            For an 8 bit string the MSB represents dot 8 and the LSB
     *            represents dot 1. The dot is set if it is set to 1. So
     *            0b11111111 means all dots are set, 101 means dots 13 are set
     *            and 0 would mean no dots are set.
     * @return A string that describes the changes made to the input since
     *         Braille isn't a 1:1 mapping. This might be useful to communicate
     *         to the user if they can't monitor the input themselves.
     */
    String handleTypedCharacter(byte dots);

    /**
     * Determine if the active input session pertains to a password typed field.
     * 
     * @return true if it's a password field otherwise false.
     */
    boolean isPasswordField();

    /**
     * Deliver a single key press to the application.
     * 
     * @param primaryCode
     *            The keycode to be delivered see Keyboard.KeyCode_* for
     *            details.
     */
    void onKey(int primaryCode);

    /**
     * Get up to n characters before the cursor. If there are not n characters
     * before the cursor returns all text before the cursor.
     * 
     * @param n
     *            The number of characters before the cursor to retrieve.
     * @return The text before the cursor.
     */
    CharSequence getTextBeforeCursor(int n);

    /**
     * Get up to n characters after the cursor. If there are not n characters
     * after the cursor returns all text after the cursor.
     * 
     * @param n
     *            The number of characters after the cursor to retrieve.
     * @return The text after the cursor.
     */
    CharSequence getTextAfterCursor(int n);

    /**
     * Gets all text for the current input field.
     * 
     * @return An ExtractedText instance which holds the text in the field as
     *         well as other data like selection offsets.
     */
    ExtractedText getAllText();

    /**
     * Perform an Android context menu action.
     * 
     * @param id
     *            The id of the action to be performed.
     *            {@link android.R.id#selectAll},
     *            {@link android.R.id#startSelectingText},
     *            {@link android.R.id#stopSelectingText},
     *            {@link android.R.id#cut}, {@link android.R.id#copy},
     *            {@link android.R.id#paste}, {@link android.R.id#copyUrl}, or
     *            {@link android.R.id#switchInputMethod}
     * @return true if the action was completed otherwise false.
     */
    boolean performContextMenuAction(int id);

    /**
     * Delete text surrounding the cursor.
     * 
     * @param before
     *            How many characters to delete before the cursor.
     * @param after
     *            How many characters to delete after the cursor.
     * @return true if the delete was successful otherwise false.
     */
    boolean deleteSurroundingText(int before, int after);

    /**
     * The mark is set to indicate one of the endpoints of the selected region,
     * the cursor is the other endpoint. If the mark is at a different point in
     * the text to the cursor this method moves it to the location of the
     * cursor. If the cursor and the mark are at the same point then this method
     * will disable the active mark.
     * 
     * @return true if the mark is still active otherwise false.
     */
    boolean toggleMark();

    /**
     * Select the active region in the underlying IME.
     * 
     * @return true if the region could be selected or false if it couldn't be
     *         selected eg. no active region.
     */
    boolean setSelection();

    /**
     * Deselects any selected text and moves the cursor to a specific location
     * in the IME.
     * 
     * @param cursor
     *            The nw cursor position.
     * @return true on success or false if the input connection is no longer
     *         valid.
     */
    boolean setSelection(int cursor);

    /**
     * Return the text that is currently selected by the underlying IME. You
     * should first call setSelection() before calling this.
     * 
     * @param flags
     *            Supplies additional options controlling how the text is
     *            returned. May be either 0 or {@link #GET_TEXT_WITH_STYLES}.
     * @return The selected text.
     */
    CharSequence getSelectedText(int flags);

    /**
     * Delete the selected text.
     * 
     * @return true on success otherwise false.
     */
    boolean deleteSelection();

    /**
     * Checks if all text in the underlying IME is currently selected.
     * 
     * @return true if select all is enabled otherwise false.
     */
    boolean isSelectAll();

    /**
     * Gets the current position of the cursor for the current input session.
     * 
     * @return The position of the cursor in the text or -1 if it can't be
     *         determined.
     */
    int getCursor();

    /**
     * Deselect the active region.
     * 
     * @return true if the region was deselected.
     */
    boolean deselect();

    /**
     * Moves the cursor to the beginning of the selection either remaining the
     * same or moving it to the location of the mark.
     * 
     * @return true if there was a selection and the cursor could be moved.
     */
    boolean setCursorToStartOfSelection();

    /**
     * Selects all text in the underlying IME.
     * 
     * @return true if all text was selected.
     */
    boolean selectAll();

    /**
     * Re-evaluate whether the input method should be running in fullscreen
     * mode, and update its UI if this has changed since the last time it was
     * called.
     */
    void updateFullscreenMode();

    /**
     * Write a string of text straight into the underlying input field.
     * 
     * @param text
     *            The text to be written.
     * @param newCursorPosition
     *            The location of the cursor after writing in terms of
     *            characters relative to the text. So 1 means the cursor will be
     *            placed after the end of the text and -1 means the cursor will
     *            be placed before the text.
     */
    void commitText(String text, int newCursorPosition);

    void finishComposingText();
}
