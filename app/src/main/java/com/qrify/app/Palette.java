package com.qrify.app;

/** Shared colour / style presets used by Customize and Settings. */
public final class Palette {

    private Palette() {
    }

    public static final String[] STYLE_NAMES = {"Square", "Dots", "Rounded", "Diamond", "Hearts", "Bars"};
    public static final String[] EYE_NAMES = {"Square", "Circle", "Rounded", "Leaf"};

    /** Each entry: {start, end}. Equal values mean a solid colour. */
    public static final int[][] FG = {
            {0xFF6A35EE, 0xFF6A35EE},
            {0xFF6A35EE, 0xFFE8348F},
            {0xFF111111, 0xFF111111},
            {0xFF1F6FEB, 0xFF1F6FEB},
            {0xFF1F6FEB, 0xFF12B3A8},
            {0xFF138A5A, 0xFF138A5A},
            {0xFFF97316, 0xFFE11D48},
            {0xFFE8348F, 0xFFE8348F},
    };
    public static final String[] FG_NAMES = {
            "Purple", "Purple-Pink", "Black", "Blue", "Blue-Teal", "Green", "Sunset", "Pink"};

    public static final int[] BG = {
            0xFFFFFFFF, 0xFFF3ECFF, 0xFFFFE9F3, 0xFFFFF6D6, 0xFFE3F8EE, 0xFFE4F1FF};
    public static final String[] BG_NAMES = {"White", "Lavender", "Rose", "Cream", "Mint", "Sky"};
}
