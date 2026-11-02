package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.Messages.MESSAGE_UNKNOWN_COMMAND;
import static seedu.address.logic.parser.CliSyntax.PREFIX_NAME;
import static seedu.address.testutil.Assert.assertThrows;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import seedu.address.logic.Messages;
import seedu.address.logic.commands.AddAppointmentCommand;
import seedu.address.logic.commands.AddCommand;
import seedu.address.logic.commands.ArchiveCommand;
import seedu.address.logic.commands.ClearCommand;
import seedu.address.logic.commands.DeleteAppointmentCommand;
import seedu.address.logic.commands.DeleteCommand;
import seedu.address.logic.commands.EditAppointmentCommand;
import seedu.address.logic.commands.EditCommand;
import seedu.address.logic.commands.EditCommand.EditPatientDescriptor;
import seedu.address.logic.commands.ExitCommand;
import seedu.address.logic.commands.FindCommand;
import seedu.address.logic.commands.HelpCommand;
import seedu.address.logic.commands.ListCommand;
import seedu.address.logic.commands.UnarchiveCommand;
import seedu.address.logic.commands.ViewAppointmentCommand;
import seedu.address.logic.commands.ViewCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.PatientScope;
import seedu.address.model.patient.Appointment;
import seedu.address.model.patient.AppointmentDetails;
import seedu.address.model.patient.Name;
import seedu.address.model.patient.Patient;
import seedu.address.model.patient.PatientContainsSubstringsPredicate;
import seedu.address.model.patient.PatientId;
import seedu.address.testutil.EditPatientDescriptorBuilder;
import seedu.address.testutil.PatientBuilder;
import seedu.address.testutil.PatientUtil;

public class LaiNurseParserTest {

    private final LaiNurseParser parser = new LaiNurseParser();

    @Test
    public void parseCommand_add() throws Exception {
        Patient patient = new PatientBuilder().build();
        AddCommand command = (AddCommand) parser.parseCommand(PatientUtil.getAddCommand(patient));
        assertEquals(new AddCommand(patient), command);
    }

    @Test
    public void parseCommand_addAppointment() throws Exception {
        AddAppointmentCommand command = (AddAppointmentCommand) parser.parseCommand(
                AddAppointmentCommand.COMMAND_WORD + " S1234567A --date 2026-10-05 09:30"
                        + " --details Bring insulin pen");
        assertEquals(new AddAppointmentCommand(new PatientId("S1234567A"),
                new Appointment("2026-10-05 09:30", Optional.of(new AppointmentDetails("Bring insulin pen")))),
                command);
    }

    @Test
    public void parseCommand_clear() throws Exception {
        assertTrue(parser.parseCommand(ClearCommand.COMMAND_WORD) instanceof ClearCommand);
        assertTrue(parser.parseCommand(ClearCommand.COMMAND_WORD + " 3") instanceof ClearCommand);
    }

    @Test
    public void parseCommand_delete() throws Exception {
        PatientId targetId = new PatientId("T0123456B");
        DeleteCommand command = (DeleteCommand) parser.parseCommand(
                DeleteCommand.COMMAND_WORD + " " + targetId.value);
        assertEquals(new DeleteCommand(targetId), command);
    }

    @Test
    public void parseCommand_archiveAndUnarchive() throws Exception {
        PatientId targetId = new PatientId("S1234567A");
        assertEquals(new ArchiveCommand(targetId), parser.parseCommand(ArchiveCommand.COMMAND_WORD + " S1234567A"));
        assertEquals(new UnarchiveCommand(targetId),
                parser.parseCommand(UnarchiveCommand.COMMAND_WORD + " S1234567A"));
    }

    @Test
    public void parseCommand_deleteAppointment() throws Exception {
        DeleteAppointmentCommand command = (DeleteAppointmentCommand) parser.parseCommand(
                DeleteAppointmentCommand.COMMAND_WORD + " S1234567A 1");
        assertEquals(new DeleteAppointmentCommand(new PatientId("S1234567A"), 1), command);
    }

    @Test
    public void parseCommand_view() throws Exception {
        ViewCommand command = (ViewCommand) parser.parseCommand(ViewCommand.COMMAND_WORD + " S1234567A");
        assertEquals(new ViewCommand(new PatientId("S1234567A")), command);
    }

    @Test
    public void parseCommand_editAppointment() throws Exception {
        EditAppointmentCommand command = (EditAppointmentCommand) parser.parseCommand(
                EditAppointmentCommand.COMMAND_WORD + " S1234567A 1 --date 2026-10-06");
        assertEquals(new EditAppointmentCommand(new PatientId("S1234567A"), 1,
                Optional.of(new Appointment("2026-10-06")), Optional.empty()), command);
    }

