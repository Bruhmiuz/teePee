package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import seedu.address.commons.util.ToStringBuilder;
import seedu.address.model.Model;
import seedu.address.model.Settings;
import seedu.address.model.Settings.Key;

/**
 * Shows the settings that decide how marks, past clashes and archived patients behave (#216), or turns one on or
 * off. The settings are saved with the other user preferences.
 */
public class SetCommand extends Command {

    public static final String COMMAND_WORD = "set";

    public static final String WORD_ON = "on";
    public static final String WORD_OFF = "off";

    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Shows the settings, or turns one on or off.\n"
            + "Parameters: [SETTING " + WORD_ON + "|" + WORD_OFF + "]\n"
            + "Settings: " + Arrays.stream(Key.values()).map(Key::getWord).collect(Collectors.joining(", ")) + "\n"
            + "Example: " + COMMAND_WORD + " " + Key.PAST_CLASHES.getWord() + " " + WORD_OFF;

    public static final String MESSAGE_SETTINGS = "Settings:\n%1$s";
    public static final String MESSAGE_SET_SUCCESS = "Turned %1$s %2$s.";

    private final Optional<Key> key; // empty to show the settings
    private final boolean isOn;

    /**
     * Creates a SetCommand that shows the settings.
     */
    public SetCommand() {
        this.key = Optional.empty();
        this.isOn = false;
    }

    /**
     * Creates a SetCommand that turns the setting {@code key} on if {@code isOn}, and off otherwise.
     */
    public SetCommand(Key key, boolean isOn) {
        requireNonNull(key);
        this.key = Optional.of(key);
        this.isOn = isOn;
    }

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        Settings settings = model.getUserPrefs().getSettings();
        if (key.isEmpty()) {
            return new CommandResult(String.format(MESSAGE_SETTINGS, describe(settings)));
        }
        model.setSettings(settings.with(key.get(), isOn));
        return new CommandResult(String.format(MESSAGE_SET_SUCCESS, key.get().getWord(), wordOf(isOn)));
    }

    /**
     * Returns each setting in {@code settings} on a line of its own, such as "past-clash: on".
     */
    static String describe(Settings settings) {
        return Arrays.stream(Key.values())
                .map(eachKey -> eachKey.getWord() + ": " + wordOf(settings.isOn(eachKey)))
                .collect(Collectors.joining("\n"));
    }

    private static String wordOf(boolean isOn) {
        return isOn ? WORD_ON : WORD_OFF;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof SetCommand otherSetCommand)) {
            return false;
        }

        return key.equals(otherSetCommand.key) && isOn == otherSetCommand.isOn;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("key", key)
                .add("isOn", isOn)
                .toString();
    }
}
