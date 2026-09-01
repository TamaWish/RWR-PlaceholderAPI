package io.github.tamawish.rwr.placeholder;

import io.github.tamawish.rwr.api.RwrApi;
import io.github.tamawish.rwr.api.model.ManagedWorldSnapshot;
import io.github.tamawish.rwr.api.model.ResetStatusSnapshot;
import java.util.List;
import java.util.Optional;
import org.bukkit.plugin.Plugin;

/**
 * Snapshot source backed by the public {@link RwrApi} service.
 *
 * <p>Loaded reflectively so the main plugin class can enable without RWR on the classpath.
 */
public final class RwrApiSnapshotSource implements SnapshotSource {
    private final Plugin plugin;

    public RwrApiSnapshotSource(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<CachedWorld> managedWorlds() {
        return RwrApi.find(plugin.getServer())
                .map(api -> api.managedWorlds().stream().map(RwrApiSnapshotSource::world).toList())
                .orElse(List.of());
    }

    @Override
    public Optional<CachedStatus> resetStatus(String worldId) {
        return RwrApi.find(plugin.getServer()).flatMap(api -> api.resetStatus(worldId)).map(RwrApiSnapshotSource::status);
    }

    private static CachedWorld world(ManagedWorldSnapshot snapshot) {
        return new CachedWorld(
                snapshot.id(),
                snapshot.worldName(),
                snapshot.displayName(),
                snapshot.state().name(),
                snapshot.resetCapable());
    }

    private static CachedStatus status(ResetStatusSnapshot snapshot) {
        return new CachedStatus(
                snapshot.worldId(),
                snapshot.worldName(),
                snapshot.phase().name(),
                snapshot.message(),
                snapshot.isActive());
    }
}
