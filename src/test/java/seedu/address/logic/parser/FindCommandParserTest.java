package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.CliSyntax.PREFIX_ADDRESS;
import static seedu.address.logic.parser.CliSyntax.PREFIX_ALL;
import static seedu.address.logic.parser.CliSyntax.PREFIX_ID;
import static seedu.address.logic.parser.CliSyntax.PREFIX_MEDICAL_HISTORY;
import static seedu.address.logic.parser.CliSyntax.PREFIX_NAME;
import static seedu.address.logic.parser.CliSyntax.PREFIX_NOTES;
import static seedu.address.logic.parser.CliSyntax.PREFIX_PHONE;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import seedu.address.logic.Messages;
import seedu.address.logic.commands.FindCommand;
import seedu.address.model.PatientScope;
import seedu.address.model.patient.PatientContainsSubstringsPredicate;

public class FindCommandParserTest {

    private static final String INVALID_FORMAT_MESSAGE =
            String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE);

    private final FindCommandParser parser = new FindCommandParser();

    @Test
    public void parse_nullArg_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> parser.parse(null));
    }

    @Test
    public void parse_emptyArg_throwsParseException() {
        assertParseFailure(parser, "", INVALID_FORMAT_MESSAGE);
        assertParseFailure(parser, " \t\n ", INVALID_FORMAT_MESSAGE);
    }

    @Test
    public void parse_eachOption_returnsFindCommand() {
        assertParseSuccess(parser, "--id 123", command("123", null, null, null, null));
        assertParseSuccess(parser, "--name Mary", command(null, "Mary", null, null, null));
        assertParseSuccess(parser, "--phone 5", command(null, null, "5", null, null));
        assertParseSuccess(parser, "--address #01-", command(null, null, null, "#01-", null));
        assertParseSuccess(parser, "--medical-history diabet", command(null, null, null, null, "diabet"));
        assertParseSuccess(parser, "--notes garden", command(null, null, null, null, null, "garden"));
    }

    @Test
    public void parse_phoneFormattingDifferences_returnsEquivalentCommand() {
        FindCommand expectedCommand = command(null, null, "6598765432", null, null);
        for (String value : List.of("6598765432", "+65 9876-5432", "\"(65) 9876.5432\"", "“65 / 9876 / 5432”",
                "\"  +65\t 98-76  5432  \"")) {
            assertParseSuccess(parser, "--phone " + value, expectedCommand);
        }
        assertParseSuccess(parser, "--phone 00-1", command(null, null, "001", null, null));
    }

    @Test
    public void parse_phoneWithoutDigits_throwsParseException() {
        String expectedMessage = "Phone search values must contain at least one digit.";
        for (String value : List.of("+-()", "abc", "\" + - () \"", "“abc”", "\uFF11\uFF12\uFF13")) {
            assertParseFailure(parser, "--phone " + value, expectedMessage);
            assertParseFailure(parser, "--name Mary --phone " + value + " --all", expectedMessage);
        }
    }

    @Test
    public void parse_allOptionsInDifferentOrders_returnsSameCommand() {
        FindCommand expectedCommand = command("123", "Mary Jane", "65", "Marina Bay", "Type 2", "Enjoys gardening");
        assertParseSuccess(parser, "--id 123 --name Mary Jane --phone 65 --address Marina Bay"
                + " --medical-history Type 2 --notes Enjoys gardening", expectedCommand);
        assertParseSuccess(parser, " --notes “Enjoys gardening” --medical-history “Type 2”"
                + " --address \"Marina Bay\" --phone “65”"
                + " --name “Mary Jane” --id \"123\"", expectedCommand);
    }

    @Test
    public void parse_whitespaceSeparators_returnsFindCommand() {
        FindCommand expectedCommand = command(null, "Mary Jane", null, "Marina Bay", null);
        assertParseSuccess(parser, "--name Mary Jane --address Marina Bay", expectedCommand);
        assertParseSuccess(parser, "\t--address\tMarina\t\tBay\t--name\tMary\tJane\t", expectedCommand);
        assertParseSuccess(parser, " \n --name \n Mary Jane \n --address Marina Bay  ", expectedCommand);
    }

    @Test
    public void parse_optionalQuotes_returnsSameCommand() {
        FindCommand expectedCommand = command(null, "Mary Jane", null, null, null);
        for (String value : List.of("Mary Jane", "\"Mary Jane\"", "“Mary Jane”", "\"  mary\t JANE  \"",
                "“ mary  Jane ”")) {
            assertParseSuccess(parser, "--name " + value, expectedCommand);
            assertParseSuccess(parser, "--notes " + value, command(null, null, null, null, null, "Mary Jane"));
        }
    }

    @Test
    public void parse_tabSeparatedNotes_returnsFindCommand() {
        assertParseSuccess(parser, "\t--notes\tEnjoys\t gardening\t",
                command(null, null, null, null, null, "Enjoys gardening"));
    }

    @Test
    public void parse_unexpectedPreamble_failure() {
        for (String arguments : List.of("John", "John Mary", "preamble --name Mary", "123 --id 123")) {
            assertParseFailure(parser, arguments, INVALID_FORMAT_MESSAGE);
        }
    }

    @Test
    public void parse_emptyValues_failure() {
        for (Prefix prefix : List.of(PREFIX_ID, PREFIX_NAME, PREFIX_PHONE, PREFIX_ADDRESS,
                PREFIX_MEDICAL_HISTORY, PREFIX_NOTES)) {
            for (String value : List.of("", " \t ", "\"\"", "\" \t \"", "“”", "“  ”")) {
                assertParseFailure(parser, prefix + value, INVALID_FORMAT_MESSAGE);
            }
        }
        assertParseFailure(parser, "--name --id 123", INVALID_FORMAT_MESSAGE);
        assertParseFailure(parser, "--id 123 --name", INVALID_FORMAT_MESSAGE);
        assertParseFailure(parser, "--name \"\" --id 123", INVALID_FORMAT_MESSAGE);
    }

    @Test
    public void parse_duplicateOptions_failure() {
        for (Prefix prefix : List.of(PREFIX_ID, PREFIX_NAME, PREFIX_PHONE, PREFIX_ADDRESS,
                PREFIX_MEDICAL_HISTORY, PREFIX_NOTES)) {
            String expectedMessage = Messages.getErrorMessageForDuplicatePrefixes(prefix);
            assertParseFailure(parser, prefix + "first " + prefix + "second", expectedMessage);
            assertParseFailure(parser, prefix + "first " + prefix.getPrefix().trim(), expectedMessage);
        }
    }

    @Test
    public void parse_unknownOptions_failure() {
        for (String option : List.of("--unknown value", "--medical-histroy asthma", "--name=Mary", "--", "--Name Mary",
                "--number 123", "--next-appointment 2026-10", "--appt 2026-10")) {
            assertParseFailure(parser, option, INVALID_FORMAT_MESSAGE);
            assertParseFailure(parser, "--name Mary " + option, INVALID_FORMAT_MESSAGE);
        }
    }

    @Test
    public void parse_malformedQuotes_failure() {
        for (String value : List.of("\"", "“", "\"Mary", "“Mary", "\"Mary”", "“Mary\"", "Mary\"", "Mary”",
                "”Mary”", "\"Mary\" trailing", "“Mary” trailing", "\"Mary\" \"Jane\"", "“Mary” “Jane”")) {
            assertParseFailure(parser, "--name " + value, INVALID_FORMAT_MESSAGE);
            assertParseFailure(parser, "--notes " + value, INVALID_FORMAT_MESSAGE);
        }
    }

    @Test
    public void parse_reservedOptionsInsideValues_failure() {
        for (String arguments : List.of("--name \"Mary --unknown value\"", "--name \"--unknown\"",
                "--name “--id”", "--name \"Mary --address Jurong\"", "--name Mary\t--unknown\tvalue",
                "--notes \"Enjoys --unknown gardening\"", "--notes “--name”",
                "--phone \"9876 --unknown 5432\"")) {
            assertParseFailure(parser, arguments, INVALID_FORMAT_MESSAGE);
        }
    }

    @Test
    public void parse_embeddedDoubleHyphenText_success() {
        assertParseSuccess(parser, "--name Anne--Marie --address \"Block--A\"",
                command(null, "Anne--Marie", null, "Block--A", null));
    }

    private FindCommand command(String id, String name, String phone, String address, String medicalHistory) {
        return command(id, name, phone, address, medicalHistory, null);
    }

    private FindCommand command(String id, String name, String phone, String address, String medicalHistory,
            String notes) {
        return new FindCommand(new PatientContainsSubstringsPredicate(Optional.ofNullable(id),
                Optional.ofNullable(name), Optional.ofNullable(phone), Optional.ofNullable(address),
                Optional.ofNullable(medicalHistory), Optional.ofNullable(notes)));
    }

    @Test
    public void parse_scopeOption_returnsFindCommandWithScope() {
        PatientContainsSubstringsPredicate mary = new PatientContainsSubstringsPredicate(Optional.empty(),
                Optional.of("Mary"), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        assertParseSuccess(parser, "--name Mary --all", new FindCommand(mary, Optional.of(PatientScope.ALL)));
        assertParseSuccess(parser, "--archived --name Mary", new FindCommand(mary,
                Optional.of(PatientScope.ARCHIVED)));
        assertParseSuccess(parser, "--name Mary", new FindCommand(mary, Optional.empty()));
    }

    @Test
    public void parse_scopeOptionMisused_throwsParseException() {
        // a scope alone searches nothing
        assertParseFailure(parser, "--all", INVALID_FORMAT_MESSAGE);
        assertParseFailure(parser, "--name Mary --all --archived", Messages.MESSAGE_CONFLICTING_SCOPES);
        assertParseFailure(parser, "--name Mary --all --all", Messages.getErrorMessageForDuplicatePrefixes(PREFIX_ALL));
        // the scope options take no value
        assertParseFailure(parser, "--all Mary --name Jane", INVALID_FORMAT_MESSAGE);
    }
}
