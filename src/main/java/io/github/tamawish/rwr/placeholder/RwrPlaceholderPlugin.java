package io.github.tamawish.rwr.placeholder;

import io.github.tamawish.rwr.placeholder.command.PlaceholderCommand;
import io.github.tamawish.rwr.placeholder.config.PlaceholderConfig;
import io.github.tamawish.rwr.placeholder.locale.LocaleService;
import java.time.Clock;
import java.util.Optional;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * PlaceholderAPI add-on for ResourceWorldResetter.
 *
 * <p>Soft-depends on both RWR runtime plugin names and PlaceholderAPI. The expansion registers
 * whenever PlaceholderAPI is present; placeholders return configured fallbacks when {@code RwrApi}
 * is absent. Event listeners and snapshot access are loaded only when the API service is registered.
 */
public final class RwrPlaceholderPlugin extends JavaPlugin implements Listener {
    private static final String API_CLASS = "io.github.tamawish.rwr.api.RwrApi";
    private static final String LISTENER_CLASS =
            "io.github.tamawish.rwr.placeholder.listener.ResetEventListener";
    private static final String SOURCE_CLASS =
            "io.github.tamawish.rwr.placeholder.RwrApiSnapshotSource";
    private static final String EXPANSION_CLASS = "io.github.tamawish.rwr.placeholder.RwrExpansion";
    private static final String PLACEHOLDER_API = "PlaceholderAPI";
    private static final String[] RUNTIME_PLUGINS = {
        "ResourceWorldResetter", "ResourceWorldResetter-Paper-Folia"
    };

    private PlaceholderConfig config;
    private LocaleService locale;
    private PlaceholderCache cache;
    private Listener listener;
    private Object expansion;
    private volatile boolean apiAvailable;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        cache = new PlaceholderCache(Clock.systemUTC(), PlaceholderConfig.DEFAULT_TTL_MS);
        Optional<String> startupError = reloadServices();
        if (locale == null) {
            locale = new LocaleService(this);
            locale.reload("en_US");
        }

        PluginCommand command = getCommand("rwrplaceholder");
        if (command != null) {
            PlaceholderCommand executor = new PlaceholderCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        } else {
            getLogger().severe("Command 'rwrplaceholder' missing from plugin.yml");
        }

