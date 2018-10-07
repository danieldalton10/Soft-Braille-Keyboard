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

import android.view.inputmethod.ExtractedText;

/**
 * A series of handy utilities for editing and manipulating text through an IME.
 * 
 */
public class EditingUtilities {
    public static final int MAX_WORD_LENGTH = 30;
    public static final int MAX_LINE_LENGTH = 1000;
    public static final String WORD_SEPARATORS = " \n\t";
    public static final String LINE_SEPARATOR = "\n";

    public static Word getBlock(String before, String after, String sep) {
        int start = before.length();
        int end = -1;
        // now work outwards until we find a separator
        while (start > 0 && !matchesSeparator(before.charAt(start - 1), sep)) {
            --start;
        }
        while (++end < after.length()
                && !matchesSeparator(after.charAt(end), sep)) {
        }

        return new Word(before.substring(start) + after.substring(0, end),
                before.length() - start, end);
    }

    private static Word skipSeparator(String before, String after, String sep,
            int maxSkip) {
        int start = before.length();
        int end = -1;
        // Make before[start] and after[end] point to characters that
        // are non-separators.
        int skip = 0;
        while ((maxSkip == -1 || skip < maxSkip) && start > 0
                && matchesSeparator(before.charAt(start - 1), sep)) {
            --start;
            ++skip;
        }

        skip = 0;
        while ((maxSkip == -1 || skip <= maxSkip) && ++end < after.length()
                && matchesSeparator(after.charAt(end), sep)) {
            ++skip;
        }
        return new Word("", before.length() - start, end);
    }

    private static Word getBlockForMovement(String before, String after,
            String sep, int maxSkip) {
        Word initialSpace = skipSeparator(before, after, sep, maxSkip);
        Word word = getBlock(
                before.substring(0, before.length() - initialSpace.charsBefore),
                after.substring(initialSpace.charsAfter), sep);
        Word spaceAfter = skipSeparator(
                before.substring(0, before.length() - initialSpace.charsBefore
                        - word.charsBefore),
                after.substring(initialSpace.charsAfter + word.charsAfter),
                sep, maxSkip);
        // Update the before and after parameters with the whitespace we skipped
        if (initialSpace.charsBefore > 0) {
            word.charsBefore += initialSpace.charsBefore;
        }
        if (initialSpace.charsAfter == 0) {
            word.charsAfter += spaceAfter.charsAfter;
        } else {
            word.charsAfter = initialSpace.charsAfter;
        }
        return word;
    }

    private static boolean matchesSeparator(char character, String sep) {
        for (int i = 0; i < sep.length(); i++) {
            if (sep.charAt(i) == character) {
                return true;
            }
        }
        return false;
    }

    public static Word moveToPreviousCharacter(KeyboardListener listener) {
        int cursor = listener.getCursor();
        if (cursor == -1) {
            return null;
        }
        Word word = new Word("", 0, 0);
        if (cursor > 0) {
            CharSequence text = listener.getTextBeforeCursor(1);
            if (text != null) {
                word = new Word(text.toString(), 1, 0);
                word.moveLeft = true;
            } else {
                return null;
            }
            listener.setSelection(cursor - 1);
        }
        return word;
    }

    public static Word moveToNextCharacter(KeyboardListener listener) {
        int cursor = listener.getCursor();
        CharSequence text = listener.getTextAfterCursor(2);
        if (cursor == -1 || text == null) {
            return null;
        }
        Word word = new Word("", 0, 0);

        if (text.length() > 1) {
            word = new Word(text.subSequence(1, text.length()).toString(), 0, 1);
            word.moveRight = true;
        }
        listener.setSelection(cursor + 1);
        return word;
    }

    private static Word moveToPrevious(KeyboardListener listener,
            String separator, int maxLength, int maxSkip) {
        int cursor = listener.getCursor();
        CharSequence before = listener.getTextBeforeCursor(maxLength);
        CharSequence after = listener.getTextAfterCursor(maxLength);
        if (cursor == -1 || before == null || after == null) {
            return null;
        }

        Word word = getBlockForMovement(before.toString(), after.toString(),
                separator, maxSkip);
        if (word.charsBefore == 0) {
            word.word = "";
        } else {
            listener.setSelection(cursor - word.charsBefore);
            after = listener.getTextAfterCursor(maxLength);
            word.word = getBlock("", after.toString(), separator).word;
            word.moveLeft = true;
        }
        return word;
    }

