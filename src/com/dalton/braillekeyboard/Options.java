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

import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Options {
    public interface OptionList {
        OptionList[] getValues();

        int getResource();

        String getValue();
    }

    public enum KeyboardFeedback implements OptionList {
        NONE(0, R.string.keyboard_feedback_none), VIBRATE(1,
                R.string.keyboard_feedback_vibrate), SOUND(2,
                R.string.keyboard_feedback_sound), ALL(3,
                R.string.keyboard_feedback_all);

        public final int value;
        public final int resource;

        KeyboardFeedback(int value, int resource) {
            this.value = value;
            this.resource = resource;
        }

        public static KeyboardFeedback valueOf(int value) {
            for (KeyboardFeedback keyboardFeedback : values()) {
                if (keyboardFeedback.value == value) {
                    return keyboardFeedback;
                }
            }
            throw new IllegalArgumentException("Invalid value: " + value);
        }

        public static KeyboardFeedback next(KeyboardFeedback feedback) {
            int value = feedback.value + 1 >= values().length ? 0
                    : feedback.value + 1;
            return values()[value];
        }

        public OptionList[] getValues() {
            return values();
        }

        public String getValue() {
            return String.valueOf(value);
        }

        public int getResource() {
            return resource;
        }
    }

    public enum KeyboardEcho implements OptionList {
        NONE(0, R.string.keyboard_echo_none), CHARACTER(1,
                R.string.keyboard_echo_character), WORD(2,
                R.string.keyboard_echo_word), ALL(3, R.string.keyboard_echo_all);

        public final int value;
        public final int resource;

        KeyboardEcho(int value, int resource) {
            this.value = value;
            this.resource = resource;
        }

        public static KeyboardEcho valueOf(int value) {
            for (KeyboardEcho keyboardEcho : values()) {
                if (keyboardEcho.value == value) {
                    return keyboardEcho;
                }
            }
            throw new IllegalArgumentException("Invalid value: " + value);
        }

        public static KeyboardEcho next(KeyboardEcho echo) {
            int value = echo.value + 1 >= values().length ? 0 : echo.value + 1;
            return values()[value];
        }

        public OptionList[] getValues() {
            return values();
        }

        public String getValue() {
            return String.valueOf(value);
        }

        public int getResource() {
            return resource;
        }
    }

    public enum KeyboardType {
        AUTO, VERTICAL, HORIZONTAL;

        public static KeyboardType valueOf(int value) {
            switch (value) {
            case 1:
                return VERTICAL;
            case 2:
                return HORIZONTAL;
            default:
                return AUTO;
            }
        }
    }

    public static boolean getBooleanPreference(Context context, int resource,
            boolean defaultValue) {
        SharedPreferences sharedPref = PreferenceManager
                .getDefaultSharedPreferences(context);
        return sharedPref.getBoolean(context.getString(resource), defaultValue);
    }

    public static String getStringPreference(Context context, int resource,
            String defaultValue) {
        SharedPreferences sharedPref = PreferenceManager
                .getDefaultSharedPreferences(context);
        return sharedPref.getString(context.getString(resource), defaultValue);
    }

    public static boolean switchBooleanPreference(Context context,
            int resource, boolean defaultValue) {
        SharedPreferences sharedPref = PreferenceManager
                .getDefaultSharedPreferences(context);
        boolean pref = getBooleanPreference(context, resource, defaultValue);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(context.getString(resource), !pref);
        editor.commit();
        return !pref;
    }

    public static void writeStringPreference(Context context, int resource,
            String value) {
        SharedPreferences sharedPref = PreferenceManager
                .getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(context.getString(resource), value);
        editor.commit();
    }

    public static Set<String> getStringSetPreference(Context context,
            int resource, Set<String> defaultValue) {
        SharedPreferences sharedPref = PreferenceManager
                .getDefaultSharedPreferences(context);
        return sharedPref.getStringSet(context.getString(resource),
                defaultValue);
    }

    public static void writeStringSetPreference(Context context, int resource,
            Set<String> value) {
        SharedPreferences sharedPref = PreferenceManager
                .getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putStringSet(context.getString(resource), value);
        editor.commit();
    }
}
