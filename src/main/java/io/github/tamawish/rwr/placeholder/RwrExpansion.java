package io.github.tamawish.rwr.placeholder;

import io.github.tamawish.rwr.placeholder.config.PlaceholderConfig;
import io.github.tamawish.rwr.placeholder.locale.LocaleService;
import java.util.ArrayList;
import java.util.List;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * PlaceholderAPI expansion registered under the {@code rwr} identifier.
 *
 * <p>Loaded reflectively so the main plugin class can enable without PlaceholderAPI.
 */
public final class RwrExpansion extends PlaceholderExpansion {
    private final RwrPlaceholderPlugin plugin;

    public RwrExpansion(RwrPlaceholderPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "rwr";
    }

    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        PlaceholderCache cache = plugin.cache();
        PlaceholderConfig config = plugin.placeholderConfig();
        LocaleService locale = plugin.locale();
        if (cache == null || config == null || locale == null) {
            return "";
        }
        cache.refreshIfNeeded();
        return PlaceholderParser.parse(params, cache.knownWorldIds())
                .map(parsed -> {
                    String playerWorld = player instanceof Player online ? online.getWorld().getName() : null;
                    return new PlaceholderRenderer(cache).render(parsed, config, locale, playerWorld);
                })
                .orElse(null);
    }

    @Override
    public List<String> getPlaceholders() {
        List<String> placeholders = new ArrayList<>();
        placeholders.add("%rwr_worlds%");
        placeholders.add("%rwr_world_names%");
        placeholders.add("%rwr_id%");
        placeholders.add("%rwr_name%");
        placeholders.add("%rwr_world%");
        placeholders.add("%rwr_state%");
        placeholders.add("%rwr_phase%");
        placeholders.add("%rwr_status%");
        placeholders.add("%rwr_can_reset%");
        placeholders.add("%rwr_resetting%");
        placeholders.add("%rwr_countdown%");
        placeholders.add("%rwr_next_reset%");
        placeholders.add("%rwr_last_outcome%");
        placeholders.add("%rwr_world_<id>_id%");
        placeholders.add("%rwr_world_<id>_name%");
        placeholders.add("%rwr_world_<id>_world%");
        placeholders.add("%rwr_world_<id>_state%");
        placeholders.add("%rwr_world_<id>_phase%");
        placeholders.add("%rwr_world_<id>_status%");
        placeholders.add("%rwr_world_<id>_can_reset%");
        placeholders.add("%rwr_world_<id>_resetting%");
        placeholders.add("%rwr_world_<id>_countdown%");
        placeholders.add("%rwr_world_<id>_next_reset%");
        placeholders.add("%rwr_world_<id>_last_outcome%");
        return placeholders;
    }
}
