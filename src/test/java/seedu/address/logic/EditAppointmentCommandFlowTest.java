package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.testutil.Assert.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.CommandResult.Navigation;
import seedu.address.logic.commands.CommandResult.View;
import seedu.address.logic.commands.DeleteAppointmentCommand;
import seedu.address.logic.commands.EditAppointmentCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.LaiNurse;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.patient.Appointment;
import seedu.address.model.patient.AppointmentDetails;
import seedu.address.model.patient.Patient;
import seedu.address.model.patient.PatientAppointment;
import seedu.address.model.patient.PatientId;
import seedu.address.storage.JsonLaiNurseStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.Storage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PatientBuilder;
import seedu.address.testutil.SampleData;

/**
 * Tests editappt commands from user input through model updates and JSON persistence, starting from the sample data.
 */
public class EditAppointmentCommandFlowTest {
    // appointments 2026-10-05 09:30 (INSULIN REVIEW) and 2026-11-02 (MONTHLY BLOOD TEST)
    private static final String ALEX_ID = "S1234567A";
    private static final String BERNICE_ID = "S2345678B"; // appointment 2026-10-06, with no details

    private static final String INVALID_FORMAT_MESSAGE =
            String.format(MESSAGE_INVALID_COMMAND_FORMAT, EditAppointmentCommand.MESSAGE_USAGE);

    @TempDir
    public Path temporaryFolder;

    private Model model;
    private Storage storage;
    private Logic logic;

    @BeforeEach
    public void setUp() throws Exception {
        model = new ModelManager(SampleData.getSampleLaiNurse(), new UserPrefs());
        storage = new StorageManager(new JsonLaiNurseStorage(temporaryFolder.resolve("laiNurse.json")),
                new JsonUserPrefsStorage(temporaryFolder.resolve("userPrefs.json")));
        storage.saveLaiNurse(model.getLaiNurse());
        logic = new LogicManager(model, storage);
    }

    @Test
    public void execute_moveAppointment_keepsDetailsAndPersists() throws Exception {
        Patient expected = new PatientBuilder(patient(ALEX_ID)).withAppointments()
                .withAppointment("2026-10-06 10:00", "Insulin review")
                .withAppointment("2026-11-02", "Monthly blood test").build();
        assertEditPersisted("editappt S1234567A 1 --date 2026-10-06 10:00", expected,
                appointment("2026-10-06 10:00", "Insulin review"),
                "2026-10-06 10:00; Details: INSULIN REVIEW (moved from 2026-10-05 09:30)");
    }

    @Test
    public void execute_setDetails_persists() throws Exception {
        Patient expected = new PatientBuilder(patient(BERNICE_ID)).withAppointments()
                .withAppointment("2026-10-06", "Check blood pressure").build();
        assertEditPersisted("editappt s2345678b 1 --details Check   blood pressure", expected,
                appointment("2026-10-06", "Check blood pressure"), "2026-10-06; Details: CHECK BLOOD PRESSURE");
    }

    @Test
    public void execute_clearDetails_persists() throws Exception {
        Patient expected = new PatientBuilder(patient(ALEX_ID)).withAppointments("2026-10-05 09:30")
                .withAppointment("2026-11-02", "Monthly blood test").build();
        assertEditPersisted("editappt S1234567A 1 --details", expected, new Appointment("2026-10-05 09:30"),
                "2026-10-05 09:30");
    }

    @Test
    public void execute_addToDetails_persists() throws Exception {
        Patient expected = new PatientBuilder(patient(ALEX_ID)).withAppointments()
                .withAppointment("2026-10-05 09:30", "Insulin review")
                .withAppointment("2026-11-03", "Monthly blood test; Fasting").build();
        assertEditPersisted("editappt S1234567A 2 --date 2026-11-03 --add-details fasting", expected,
                appointment("2026-11-03", "Monthly blood test; Fasting"),
                "2026-11-03; Details: MONTHLY BLOOD TEST; FASTING (moved from 2026-11-02)");
    }

    @Test
    public void execute_addToEmptyDetails_setsThem() throws Exception {
        Patient expected = new PatientBuilder(patient(BERNICE_ID)).withAppointments()
                .withAppointment("2026-10-06", "Check blood pressure").build();
        assertEditPersisted("editappt S2345678B 1 --add-details Check blood pressure", expected,
                appointment("2026-10-06", "Check blood pressure"), "2026-10-06; Details: CHECK BLOOD PRESSURE");
    }

