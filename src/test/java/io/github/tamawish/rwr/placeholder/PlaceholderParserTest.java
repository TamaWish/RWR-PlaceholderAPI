package io.github.tamawish.rwr.placeholder;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PlaceholderParserTest {
    @Test
    void parsesGlobalWorldsList() {
        Optional<PlaceholderParser.ParsedPlaceholder> parsed =
                PlaceholderParser.parse("worlds", List.of("resource"));
        assertThat(parsed).hasValueSatisfying(value -> {
            assertThat(value.key()).isEqualTo(PlaceholderKeys.WORLDS);
            assertThat(value.worldId()).isEmpty();
            assertThat(value.playerWorld()).isFalse();
        });
    }

    @Test
    void prefersLongestKnownWorldId() {
        List<String> ids = List.of("resource", "resource_nether");
        Optional<PlaceholderParser.ParsedPlaceholder> parsed =
                PlaceholderParser.parse("world_resource_nether_phase", ids);
        assertThat(parsed).hasValueSatisfying(value -> {
            assertThat(value.key()).isEqualTo(PlaceholderKeys.PHASE);
            assertThat(value.worldId()).contains("resource_nether");
        });
    }

    @Test
    void stillMatchesShorterSiblingId() {
        List<String> ids = List.of("resource", "resource_nether");
        Optional<PlaceholderParser.ParsedPlaceholder> parsed =
                PlaceholderParser.parse("world_resource_phase", ids);
        assertThat(parsed).hasValueSatisfying(value -> {
            assertThat(value.key()).isEqualTo(PlaceholderKeys.PHASE);
            assertThat(value.worldId()).contains("resource");
        });
    }

    @Test
    void matchesMultiWordSuffixesBeforeShorterOnes() {
        Optional<PlaceholderParser.ParsedPlaceholder> parsed =
                PlaceholderParser.parse("world_resource_last_outcome", List.of("resource"));
        assertThat(parsed).hasValueSatisfying(value -> {
            assertThat(value.key()).isEqualTo(PlaceholderKeys.LAST_OUTCOME);
            assertThat(value.worldId()).contains("resource");
        });
    }

    @Test
    void parsesUnknownWorldIdFromSuffix() {
        Optional<PlaceholderParser.ParsedPlaceholder> parsed =
                PlaceholderParser.parse("world_mining_can_reset", List.of());
        assertThat(parsed).hasValueSatisfying(value -> {
            assertThat(value.key()).isEqualTo(PlaceholderKeys.CAN_RESET);
            assertThat(value.worldId()).contains("mining");
        });
    }

    @Test
    void parsesPlayerConvenienceKeys() {
        Optional<PlaceholderParser.ParsedPlaceholder> parsed =
                PlaceholderParser.parse("countdown", List.of("resource"));
        assertThat(parsed).hasValueSatisfying(value -> {
            assertThat(value.key()).isEqualTo(PlaceholderKeys.COUNTDOWN);
            assertThat(value.playerWorld()).isTrue();
            assertThat(value.worldId()).isEmpty();
        });
    }

    @Test
    void isCaseInsensitive() {
        Optional<PlaceholderParser.ParsedPlaceholder> parsed =
                PlaceholderParser.parse("WORLD_RESOURCE_NAME", List.of("resource"));
        assertThat(parsed).hasValueSatisfying(value -> {
            assertThat(value.key()).isEqualTo(PlaceholderKeys.NAME);
            assertThat(value.worldId()).contains("resource");
        });
    }

    @Test
    void rejectsBlankAndUnknownIdentifiers() {
        assertThat(PlaceholderParser.parse("", List.of())).isEmpty();
        assertThat(PlaceholderParser.parse("world_resource", List.of("resource"))).isEmpty();
        assertThat(PlaceholderParser.parse("nope", List.of())).isEmpty();
    }
}
