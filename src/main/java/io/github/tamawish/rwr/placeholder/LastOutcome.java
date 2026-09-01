package io.github.tamawish.rwr.placeholder;

import java.util.Locale;
import java.util.Objects;

/** Terminal reset outcome mapped from a post-reset event. */
public enum LastOutcome {
    SUCCESS,
    FAILED,
    CANCELLED,
    INTERRUPTED;

    /**
     * Maps a terminal {@code ResetPhase} name and optional failure type name.
     *
     * <p>{@code FAILED} + {@code EVENT_CANCELLED} is cancelled. There is no {@code CANCELLED}
     * phase on the public API.
     */
    public static LastOutcome from(String phaseName, String failureTypeOrNull) {
        String phase = normalize(phaseName);
        String failure = failureTypeOrNull == null ? "" : failureTypeOrNull.trim().toUpperCase(Locale.ROOT);
        if ("COMPLETE".equals(phase)) {
            return SUCCESS;
        }
        if ("INTERRUPTED".equals(phase)) {
            return INTERRUPTED;
        }
        if ("FAILED".equals(phase) && "EVENT_CANCELLED".equals(failure)) {
            return CANCELLED;
        }
        return FAILED;
    }

    public String localeKey() {
        return "outcome-" + name().toLowerCase(Locale.ROOT);
    }

    private static String normalize(String phaseName) {
        return phaseName == null ? "" : phaseName.trim().toUpperCase(Locale.ROOT);
    }
}
