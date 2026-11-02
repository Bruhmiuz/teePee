package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.logic.parser.CliSyntax.PREFIX_ADD_NOTES;
import static seedu.address.logic.parser.CliSyntax.PREFIX_NOTES;
import static seedu.address.logic.parser.CliSyntax.PREFIX_PHONE;
import static seedu.address.testutil.Assert.assertThrows;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import seedu.address.logic.Messages;
import seedu.address.logic.commands.OptionalFieldEdit;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.patient.Address;
import seedu.address.model.patient.Appointment;
import seedu.address.model.patient.AppointmentDetails;
import seedu.address.model.patient.MedicalHistory;
import seedu.address.model.patient.Name;
import seedu.address.model.patient.Notes;
import seedu.address.model.patient.PatientId;
import seedu.address.model.patient.Phone;

public class ParserUtilTest {
    private static final String INVALID_ID = "S1234-567";
    private static final String INVALID_NAME = " ";
    private static final String INVALID_PHONE = "+65abc";
    private static final String INVALID_ADDRESS = " ";
    private static final String INVALID_DATE_WITHOUT_TIME = "2026/06/07";
    private static final String INVALID_DATE_WITH_TIME = "2026-06-07 1807";

    private static final String VALID_ID = "S1234567A";
    private static final String VALID_NAME = "Rachel Walker";
    private static final String VALID_PHONE = "123456";
    private static final String VALID_ADDRESS = "123 Main Street #0505";
    private static final String VALID_MEDICAL_HISTORY = "dementia";
    private static final String VALID_DATE_WITHOUT_TIME = "2026-06-07";
    private static final String VALID_DATE_WITH_TIME = "2026-06-07 18:07";

    private static final String WHITESPACE = " \t\r\n";

    private static final LocalDate VALID_DATE_OBJECT = LocalDate.of(2026, 6, 7);

    @Test
    public void parseAppointmentNumber_notPositiveWholeNumber_throwsParseException() {
        for (String number : List.of("", "0", "00", "-1", "+1", "1.5", "1 2", "two", "#2")) {
            assertThrows(ParseException.class, Appointment.MESSAGE_NUMBER_CONSTRAINTS, ()
                    -> ParserUtil.parseAppointmentNumber(number));
        }
    }

    @Test
    public void parseAppointmentNumber_positiveWholeNumber_returnsNumber() throws Exception {
        assertEquals(1, ParserUtil.parseAppointmentNumber("1"));
        assertEquals(2, ParserUtil.parseAppointmentNumber("002"));
        // numbers larger than any appointment number are kept
        assertEquals(99999999999L, ParserUtil.parseAppointmentNumber("99999999999"));
        assertEquals(Long.MAX_VALUE, ParserUtil.parseAppointmentNumber("9223372036854775807"));
    }

    @Test
    public void parseAppointmentNumber_tooLargeForLong_throwsParseExceptionWithTypedNumber() {
        assertThrows(ParseException.class, "Appointment number 9223372036854775808 is too large.", ()
                -> ParserUtil.parseAppointmentNumber("9223372036854775808"));
    }

