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
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.util.TypedValue;

/**
 * Implementation of the VerticalPad style Braille keyboard.
 * 
 * This style of keyboard is designed for smart phone or smaller screens. Dots
 * are arranged as follows. Two columns of dots from top to bottom left column
 * is 1, 2, 3 and right is 4, 5 and 6. Dots 7 and 8 are below dots 3 and 6 if
 * present respectively. Users generally hold the screen facing away from them
 * to use this layout.
 */
public class VerticalPad extends Pad {

    private static final int MAX_COLUMN_WIDTH_DP = 120; // 2/3 inch
    private static final int MAX_HORIZONTAL_DISTANCE = 120; // 2/3 inch
    private static final int MAX_VERTICAL_DISTANCE = 80; // 0.5 inch

    public VerticalPad(Context context, Coords[] coords, int width, int height,
            boolean portrait, boolean invert, boolean useEightDots) {
        super(context, coords, width, height, R.string.pad_vertical, invert);
        int prefKey = getPrefKey(portrait, invert);
        save(context, prefKey, portrait);
        int maxColumnWidth = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, MAX_COLUMN_WIDTH_DP, context
                        .getResources().getDisplayMetrics());
        sortKeys(keys, maxColumnWidth, portrait);
        if (useEightDots) {
            insertSpecialDots(portrait);
        }
    }

    public static int getPrefKey(boolean portrait, boolean invert) {
        if (portrait && !invert) {
            return R.string.pref_keyboard_save_vertical_portrait_key;
        } else if (!portrait && !invert) {
            return R.string.pref_keyboard_save_vertical_landscape_key;
        } else if (portrait && invert) {
            return R.string.pref_keyboard_save_vertical_portrait_invert_key;
        } else {
            return R.string.pref_keyboard_save_vertical_landscape_invert_key;
        }
    }

    public static Pad displayDefaultPad(Context context, int width, int height,
            boolean portrait, boolean invert, boolean useEightDots) {
        int centreWidth = width / 2;
        int offsetWidth = Math.min(width / 5, (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, MAX_HORIZONTAL_DISTANCE, context
                        .getResources().getDisplayMetrics()));
        int offsetHeight = Math.min(height / 4, (int) TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                        MAX_VERTICAL_DISTANCE, context.getResources()
                                .getDisplayMetrics()));
        Coords[] coords = new Coords[6];
        // compute the coordinates
        int y = offsetHeight;
        for (int i = 0; i < coords.length; i++) {
            if (i % (coords.length / 2) == 0) {
                y = offsetHeight;
            }
            int x;
            if (i < coords.length / 2) {
                x = centreWidth - offsetWidth;
            } else {
                x = centreWidth + offsetWidth;
            }
            coords[i] = new Coords(x, y);
            y += offsetHeight;
        }
        if (invert) {
            coords = swapKeys(coords);
        }
        return new VerticalPad(context, coords, width, height, portrait,
                invert, useEightDots);
    }

    private static Coords[] swapKeys(Coords[] keys) {
        for (int i = 1; i <= keys.length / 2; i++) {
            Coords temp = keys[i - 1];
            keys[i - 1] = keys[keys.length - i];
            keys[keys.length - i] = temp;
        }
        return keys;
    }

    private void sortKeys(List<Coords> coords, int errorMargin, boolean portrait) {
        List<Coords> leftList = new ArrayList<Coords>();
        List<Coords> rightList = new ArrayList<Coords>();
        int left = getColumn(coords, Column.LEFT);
        int right = getColumn(coords, Column.RIGHT);
        for (Coords coord : coords) {
            if (isInKeyColumn(coord, left, errorMargin)) {
                leftList.add(coord);
            } else if (isInKeyColumn(coord, right, errorMargin)) {
                rightList.add(coord);
            } else {
                throw new IllegalArgumentException("Couldn't set the keyboard");
            }
        }
        Collections.sort(leftList, comparatorY);
        Collections.sort(rightList, comparatorY);
        keys.clear();
        if (!portrait) {
            keys.addAll(leftList);
            keys.addAll(rightList);
        } else {
            if (invert) {
                Collections.reverse(leftList);
                Collections.reverse(rightList);
                keys.addAll(leftList);
                keys.addAll(rightList);
            } else {
                keys.addAll(rightList);
                keys.addAll(leftList);
            }
        }
    }

    private void insertSpecialDots(boolean portrait) {
        int yGapLeft = getYGap(keys.subList(0, 3));
        int yGapRight = getYGap(keys.subList(3, 6));
        int leftX = (Collections.min(keys.subList(0, 3), comparatorX).x + Collections
                .max(keys.subList(0, 3), comparatorX).x) / 2;
        int leftY = invert && portrait ? keys.get(2).y - yGapLeft
                : keys.get(2).y + yGapLeft;
        int rightX = (Collections.min(keys.subList(3, 6), comparatorX).x + Collections
                .max(keys.subList(3, 6), comparatorX).x) / 2;
        int rightY = invert && portrait ? keys.get(5).y - yGapRight : keys
                .get(5).y + yGapRight;
        keys.add(new Coords(leftX, leftY > viewHeight ? viewHeight : leftY));
        keys.add(new Coords(rightX, rightY > viewHeight ? viewHeight : rightY));
    }

    public static int getColumn(List<Coords> coords, Column column) {
        if (column == Column.LEFT) {
            return (int) Collections.max(coords, comparatorX).x;
        } else {
            return (int) Collections.min(coords, comparatorX).x;
        }
    }

    public static boolean isInKeyColumn(Coords coord, int compareValue,
            int errorMargin) {
        return Math.abs(compareValue - coord.x) < errorMargin;
    }

    @Override
    public Swipe getSwipe(Coords[] coords, boolean swap) {
        return getGenericSwipeAction(coords, swap);
    }
}
