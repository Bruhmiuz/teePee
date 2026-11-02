package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.Messages.MESSAGE_UNKNOWN_COMMAND;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import seedu.address.commons.core.FeatureFlags;
import seedu.address.commons.core.LogsCenter;
import seedu.address.logic.commands.AddAppointmentCommand;
import seedu.address.logic.commands.AddCommand;
import seedu.address.logic.commands.ArchiveCommand;
import seedu.address.logic.commands.ClearAppointmentCommand;
import seedu.address.logic.commands.ClearCommand;
import seedu.address.logic.commands.Command;
import seedu.address.logic.commands.DeleteAppointmentCommand;
import seedu.address.logic.commands.DeleteCommand;
import seedu.address.logic.commands.EditAppointmentCommand;
import seedu.address.logic.commands.EditCommand;
import seedu.address.logic.commands.ExitCommand;
import seedu.address.logic.commands.FindCommand;
import seedu.address.logic.commands.HelpCommand;
import seedu.address.logic.commands.MarkAppointmentCommand;
import seedu.address.logic.commands.SetCommand;
import seedu.address.logic.commands.UnarchiveCommand;
import seedu.address.logic.commands.UnmarkAppointmentCommand;
import seedu.address.logic.commands.ViewAppointmentCommand;
import seedu.address.logic.commands.ViewCommand;
import seedu.address.logic.parser.exceptions.ParseException;

/**
 * Parses user input.
 */
public class LaiNurseParser {

    /**
     * Used for initial separation of command word and args.
     */
    private static final Pattern BASIC_COMMAND_FORMAT = Pattern.compile("(?<commandWord>\\S+)(?<arguments>.*)");
    private static final Logger logger = LogsCenter.getLogger(LaiNurseParser.class);

    /**
     * Parses user input into command for execution.
     *
     * @param userInput full user input string
     * @return the command based on the user input
     * @throws ParseException if the user input does not conform to the expected format
     */
    public Command parseCommand(String userInput) throws ParseException {
        // \h matches every horizontal whitespace character that \s does not, such as the non-breaking
        // space that formatted text sources insert in place of a space. Translating here, before the
        // command word is matched, lets every command, ID and value treat pasted narrow spaces the
        // same as spaces.
        final String separatorUserInput = userInput.replaceAll("\\h", " ");
        final Matcher matcher = BASIC_COMMAND_FORMAT.matcher(separatorUserInput.trim());
        if (!matcher.matches()) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, HelpCommand.MESSAGE_USAGE));
        }

        final String commandWord = matcher.group("commandWord");
        final String arguments = matcher.group("arguments");

        // Note to developers: Change LOG_LEVEL in LogsCenter to enable lower level (i.e., FINE, FINER and lower)
        // log messages such as the one below.
        // Lower level log messages are used sparingly to minimize noise in the code.
        logger.fine("Command word: " + commandWord + "; Arguments: " + arguments);

        return switch (commandWord) {
            case AddCommand.COMMAND_WORD -> new AddCommandParser().parse(arguments);
            case AddAppointmentCommand.COMMAND_WORD -> new AddAppointmentCommandParser().parse(arguments);
            case EditCommand.COMMAND_WORD -> new EditCommandParser().parse(arguments);
            case EditAppointmentCommand.COMMAND_WORD -> new EditAppointmentCommandParser().parse(arguments);
            case DeleteCommand.COMMAND_WORD -> new DeleteCommandParser().parse(arguments);
            case DeleteAppointmentCommand.COMMAND_WORD -> new DeleteAppointmentCommandParser().parse(arguments);
            case MarkAppointmentCommand.COMMAND_WORD -> new MarkAppointmentCommandParser().parse(arguments);
            case UnmarkAppointmentCommand.COMMAND_WORD -> new UnmarkAppointmentCommandParser().parse(arguments);
            case ArchiveCommand.COMMAND_WORD -> new ArchiveCommandParser().parse(arguments);
            case UnarchiveCommand.COMMAND_WORD -> new UnarchiveCommandParser().parse(arguments);
            case ClearCommand.COMMAND_WORD -> FeatureFlags.isSelectiveClearEnabled()
                    ? new ClearCommandParser().parse(arguments)
                    : new ClearCommand();
            case ClearAppointmentCommand.COMMAND_WORD -> {
                if (!FeatureFlags.isSelectiveClearEnabled()) {
                    throw new ParseException(MESSAGE_UNKNOWN_COMMAND);
                }
                yield new ClearAppointmentCommandParser().parse(arguments);
            }
            case FindCommand.COMMAND_WORD -> new FindCommandParser().parse(arguments);
            case ViewAppointmentCommand.COMMAND_WORD -> new ViewAppointmentCommandParser().parse(arguments);
            case ViewCommand.COMMAND_WORD -> new ViewCommandParser().parse(arguments);
            case ExitCommand.COMMAND_WORD -> new ExitCommand();
            case HelpCommand.COMMAND_WORD -> new HelpCommandParser().parse(arguments);
            case SetCommand.COMMAND_WORD -> {
                if (!FeatureFlags.isSettingsEnabled()) {
                    throw new ParseException(MESSAGE_UNKNOWN_COMMAND);
                }
                yield new SetCommandParser().parse(arguments);
            }
            default -> {
                logger.finer("This user input caused a ParseException: " + userInput);
                throw new ParseException(MESSAGE_UNKNOWN_COMMAND);
            }
        };
    }

}