        getServer().getPluginManager().registerEvents(this, this);
        refreshApiAvailability();
        refreshPlaceholderApi();
        boolean papi = isPlaceholderApiAvailable();
        getLogger().info(locale.format(
                apiAvailable && papi ? "log.enabled" : "log.enabled-degraded",
                "api_state",
                apiAvailable ? "available" : "unavailable",
                "papi_state",
                papi ? "available" : "unavailable"));
        startupError.ifPresent(error -> getLogger().severe("RWR-PlaceholderAPI configuration is inactive: " + error));
        if (!apiAvailable) {
            getLogger().warning(locale.raw("log.disabled-no-api"));
        }
        if (!papi) {
            getLogger().warning(locale.raw("log.disabled-no-papi"));
        }
    }

    @Override
    public void onDisable() {
        unregisterApiListener();
        unregisterExpansion();
        if (cache != null) {
            cache.setSource(null);
        }
        HandlerList.unregisterAll((Listener) this);
        apiAvailable = false;
    }

    /**
     * Reloads config and locale from the data folder.
     *
     * @return empty on success, otherwise a short failure reason
     */
    public Optional<String> reloadServices() {
        try {
            reloadConfig();
            PlaceholderConfig candidateConfig = PlaceholderConfig.load(getConfig());
            LocaleService candidateLocale = new LocaleService(this);
            candidateLocale.reload(candidateConfig.locale());
            config = candidateConfig;
            locale = candidateLocale;
            if (cache == null) {
                cache = new PlaceholderCache(Clock.systemUTC(), candidateConfig.cacheTtlMs());
            } else {
                cache.setTtlMs(candidateConfig.cacheTtlMs());
                cache.invalidateSnapshots();
            }
            refreshApiAvailability();
            refreshPlaceholderApi();
            return Optional.empty();
        } catch (RuntimeException ex) {
            String detail = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            getLogger().severe("Failed to reload RWR-PlaceholderAPI: " + detail);
            return Optional.of(detail);
        }
    }

    /** Re-checks whether the public RWR API service is registered. */
    public void refreshApiAvailability() {
        boolean present = findApiService();
        apiAvailable = present;
        if (cache == null) {
            return;
        }
        if (present) {
            SnapshotSource snapshotSource = loadSnapshotSource();
            cache.setSource(snapshotSource);
            if (snapshotSource != null) {
                registerApiListener();
            } else {
                unregisterApiListener();
            }
        } else {
            cache.setSource(null);
            unregisterApiListener();
        }
    }

    /** Registers or unregisters the PlaceholderAPI expansion based on plugin presence. */
    public void refreshPlaceholderApi() {
        Plugin papi = getServer().getPluginManager().getPlugin(PLACEHOLDER_API);
        if (papi != null && papi.isEnabled()) {
            registerExpansion();
        } else {
            unregisterExpansion();
        }
    }

    public boolean isApiAvailable() {
        return apiAvailable;
    }

    public boolean isPlaceholderApiAvailable() {
        Plugin papi = getServer().getPluginManager().getPlugin(PLACEHOLDER_API);
        return papi != null && papi.isEnabled();
    }

    public boolean isExpansionRegistered() {
        return expansion != null;
    }

    public PlaceholderConfig placeholderConfig() {
        return config;
    }

    public LocaleService locale() {
        return locale;
    }

    public PlaceholderCache cache() {
        return cache;
    }

    @EventHandler
    public void onPluginEnabled(PluginEnableEvent event) {
        String name = event.getPlugin().getName();
        if (isRuntimePlugin(name)) {
            refreshApiAvailability();
        }
        if (PLACEHOLDER_API.equals(name)) {
            refreshPlaceholderApi();
        }
    }

    @EventHandler
    public void onPluginDisabled(PluginDisableEvent event) {
        String name = event.getPlugin().getName();
        if (isRuntimePlugin(name)) {
            refreshApiAvailability();
        }
        if (PLACEHOLDER_API.equals(name)) {
            refreshPlaceholderApi();
        }
    }

    private boolean findApiService() {
        for (String pluginName : RUNTIME_PLUGINS) {
            Plugin runtime = getServer().getPluginManager().getPlugin(pluginName);
            if (runtime == null || !runtime.isEnabled()) {
                continue;
            }
            try {
                Class<?> apiType = Class.forName(API_CLASS, false, runtime.getClass().getClassLoader());
                if (hasRegistration(apiType)) {
                    return true;
                }
            } catch (ClassNotFoundException | LinkageError | SecurityException ex) {
                getLogger().fine("RWR runtime does not expose the 5.1 API: " + ex.getMessage());
            }
        }
        return false;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean hasRegistration(Class<?> apiType) {
        RegisteredServiceProvider<?> registration =
                getServer().getServicesManager().getRegistration((Class) apiType);
        return registration != null;
    }

    private SnapshotSource loadSnapshotSource() {
        try {
            Class<?> sourceType = Class.forName(SOURCE_CLASS, true, getClassLoader());
            return (SnapshotSource) sourceType.getConstructor(Plugin.class).newInstance(this);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            getLogger().warning("RWR API snapshot classes are unavailable; placeholders will use fallbacks.");
            apiAvailable = false;
            return null;
        }
    }

    private void registerApiListener() {
        if (listener != null) {
            return;
        }
        try {
            Class<?> listenerType = Class.forName(LISTENER_CLASS, true, getClassLoader());
            listener = (Listener) listenerType.getConstructor(RwrPlaceholderPlugin.class).newInstance(this);
            getServer().getPluginManager().registerEvents(listener, this);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            listener = null;
            // Snapshot placeholders remain usable even if the optional event overlay
            // cannot be registered. Do not report the whole API as unavailable.
            getLogger().warning(
                    "RWR API snapshots are available, but event cache overlays remain disabled: "
                            + error.getClass().getSimpleName());
        }
    }

    private void unregisterApiListener() {
        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }
    }

    private void registerExpansion() {
        if (expansion != null) {
            return;
        }
        try {
            Class<?> expansionType = Class.forName(EXPANSION_CLASS, true, getClassLoader());
            Object instance = expansionType.getConstructor(RwrPlaceholderPlugin.class).newInstance(this);
            Object registered = expansionType.getMethod("register").invoke(instance);
            if (Boolean.TRUE.equals(registered)) {
                expansion = instance;
            } else {
                getLogger().warning("PlaceholderAPI rejected the rwr expansion.");
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            expansion = null;
            getLogger().warning("PlaceholderAPI expansion could not be registered: " + error.getMessage());
        }
    }

    private void unregisterExpansion() {
        if (expansion == null) {
            return;
        }
        try {
            expansion.getClass().getMethod("unregister").invoke(expansion);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            // PlaceholderAPI may already have unloaded the expansion.
        }
        expansion = null;
    }

    private static boolean isRuntimePlugin(String name) {
        for (String runtime : RUNTIME_PLUGINS) {
            if (runtime.equals(name)) {
                return true;
            }
        }
        return false;
    }
}
