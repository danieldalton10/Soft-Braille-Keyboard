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

import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.util.TypedValue;

import com.dalton.braillekeyboard.Options.KeyboardType;
import com.dalton.braillekeyboard.Pad.Coords;

/**
 * Static methods to load a default pad from android settings or to calculate
 * dot positions according to supplied dimensions.
 * 
 * There is also a method that handles calibration and selecting a keyboard
 * based on the position of fingers.
 * 
 * If you add a new Pad you'll need to update both of these methods so it can be
 * used.
 */
public class PadUtilities {
    private static final int ERROR_DP = 120; // 2/3 inch
    private static final int MAX_SCREEN_SIZE = 160 * 6; // 6 inch

    private static void setPadStyle(Context context, Pad pad) {
        String style = Options.getStringPreference(context,
                R.string.pref_keyboard_style_key,
                context.getString(R.string.pref_keyboard_style_normal_value));

        if (style.equals(context
                .getString(R.string.pref_keyboard_style_slate_value))) {
            pad.makeSlateLayout();
        } else if (style.equals(context
                .getString(R.string.pref_keyboard_style_top_bottom_value))) {
            pad.swapTopBottom();
        }
    }

    public static Pad selectPad(Context context, Coords[] coords, int width,
            int height, boolean portrait, boolean useEightDots) {
        Pad pad;
        boolean autoSet = Options.getBooleanPreference(context,
                R.string.pref_auto_match_keyboard_key,
                Boolean.parseBoolean(context
                        .getString(R.string.pref_auto_match_keyboard_default)));
        boolean invert = Options.getBooleanPreference(context,
                R.string.pref_keyboard_invert_key, Boolean.parseBoolean(context
                        .getString(R.string.pref_keyboard_invert_default)));
        KeyboardType keyboard = KeyboardType
                .valueOf(Integer.parseInt(Options.getStringPreference(
                        context,
                        R.string.pref_default_keyboard_key,
                        context.getString(R.string.pref_default_keyboard_default))));
        if (autoSet || keyboard == KeyboardType.AUTO) {
            List<Coords> keys = Arrays.asList(coords);
            int left = VerticalPad.getColumn(keys, VerticalPad.Column.LEFT);
            int right = VerticalPad.getColumn(keys, VerticalPad.Column.RIGHT);
            int leftCount = 0;
            int rightCount = 0;
            int errorMargin = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, ERROR_DP, context
                            .getResources().getDisplayMetrics());
            for (Coords coord : keys) {
                if (VerticalPad.isInKeyColumn(coord, left, errorMargin)) {
                    ++leftCount;
                } else if (VerticalPad.isInKeyColumn(coord, right, errorMargin)) {
                    ++rightCount;
                }
            }

            if (leftCount == 3 && rightCount == 3) {
                pad = new VerticalPad(context, coords, width, height, portrait,
                        invert, useEightDots);
            } else {
                pad = new HorizontalPad(context, coords, width, height,
                        portrait, invert, useEightDots);
            }
        } else {
            if (keyboard == KeyboardType.HORIZONTAL) {
                pad = new HorizontalPad(context, coords, width, height,
                        portrait, invert, useEightDots);
            } else {
                pad = new VerticalPad(context, coords, width, height, portrait,
                        invert, useEightDots);
            }
        }

        setPadStyle(context, pad);
        return pad;
    }

    public static Pad displayDefaultPad(Context context, int width, int height,
            boolean portrait, boolean useEightDots) {
        Pad pad;
        boolean invert = Options.getBooleanPreference(context,
                R.string.pref_keyboard_invert_key, Boolean.parseBoolean(context
                        .getString(R.string.pref_keyboard_invert_default)));
        KeyboardType keyboard = KeyboardType
                .valueOf(Integer.parseInt(Options.getStringPreference(
                        context,
                        R.string.pref_default_keyboard_key,
                        context.getString(R.string.pref_default_keyboard_default))));
        int maxScreen = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, MAX_SCREEN_SIZE, context
                        .getResources().getDisplayMetrics());
        int screen = (int) Math.sqrt(Math.pow(width, 2) + Math.pow(height, 2));
        if (keyboard == KeyboardType.HORIZONTAL
                || (screen > maxScreen && KeyboardType.AUTO == keyboard)) {
            Coords[] coords = null;
            int prefKey = HorizontalPad.getPrefKey(portrait, invert);
            if ((coords = Pad.load(context, width, height, prefKey, portrait)) != null) {
                pad = new HorizontalPad(context, coords, width, height,
                        portrait, invert, useEightDots);
            } else {
                pad = HorizontalPad.displayDefaultPad(context, width, height,
                        portrait, invert, useEightDots);
            }
        } else {
            Coords[] coords = null;
            int prefKey = VerticalPad.getPrefKey(portrait, invert);
            if ((coords = Pad.load(context, width, height, prefKey, portrait)) != null) {
                pad = new VerticalPad(context, coords, width, height, portrait,
                        invert, useEightDots);
            } else {
                pad = VerticalPad.displayDefaultPad(context, width, height,
                        portrait, invert, useEightDots);
            }
        }

        setPadStyle(context, pad);
        return pad;
    }
}
