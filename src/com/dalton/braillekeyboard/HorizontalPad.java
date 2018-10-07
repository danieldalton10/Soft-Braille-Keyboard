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

import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.util.TypedValue;

/**
 * Implements a horizontal style Braille Pad.
 * 
 * This is mor like a conventional perkins Brailler where dots are arranged from
 * left to right. Dot 3 on the far left, then dot 2, dot 1, and on the right
 * hand they span 4, 5, 6 where 6 is the far right dot.
 * 
 * This implements the appropriate methods to facilitate loading, drawing and
 * saving/restoring a pad of this nature.
 * 
 * It also normalises swipes for the SBK Pad so that it is consistent with other
 * implementations of Pad.
 */
public class HorizontalPad extends Pad {
    private static final int MAX_HORIZONTAL_DISTANCE = 80; // 2/3 inch;

    public HorizontalPad(Context context, Coords[] coords, int width,
            int height, boolean portrait, boolean invert, boolean useEightDots) {
        super(context, coords, width, height, R.string.pad_horizontal, invert);
        save(context, getPrefKey(portrait, invert), portrait);
        sortKeys(keys, portrait, useEightDots);
    }

    public static int getPrefKey(boolean portrait, boolean invert) {
        if (portrait && !invert) {
            return R.string.pref_keyboard_save_horizontal_portrait_key;
        } else if (!portrait && !invert) {
            return R.string.pref_keyboard_save_horizontal_landscape_key;
        } else if (portrait && invert) {
            return R.string.pref_keyboard_save_horizontal_portrait_invert_key;
        } else {
            return R.string.pref_keyboard_save_horizontal_landscape_invert_key;
        }
    }

    public static Pad displayDefaultPad(Context context, int width, int height,
            boolean portrait, boolean invert, boolean useEightDots) {
        int offsetWidth = Math.min(width / 8, (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, MAX_HORIZONTAL_DISTANCE, context
                        .getResources().getDisplayMetrics()));
        Coords[] coords = new Coords[6];
        // compute the coordinates
        int y = height / 2;
        int x = offsetWidth;
        for (int i = 0; i < coords.length / 2; i++) {
            coords[i] = new Coords(x, y);
            x += offsetWidth;
        }
        x = width - offsetWidth;
        for (int i = coords.length / 2; i < coords.length; i++) {
            coords[i] = new Coords(x, y);
            x -= offsetWidth;
        }
        if (invert) {
            coords = swapKeys(coords);
        }
        return new HorizontalPad(context, coords, width, height, portrait,
                invert, useEightDots);
    }

    private static Coords[] swapKeys(Coords[] keys) {
        for (int i = 0; i < keys.length / 2; i++) {
            Coords temp = keys[i];
            keys[i] = keys[i + keys.length / 2];
            keys[i + keys.length / 2] = temp;
        }
        return keys;
    }

    private void insertSpecialDots() {
        int xGapLeft = getXGap(keys.subList(0, 3));
        int yGapLeft = getYGap(keys.subList(0, 3));
        int xGapRight = getXGap(keys.subList(3, 6));
        int yGapRight = getYGap(keys.subList(3, 6));
        int leftX = keys.get(2).x - xGapLeft;
        int leftY = keys.get(2).y - yGapLeft;
        int rightX = keys.get(5).x + xGapRight;
        int rightY = keys.get(5).y - yGapRight;
        keys.add(new Coords(leftX < 0 ? 0 : leftX, leftY < 0 ? 0 : leftY));
        keys.add(new Coords(rightX > viewWidth ? viewWidth : rightX,
                rightY < 0 ? 0 : rightY));
    }

    private void sortKeys(List<Coords> coords, boolean portrait,
            boolean useEightDots) {
        Collections.sort(coords, comparatorX);
        Collections.swap(keys, 0, 2);
        if (useEightDots) {
            insertSpecialDots();
        }
        if (portrait && !invert) {
            swapLeftRight();
        }
    }

    private void swapLeftRight() {
        int i = 0;
        int j = 3;
        while (i < 3 && j < 6) {
            Collections.swap(keys, i++, j++);
        }
        if (keys.size() == 8) {
            Collections.swap(keys, 6, 7);
        }
    }

    @Override
    public Swipe getSwipe(Coords[] coords, boolean swap) {
        Swipe swipe = getGenericSwipeAction(coords, swap);
        switch (swipe) {
        case ONE_LEFT:
            return Swipe.ONE_RIGHT;
        case ONE_RIGHT:
            return Swipe.ONE_LEFT;
        case TWO_LEFT:
            return Swipe.TWO_RIGHT;
        case TWO_RIGHT:
            return Swipe.TWO_LEFT;
        case THREE_LEFT:
            return Swipe.THREE_RIGHT;
        case THREE_RIGHT:
            return Swipe.THREE_LEFT;
        case FOUR_LEFT:
            return Swipe.FOUR_RIGHT;
        case FOUR_RIGHT:
            return Swipe.FOUR_LEFT;
        case FIVE_LEFT:
            return Swipe.FIVE_RIGHT;
        case FIVE_RIGHT:
            return Swipe.FIVE_LEFT;
        case SIX_LEFT:
            return Swipe.SIX_RIGHT;
        case SIX_RIGHT:
            return Swipe.SIX_LEFT;
        case HOLD_SIX_LEFT:
            return Swipe.HOLD_SIX_RIGHT;
        case HOLD_SIX_RIGHT:
            return Swipe.HOLD_SIX_LEFT;
        case HOLD_THREE_LEFT:
            return Swipe.HOLD_THREE_RIGHT;
        case HOLD_THREE_RIGHT:
            return Swipe.HOLD_THREE_LEFT;
        case HOLD_ONE_LEFT:
            return Swipe.HOLD_ONE_RIGHT;
        case HOLD_ONE_RIGHT:
            return Swipe.HOLD_ONE_LEFT;
        case HOLD_FOUR_LEFT:
            return Swipe.HOLD_FOUR_RIGHT;
        case HOLD_FOUR_RIGHT:
            return Swipe.HOLD_FOUR_LEFT;
        default:
            return swipe;
        }
    }
}
