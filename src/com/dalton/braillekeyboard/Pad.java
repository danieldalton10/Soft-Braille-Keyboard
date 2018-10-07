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
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import android.content.Context;
import android.util.TypedValue;

/**
 * Describes a Braille keyboard.
 * 
 * There are various types of Braille keyboards and you are free to implement
 * your own as long as they can provide the arrangement of the dots and the
 * swiping implementation.
 * 
 * See HorizontalPad or VerticalPad for examples.
 * 
 */
public abstract class Pad {
    private static final int DOT_FOUR = 3;
    private static final int DOT_SEVEN = 6;
    private static final int MAX_DOTS = 8;

    private final int SWIPE_MARGINE;

    public final int padString;

    private final int swipeThreshold;
    private final XY[] differences;

    public enum Column {
        LEFT, RIGHT;
    }

    public enum Swipe {
        NONE, UNKNOWN, ONE_LEFT, ONE_RIGHT, ONE_UP, ONE_DOWN, TWO_LEFT, TWO_RIGHT, TWO_DOWN, TWO_UP, THREE_LEFT, THREE_RIGHT, THREE_DOWN, THREE_UP, FOUR_LEFT, FOUR_RIGHT, FOUR_UP, FOUR_DOWN, FIVE_LEFT, FIVE_RIGHT, FIVE_DOWN, FIVE_UP, SIX_LEFT, SIX_RIGHT, SIX_DOWN, SIX_UP, HOLD_SIX_LEFT, HOLD_SIX_RIGHT, HOLD_SIX_UP, HOLD_SIX_DOWN, HOLD_THREE_LEFT, HOLD_THREE_RIGHT, HOLD_THREE_UP, HOLD_THREE_DOWN, HOLD_ONE_UP, HOLD_ONE_DOWN, HOLD_ONE_LEFT, HOLD_ONE_RIGHT, HOLD_FOUR_LEFT, HOLD_FOUR_RIGHT, HOLD_FOUR_DOWN, HOLD_FOUR_UP;

