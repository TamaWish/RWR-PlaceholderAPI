package io.github.tamawish.rwr.placeholder.listener;

import io.github.tamawish.rwr.api.event.ResourceWorldPostResetEvent;
import io.github.tamawish.rwr.api.event.ResourceWorldPreResetEvent;
import io.github.tamawish.rwr.api.event.ResourceWorldResetWarningEvent;
import io.github.tamawish.rwr.placeholder.LastOutcome;
import io.github.tamawish.rwr.placeholder.PlaceholderCache;
import io.github.tamawish.rwr.placeholder.RwrPlaceholderPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Keeps the placeholder cache current from RWR lifecycle events.
 *
 * <p>Loaded reflectively so the main plugin class can enable without RWR API classes.
 */
public final class ResetEventListener implements Listener {
    private final RwrPlaceholderPlugin plugin;

    public ResetEventListener(RwrPlaceholderPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWarning(ResourceWorldResetWarningEvent event) {
        PlaceholderCache cache = plugin.cache();
        if (cache == null || !plugin.isApiAvailable()) {
            return;
        }
        cache.recordWarning(event.getWorldId(), event.getScheduledResetAt());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPreReset(ResourceWorldPreResetEvent event) {
        PlaceholderCache cache = plugin.cache();
        if (cache == null || !plugin.isApiAvailable()) {
            return;
        }
        cache.invalidateSnapshots();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPostReset(ResourceWorldPostResetEvent event) {
        PlaceholderCache cache = plugin.cache();
        if (cache == null || !plugin.isApiAvailable()) {
            return;
        }
        String failure = event.getFailure().map(Enum::name).orElse(null);
        cache.recordOutcome(event.getWorldId(), LastOutcome.from(event.getPhase().name(), failure));
    }
}
