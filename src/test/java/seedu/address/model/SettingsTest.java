package seedu.address.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import seedu.address.model.Settings.Key;

public class SettingsTest {

    @Test
    public void constructor_default_behavesAsWithoutSettings() {
        Settings settings = new Settings();
        assertTrue(settings.isMarkStoppingClashes());
        assertFalse(settings.isMarkCountedAsDone());
        assertTrue(settings.isPastClashing());
        assertFalse(settings.isArchivedClashing());
        assertEquals(Settings.DEFAULT, settings);
    }

    @Test
    public void with_eachKey_changesOnlyThatSetting() {
        for (Key key : Key.values()) {
            Settings changed = Settings.DEFAULT.with(key, !Settings.DEFAULT.isOn(key));
            assertEquals(!Settings.DEFAULT.isOn(key), changed.isOn(key));
            for (Key other : Key.values()) {
                if (other != key) {
                    assertEquals(Settings.DEFAULT.isOn(other), changed.isOn(other));
                }
            }
            assertNotEquals(Settings.DEFAULT, changed);
            assertEquals(Settings.DEFAULT, changed.with(key, Settings.DEFAULT.isOn(key)));
        }
        assertThrows(NullPointerException.class, () -> Settings.DEFAULT.with(null, true));
    }

    @Test
    public void keyOf_word_findsTheSetting() {
        assertEquals(Optional.of(Key.PAST_CLASHES), Key.of("past-clash"));
        assertEquals(Optional.of(Key.MARK_STOPS_CLASHES), Key.of("mark-noclash"));
        assertEquals(Optional.empty(), Key.of("Past-Clash"));
        assertEquals(Optional.empty(), Key.of("unknown"));
    }

    @Test
    public void equalsAndHashCode() {
        Settings settings = new Settings(false, true, false, true);
        assertEquals(settings, new Settings(false, true, false, true));
        assertEquals(settings.hashCode(), new Settings(false, true, false, true).hashCode());
        assertNotEquals(settings, null);
        assertNotEquals(settings, Settings.DEFAULT);
    }
}