        public static Swipe valueOf(int value) {
            // bit strings to represent different swipe actions
            final int VALUE_NONE = 0;
            final int VALUE_ONE_LEFT = 1;
            final int VALUE_ONE_RIGHT = 2;
            final int VALUE_ONE_DOWN = 3;
            final int VALUE_ONE_UP = 4;
            final int VALUE_TWO_LEFT = 8;
            final int VALUE_TWO_RIGHT = 16;
            final int VALUE_TWO_DOWN = 24;
            final int VALUE_TWO_UP = 32;
            final int VALUE_THREE_LEFT = 64;
            final int VALUE_THREE_RIGHT = 128;
            final int VALUE_THREE_DOWN = 192;
            final int VALUE_THREE_UP = 256;
            final int VALUE_FOUR_LEFT = 512;
            final int VALUE_FOUR_RIGHT = 1024;
            final int VALUE_FOUR_DOWN = 1536;
            final int VALUE_FOUR_UP = 2048;
            final int VALUE_FIVE_LEFT = 4096;
            final int VALUE_FIVE_RIGHT = 8192;
            final int VALUE_FIVE_DOWN = 12288;
            final int VALUE_FIVE_UP = 16384;
            final int VALUE_SIX_LEFT = 32768;
            final int VALUE_SIX_RIGHT = 65536;
            final int VALUE_SIX_DOWN = 98304;
            final int VALUE_SIX_UP = 131072;
            final int VALUE_HOLD_SIX_ONE_LEFT = 229377;
            final int VALUE_HOLD_SIX_TWO_LEFT = 229384;
            final int VALUE_HOLD_SIX_THREE_LEFT = 229440;
            final int VALUE_HOLD_SIX_ONE_RIGHT = 229378;
            final int VALUE_HOLD_SIX_TWO_RIGHT = 229392;
            final int VALUE_HOLD_SIX_THREE_RIGHT = 229504;
            final int VALUE_HOLD_SIX_ONE_DOWN = 229379;
            final int VALUE_HOLD_SIX_TWO_DOWN = 229400;
            final int VALUE_HOLD_SIX_THREE_DOWN = 229568;
            final int VALUE_HOLD_SIX_ONE_UP = 229380;
            final int VALUE_HOLD_SIX_TWO_UP = 229408;
            final int VALUE_HOLD_SIX_THREE_UP = 229632;
            final int VALUE_HOLD_THREE_FOUR_LEFT = 960;
            final int VALUE_HOLD_THREE_FIVE_LEFT = 4544;
            final int VALUE_HOLD_THREE_SIX_LEFT = 33216;
            final int VALUE_HOLD_THREE_FOUR_RIGHT = 1472;
            final int VALUE_HOLD_THREE_FIVE_RIGHT = 8640;
            final int VALUE_HOLD_THREE_SIX_RIGHT = 65984;
            final int VALUE_HOLD_THREE_FOUR_DOWN = 1984;
            final int VALUE_HOLD_THREE_FIVE_DOWN = 12736;
            final int VALUE_HOLD_THREE_SIX_DOWN = 98752;
            final int VALUE_HOLD_THREE_FOUR_UP = 2496;
            final int VALUE_HOLD_THREE_FIVE_UP = 16832;
            final int VALUE_HOLD_THREE_SIX_UP = 131520;
            final int VALUE_HOLD_ONE_FOUR_UP = 2055;
            final int VALUE_HOLD_ONE_FIVE_UP = 16391;
            final int VALUE_HOLD_ONE_SIX_UP = 131078;
            final int VALUE_HOLD_ONE_FOUR_DOWN = 1543;
            final int VALUE_HOLD_ONE_FIVE_DOWN = 12295;
            final int VALUE_HOLD_ONE_SIX_DOWN = 98311;
            final int VALUE_HOLD_ONE_FOUR_RIGHT = 1031;
            final int VALUE_HOLD_ONE_FIVE_RIGHT = 8199;
            final int VALUE_HOLD_ONE_SIX_RIGHT = 65543;
            final int VALUE_HOLD_ONE_FOUR_LEFT = 519;
            final int VALUE_HOLD_ONE_FIVE_LEFT = 4103;
            final int VALUE_HOLD_ONE_SIX_LEFT = 32775;
            final int VALUE_HOLD_FOUR_ONE_LEFT = 3585;
            final int VALUE_HOLD_FOUR_TWO_LEFT = 3592;
            final int VALUE_HOLD_FOUR_THREE_LEFT = 3648;
            final int VALUE_HOLD_FOUR_ONE_RIGHT = 3586;
            final int VALUE_HOLD_FOUR_TWO_RIGHT = 3600;
            final int VALUE_HOLD_FOUR_THREE_RIGHT = 3712;
            final int VALUE_HOLD_FOUR_ONE_DOWN = 3587;
            final int VALUE_HOLD_FOUR_TWO_DOWN = 3608;
            final int VALUE_HOLD_FOUR_THREE_DOWN = 3776;
            final int VALUE_HOLD_FOUR_ONE_UP = 3588;
            final int VALUE_HOLD_FOUR_TWO_UP = 3616;
            final int VALUE_HOLD_FOUR_THREE_UP = 3840;
            switch (value) {
            case VALUE_NONE:
                return NONE;
            case VALUE_ONE_LEFT:
                return ONE_LEFT;
            case VALUE_ONE_RIGHT:
                return ONE_RIGHT;
            case VALUE_ONE_DOWN:
                return ONE_DOWN;
            case VALUE_ONE_UP:
                return ONE_UP;
            case VALUE_TWO_LEFT:
                return TWO_LEFT;
            case VALUE_TWO_RIGHT:
                return TWO_RIGHT;
            case VALUE_TWO_DOWN:
                return TWO_DOWN;
            case VALUE_TWO_UP:
                return TWO_UP;
            case VALUE_THREE_LEFT:
                return THREE_LEFT;
            case VALUE_THREE_RIGHT:
                return THREE_RIGHT;
            case VALUE_THREE_DOWN:
                return THREE_DOWN;
            case VALUE_THREE_UP:
                return THREE_UP;
            case VALUE_FOUR_LEFT:
                return FOUR_LEFT;
            case VALUE_FOUR_RIGHT:
                return FOUR_RIGHT;
            case VALUE_FOUR_DOWN:
                return FOUR_DOWN;
            case VALUE_FOUR_UP:
                return FOUR_UP;
            case VALUE_FIVE_LEFT:
                return FIVE_LEFT;
            case VALUE_FIVE_RIGHT:
                return FIVE_RIGHT;
            case VALUE_FIVE_UP:
                return FIVE_UP;
            case VALUE_FIVE_DOWN:
                return FIVE_DOWN;
            case VALUE_SIX_LEFT:
                return SIX_LEFT;
            case VALUE_SIX_RIGHT:
                return SIX_RIGHT;
            case VALUE_SIX_DOWN:
                return SIX_DOWN;
            case VALUE_SIX_UP:
                return SIX_UP;
            case VALUE_HOLD_SIX_ONE_RIGHT:
            case VALUE_HOLD_SIX_TWO_RIGHT:
            case VALUE_HOLD_SIX_THREE_RIGHT:
                return HOLD_SIX_RIGHT;
            case VALUE_HOLD_SIX_ONE_LEFT:
            case VALUE_HOLD_SIX_TWO_LEFT:
            case VALUE_HOLD_SIX_THREE_LEFT:
                return HOLD_SIX_LEFT;
            case VALUE_HOLD_SIX_ONE_DOWN:
            case VALUE_HOLD_SIX_TWO_DOWN:
            case VALUE_HOLD_SIX_THREE_DOWN:
                return HOLD_SIX_DOWN;
            case VALUE_HOLD_SIX_ONE_UP:
            case VALUE_HOLD_SIX_TWO_UP:
            case VALUE_HOLD_SIX_THREE_UP:
                return HOLD_SIX_UP;
            case VALUE_HOLD_THREE_SIX_LEFT:
            case VALUE_HOLD_THREE_FIVE_LEFT:
            case VALUE_HOLD_THREE_FOUR_LEFT:
                return HOLD_THREE_LEFT;
            case VALUE_HOLD_THREE_SIX_RIGHT:
            case VALUE_HOLD_THREE_FIVE_RIGHT:
            case VALUE_HOLD_THREE_FOUR_RIGHT:
                return HOLD_THREE_RIGHT;
            case VALUE_HOLD_THREE_SIX_UP:
            case VALUE_HOLD_THREE_FIVE_UP:
            case VALUE_HOLD_THREE_FOUR_UP:
                return HOLD_THREE_UP;
            case VALUE_HOLD_THREE_SIX_DOWN:
            case VALUE_HOLD_THREE_FIVE_DOWN:
            case VALUE_HOLD_THREE_FOUR_DOWN:
                return HOLD_THREE_DOWN;
            case VALUE_HOLD_ONE_SIX_DOWN:
            case VALUE_HOLD_ONE_FIVE_DOWN:
            case VALUE_HOLD_ONE_FOUR_DOWN:
                return HOLD_ONE_DOWN;
            case VALUE_HOLD_ONE_SIX_UP:
            case VALUE_HOLD_ONE_FIVE_UP:
            case VALUE_HOLD_ONE_FOUR_UP:
                return HOLD_ONE_UP;
            case VALUE_HOLD_ONE_SIX_RIGHT:
            case VALUE_HOLD_ONE_FIVE_RIGHT:
            case VALUE_HOLD_ONE_FOUR_RIGHT:
                return HOLD_ONE_RIGHT;
            case VALUE_HOLD_ONE_SIX_LEFT:
            case VALUE_HOLD_ONE_FIVE_LEFT:
            case VALUE_HOLD_ONE_FOUR_LEFT:
                return HOLD_ONE_LEFT;
            case VALUE_HOLD_FOUR_ONE_LEFT:
            case VALUE_HOLD_FOUR_TWO_LEFT:
            case VALUE_HOLD_FOUR_THREE_LEFT:
                return HOLD_FOUR_LEFT;
            case VALUE_HOLD_FOUR_ONE_RIGHT:
            case VALUE_HOLD_FOUR_TWO_RIGHT:
            case VALUE_HOLD_FOUR_THREE_RIGHT:
                return HOLD_FOUR_RIGHT;
            case VALUE_HOLD_FOUR_ONE_DOWN:
            case VALUE_HOLD_FOUR_TWO_DOWN:
            case VALUE_HOLD_FOUR_THREE_DOWN:
                return HOLD_FOUR_DOWN;
            case VALUE_HOLD_FOUR_ONE_UP:
            case VALUE_HOLD_FOUR_TWO_UP:
            case VALUE_HOLD_FOUR_THREE_UP:
                return HOLD_FOUR_UP;
            default:
                return UNKNOWN;
            }
        }
    }