    private static Word moveToNext(KeyboardListener listener, String separator,
            int maxLength, int maxSkip) {
        int cursor = listener.getCursor();
        CharSequence before = listener.getTextBeforeCursor(maxLength);
        CharSequence after = listener.getTextAfterCursor(maxLength);
        if (cursor == -1 || after == null || before == null) {
            return null;
        }

        Word word = getBlockForMovement(before.toString(), after.toString(),
                separator, maxSkip);
        if (word.charsAfter == 0) {
            word.word = "";
        } else {
            listener.setSelection(cursor + word.charsAfter);
            after = listener.getTextAfterCursor(maxLength);
            word.word = getBlock("", after == null ? "" : after.toString(),
                    separator).word;

            if (after != null && after.length() > 0) {
                word.moveRight = true;
            }
        }

        return word;
    }

    public static Word moveToPreviousWord(KeyboardListener listener) {
        return moveToPrevious(listener, WORD_SEPARATORS, MAX_WORD_LENGTH, -1);
    }

    public static Word moveToNextWord(KeyboardListener listener) {
        return moveToNext(listener, WORD_SEPARATORS, MAX_WORD_LENGTH, -1);
    }

    public static Word moveToPreviousLine(KeyboardListener listener) {
        return moveToPrevious(listener, LINE_SEPARATOR, MAX_LINE_LENGTH, 1);
    }

    public static Word moveToNextLine(KeyboardListener listener) {
        return moveToNext(listener, LINE_SEPARATOR, MAX_LINE_LENGTH, 1);
    }

    public static Word moveToHome(KeyboardListener listener) {
        Word word = new Word("", 0, 0);
        listener.setSelection(0);
        return word;
    }

    public static Word moveToEnd(KeyboardListener listener) {
        ExtractedText extractedText = listener.getAllText();
        if (extractedText == null) {
            return null;
        }

        if (extractedText.text != null) {
            int end = extractedText.text.length() + extractedText.startOffset;
            listener.setSelection(end);
        }
        return new Word("", 0, 0);
    }

    public static String getCharacter(KeyboardListener listener) {
        CharSequence text = listener.getTextAfterCursor(1);
        return text == null ? null : text.toString();
    }

    public static Word getWord(KeyboardListener listener) {
        CharSequence before = listener.getTextBeforeCursor(MAX_WORD_LENGTH);
        CharSequence after = listener.getTextAfterCursor(MAX_WORD_LENGTH);

        if (after != null && before != null) {
            return getBlock(before.toString(), after.toString(),
                    WORD_SEPARATORS);
        }
        return null;
    }

    public static Word getLine(KeyboardListener listener) {
        CharSequence before = listener.getTextBeforeCursor(MAX_LINE_LENGTH);
        CharSequence after = listener.getTextAfterCursor(MAX_LINE_LENGTH);
        if (before != null && after != null) {
            return getBlock(before.toString(), after.toString(), LINE_SEPARATOR);
        }
        return null;
    }

    public static String getAllText(KeyboardListener listener) {
        String text = null;
        ExtractedText extractedText = listener.getAllText();
        if (extractedText != null) {
            if (extractedText.text != null) {
                text = extractedText.text.toString();
            }
        }
        return text;
    }

    public static Word skipSepBackwards(KeyboardListener listener, String sep) {
        CharSequence after = "";
        CharSequence before = listener.getTextBeforeCursor(MAX_LINE_LENGTH);
        if (after != null && before != null) {
            Word word = skipSeparator(before.toString(), after.toString(), sep,
                    -1);
            int cursor = listener.getCursor();

            if (cursor > 0) {
                listener.setSelection(cursor - word.charsBefore);
                return word;
            }
        }
        return null;
    }

    public static class Word {
        public int charsAfter;
        public int charsBefore;
        public String word;
        public boolean moveLeft;
        public boolean moveRight;

        public Word(String word, int charsBefore, int charsAfter) {
            this.word = word;
            this.charsBefore = charsBefore;
            this.charsAfter = charsAfter;
        }
    }

    public static int characterCount(CharSequence text) {
        return text != null ? text.length() : 0;
    }

    public static int wordCount(CharSequence text) {
        if (characterCount(text) == 0) {
            return 0;
        }
        return text.toString().split("\\s+").length;
    }

    public static int lineCount(CharSequence text) {
        if (characterCount(text) == 0) {
            return 0;
        }
        return text.toString().split("\\n").length;
    }
}
