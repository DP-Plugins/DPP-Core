package com.darksoldier1404.dppc.api.inventory;

import com.darksoldier1404.dppc.annotation.DPPCoreVersion;

/**
 * Function a slot plays in the default DInventory paging tool bar.
 * The role determines which NBT tags {@link DefaultPageTools#tagFor} stamps onto the item,
 * which in turn drives the pagination handling in the inventory listener.
 */
@DPPCoreVersion(since = "5.4.3")
public enum PageToolRole {
    /** Decorative filler: clicks are cancelled but nothing else happens. */
    DECORATION,
    /** Moves to the previous page when clicked. */
    PREV,
    /** Moves to the next page when clicked. */
    NEXT,
    /** Displays the current page; supports {current}/{total} placeholders in the display name. */
    CURRENT;

    /**
     * @return the next role in the editor cycle order (wraps around).
     */
    public PageToolRole next() {
        PageToolRole[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    /**
     * @param name role name, case-insensitive; {@code null} or unknown values fall back to {@link #DECORATION}.
     * @return the matching role, never {@code null}.
     */
    public static PageToolRole fromString(String name) {
        if (name == null) return DECORATION;
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return DECORATION;
        }
    }
}