    @Test
    public void execute_moveAndReplaceDetails_persists() throws Exception {
        Patient expected = new PatientBuilder(patient(ALEX_ID)).withAppointments()
                .withAppointment("2026-10-05 09:30", "Insulin review")
                .withAppointment("2026-11-03 08:00", "Fasting blood test").build();
        assertEditPersisted("editappt S1234567A\t2\t--date\t2026-11-03 08:00"
                + "\t--details\tFasting blood test", expected, appointment("2026-11-03 08:00", "Fasting blood test"),
                "2026-11-03 08:00; Details: FASTING BLOOD TEST (moved from 2026-11-02)");
    }

    @Test
    public void execute_patientNotInDisplayedList_edited() throws Exception {
        logic.execute("find --name bernice");
        Patient expected = new PatientBuilder(patient(ALEX_ID)).withAppointments()
                .withAppointment("2026-10-05 09:30", "Bring insulin pen")
                .withAppointment("2026-11-02", "Monthly blood test").build();

        CommandResult result = logic.execute("editappt S1234567A 1 --details Bring insulin pen");

        assertEquals(expected, patient(ALEX_ID));
        assertEquals(Optional.of(View.APPOINTMENTS), result.getView());
    }

    @Test
    public void execute_missingAppointment_rejectedWithoutChanges() throws Exception {
        assertRejectedCommandLeavesDataUnchanged("editappt S1234567A 99 --date 2026-10-06",
                CommandException.class,
                String.format(Messages.MESSAGE_APPOINTMENT_NUMBER_NOT_FOUND, ALEX_ID, 99));
    }

    @Test
    public void execute_moveOntoOtherAppointment_warnsAndPersistsBoth() throws Exception {
        CommandResult result = logic.execute("editappt S1234567A 2 --date 2026-10-05 09:30");
        assertEquals("Edited appointment 2 of ALEX YEOH (S1234567A): 2026-10-05 09:30; Details: MONTHLY BLOOD TEST"
                + " (moved from 2026-11-02)\n"
                + "Warning: it clashes with this patient's appointment #1.", result.getFeedbackToUser());
        assertEquals(List.of(1, 2), patient(ALEX_ID).getSchedule().asList().stream()
                .map(Appointment::getNumber).toList());
        assertEquals(model.getLaiNurse(), storage.readLaiNurse().orElseThrow());
    }

    @Test
    public void execute_moveOntoDateOnlyAppointment_noWarning() throws Exception {
        // appointments with no time never clash, and the moved one comes after the blood test there
        CommandResult result = logic.execute("editappt S1234567A 1 --date 2026-11-02");
        assertEquals("Edited appointment 2 of ALEX YEOH (S1234567A): 2026-11-02; Details: INSULIN REVIEW"
                + " (moved from 2026-10-05 09:30)", result.getFeedbackToUser());
    }

    @Test
    public void execute_invalidInput_rejectedWithoutChanges() throws Exception {
        assertRejectedCommandLeavesDataUnchanged("editappt S0000000Z 1 --date 2026-10-06",
                CommandException.class, String.format(Messages.MESSAGE_PATIENT_NOT_FOUND, "S0000000Z"));
        assertRejectedCommandLeavesDataUnchanged("editappt S1234567A 1",
                ParseException.class, EditAppointmentCommand.MESSAGE_NOT_EDITED);
        assertRejectedCommandLeavesDataUnchanged("editappt S1234567A --date 2026-10-06", ParseException.class,
                INVALID_FORMAT_MESSAGE);
        assertRejectedCommandLeavesDataUnchanged("editappt S1234567A 1 --date 2026-02-30",
                ParseException.class, Appointment.MESSAGE_CONSTRAINTS);
        assertRejectedCommandLeavesDataUnchanged("editappt S1234567A 1 --add-details",
                ParseException.class, AppointmentDetails.MESSAGE_CONSTRAINTS);
        assertRejectedCommandLeavesDataUnchanged("editappt S1234567A 1 --details --add-details Fasting",
                ParseException.class, String.format(Messages.MESSAGE_CONFLICTING_FIELD_EDITS, "--details",
                        "--add-details"));
        for (String number : List.of("0", "-1", "+1", "1.5")) {
            assertRejectedCommandLeavesDataUnchanged("editappt S1234567A " + number + " --details Review",
                    ParseException.class, Appointment.MESSAGE_NUMBER_CONSTRAINTS);
        }
    }

