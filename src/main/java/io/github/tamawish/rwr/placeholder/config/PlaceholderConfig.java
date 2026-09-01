package io.github.tamawish.rwr.placeholder.config;

import java.time.DateTimeException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import org.bukkit.configuration.file.FileConfiguration;

/** Immutable snapshot of RWR-PlaceholderAPI settings. */
public final class PlaceholderConfig {
    public static final int DEFAULT_TTL_MS = 1_500;
    public static final int MIN_TTL_MS = 250;
    public static final int MAX_TTL_MS = 10_000;
    public static final String DEFAULT_DATETIME = "yyyy-MM-dd HH:mm:ss 'UTC'";

    private final String locale;
    private final int cacheTtlMs;
    private final DateTimeFormatter datetimeFormatter;
    private final String worldsSeparator;
    private final String trueValue;
    private final String falseValue;
    private final String fallback;
    private final String noApiFallback;

    private PlaceholderConfig(
            String locale,
            int cacheTtlMs,
            DateTimeFormatter datetimeFormatter,
            String worldsSeparator,
            String trueValue,
            String falseValue,
            String fallback,
            String noApiFallback) {
        this.locale = locale;
        this.cacheTtlMs = cacheTtlMs;
        this.datetimeFormatter = datetimeFormatter;
        this.worldsSeparator = worldsSeparator;
        this.trueValue = trueValue;
        this.falseValue = falseValue;
        this.fallback = fallback;
        this.noApiFallback = noApiFallback;
    }

    /** Loads and validates configuration from the plugin config file. */
    public static PlaceholderConfig load(FileConfiguration yaml) {
        Objects.requireNonNull(yaml, "yaml");
        String locale = normalizeLocale(yaml.getString("locale", "en_US"));
        int ttl = clamp(yaml.getInt("cache-ttl-ms", DEFAULT_TTL_MS), MIN_TTL_MS, MAX_TTL_MS);
        DateTimeFormatter formatter = parseFormatter(yaml.getString("datetime-format", DEFAULT_DATETIME));
        String separator = yaml.getString("worlds-separator", ", ");
        if (separator == null) {
            separator = ", ";
        }
        String trueValue = nullToDefault(yaml.getString("boolean.true"), "true");
        String falseValue = nullToDefault(yaml.getString("boolean.false"), "false");
        String fallback = yaml.getString("fallback", "");
        if (fallback == null) {
            fallback = "";
        }
        String noApi = yaml.getString("no-api-fallback", fallback);
        if (noApi == null) {
            noApi = fallback;
        }
        return new PlaceholderConfig(
                locale, ttl, formatter, separator, trueValue, falseValue, fallback, noApi);
    }

    private static DateTimeFormatter parseFormatter(String pattern) {
        String raw = pattern == null || pattern.isBlank() ? DEFAULT_DATETIME : pattern;
        try {
            return DateTimeFormatter.ofPattern(raw).withZone(ZoneOffset.UTC);
        } catch (IllegalArgumentException | DateTimeException ex) {
            return DateTimeFormatter.ofPattern(DEFAULT_DATETIME).withZone(ZoneOffset.UTC);
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String normalizeLocale(String raw) {
        if (raw == null || raw.isBlank()) {
            return "en_US";
        }
        String cleaned = raw.trim().replace('-', '_');
        int underscore = cleaned.indexOf('_');
        if (underscore > 0 && underscore < cleaned.length() - 1) {
            return cleaned.substring(0, underscore).toLowerCase(Locale.ROOT)
                    + "_"
                    + cleaned.substring(underscore + 1).toUpperCase(Locale.ROOT);
        }
        return cleaned;
    }

    private static String nullToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public String locale() {
        return locale;
    }

    public int cacheTtlMs() {
        return cacheTtlMs;
    }

    public DateTimeFormatter datetimeFormatter() {
        return datetimeFormatter;
    }

    public String worldsSeparator() {
        return worldsSeparator;
    }

    public String trueValue() {
        return trueValue;
    }

    public String falseValue() {
        return falseValue;
    }

    public String fallback() {
        return fallback;
    }

    public String noApiFallback() {
        return noApiFallback;
    }
}
