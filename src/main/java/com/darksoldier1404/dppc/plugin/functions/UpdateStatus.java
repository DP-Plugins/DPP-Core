package com.darksoldier1404.dppc.plugin.functions;

import com.darksoldier1404.dppc.annotation.DPPCoreVersion;

/**
 * Result of comparing a plugin's installed version against the latest known version.
 * Drives the wool colour shown in the {@code /dppcp} information panel.
 */
@DPPCoreVersion(since = "5.4.3")
public enum UpdateStatus {
    /** Installed version is the latest. Shown as a green wool. */
    UP_TO_DATE,
    /** A newer version exists. Shown as a red wool. */
    OUTDATED,
    /** The version could not be verified (never checked, API failure, or unparseable). Shown as a gray wool. */
    UNKNOWN;

    /**
     * Determines the status from the installed and latest version strings.
     *
     * @param current the installed version.
     * @param latest  the latest version from the API; {@code null}, empty or {@code "0.0.0"} means
     *                "could not verify".
     * @return {@link #UNKNOWN} when verification is not possible, otherwise {@link #OUTDATED} or
     *         {@link #UP_TO_DATE}.
     */
    public static UpdateStatus of(String current, String latest) {
        if (latest == null || latest.isEmpty() || latest.equals("0.0.0")) return UNKNOWN;
        if (current == null || current.isEmpty()) return UNKNOWN;
        try {
            String[] latestParts = latest.split("\\.");
            String[] currentParts = current.split("\\.");
            for (int i = 0; i < Math.min(latestParts.length, currentParts.length); i++) {
                int latestPart = Integer.parseInt(latestParts[i].trim());
                int currentPart = Integer.parseInt(currentParts[i].trim());
                if (latestPart > currentPart) return OUTDATED;
                if (latestPart < currentPart) return UP_TO_DATE;
            }
            return UP_TO_DATE;
        } catch (NumberFormatException e) {
            return UNKNOWN;
        }
    }
}