    @Test
    public void parseCommand_appointmentHorizontalSpaces_success() throws Exception {
        assertEquals(new AddAppointmentCommand(new PatientId("S1234567A"), new Appointment("2026-10-05 09:30")),
                parser.parseCommand("addappt\u00a0s1234567a\u202f--date\u00a02026-10-05\u00a009:30"));
        assertEquals(new DeleteAppointmentCommand(new PatientId("S1234567A"), 1),
                parser.parseCommand("deleteappt\u00a0s1234567a\u202f1"));
        assertEquals(new EditAppointmentCommand(new PatientId("S1234567A"), 1,
                Optional.of(new Appointment("2026-10-06")), Optional.empty()),
                parser.parseCommand("editappt\u00a0s1234567a\u202f1\u00a0--date\u202f2026-10-06"));
    }

    @Test
    public void parseCommand_edit() throws Exception {
        Patient patient = new PatientBuilder().withNotes("Enjoys gardening").build();
        EditPatientDescriptor descriptor = new EditPatientDescriptorBuilder(patient).build();
        EditCommand command = (EditCommand) parser.parseCommand(EditCommand.COMMAND_WORD + " "
                + patient.getId().value + " " + PatientUtil.getEditPatientDescriptorDetails(descriptor));
        assertEquals(new EditCommand(patient.getId(), descriptor), command);
    }

    @Test
    public void parseCommand_exit() throws Exception {
        assertTrue(parser.parseCommand(ExitCommand.COMMAND_WORD) instanceof ExitCommand);
        assertTrue(parser.parseCommand(ExitCommand.COMMAND_WORD + " 3") instanceof ExitCommand);
    }

    @Test
    public void parseCommand_find() throws Exception {
        FindCommand command = (FindCommand) parser.parseCommand(
                FindCommand.COMMAND_WORD + " --name \"John\" --address \"Marina Bay\" --id \"123\"");
        assertEquals(new FindCommand(new PatientContainsSubstringsPredicate(Optional.of("123"), Optional.of("John"),
                Optional.empty(), Optional.of("Marina Bay"), Optional.empty(), Optional.empty())), command);
    }

    @Test
    public void parseCommand_help() throws Exception {
        assertTrue(parser.parseCommand(HelpCommand.COMMAND_WORD) instanceof HelpCommand);
        assertTrue(parser.parseCommand(HelpCommand.COMMAND_WORD + " add") instanceof HelpCommand);
        assertEquals(new HelpCommand("delete"), parser.parseCommand(HelpCommand.COMMAND_WORD + " delete"));
        assertEquals(new HelpCommand("window"), parser.parseCommand(HelpCommand.COMMAND_WORD + " window"));
        assertEquals(new HelpCommand("help"), parser.parseCommand(HelpCommand.COMMAND_WORD + " help"));
    }

    @Test
    public void parseCommand_helpInvalidFormat_throwsParseException() {
        assertThrows(ParseException.class, () ->
                parser.parseCommand(HelpCommand.COMMAND_WORD + " -- add"));
        assertThrows(ParseException.class, () ->
                parser.parseCommand(HelpCommand.COMMAND_WORD + " add patient"));
        // The '--COMMAND' flag form is no longer supported
        assertThrows(ParseException.class, () ->
                parser.parseCommand(HelpCommand.COMMAND_WORD + " --add"));
        assertThrows(ParseException.class, () ->
                parser.parseCommand(HelpCommand.COMMAND_WORD + " --window"));
        // Unknown targets are rejected at parse time
        assertThrows(ParseException.class, () ->
                parser.parseCommand(HelpCommand.COMMAND_WORD + " 3"));
        assertThrows(ParseException.class, () ->
                parser.parseCommand(HelpCommand.COMMAND_WORD + " nonexistent"));
    }

    @Test
    public void parseCommand_list_unknownCommand() {
        assertThrows(ParseException.class, MESSAGE_UNKNOWN_COMMAND, () -> parser.parseCommand("list"));
    }

    @Test
    public void parseCommand_viewWithoutId_listCommand() throws Exception {
        // view without an ID lists the patients in a scope
        assertEquals(new ListCommand(), parser.parseCommand(ViewCommand.COMMAND_WORD));
        assertEquals(new ListCommand(PatientScope.ALL), parser.parseCommand(ViewCommand.COMMAND_WORD + " --all"));
    }

    @Test
    public void parseCommand_viewAppointment() throws Exception {
        assertEquals(new ViewAppointmentCommand(LocalDate.of(2026, 10, 15)),
                parser.parseCommand(ViewAppointmentCommand.COMMAND_WORD + " 2026-10-15"));
        ViewAppointmentCommand rangeCommand = new ViewAppointmentCommand(Optional.of(LocalDate.of(2026, 10, 1)),
                Optional.of(LocalDate.of(2026, 10, 31)));
        assertEquals(rangeCommand,
                parser.parseCommand(ViewAppointmentCommand.COMMAND_WORD + " --from 2026-10-01 --to 2026-10-31"));
    }

    @Test
    public void parseCommand_horizontalSpaces_actAsSpaces() throws Exception {
        Patient patient = new PatientBuilder().build();

        // every horizontal space, such as the non-breaking space copied out of formatted text, acts
        // as if a plain space had been typed, after the command word and around the options too
        String nbspCommand = "\u00A0"
                + PatientUtil.getAddCommand(patient).replace(" ", "\u00A0");
        assertTrue(nbspCommand.contains("\u00A0"), "test input should contain horizontal spaces");
        assertEquals(parser.parseCommand(PatientUtil.getAddCommand(patient)), parser.parseCommand(nbspCommand));
    }

