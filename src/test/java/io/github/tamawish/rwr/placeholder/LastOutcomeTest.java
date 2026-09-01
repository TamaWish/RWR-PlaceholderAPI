package io.github.tamawish.rwr.placeholder;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LastOutcomeTest {
    @Test
    void mapsCompleteToSuccess() {
        assertThat(LastOutcome.from("COMPLETE", null)).isEqualTo(LastOutcome.SUCCESS);
        assertThat(LastOutcome.from("complete", "")).isEqualTo(LastOutcome.SUCCESS);
    }

    @Test
    void mapsEventCancelledToCancelled() {
        assertThat(LastOutcome.from("FAILED", "EVENT_CANCELLED")).isEqualTo(LastOutcome.CANCELLED);
    }

    @Test
    void mapsOtherFailuresToFailed() {
        assertThat(LastOutcome.from("FAILED", "EVACUATION_FAILED")).isEqualTo(LastOutcome.FAILED);
        assertThat(LastOutcome.from("FAILED", null)).isEqualTo(LastOutcome.FAILED);
    }

    @Test
    void mapsInterrupted() {
        assertThat(LastOutcome.from("INTERRUPTED", "INTERRUPTED_OPERATION"))
                .isEqualTo(LastOutcome.INTERRUPTED);
    }

    @Test
    void localeKeyUsesLowercaseOutcome() {
        assertThat(LastOutcome.CANCELLED.localeKey()).isEqualTo("outcome-cancelled");
    }
}