    protected final List<Coords> keys = new ArrayList<Coords>(MAX_DOTS);

    protected static final Comparator<Coords> comparatorX = new Comparator<Coords>() {
        @Override
        public int compare(Coords o1, Coords o2) {
            return (int) (o1.x - o2.x);
        }
    };

    protected static final Comparator<Coords> comparatorY = new Comparator<Coords>() {
        @Override
        public int compare(Coords o1, Coords o2) {
            return (int) (o1.y - o2.y);
        }
    };

    protected final int viewWidth;
    protected final int viewHeight;

    protected final boolean invert;

    public Pad(Context context, Coords[] coords, int width, int height,
            int padString, boolean invert) {
        SWIPE_MARGINE = Integer.parseInt(Options.getStringPreference(context,
                R.string.pref_swipe_sensitivity_key,
                context.getString(R.string.default_swipe_sensitivity)));
        this.invert = invert;
        this.padString = padString;
        viewHeight = height;
        viewWidth = width;
        swipeThreshold = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, SWIPE_MARGINE, context
                        .getResources().getDisplayMetrics());
        for (Coords coord : coords) {
            keys.add(coord);
        }
        differences = new XY[MAX_DOTS];
    }

    public Coords[] getBrailleDots(Coords[] coords, int dots) {
        dots = dots > keys.size() ? keys.size() : dots;
        if (dots > keys.size()) {
            throw new IllegalArgumentException("Requires " + dots
                    + " keys only " + keys.size() + " set");
        }
        int[] total = getAverageLeftRightColumns();
        List<Coords> list = new ArrayList<Coords>();
        for (Coords coord : coords) {
            if (coord != null) {
                list.add(coord);
            }
        }
        Coords[] brailleDots = new Coords[coords.length];
        while (list.size() > 0) {
            matchDotToCoord(dots, total, list, brailleDots);
        }
        return brailleDots;
    }

    private void matchDotToCoord(int dots, int[] total, List<Coords> list,
            Coords[] outputDots) {
        Coords coords = list.remove(0);
        int leftRight = Math.abs(coords.x - total[0]) <= Math.abs(coords.x
                - total[1]) ? 0 : 1;
        int best = -1;
        int bestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < dots; i++) {
            int j = getColumn(i) == Column.LEFT ? 0 : 1;
            if (j == leftRight) {
                int result = getDistance(i, coords);
                if (result < bestDistance) {
                    if (outputDots[i] != null) {
                        if (result < getDistance(i, outputDots[i])) {
                            list.add(outputDots[i]);
                            outputDots[i] = null;
                            differences[i] = null;
                        } else {
                            continue;
                        }
                    }
                    bestDistance = result;
                    best = i;
                }
            }
        }
        // sometimes we may not resolve a dot eg. 4 fingers in the left column
        // of a six dot keyboard. Also seems to crash when typing really
        // quickly. Could have something to do with wrapid touch events, but
        // unsure.
        if (best >= 0) {
            differences[best] = keys.get(best).getUpdate(coords.x, coords.y);
            outputDots[best] = coords;
        }
    }

    private int getDistance(int key, Coords coord) {
        double horizontal = Math.pow(Math.abs(coord.x - keys.get(key).x), 2);
        double vertical = Math.pow(Math.abs(coord.y - keys.get(key).y), 2);
        int result = (int) Math.sqrt(horizontal + vertical);
        return result;
    }

    private int[] getAverageLeftRightColumns() {
        int total[] = { 0, 0 };
        for (int i = 0; i < keys.size(); i++) {
            if (keys.get(i) != null) {
                int j = getColumn(i) == Column.LEFT ? 0 : 1;
                total[j] += keys.get(i).x;
            }
        }
        total[0] /= (keys.size() / 2);
        total[1] /= (keys.size() / 2);
        return total;
    }

    protected Column getColumn(int key) {
        return key < DOT_FOUR || key == DOT_SEVEN ? Column.LEFT : Column.RIGHT;
    }

    public void updateKeys(boolean portrait) {
        XY[] newDiff = new XY[2];
        newDiff[0] = new XY(0, 0);
        newDiff[1] = new XY(0, 0);
        int count[] = { 0, 0 };
        for (int i = 0; i < differences.length; i++) {
            int j = getColumn(i) == Column.LEFT ? 0 : 1;
            if (differences[i] != null) {
                count[j] += 1;
                newDiff[j].x += differences[i].x;
                newDiff[j].y += differences[i].y;
                differences[i] = null;
            }
        }
        if (count[0] > 0) {
            newDiff[0].x /= count[0];
            newDiff[0].y /= count[0];
        }
        if (count[1] > 0) {
            newDiff[1].x /= count[1];
            newDiff[1].y /= count[1];
        }

        for (int i = 0; i < keys.size(); i++) {
            int j = getColumn(i) == Column.LEFT ? 0 : 1;
            if (keys.get(i) != null) {
                keys.get(i).update(newDiff[j]);
            }
        }
    }

    protected Swipe getGenericSwipeAction(Coords[] coords, boolean swap) {
        final int REQUIRED_BITS = 3;
        StringBuilder sb = new StringBuilder();
        for (int i = coords.length - 1; i >= 0; i--) {
            String bitString = "";
            if (coords[i] != null) {
                bitString = Integer.toBinaryString(coords[i].swipeDirection(
                        swipeThreshold, swipeThreshold, swap, invert));
            }
            // zero padding
            for (int j = 0; j < REQUIRED_BITS - bitString.length(); j++) {
                sb.append("0");
            }
            sb.append(bitString);
        }
        for (int i = 0; i < sb.length(); i += 3) {
            if (!sb.substring(i, i + 3).equals("000")
                    && !sb.substring(i, i + 3).equals("111")) {
                return Swipe.valueOf(Integer.parseInt(sb.toString(), 2));
            }
        }
        return Swipe.NONE;
    }

    abstract Swipe getSwipe(Coords[] coords, boolean swap);

    protected static int getXGap(List<Coords> list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i).x;
        }
        return getGap(array);
    }

    protected static int getYGap(List<Coords> list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i).y;
        }
        return getGap(array);
    }

    private static int getGap(int[] array) {
        int[] gaps = new int[array.length - 1];
        for (int i = 1; i < array.length; i++) {
            gaps[i - 1] = Math.abs(array[i] - array[i - 1]);
        }
        int difference = 0;
        for (int gap : gaps) {
            difference += gap;
        }
        return difference / gaps.length;
    }

    protected void save(Context context, int prefKey, boolean portrait) {
        Set<String> points = new HashSet<String>();
        for (Coords key : keys) {
            points.add(savePointString(key, portrait));
        }
        Options.writeStringSetPreference(context, prefKey, points);
    }

    public static Coords[] load(Context context, int viewWidth, int viewHeight,
            int prefKey, boolean portrait) {
        Set<String> points = Options.getStringSetPreference(context, prefKey,
                null);
        if (points != null) {
            int[] centre = { portrait ? viewHeight / 2 : viewWidth / 2,
                    portrait ? viewWidth / 2 : viewHeight / 2 };
            Coords[] coords = new Coords[points.size()];
            int i = 0;
            for (String point : points) {
                coords[i++] = new Coords(centre, point);
            }
            return coords;
        }
        return null;
    }

    private String savePointString(Coords key, boolean portrait) {
        int centre[] = { portrait ? viewHeight / 2 : viewWidth / 2,
                portrait ? viewWidth / 2 : viewHeight / 2 };
        StringBuilder sb = new StringBuilder();
        sb.append(String.valueOf(key.id));
        sb.append(',');
        sb.append(String.valueOf(key.x - centre[0]));
        sb.append(',');
        sb.append(String.valueOf(key.y - centre[1]));
        return sb.toString();
    }

    public List<Coords> getKeys() {
        return keys;
    }

    public void makeSlateLayout() {
        int right = 3;
        for (int i = 0; i < right; i++) {
            Collections.swap(keys, i, right + i);
        }

        if (keys.size() == 8) {
            Collections.swap(keys, 6, 7);
        }
    }

    public void swapTopBottom() {
        Collections.swap(keys, 0, 2);
        Collections.swap(keys, 3, 5);
    }

    public static class Coords {
        public static final byte DOT_NONE = 7;
        public static final byte DOT_LEFT = 1;
        public static final byte DOT_RIGHT = 2;
        public static final byte DOT_DOWN = 3;
        public static final byte DOT_UP = 4;
        private static final int HISTORY_SIZE = 5;

        public final int id;
        private final Queue<XY> coordHistory = new LinkedList<XY>();

        public int x;
        public int y;

        private int secondX;
        private int secondY;

        public Coords(int x, int y) {
            this(-1, x, y);
        }

        public Coords(int id, int x, int y) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.secondX = x;
            this.secondY = y;
            coordHistory.add(new XY(x, y));
        }

        public Coords(int[] centre, String point) {
            String[] components = point.split(",");
            x = centre[0] + Integer.parseInt(components[1]);
            y = centre[1] + Integer.parseInt(components[2]);
            id = Integer.parseInt(components[0]);
            coordHistory.add(new XY(x, y));
        }

        public int getSecondX() {
            return secondX;
        }

        public int getSecondY() {
            return secondY;
        }

        public void setSecondCords(int x, int y) {
            secondX = x;
            secondY = y;
        }

        public byte swipeDirection(int xSwipeThreshold, int ySwipeThreshold,
                boolean swap, boolean invert) {
            byte swipe = swipeDirection(xSwipeThreshold, ySwipeThreshold, swap);
            if (!swap || !invert) {
                return swipe;
            }
            switch (swipe) {
            case DOT_UP:
                return DOT_DOWN;
            case DOT_DOWN:
                return DOT_UP;
            case DOT_LEFT:
                return DOT_RIGHT;
            case DOT_RIGHT:
                return DOT_LEFT;
            default:
                return swipe;
            }
        }

        private byte swipeDirection(int xSwipeThreshold, int ySwipeThreshold,
                boolean swap) {
            int xDiff = x - secondX;
            int yDiff = y - secondY;
            if (Math.abs(xDiff) > Math.abs(yDiff)) {
                if (xDiff > xSwipeThreshold) {
                    return swap ? DOT_LEFT : DOT_RIGHT;
                } else if (xDiff < (0 - xSwipeThreshold)) {
                    return swap ? DOT_RIGHT : DOT_LEFT;
                }
            } else if (Math.abs(yDiff) >= Math.abs(xDiff)) {
                if (yDiff > ySwipeThreshold) {
                    return DOT_UP;
                } else if (yDiff < (0 - ySwipeThreshold)) {
                    return DOT_DOWN;
                }
            }
            return DOT_NONE;
        }

        public XY getUpdate(int x, int y) {
            if (coordHistory.size() >= HISTORY_SIZE) {
                coordHistory.poll();
            }
            coordHistory.add(new XY(x, y));
            return getXYDifference();
        }

        private XY getXYDifference() {
            double total = 0;
            double i = Math.pow(2d, (double) coordHistory.size());
            double x = 0;
            double y = 0;
            for (XY item : coordHistory) {
                i /= 2;
                x += (1 / i) * item.x;
                y += (1 / i) * item.y;
                total += 1 / i;
            }
            int newX = (int) (x / total);
            int newY = (int) (y / total);
            return new XY(newX - this.x, newY - this.y);
        }

        public void update(XY diff) {
            this.x += diff.x;
            this.y += diff.y;
        }
    }

    public static class XY {
        public int x;
        public int y;

        public XY(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
