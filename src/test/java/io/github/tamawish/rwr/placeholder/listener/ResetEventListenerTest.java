package io.github.tamawish.rwr.placeholder.listener;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.tamawish.rwr.api.event.ResourceWorldPostResetEvent;
import io.github.tamawish.rwr.api.event.ResourceWorldResetWarningEvent;
import io.github.tamawish.rwr.api.model.FailureSafety;
import io.github.tamawish.rwr.api.model.ResetFailureType;
import io.github.tamawish.rwr.api.model.ResetPhase;
import io.github.tamawish.rwr.placeholder.LastOutcome;
import io.github.tamawish.rwr.placeholder.PlaceholderCache;
import io.github.tamawish.rwr.placeholder.RwrPlaceholderPlugin;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ResetEventListenerTest {
    @Test
    void warningStoresScheduledInstant() {
        RwrPlaceholderPlugin plugin = mock(RwrPlaceholderPlugin.class);
        PlaceholderCache cache = mock(PlaceholderCache.class);
        Instant scheduled = Instant.parse("2026-08-31T12:00:00Z");
        when(plugin.cache()).thenReturn(cache);
        when(plugin.isApiAvailable()).thenReturn(true);

        new ResetEventListener(plugin)
                .onWarning(new ResourceWorldResetWarningEvent("resource", "resource_world", 5, scheduled));

        verify(cache).recordWarning("resource", scheduled);
    }

    @Test
    void postResetMapsCancelledFailure() {
        RwrPlaceholderPlugin plugin = mock(RwrPlaceholderPlugin.class);
        PlaceholderCache cache = mock(PlaceholderCache.class);
        when(plugin.cache()).thenReturn(cache);
        when(plugin.isApiAvailable()).thenReturn(true);

        new ResetEventListener(plugin)
                .onPostReset(new ResourceWorldPostResetEvent(
                        "op-1",
                        "resource",
                        "resource_world",
                        ResetPhase.FAILED,
                        ResetFailureType.EVENT_CANCELLED,
                        FailureSafety.SAFE_TO_RETRY,
                        "Cancelled by listener"));

        verify(cache).recordOutcome("resource", LastOutcome.CANCELLED);
    }
}