    /**
     * Asserts that {@code command} succeeds, replaces the patient with {@code expected}'s ID by {@code expected},
     * reports {@code expectedAppointmentText}, selects {@code expectedAppointment} in the appointment view, and
     * saves the change to the data file.
     */
    private void assertEditPersisted(String command, Patient expected, Appointment expectedAppointment,
            String expectedAppointmentText) throws Exception {
        expectedAppointment = expected.getSchedule().findAtDateTime(expectedAppointment).getLast();
        LaiNurse expectedLaiNurse = new LaiNurse(model.getLaiNurse());
        expectedLaiNurse.setPatient(patient(expected.getId().value), expected);

        CommandResult result = logic.execute(command);

        assertEquals(String.format(EditAppointmentCommand.MESSAGE_EDIT_APPOINTMENT_SUCCESS,
                expectedAppointment.getNumber(), expected.getName(),
                expected.getId(), expectedAppointmentText)
                + Messages.formatClashWarning(model.getAppointmentList(),
                        new PatientAppointment(expected, expectedAppointment)), result.getFeedbackToUser());
        assertEquals(Optional.of(Navigation.toAppointment(new PatientAppointment(expected, expectedAppointment))),
                result.getNavigation());
        assertEquals(expectedLaiNurse, model.getLaiNurse());
        assertEquals(expectedLaiNurse, storage.readLaiNurse().orElseThrow());
    }

    /**
     * Asserts that {@code command} fails with the given exception and message,
     * and leaves the model, the displayed list and the data file unchanged.
     */
    private void assertRejectedCommandLeavesDataUnchanged(String command, Class<? extends Throwable> expectedException,
            String expectedMessage) throws Exception {
        LaiNurse expectedLaiNurse = new LaiNurse(model.getLaiNurse());
        List<Patient> expectedFilteredList = List.copyOf(model.getFilteredPatientList());
        String expectedJson = Files.readString(storage.getLaiNurseFilePath());

        assertThrows(expectedException, expectedMessage, () -> logic.execute(command));

        assertEquals(expectedLaiNurse, model.getLaiNurse());
        assertEquals(expectedFilteredList, model.getFilteredPatientList());
        assertEquals(expectedJson, Files.readString(storage.getLaiNurseFilePath()));
    }

    private Patient patient(String id) {
        PatientId patientId = new PatientId(id);
        return model.getLaiNurse().getPatientList().stream()
                .filter(patient -> patient.getId().equals(patientId)).findFirst().orElseThrow();
    }

    private static Appointment appointment(String dateTime, String details) {
        return new Appointment(dateTime, Optional.of(new AppointmentDetails(details)));
    }
    @Test
    public void execute_legacySelector_rejectedWithoutChanges() throws Exception {
        logic.execute("addappt S1234567A --date 2026-10-05 09:30 --details Second visit");
        assertRejectedCommandLeavesDataUnchanged("editappt S1234567A --appt 2026-10-05 09:30 --date 2026-10-06",
                ParseException.class, INVALID_FORMAT_MESSAGE);
        assertRejectedCommandLeavesDataUnchanged("deleteappt S1234567A --appt 2026-10-05 09:30",
                ParseException.class,
                String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeleteAppointmentCommand.MESSAGE_USAGE));
    }

    @Test
    public void execute_numberSelectsDuplicate_movesItAndRenumbers() throws Exception {
        logic.execute("addappt S1234567A --date 2026-10-05 09:30 --details Second visit");
        // the second visit at 09:30 is #2, and moving it before the others makes it #1
        CommandResult result = logic.execute("editappt S1234567A 2 --date 2026-09-01");
        Appointment moved = patient(ALEX_ID).getSchedule().findByNumber(1).orElseThrow();
        assertEquals("2026-09-01", moved.value);
        assertEquals("SECOND VISIT", moved.getDetails().orElseThrow().value);
        assertEquals(1, result.getNavigation().orElseThrow().appointment().orElseThrow()
                .getAppointment().getNumber());
        assertEquals(1, patient(ALEX_ID).getSchedule().findAtDateTime(new Appointment("2026-10-05 09:30")).size());
        assertEquals(model.getLaiNurse(), storage.readLaiNurse().orElseThrow());
    }

    @Test
    public void execute_missingNumber_rejectedWithoutChanges() throws Exception {
        assertRejectedCommandLeavesDataUnchanged("editappt S1234567A 99 --details Review", CommandException.class,
                "Patient S1234567A has no appointment #99.");
        assertRejectedCommandLeavesDataUnchanged("deleteappt S1234567A 99", CommandException.class,
                "Patient S1234567A has no appointment #99.");
    }

    @Test
    public void execute_detailsOnlyClash_warnsAboutEveryOtherVisit() throws Exception {
        logic.execute("addappt S1234567A --date 2026-10-05 09:30");
        logic.execute("addappt S2345678B --date 2026-10-05 09:30");
        CommandResult result = logic.execute("editappt S1234567A 1 --details Review");
        assertEquals("Edited appointment 1 of ALEX YEOH (S1234567A): 2026-10-05 09:30; Details: REVIEW\n"
                + "Warning: it clashes with this patient's appointment #2; "
                + "BERNICE YU (S2345678B), appointment #1.", result.getFeedbackToUser());
    }

}
