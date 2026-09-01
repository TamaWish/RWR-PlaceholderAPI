package io.github.tamawish.rwr.placeholder;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;

/** Identifier suffixes supported by the {@code rwr} PlaceholderAPI expansion. */
public enum PlaceholderKeys {
    WORLDS("worlds", false),
    ID("id", true),
    NAME("name", true),
    WORLD("world", true),
    STATE("state", true),
    PHASE("phase", true),
    STATUS("status", true),
    CAN_RESET("can_reset", true),
    RESETTING("resetting", true),
    COUNTDOWN("countdown", true),
    NEXT_RESET("next_reset", true),
    LAST_OUTCOME("last_outcome", true);

    private static final PlaceholderKeys[] WORLD_KEYS_LONGEST_FIRST = Arrays.stream(values())
            .filter(PlaceholderKeys::worldScoped)
            .sorted(Comparator.comparingInt((PlaceholderKeys key) -> key.suffix.length()).reversed())
            .toArray(PlaceholderKeys[]::new);

    private final String suffix;
    private final boolean worldScoped;

    PlaceholderKeys(String suffix, boolean worldScoped) {
        this.suffix = suffix;
        this.worldScoped = worldScoped;
    }

    public String suffix() {
        return suffix;
    }

    public boolean worldScoped() {
        return worldScoped;
    }

    static PlaceholderKeys[] worldKeysLongestFirst() {
        return WORLD_KEYS_LONGEST_FIRST;
    }

    static Optional<PlaceholderKeys> fromSuffix(String suffix) {
        if (suffix == null || suffix.isBlank()) {
            return Optional.empty();
        }
        String normalized = suffix.trim().toLowerCase(Locale.ROOT);
        for (PlaceholderKeys key : values()) {
            if (key.suffix.equals(normalized)) {
                return Optional.of(key);
            }
        }
        return Optional.empty();
    }
}
