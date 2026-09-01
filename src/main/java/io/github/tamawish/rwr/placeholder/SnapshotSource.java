package io.github.tamawish.rwr.placeholder;

import java.util.List;
import java.util.Optional;

/** Read-only RWR snapshot access used by the placeholder cache. */
public interface SnapshotSource {
    List<CachedWorld> managedWorlds();

    Optional<CachedStatus> resetStatus(String worldId);
}