    @Test
    public void parseId_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ParserUtil.parseId((String) null));
    }

    @Test
    public void parseId_invalidValue_throwsParseException() {
        assertThrows(ParseException.class, PatientId.MESSAGE_CONSTRAINTS, () -> ParserUtil.parseId(INVALID_ID));
    }

    @Test
    public void parseId_validValueWithoutWhitespace_returnsId() throws Exception {
        PatientId expectedId = new PatientId(VALID_ID);
        assertEquals(expectedId, ParserUtil.parseId(VALID_ID));
    }

    @Test
    public void parseId_validValueWithWhitespace_returnsTrimmedId() throws Exception {
        String idWithWhitespace = WHITESPACE + VALID_ID + WHITESPACE;
        PatientId expectedId = new PatientId(VALID_ID);
        assertEquals(expectedId, ParserUtil.parseId(idWithWhitespace));
    }

    @Test
    public void parseName_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ParserUtil.parseName((String) null));
    }

    @Test
    public void parseName_invalidValue_throwsParseException() {
        assertThrows(ParseException.class, () -> ParserUtil.parseName(INVALID_NAME));
    }

    @Test
    public void parseName_validValueWithoutWhitespace_returnsName() throws Exception {
        Name expectedName = new Name(VALID_NAME);
        assertEquals(expectedName, ParserUtil.parseName(VALID_NAME));
    }

    @Test
    public void parseName_validValueWithWhitespace_returnsTrimmedName() throws Exception {
        String nameWithWhitespace = WHITESPACE + VALID_NAME + WHITESPACE;
        Name expectedName = new Name(VALID_NAME);
        assertEquals(expectedName, ParserUtil.parseName(nameWithWhitespace));
    }

    @Test
    public void parsePhone_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ParserUtil.parsePhone((String) null));
    }

    @Test
    public void parsePhone_invalidValue_throwsParseException() {
        assertThrows(ParseException.class, () -> ParserUtil.parsePhone(INVALID_PHONE));
    }

    @Test
    public void parsePhone_validValueWithoutWhitespace_returnsPhone() throws Exception {
        Phone expectedPhone = new Phone(VALID_PHONE);
        assertEquals(expectedPhone, ParserUtil.parsePhone(VALID_PHONE));
    }

    @Test
    public void parsePhone_validValueWithWhitespace_returnsTrimmedPhone() throws Exception {
        String phoneWithWhitespace = WHITESPACE + VALID_PHONE + WHITESPACE;
        Phone expectedPhone = new Phone(VALID_PHONE);
        assertEquals(expectedPhone, ParserUtil.parsePhone(phoneWithWhitespace));
    }

    @Test
    public void parseAddress_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ParserUtil.parseAddress((String) null));
    }

    @Test
    public void parseAddress_invalidValue_throwsParseException() {
        assertThrows(ParseException.class, () -> ParserUtil.parseAddress(INVALID_ADDRESS));
    }

    @Test
    public void parseAddress_validValueWithoutWhitespace_returnsAddress() throws Exception {
        Address expectedAddress = new Address(VALID_ADDRESS);
        assertEquals(expectedAddress, ParserUtil.parseAddress(VALID_ADDRESS));
    }

    @Test
    public void parseAddress_validValueWithWhitespace_returnsTrimmedAddress() throws Exception {
        String addressWithWhitespace = WHITESPACE + VALID_ADDRESS + WHITESPACE;
        Address expectedAddress = new Address(VALID_ADDRESS);
        assertEquals(expectedAddress, ParserUtil.parseAddress(addressWithWhitespace));
    }

    @Test
    public void parseMedicalHistory_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ParserUtil.parseMedicalHistory((String) null));
    }

    @Test
    public void parseMedicalHistory_validValueWithoutWhitespace_returnsMedicalHistory() throws Exception {
        MedicalHistory expectedMedicalHistory = new MedicalHistory(VALID_MEDICAL_HISTORY);
        assertEquals(expectedMedicalHistory, ParserUtil.parseMedicalHistory(VALID_MEDICAL_HISTORY));
    }

    @Test
    public void parseMedicalHistory_validValueWithWhitespace_returnsTrimmedMedicalHistory() throws Exception {
        String medicalHistoryWithWhitespace = WHITESPACE + VALID_MEDICAL_HISTORY + WHITESPACE;
        MedicalHistory expectedMedicalHistory = new MedicalHistory(VALID_MEDICAL_HISTORY);
        assertEquals(expectedMedicalHistory, ParserUtil.parseMedicalHistory(medicalHistoryWithWhitespace));
    }

    @Test
    public void parseNotes_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ParserUtil.parseNotes(null));
    }

    @Test
    public void parseNotes_blank_throwsParseException() {
        for (String notes : List.of("", " ", WHITESPACE)) {
            assertThrows(ParseException.class, Notes.MESSAGE_CONSTRAINTS, () -> ParserUtil.parseNotes(notes));
        }
    }

    @Test
    public void parseNotes_validText_normalized() throws Exception {
        assertEquals("ENJOYS GARDENING; WALKS AT 7AM",
                ParserUtil.parseNotes("  Enjoys\t gardening;\n walks at 7am  ").value);
    }

    @Test
    public void parseAppointmentDetails_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ParserUtil.parseAppointmentDetails(null));
    }

    @Test
    public void parseAppointmentDetails_blank_throwsParseException() {
        for (String details : List.of("", " ", WHITESPACE)) {
            assertThrows(ParseException.class, AppointmentDetails.MESSAGE_CONSTRAINTS, ()
                    -> ParserUtil.parseAppointmentDetails(details));
        }
    }

    @Test
    public void parseAppointmentDetails_validText_normalized() throws Exception {
        assertEquals("BRING INSULIN PEN", ParserUtil.parseAppointmentDetails("  Bring\t insulin\n pen  ").value);
    }

    @Test
    public void parseAppointment_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ParserUtil.parseAppointment((String) null));
    }

    @Test
    public void parseAppointment_invalidValue_throwsParseException() {
        assertThrows(ParseException.class, () -> ParserUtil.parseAppointment(INVALID_DATE_WITHOUT_TIME));
        assertThrows(ParseException.class, () -> ParserUtil.parseAppointment(INVALID_DATE_WITH_TIME));
    }

    @Test
    public void parseAppointment_validValueWithoutTime_returnsAppointment() throws Exception {
        Appointment expectedAppointment = new Appointment(VALID_DATE_WITHOUT_TIME);
        assertEquals(expectedAppointment, ParserUtil.parseAppointment(VALID_DATE_WITHOUT_TIME));
    }

    @Test
    public void parseAppointment_validValueWithTime_returnsAppointment() throws Exception {
        Appointment expectedAppointment = new Appointment(VALID_DATE_WITH_TIME);
        assertEquals(expectedAppointment, ParserUtil.parseAppointment(VALID_DATE_WITH_TIME));
    }

    @Test
    public void parseAppointment_validValueWithoutTimeWithWhitespace_returnsTrimmedAppointment() throws Exception {
        String appointmentWithWhitespace = WHITESPACE + VALID_DATE_WITHOUT_TIME + WHITESPACE;
        Appointment expectedAppointment = new Appointment(VALID_DATE_WITHOUT_TIME);
        assertEquals(expectedAppointment, ParserUtil.parseAppointment(appointmentWithWhitespace));
    }

    @Test
    public void parseAppointment_validValueWithTimeWithExtraWhitespace_returnsTrimmedAppointment() throws Exception {
        String appointmentWithExtraWhitespace = WHITESPACE + VALID_DATE_WITH_TIME + WHITESPACE;
        Appointment expectedAppointment = new Appointment(VALID_DATE_WITH_TIME);
        assertEquals(expectedAppointment, ParserUtil.parseAppointment(appointmentWithExtraWhitespace));
    }

    @Test
    public void parseDate_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ParserUtil.parseDate((String) null));
    }

    @Test
    public void parseDate_invalidValue_throwsParseException() {
        assertThrows(ParseException.class, () -> ParserUtil.parseDate(INVALID_DATE_WITHOUT_TIME));
        assertThrows(ParseException.class, () -> ParserUtil.parseDate(VALID_DATE_WITH_TIME));
    }

    @Test
    public void parseDate_validValueWithoutWhitespace_returnsDate() throws Exception {
        assertEquals(VALID_DATE_OBJECT, ParserUtil.parseDate(VALID_DATE_WITHOUT_TIME));
    }

    @Test
    public void parseDate_validValueWithWhitespace_returnsTrimmedDate() throws Exception {
        String dateWithWhitespace = WHITESPACE + VALID_DATE_WITHOUT_TIME + WHITESPACE;
        assertEquals(VALID_DATE_OBJECT, ParserUtil.parseDate(dateWithWhitespace));
    }

    @Test
    public void parseOptionalFieldEdit_neitherPrefix_returnsEmpty() throws Exception {
        assertEquals(Optional.empty(), parseNotesEdit(" --phone 91234567"));
    }

    @Test
    public void parseOptionalFieldEdit_replacePrefix_replacesOrClears() throws Exception {
        assertEquals(Optional.of(OptionalFieldEdit.replace(new Notes("Enjoys gardening"))),
                parseNotesEdit(" --notes Enjoys gardening"));
        assertEquals(Optional.of(OptionalFieldEdit.clear()), parseNotesEdit(" --notes "));
        assertEquals(Optional.of(OptionalFieldEdit.clear()), parseNotesEdit(" --notes " + WHITESPACE));
    }

    @Test
    public void parseOptionalFieldEdit_appendPrefix_appends() throws Exception {
        assertEquals(Optional.of(OptionalFieldEdit.append(new Notes("Enjoys gardening"))),
                parseNotesEdit(" --add-notes Enjoys gardening"));
    }

    @Test
    public void parseOptionalFieldEdit_blankAppend_throwsParseException() {
        assertThrows(ParseException.class, Notes.MESSAGE_CONSTRAINTS, () -> parseNotesEdit(" --add-notes "));
        assertThrows(ParseException.class, Notes.MESSAGE_CONSTRAINTS, () -> parseNotesEdit(" --add-notes "
                + WHITESPACE));
    }

    @Test
    public void parseOptionalFieldEdit_bothPrefixes_throwsParseException() {
        String expectedMessage = String.format(Messages.MESSAGE_CONFLICTING_FIELD_EDITS, "--notes", "--add-notes");
        assertThrows(ParseException.class, expectedMessage, () -> parseNotesEdit(" --notes A --add-notes B"));
        assertThrows(ParseException.class, expectedMessage, () -> parseNotesEdit(" --add-notes B --notes "));
    }

    /**
     * Returns the edit of the notes that {@code args} gives with {@code --notes} or {@code --add-notes}.
     */
    private static Optional<OptionalFieldEdit<Notes>> parseNotesEdit(String args) throws ParseException {
        ArgumentMultimap argMultimap = ArgumentTokenizer.tokenize(args, PREFIX_NOTES, PREFIX_ADD_NOTES,
                PREFIX_PHONE);
        return ParserUtil.parseOptionalFieldEdit(argMultimap, PREFIX_NOTES, PREFIX_ADD_NOTES, ParserUtil::parseNotes);
    }
}
