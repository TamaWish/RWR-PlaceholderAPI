package io.github.tamawish.rwr.placeholder;

import io.github.tamawish.rwr.placeholder.config.PlaceholderConfig;
import io.github.tamawish.rwr.placeholder.locale.LocaleService;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/** Maps a parsed placeholder request onto cached RWR state. */
public final class PlaceholderRenderer {
    private final PlaceholderCache cache;

    public PlaceholderRenderer(PlaceholderCache cache) {
        this.cache = Objects.requireNonNull(cache, "cache");
    }

    /**
     * Renders a parsed placeholder.
     *
     * @param parsed parsed identifier
     * @param config formatting settings
     * @param locale locale labels
     * @param playerWorldName Bukkit world name of the requesting player, or {@code null}
     * @return formatted value, never {@code null}
     */
    public String render(
            PlaceholderParser.ParsedPlaceholder parsed,
            PlaceholderConfig config,
            LocaleService locale,
            String playerWorldName) {
        Objects.requireNonNull(parsed, "parsed");
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(locale, "locale");
        if (!cache.apiAvailable()) {
            return config.noApiFallback();
        }

        if (!parsed.key().worldScoped()) {
            return renderWorldList(parsed.key(), config);
        }

        Optional<CachedWorld> world = resolveWorld(parsed, playerWorldName);
        if (world.isEmpty()) {
            return config.fallback();
        }
        CachedWorld resolved = world.get();
        Optional<CachedStatus> status = cache.status(resolved.id());
        return switch (parsed.key()) {
            case WORLDS -> cache.refreshIfNeeded().worlds().stream()
                    .map(CachedWorld::id)
                    .collect(Collectors.joining(config.worldsSeparator()));
            case WORLD_NAMES -> cache.refreshIfNeeded().worlds().stream()
                    .map(CachedWorld::displayName)
                    .collect(Collectors.joining(config.worldsSeparator()));
            case ID -> resolved.id();
            case NAME -> resolved.displayName();
            case WORLD -> resolved.worldName();
            case STATE -> label(locale, "state-" + resolved.stateName(), resolved.stateName());
            case PHASE -> {
                String phase = status.map(CachedStatus::phaseName).orElse("IDLE");
                yield label(locale, "phase-" + phase.toUpperCase(Locale.ROOT), phase.toLowerCase(Locale.ROOT));
            }
            case STATUS -> status.map(CachedStatus::message).orElse(config.fallback());
            case CAN_RESET -> bool(config, resolved.resetCapable());
            case RESETTING -> bool(config, status.map(CachedStatus::active).orElse(false));
            case COUNTDOWN -> {
                if (!cache.hasCountdown(resolved.id())) {
                    yield config.fallback();
                }
                yield Long.toString(cache.countdownSeconds(resolved.id()));
            }
            case NEXT_RESET -> cache.scheduledResetAt(resolved.id())
                    .map(instant -> formatInstant(config, instant))
                    .orElse(config.fallback());
            case LAST_OUTCOME -> cache.lastOutcome(resolved.id())
                    .map(outcome -> label(locale, outcome.localeKey(), outcome.name().toLowerCase(Locale.ROOT)))
                    .orElse(config.fallback());
        };
    }

    private String renderWorldList(PlaceholderKeys key, PlaceholderConfig config) {
        return switch (key) {
            case WORLDS -> cache.refreshIfNeeded().worlds().stream()
                    .map(CachedWorld::id)
                    .collect(Collectors.joining(config.worldsSeparator()));
            case WORLD_NAMES -> cache.refreshIfNeeded().worlds().stream()
                    .map(CachedWorld::displayName)
                    .collect(Collectors.joining(config.worldsSeparator()));
            default -> config.fallback();
        };
    }

    private Optional<CachedWorld> resolveWorld(PlaceholderParser.ParsedPlaceholder parsed, String playerWorldName) {
        if (parsed.playerWorld()) {
            return cache.worldByBukkitName(playerWorldName);
        }
        return parsed.worldId().flatMap(cache::world);
    }

    private static String bool(PlaceholderConfig config, boolean value) {
        return value ? config.trueValue() : config.falseValue();
    }

    private static String label(LocaleService locale, String key, String fallback) {
        String mapped = locale.raw(key);
        return mapped.isEmpty() ? fallback : mapped;
    }

    private static String formatInstant(PlaceholderConfig config, Instant instant) {
        return config.datetimeFormatter().format(instant);
    }
}
