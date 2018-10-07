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

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.view.textservice.SentenceSuggestionsInfo;
import android.view.textservice.SpellCheckerSession;
import android.view.textservice.SpellCheckerSession.SpellCheckerSessionListener;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextInfo;
import android.view.textservice.TextServicesManager;

public class SpellChecker {
    private static final int MAX_SUGGESTIONS = 8;
    private static final int MAX_SENTENCE_LENGTH = 200;
    private static final int SENTENCES_TO_CONSIDER = 6;

    public interface SpellingSuggestionsReadyListener {
        void suggestionsReady(Suggestion result);
    }

    public enum Direction {
        LEFT, UNDER_CURSOR, RIGHT;
    }

    private final SpellCheckerSessionListener spellCheckerListener = new SpellCheckerSessionListener() {
        @Override
        public void onGetSuggestions(final SuggestionsInfo[] arg0) {
        }

        /**
         * Callback for
         * {@link SpellCheckerSession#getSentenceSuggestions(TextInfo[], int)}
         * 
         * @param results
         *            an array of {@link SentenceSuggestionsInfo}s. These
         *            results are suggestions for {@link TextInfo}s queried by
         *            {@link SpellCheckerSession#getSentenceSuggestions(TextInfo[], int)}
         *            .
         */
        @Override
        @SuppressLint("NewApi")
        public void onGetSentenceSuggestions(SentenceSuggestionsInfo[] arg0) {
            if (arg0.length == 1) {
                SentenceSuggestionsInfo ssi = arg0[0];
                Suggestion results = null;
                if (ssi != null) {
                    int start = direction == Direction.LEFT ? ssi
                            .getSuggestionsCount() - 1 : 0;
                    int end = direction == Direction.LEFT ? -1 : ssi
                            .getSuggestionsCount();
                    int step = end < start ? -1 : 1;
                    int i = start;
                    while (i != end && results == null) {
                        if (isDirection(direction, cursor, ssi.getLengthAt(i),
                                ssi.getOffsetAt(i) + startOffset)) {
                            results = compileSuggestions(
                                    ssi.getSuggestionsInfoAt(i),
                                    ssi.getLengthAt(i), ssi.getOffsetAt(i)
                                            + startOffset);
                        }
                        i += step;
                    }
                }

                boolean moreToExpand = expandOffsets(true);
                if (results != null || !moreToExpand) {
                    listener.suggestionsReady(results);
                } else {
                    doSpellCheck();
                }
            } else {
                throw new IllegalArgumentException(
                        "Only supports one texinfo - got : " + arg0.length);
            }
        }
    };

    private final SpellCheckerSession spellChecker;
    private SpellingSuggestionsReadyListener listener;
    private int cursor;
    private Direction direction;
    private String text;
    private int startOffset;
    private int endOffset;

    public SpellChecker(Context context) {
        final TextServicesManager tsm = (TextServicesManager) context
                .getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE);
        spellChecker = tsm.newSpellCheckerSession(null, null,
                spellCheckerListener, true);
    }

    public boolean checkSpelling(SpellingSuggestionsReadyListener listener,
            String text, int cursor, Direction direction) {
        if (isSpellCheckAvailable() && text.length() > 0) {
            this.cursor = cursor;
            this.direction = direction;
            this.text = text;
            this.listener = listener;
            initOffsets();
            doSpellCheck();
            return true;
        } else {
            return false;
        }
    }

    @SuppressLint("NewApi")
    private void doSpellCheck() {
        spellChecker.cancel();

        // Append a space (" ") to the input string to the spelling checker.
        // This resolves some edge cases like a word followed by a period
        // without a following space.
        spellChecker.getSentenceSuggestions(
                new TextInfo[] { new TextInfo(text.substring(startOffset,
                        endOffset) + " ") }, MAX_SUGGESTIONS);
    }

    public void destroy() {
        if (spellChecker != null) {
            spellChecker.close();
        }
    }

    public boolean isSpellCheckAvailable() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                && spellChecker != null;
    }

    private Suggestion compileSuggestions(SuggestionsInfo suggestionInfo,
            int length, int offset) {
        if (suggestionInfo.getSuggestionsAttributes() == SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY
                || !isPotentialWord(text.substring(offset, offset + length))) {
            return null;
        }

        Suggestion suggestion = new Suggestion(text.substring(offset, length
                + offset), offset);
        for (int i = 0; i < suggestionInfo.getSuggestionsCount(); i++) {
            suggestion.results.add(suggestionInfo.getSuggestionAt(i));
        }

        return suggestion;
    }

    private static boolean isDirection(Direction direction, int cursor,
            int length, int offset) {
        switch (direction) {
        case UNDER_CURSOR:
            return cursor >= offset && cursor < (length + offset);
        case LEFT:
            return (length + offset) <= cursor;
        case RIGHT:
            return offset > cursor;
        default:
            throw new IllegalArgumentException(
                    "No implementation for direction = " + direction);
        }
    }

    private static boolean isPotentialWord(String word) {
        for (int i = 0; i < word.length(); i++) {
            if (Character.isLetter(word.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private void initOffsets() {
        startOffset = Math.max(0, cursor - MAX_SENTENCE_LENGTH);
        endOffset = Math.min(cursor + MAX_SENTENCE_LENGTH, text.length());
        expandOffsets(false);
    }

    private boolean expandOffsets(boolean shrinkVisitedRegion) {
        int tempOffset;
        boolean expanded;
        switch (direction) {
        case UNDER_CURSOR:
            return false;
        case LEFT:
            tempOffset = Math.max(0, startOffset - MAX_SENTENCE_LENGTH
                    * SENTENCES_TO_CONSIDER);
            expanded = startOffset != tempOffset;
            if (shrinkVisitedRegion) {
                endOffset = startOffset;
            }
            startOffset = tempOffset;
            break;
        case RIGHT:
            tempOffset = Math.min(text.length(), endOffset
                    + MAX_SENTENCE_LENGTH * SENTENCES_TO_CONSIDER);
            expanded = endOffset != tempOffset;
            if (shrinkVisitedRegion) {
                startOffset = endOffset;
            }
            endOffset = tempOffset;
            break;
        default:
            throw new IllegalArgumentException("No implementation for: "
                    + direction);
        }

        if (expanded) {
            normaliseOffsets();
        }
        return expanded;
    }

    private void normaliseOffsets() {
        for (int i = startOffset; i >= 0; i--) {
            startOffset = i;
            if (Character.isWhitespace(text.charAt(i))) {
                break;
            }
        }

        for (int i = endOffset; i < text.length(); i++) {
            endOffset = i;
            if (Character.isWhitespace(text.charAt(i))) {
                break;
            }
        }
    }

    public static class Suggestion {
        public final List<String> results = new ArrayList<String>();
        public final int offset;
        private int current = 0;
        private int length = -1;

        public Suggestion(String word, int offset) {
            this.offset = offset;
            results.add(word);
        }

        public String next() {
            current = ++current >= results.size() ? 0 : current;
            return results.get(current);
        }

        public String prev() {
            current = --current < 0 ? results.size() - 1 : current;
            return results.get(current);
        }

        public String getCurrent() {
            return results.get(current);
        }

        public void setLength() {
            length = results.get(current).length();
        }

        public int getLength() {
            return length == -1 ? results.get(0).length() : length;
        }

        public boolean isMisspelledWord() {
            return current == 0;
        }
    }
}
