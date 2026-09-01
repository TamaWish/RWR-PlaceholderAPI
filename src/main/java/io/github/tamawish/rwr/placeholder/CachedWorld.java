package io.github.tamawish.rwr.placeholder;

import java.util.Objects;

/** API-free view of one managed world. */
public record CachedWorld(
        String id, String worldName, String displayName, String stateName, boolean resetCapable) {
    public CachedWorld {
        id = requireText(id, "id");
        worldName = requireText(worldName, "worldName");
        displayName = requireText(displayName, "displayName");
        stateName = requireText(stateName, "stateName");
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