    @Test
    public void parseCommand_horizontalSpaces_blankAndRepeatedValuesStillRejected() {
        // a value with no text between it and the next option is still blank
        assertThrows(ParseException.class, Name.MESSAGE_CONSTRAINTS, () -> parser.parseCommand(
                AddCommand.COMMAND_WORD + " --id T0123456B --name\u00A0--phone 91234567"
                        + " --address Blk 3"));

        // an option repeated with other horizontal spaces between the two is still single-valued
        String duplicateNameArgs = AddCommand.COMMAND_WORD + " --id T0123456B --name\u00A0Alex"
                + " --name\u00A0Bob --phone 91234567 --address Blk 4";
        assertThrows(ParseException.class, Messages.getErrorMessageForDuplicatePrefixes(PREFIX_NAME), ()
                -> parser.parseCommand(duplicateNameArgs));
    }

    @Test
    public void parseCommand_zeroWidthSpaceInCommandWord_throwsParseException() {
        // Zero-width characters are neither \h nor \s, so they stay glued to the command word,
        // which then matches no registered command.
        assertThrows(ParseException.class, MESSAGE_UNKNOWN_COMMAND, () -> parser.parseCommand("view\u200B"));
        assertThrows(ParseException.class, MESSAGE_UNKNOWN_COMMAND, () -> parser.parseCommand("\u200Blist"));
        assertThrows(ParseException.class, MESSAGE_UNKNOWN_COMMAND, () -> parser.parseCommand(
                "add\u200B --id T0123456B --name Alex Yu --phone 91234567 --address Blk 1"));
    }

    @Test
    public void parseCommand_zeroWidthSpaceInId_throwsParseException() {
        // The zero-width family survives normalization, so an ID carrying one fails the
        // letter-and-digit check instead of silently matching another patient.
        // addappt maps any bad ID to its format error; add surfaces the constraint directly.
        assertThrows(ParseException.class, String.format(MESSAGE_INVALID_COMMAND_FORMAT,
                AddAppointmentCommand.MESSAGE_USAGE), () -> parser.parseCommand(
                AddAppointmentCommand.COMMAND_WORD + " S1234567A\u200B --date 2026-10-05"));
        assertThrows(ParseException.class, PatientId.MESSAGE_CONSTRAINTS, () -> parser.parseCommand(
                AddCommand.COMMAND_WORD + " --id T0123456\u00ADB --name Alex Yu --phone 91234567 --address Blk 2"));
    }

    @Test
    public void parseCommand_zeroWidthSpaceGluesTokens_throwsParseException() {
        // A zero-width space between a value and the next option hides that option, so the glued
        // text is parsed as one invalid ID instead of two arguments.
        assertThrows(ParseException.class, PatientId.MESSAGE_CONSTRAINTS, () -> parser.parseCommand(
                AddCommand.COMMAND_WORD + " --id T0123456B\u200B--notes gardening"
                        + " --name Alex Yu --phone 91234567 --address Blk 3"));
        // A zero-width space inside an option word makes the whole token an unregistered option.
        assertThrows(ParseException.class,
                String.format(MESSAGE_INVALID_COMMAND_FORMAT, AddCommand.MESSAGE_USAGE), () ->
                parser.parseCommand(AddCommand.COMMAND_WORD + " --id\u200BT0123456B --name Alex"
                        + " --phone 91234567 --address Blk 4"));
    }

    @Test
    public void parseCommand_zeroWidthSpaceInDate_throwsParseException() {
        assertThrows(ParseException.class, () -> parser.parseCommand(
                AddAppointmentCommand.COMMAND_WORD + " S1234567A --date 2026-10-0\u200B5"));
    }

    @Test
    public void parseCommand_unrecognisedInput_throwsParseException() {
        assertThrows(ParseException.class, String.format(MESSAGE_INVALID_COMMAND_FORMAT, HelpCommand.MESSAGE_USAGE), ()
            -> parser.parseCommand(""));
    }

    @Test
    public void parseCommand_unknownCommand_throwsParseException() {
        assertThrows(ParseException.class, MESSAGE_UNKNOWN_COMMAND, () -> parser.parseCommand("unknownCommand"));
    }

    @Test
    public void parseCommand_upperCaseCommandWord_throwsParseException() {
        // Command words are case-sensitive: syntax is typed in lowercase (see the UG command-format notes)
        assertThrows(ParseException.class, MESSAGE_UNKNOWN_COMMAND, () -> parser.parseCommand("LIST"));
        assertThrows(ParseException.class, MESSAGE_UNKNOWN_COMMAND, () -> parser.parseCommand("Help list"));
        assertThrows(ParseException.class, MESSAGE_UNKNOWN_COMMAND, () ->
                parser.parseCommand("AddAppt S1234567A 2026-10-05 09:30"));
    }
}
