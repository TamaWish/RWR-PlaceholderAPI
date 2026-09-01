package io.github.tamawish.rwr.placeholder;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Parses PlaceholderAPI params after the {@code rwr_} identifier.
 *
 * <p>World IDs may contain underscores. Parsing matches known IDs longest-first, then falls
 * back to a suffix-from-end parse for unknown IDs.
 */
public final class PlaceholderParser {
    private PlaceholderParser() {}

    /**
     * Parses a params string such as {@code world_resource_nether_phase} or {@code countdown}.
     *
     * @param params PlaceholderAPI params (everything after {@code %rwr_})
     * @param knownWorldIds canonical RWR world IDs currently in cache
     * @return parsed key and optional world ID, or empty when the identifier is not recognized
     */
    public static Optional<ParsedPlaceholder> parse(String params, Collection<String> knownWorldIds) {
        if (params == null || params.isBlank()) {
            return Optional.empty();
        }
        String raw = params.trim();
        if (equalsIgnoreCase(raw, "worlds")) {
            return Optional.of(ParsedPlaceholder.global(PlaceholderKeys.WORLDS));
        }

        Optional<PlaceholderKeys> playerKey = PlaceholderKeys.fromSuffix(raw);
        if (playerKey.isPresent() && playerKey.get().worldScoped()) {
            return Optional.of(ParsedPlaceholder.playerWorld(playerKey.get()));
        }

        if (!startsWithIgnoreCase(raw, "world_")) {
            return Optional.empty();
        }
        String rest = raw.substring("world_".length());
        if (rest.isEmpty()) {
            return Optional.empty();
        }

        List<String> ids = knownWorldIds == null
                ? List.of()
                : knownWorldIds.stream()
                        .filter(Objects::nonNull)
                        .filter(id -> !id.isBlank())
                        .sorted(Comparator.comparingInt(String::length).reversed())
                        .toList();
        for (String id : ids) {
            String prefix = id + "_";
            if (startsWithIgnoreCase(rest, prefix)) {
                String suffix = rest.substring(id.length() + 1);
                Optional<PlaceholderKeys> key = PlaceholderKeys.fromSuffix(suffix);
                if (key.isPresent()) {
                    return Optional.of(ParsedPlaceholder.world(key.get(), id));
                }
            }
        }

        for (PlaceholderKeys key : PlaceholderKeys.worldKeysLongestFirst()) {
            String marker = "_" + key.suffix();
            if (endsWithIgnoreCase(rest, marker)) {
                String id = rest.substring(0, rest.length() - marker.length());
                if (!id.isBlank()) {
                    return Optional.of(ParsedPlaceholder.world(key, id));
                }
            }
        }
        return Optional.empty();
    }

    private static boolean equalsIgnoreCase(String value, String expected) {
        return value.equalsIgnoreCase(expected);
    }

    private static boolean startsWithIgnoreCase(String value, String prefix) {
        return value.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    private static boolean endsWithIgnoreCase(String value, String suffix) {
        if (value.length() < suffix.length()) {
            return false;
        }
        return value.regionMatches(true, value.length() - suffix.length(), suffix, 0, suffix.length());
    }

    /** Parsed placeholder request. */
    public record ParsedPlaceholder(PlaceholderKeys key, Optional<String> worldId, boolean playerWorld) {
        public ParsedPlaceholder {
            Objects.requireNonNull(key, "key");
            worldId = worldId == null ? Optional.empty() : worldId.map(id -> id.toLowerCase(Locale.ROOT));
        }

        static ParsedPlaceholder global(PlaceholderKeys key) {
            return new ParsedPlaceholder(key, Optional.empty(), false);
        }

        static ParsedPlaceholder world(PlaceholderKeys key, String worldId) {
            return new ParsedPlaceholder(key, Optional.of(worldId), false);
        }

        static ParsedPlaceholder playerWorld(PlaceholderKeys key) {
            return new ParsedPlaceholder(key, Optional.empty(), true);
        }
    }
}
