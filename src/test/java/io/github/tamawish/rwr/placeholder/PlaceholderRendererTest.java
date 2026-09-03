package io.github.tamawish.rwr.placeholder;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.tamawish.rwr.placeholder.config.PlaceholderConfig;
import io.github.tamawish.rwr.placeholder.locale.LocaleService;
import java.time.Instant;
import java.time.InstantSource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class PlaceholderRendererTest {
    @Test
    void rendersSnapshotFieldsAndFallbacks() {
        PlaceholderCache cache = cacheWith(Instant.parse("2026-08-31T00:00:00Z"));
        PlaceholderConfig config = PlaceholderConfig.load(new YamlConfiguration());
        LocaleService locale = new LocaleService(localeMap());
        PlaceholderRenderer renderer = new PlaceholderRenderer(cache);

        assertThat(render(renderer, config, locale, "worlds")).isEqualTo("resource, resource_nether");
        assertThat(render(renderer, config, locale, "world_names")).isEqualTo("Resource, Nether");
        assertThat(render(renderer, config, locale, "world_resource_name")).isEqualTo("Resource");
        assertThat(render(renderer, config, locale, "world_resource_world")).isEqualTo("resource_world");
        assertThat(render(renderer, config, locale, "world_resource_state")).isEqualTo("managed");
        assertThat(render(renderer, config, locale, "world_resource_phase")).isEqualTo("evacuate");
        assertThat(render(renderer, config, locale, "world_resource_status")).isEqualTo("Moving players");
        assertThat(render(renderer, config, locale, "world_resource_can_reset")).isEqualTo("true");
        assertThat(render(renderer, config, locale, "world_resource_resetting")).isEqualTo("true");
        assertThat(render(renderer, config, locale, "world_unknown_name")).isEqualTo("");
    }

    @Test
    void rendersEventOverlaysAndPlayerWorld() {
        Instant now = Instant.parse("2026-08-31T00:00:00Z");
        PlaceholderCache cache = cacheWith(now);
        cache.recordWarning("resource", Instant.parse("2026-08-31T00:05:00Z"));
        cache.recordOutcome("resource_nether", LastOutcome.CANCELLED);
        PlaceholderConfig config = PlaceholderConfig.load(new YamlConfiguration());
        LocaleService locale = new LocaleService(localeMap());
        PlaceholderRenderer renderer = new PlaceholderRenderer(cache);

        assertThat(render(renderer, config, locale, "world_resource_countdown")).isEqualTo("300");
        assertThat(render(renderer, config, locale, "world_resource_next_reset"))
                .isEqualTo("2026-08-31 00:05:00 UTC");
        assertThat(render(renderer, config, locale, "world_resource_nether_last_outcome"))
                .isEqualTo("cancelled");
        assertThat(render(renderer, config, locale, "world_resource_last_outcome")).isEqualTo("");

        PlaceholderParser.ParsedPlaceholder playerPhase =
                PlaceholderParser.parse("phase", cache.knownWorldIds()).orElseThrow();
        assertThat(renderer.render(playerPhase, config, locale, "resource_world")).isEqualTo("evacuate");
        assertThat(renderer.render(playerPhase, config, locale, "lobby")).isEqualTo("");
    }

    @Test
    void returnsNoApiFallbackWhenServiceMissing() {
        PlaceholderCache cache = new PlaceholderCache(InstantSource.system(), 1_500);
        PlaceholderConfig config = PlaceholderConfig.load(new YamlConfiguration());
        LocaleService locale = new LocaleService(localeMap());
        PlaceholderRenderer renderer = new PlaceholderRenderer(cache);
        assertThat(render(renderer, config, locale, "worlds")).isEqualTo("");
    }

    private static String render(
            PlaceholderRenderer renderer, PlaceholderConfig config, LocaleService locale, String params) {
        PlaceholderParser.ParsedPlaceholder parsed =
                PlaceholderParser.parse(params, List.of("resource", "resource_nether")).orElseThrow();
        return renderer.render(parsed, config, locale, null);
    }

    private static PlaceholderCache cacheWith(Instant now) {
        PlaceholderCache cache = new PlaceholderCache(() -> now, 1_500);
        cache.setSource(new SnapshotSource() {
            @Override
            public List<CachedWorld> managedWorlds() {
                return List.of(
                        new CachedWorld("resource", "resource_world", "Resource", "MANAGED", true),
                        new CachedWorld("resource_nether", "resource_nether", "Nether", "MANAGED", false));
            }

            @Override
            public Optional<CachedStatus> resetStatus(String worldId) {
                if ("resource".equalsIgnoreCase(worldId)) {
                    return Optional.of(new CachedStatus(
                            "resource", "resource_world", "EVACUATE", "Moving players", true));
                }
                return Optional.of(new CachedStatus(worldId, worldId, "IDLE", "Idle", false));
            }
        });
        return cache;
    }

    private static Map<String, String> localeMap() {
        Map<String, String> strings = new LinkedHashMap<>();
        strings.put("state-MANAGED", "managed");
        strings.put("phase-EVACUATE", "evacuate");
        strings.put("outcome-cancelled", "cancelled");
        return strings;
    }
}
