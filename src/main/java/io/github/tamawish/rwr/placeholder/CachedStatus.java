package io.github.tamawish.rwr.placeholder;

import java.util.Objects;

/** API-free view of one world's reset status. */
public record CachedStatus(
        String worldId, String worldName, String phaseName, String message, boolean active) {
    public CachedStatus {
        worldId = requireText(worldId, "worldId");
        worldName = requireText(worldName, "worldName");
        phaseName = requireText(phaseName, "phaseName");
        message = requireText(message, "message");
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
