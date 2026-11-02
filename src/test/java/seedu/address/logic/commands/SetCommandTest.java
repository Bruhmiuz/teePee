package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import seedu.address.commons.core.FeatureFlags;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.Settings;
import seedu.address.model.Settings.Key;

public class SetCommandTest {

    private final Model model = new ModelManager();

    @AfterEach
    public void tearDown() {
        FeatureFlags.setSettingsEnabled(false);
    }

    @Test
    public void execute_noSetting_listsEverySetting() {
        String feedback = new SetCommand().execute(model).getFeedbackToUser();
        assertEquals(String.format(SetCommand.MESSAGE_SETTINGS, "mark-noclash: on\nmark-past: off\n"
                + "past-clash: on\narchived-clash: off"), feedback);
    }

    @Test
    public void execute_setting_savedAndAppliesOnlyOnceReleased() {
        CommandResult result = new SetCommand(Key.PAST_CLASHES, false).execute(model);
        assertEquals(String.format(SetCommand.MESSAGE_SET_SUCCESS, "past-clash", "off"), result.getFeedbackToUser());
        assertFalse(model.getUserPrefs().getSettings().isPastClashing());
        // the user's settings are kept, but the default ones apply while the settings are not released
        assertEquals(Settings.DEFAULT, model.getSettings().getValue());

        FeatureFlags.setSettingsEnabled(true);
        new SetCommand(Key.ARCHIVED_CLASHES, true).execute(model);
        assertFalse(model.getSettings().getValue().isPastClashing());
        assertTrue(model.getSettings().getValue().isArchivedClashing());
    }

    @Test
    public void equals() {
        SetCommand pastOff = new SetCommand(Key.PAST_CLASHES, false);
        assertEquals(pastOff, new SetCommand(Key.PAST_CLASHES, false));
        assertNotEquals(pastOff, new SetCommand(Key.PAST_CLASHES, true));
        assertNotEquals(pastOff, new SetCommand(Key.ARCHIVED_CLASHES, false));
        assertNotEquals(pastOff, new SetCommand());
        assertNotEquals(pastOff, null);
        assertEquals(new SetCommand(), new SetCommand());
    }
}
