package io.github.tamawish.rwr.placeholder.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class PlaceholderConfigTest {
    @Test
    void loadAppliesDefaultsAndNormalizesLocale() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("locale", "en-us");
        PlaceholderConfig config = PlaceholderConfig.load(yaml);
        assertThat(config.locale()).isEqualTo("en_US");
        assertThat(config.cacheTtlMs()).isEqualTo(PlaceholderConfig.DEFAULT_TTL_MS);
        assertThat(config.worldsSeparator()).isEqualTo(", ");
        assertThat(config.trueValue()).isEqualTo("true");
        assertThat(config.falseValue()).isEqualTo("false");
        assertThat(config.fallback()).isEmpty();
        assertThat(config.noApiFallback()).isEmpty();
        assertThat(config.datetimeFormatter().format(Instant.parse("2026-08-31T12:00:00Z")))
                .isEqualTo("2026-08-31 12:00:00 UTC");
    }

    @Test
    void clampsTtlAndFallsBackOnBadDatetimePattern() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("cache-ttl-ms", 50);
        yaml.set("datetime-format", "[not a pattern");
        yaml.set("boolean.true", "yes");
        yaml.set("fallback", "n/a");
        PlaceholderConfig config = PlaceholderConfig.load(yaml);
        assertThat(config.cacheTtlMs()).isEqualTo(PlaceholderConfig.MIN_TTL_MS);
        assertThat(config.trueValue()).isEqualTo("yes");
        assertThat(config.fallback()).isEqualTo("n/a");
        assertThat(config.noApiFallback()).isEqualTo("n/a");
        assertThat(config.datetimeFormatter().format(Instant.parse("2026-08-31T00:00:00Z")))
                .isEqualTo("2026-08-31 00:00:00 UTC");
    }

    @Test
    void respectsExplicitNoApiFallback() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("fallback", "-");
        yaml.set("no-api-fallback", "offline");
        yaml.set("cache-ttl-ms", 20_000);
        PlaceholderConfig config = PlaceholderConfig.load(yaml);
        assertThat(config.fallback()).isEqualTo("-");
        assertThat(config.noApiFallback()).isEqualTo("offline");
        assertThat(config.cacheTtlMs()).isEqualTo(PlaceholderConfig.MAX_TTL_MS);
    }
}
