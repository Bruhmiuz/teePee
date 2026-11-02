package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.SetCommand;
import seedu.address.model.Settings.Key;

public class SetCommandParserTest {

    private static final String INVALID_FORMAT = String.format(MESSAGE_INVALID_COMMAND_FORMAT,
            SetCommand.MESSAGE_USAGE);

    private final SetCommandParser parser = new SetCommandParser();

    @Test
    public void parse_noArguments_showsSettings() {
        assertParseSuccess(parser, "", new SetCommand());
        assertParseSuccess(parser, "   ", new SetCommand());
    }

    @Test
    public void parse_settingAndValue_setsIt() {
        assertParseSuccess(parser, "past-clash off", new SetCommand(Key.PAST_CLASHES, false));
        assertParseSuccess(parser, "  archived-clash \t on ", new SetCommand(Key.ARCHIVED_CLASHES, true));
    }

    @Test
    public void parse_invalid_throwsParseException() {
        assertParseFailure(parser, "past-clash", INVALID_FORMAT);
        assertParseFailure(parser, "past-clash maybe", INVALID_FORMAT);
        assertParseFailure(parser, "past-clash OFF", INVALID_FORMAT);
        assertParseFailure(parser, "unknown on", INVALID_FORMAT);
        assertParseFailure(parser, "past-clash off now", INVALID_FORMAT);
    }
}
