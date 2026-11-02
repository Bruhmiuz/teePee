package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import seedu.address.logic.Messages;
import seedu.address.logic.commands.ClearAppointmentCommand;
import seedu.address.logic.commands.ClearCommand;
import seedu.address.model.patient.Appointment;
import seedu.address.model.patient.PatientId;

/**
 * Tests the parsers of {@code clear} and {@code clearappt}, once selective clearing is released (#217).
 */
public class ClearParsersTest {

    private static final String CLEAR_FORMAT = String.format(MESSAGE_INVALID_COMMAND_FORMAT,
            ClearCommand.MESSAGE_USAGE);
    private static final String CLEARAPPT_FORMAT = String.format(MESSAGE_INVALID_COMMAND_FORMAT,
            ClearAppointmentCommand.MESSAGE_USAGE);
    private static final PatientId ALEX = new PatientId("S1234567A");

    private final ClearCommandParser clearParser = new ClearCommandParser();
    private final ClearAppointmentCommandParser clearApptParser = new ClearAppointmentCommandParser();

    @Test
    public void parseClear() {
        assertParseSuccess(clearParser, "", new ClearCommand(false));
        assertParseSuccess(clearParser, " --archived ", new ClearCommand(true));
        assertParseFailure(clearParser, "everything", CLEAR_FORMAT);
        assertParseFailure(clearParser, "--archived now", CLEAR_FORMAT);
        assertParseFailure(clearParser, "--all", CLEAR_FORMAT);
        assertParseFailure(clearParser, "--archived --archived",
                Messages.getErrorMessageForDuplicatePrefixes(CliSyntax.PREFIX_ARCHIVED));
    }

    @Test
    public void parseClearAppointment_valid() {
        assertParseSuccess(clearApptParser, "  ",
                new ClearAppointmentCommand(Optional.empty(), Optional.empty(), Optional.empty()));
        assertParseSuccess(clearApptParser, "S1234567A",
                new ClearAppointmentCommand(Optional.of(ALEX), Optional.empty(), Optional.empty()));
        assertParseSuccess(clearApptParser, "--to 2026-10-31 12:00 --from 2026-10-01",
                new ClearAppointmentCommand(Optional.empty(), at("2026-10-01"), at("2026-10-31 12:00")));
        assertParseSuccess(clearApptParser, "s1234567a --from 2026-10-01",
                new ClearAppointmentCommand(Optional.of(ALEX), at("2026-10-01"), Optional.empty()));
        assertParseSuccess(clearApptParser, "S1234567A --to 2026-10-31",
                new ClearAppointmentCommand(Optional.of(ALEX), Optional.empty(), at("2026-10-31")));
    }

    @Test
    public void parseClearAppointment_invalid() {
        assertParseFailure(clearApptParser, "S1234567A --id S1234567A", CLEARAPPT_FORMAT);
        assertParseFailure(clearApptParser, "--id S1234567A", CLEARAPPT_FORMAT);
        assertParseFailure(clearApptParser, "--from 2026-10-02 --to 2026-10-01",
                ClearAppointmentCommand.MESSAGE_FROM_AFTER_TO);
        assertParseFailure(clearApptParser, "--from 2026-02-30", Appointment.MESSAGE_CONSTRAINTS);
        assertParseFailure(clearApptParser, "--until 2026-10-01", CLEARAPPT_FORMAT);
    }

    private static Optional<Appointment> at(String dateTime) {
        return Optional.of(new Appointment(dateTime));
    }
}
