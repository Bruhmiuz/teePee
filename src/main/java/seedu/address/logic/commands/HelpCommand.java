package seedu.address.logic.commands;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import seedu.address.commons.core.FeatureFlags;
import seedu.address.logic.Messages;
import seedu.address.model.Model;

/**
 * Format full help instructions for every command for display and execution.
 */
public class HelpCommand extends Command {

    public static final String COMMAND_WORD = "help";

    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Shows program usage instructions.\n"
            + "Parameters: [COMMAND]\n"
            + "Example: " + COMMAND_WORD + " add\n"
            + "To open the full User Guide window: '" + COMMAND_WORD + " window'";

    public static final String SHOWING_HELP_MESSAGE = "Opened help window.";

    public static final String TARGET_WINDOW = "window";

    // Build the usage map dynamically with alphabetical order using TreeMap
    public static final Map<String, String> COMMAND_USAGE_MAP;

    static {
        Map<String, String> map = new TreeMap<>();
        map.put(AddCommand.COMMAND_WORD, AddCommand.MESSAGE_USAGE);
        map.put(AddAppointmentCommand.COMMAND_WORD, AddAppointmentCommand.MESSAGE_USAGE);
        map.put(ArchiveCommand.COMMAND_WORD, ArchiveCommand.MESSAGE_USAGE);
        map.put(ClearCommand.COMMAND_WORD, ClearCommand.MESSAGE_USAGE);
        map.put(DeleteCommand.COMMAND_WORD, DeleteCommand.MESSAGE_USAGE);
        map.put(DeleteAppointmentCommand.COMMAND_WORD, DeleteAppointmentCommand.MESSAGE_USAGE);
        map.put(EditCommand.COMMAND_WORD, EditCommand.MESSAGE_USAGE);
        map.put(EditAppointmentCommand.COMMAND_WORD, EditAppointmentCommand.MESSAGE_USAGE);
        map.put(ExitCommand.COMMAND_WORD, ExitCommand.MESSAGE_USAGE);
        map.put(FindCommand.COMMAND_WORD, FindCommand.MESSAGE_USAGE);
        map.put(HelpCommand.COMMAND_WORD, HelpCommand.MESSAGE_USAGE);
        map.put(MarkAppointmentCommand.COMMAND_WORD, MarkAppointmentCommand.MESSAGE_USAGE);
        map.put(UnarchiveCommand.COMMAND_WORD, UnarchiveCommand.MESSAGE_USAGE);
        map.put(UnmarkAppointmentCommand.COMMAND_WORD, UnmarkAppointmentCommand.MESSAGE_USAGE);
        map.put(ViewCommand.COMMAND_WORD, ViewCommand.MESSAGE_USAGE);
        map.put(ViewAppointmentCommand.COMMAND_WORD, ViewAppointmentCommand.MESSAGE_USAGE);
        if (FeatureFlags.isSelectiveClearEnabled()) {
            map.put(ClearAppointmentCommand.COMMAND_WORD, ClearAppointmentCommand.MESSAGE_USAGE);
        }
        if (FeatureFlags.isSettingsEnabled()) {
            map.put(SetCommand.COMMAND_WORD, SetCommand.MESSAGE_USAGE);
        }
        COMMAND_USAGE_MAP = Collections.unmodifiableMap(map);
    }

    public static final String AVAILABLE_COMMANDS = String.join(", ", COMMAND_USAGE_MAP.keySet());

    /**
     * Error message used by every rejected `help` argument: the invalid command format notice,
     * the usage summary, and the list of valid targets so the user can recover immediately.
     */
    public static final String MESSAGE_INVALID_HELP_ARGUMENT = String.format(Messages.MESSAGE_INVALID_COMMAND_FORMAT,
            MESSAGE_USAGE) + "\nValid targets: " + AVAILABLE_COMMANDS + ", " + TARGET_WINDOW;

    public static final String SHOWING_HELP_SUMMARY = "Commands: " + AVAILABLE_COMMANDS + "\n"
            + "Type 'help [COMMAND]' to view syntax details for a specific command.\n"
            + "Type 'help window' to open the full User Guide window, or press F1.";

    private final String target;

    /**
     * Creates a HelpCommand with no target (displays general help overview).
     */
    public HelpCommand() {
        this.target = null;
    }

    /**
     * Creates a HelpCommand targeting a specific command or window.
     */
    public HelpCommand(String target) {
        // HelpCommandParser guarantees a valid target; this assert surfaces direct construction
        // with an invalid target in tests rather than failing silently.
        assert target == null || TARGET_WINDOW.equals(target) || COMMAND_USAGE_MAP.containsKey(target);
        this.target = target;
    }

    @Override
    public CommandResult execute(Model model) {
        if (target == null) {
            return new CommandResult(SHOWING_HELP_SUMMARY);
        }

        if (target.equals(TARGET_WINDOW)) {
            return new CommandResult(SHOWING_HELP_MESSAGE, true, false);
        }

        return new CommandResult(COMMAND_USAGE_MAP.get(target));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof HelpCommand)) {
            return false;
        }

        HelpCommand otherHelpCommand = (HelpCommand) other;
        if (target == null) {
            return otherHelpCommand.target == null;
        }
        return target.equals(otherHelpCommand.target);
    }
}
