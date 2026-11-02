package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import seedu.address.logic.commands.MarkAppointmentCommand;
import seedu.address.logic.commands.UnmarkAppointmentCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.LaiNurse;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.patient.Appointment;
import seedu.address.model.patient.Patient;
import seedu.address.model.patient.PatientAppointment;
import seedu.address.model.patient.PatientId;
import seedu.address.storage.JsonLaiNurseStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.Storage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.SampleData;

/**
 * Exercises completion commands through parsing, model updates, navigation and JSON persistence.
 */
public class AppointmentStatusCommandFlowTest {

    private static final PatientId ALEX_ID = new PatientId("S1234567A");

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
    public void execute_markAndUnmark_roundTripPreservesAllOtherData() throws Exception {
        LaiNurse original = new LaiNurse(model.getLaiNurse());
        Patient originalAlex = alex();
        Appointment originalVisit = appointment(1);
        CommandResult marked = logic.execute("markappt\u00a0s1234567a\u202f1");
        assertTrue(appointment(1).isDone());
        assertEquals(originalVisit.withDone(true), appointment(1));
        assertEquals(originalAlex.getSchedule().findByNumber(2), alex().getSchedule().findByNumber(2));
        assertEquals(Optional.of(View.APPOINTMENTS), marked.getView());
        assertEquals(Optional.of(Navigation.toAppointment(new PatientAppointment(alex(), appointment(1)))),
                marked.getNavigation());
        assertTrue(marked.getAppointmentRange().isEmpty());
        assertEquals(model.getLaiNurse(), storage.readLaiNurse().orElseThrow());

        // Reload the saved data before unmarking, as on an application restart.
        model = new ModelManager(storage.readLaiNurse().orElseThrow(), new UserPrefs());
        logic = new LogicManager(model, storage);
        CommandResult unmarked = logic.execute("unmarkappt S1234567A 1");
        assertFalse(appointment(1).isDone());
        assertEquals(Optional.of(Navigation.toAppointment(new PatientAppointment(alex(), appointment(1)))),
                unmarked.getNavigation());
        assertEquals(original, model.getLaiNurse());
        assertEquals(original, storage.readLaiNurse().orElseThrow());
    }

    @Test
    public void execute_hiddenPatient_keepsFilterForBothCommands() throws Exception {
        logic.execute("find --name bernice");
        List<Patient> visible = List.copyOf(model.getFilteredPatientList());
        logic.execute("markappt S1234567A 1");
        assertEquals(visible, model.getFilteredPatientList());
        logic.execute("unmarkappt S1234567A 1");
        assertEquals(visible, model.getFilteredPatientList());
        assertEquals(model.getLaiNurse(), storage.readLaiNurse().orElseThrow());
    }

    @Test
    public void execute_sameTimeVisits_usesNumberAndWarnsAboutHiddenPatientOnUnmark() throws Exception {
        logic.execute("addappt S1234567A --date 2026-10-05 09:30 --details Second visit");
        logic.execute("addappt S2345678B --date 2026-10-05 09:30");
        logic.execute("find --name alex");
        // the second visit at 09:30 is #2, and marking it keeps its place
        CommandResult marked = logic.execute("markappt S1234567A 2");
        assertFalse(appointment(1).isDone());
        assertTrue(appointment(2).isDone());
        assertEquals("SECOND VISIT", appointment(2).getDetails().orElseThrow().value);
        assertFalse(marked.getFeedbackToUser().contains("Warning:"));

        CommandResult unmarked = logic.execute("unmarkappt S1234567A 2");
        // Alex's own other visit is not named again, and Bernice's is
        assertTrue(unmarked.getFeedbackToUser().contains("this patient's appointment #1"));
        assertFalse(unmarked.getFeedbackToUser().contains("ALEX YEOH (S1234567A), appointment"));
        assertTrue(unmarked.getFeedbackToUser().contains("BERNICE YU (S2345678B), appointment #1"));
        assertEquals(List.of(1, 2, 3), alex().getSchedule().asList().stream().map(Appointment::getNumber).toList());
        assertEquals("SECOND VISIT", appointment(2).getDetails().orElseThrow().value);
        assertEquals(model.getLaiNurse(), storage.readLaiNurse().orElseThrow());
    }

    @Test
    public void execute_editAndPatientUpdates_preserveCompletion() throws Exception {
        logic.execute("markappt S1234567A 1");
        logic.execute("editappt S1234567A 1 --details Bring pen");
        assertTrue(appointment(1).isDone());
        // moved after the blood test that is already on 2026-11-02, so it becomes #2
        CommandResult moved = logic.execute("editappt S1234567A 1 --date 2026-11-02");
        assertTrue(appointment(2).isDone());
        assertEquals("BRING PEN", appointment(2).getDetails().orElseThrow().value);
        assertFalse(appointment(1).isDone());
        assertFalse(moved.getFeedbackToUser().contains("Warning:"));
        logic.execute("edit S1234567A --phone 91234567");
        assertTrue(appointment(2).isDone());
        logic.execute("addappt S1234567A --date 2026-12-01");
        assertFalse(appointment(3).isDone());
        assertEquals(model.getLaiNurse(), storage.readLaiNurse().orElseThrow());
        // deleting #1 moves the marked appointment up to #1
        logic.execute("deleteappt S1234567A 1");
        assertTrue(appointment(1).isDone());
        assertEquals(2, alex().getSchedule().asList().size());
    }

    @Test
    public void execute_missingAndRepeatedStatus_failWithoutModelFilterOrFileChanges() throws Exception {
        assertRejected("unmarkappt S1234567A 1", CommandException.class,
                String.format(UnmarkAppointmentCommand.MESSAGE_ALREADY_UNDONE, 1, ALEX_ID));
        logic.execute("markappt S1234567A 1");
        logic.execute("find --name bernice");
        assertRejected("markappt S1234567A 1", CommandException.class,
                String.format(MarkAppointmentCommand.MESSAGE_ALREADY_DONE, 1, ALEX_ID));
        for (String command : List.of("markappt", "unmarkappt")) {
            assertRejected(command + " S0000000Z 1", CommandException.class,
                    String.format(Messages.MESSAGE_PATIENT_NOT_FOUND, "S0000000Z"));
            assertRejected(command + " S1234567A 99", CommandException.class,
                    String.format(Messages.MESSAGE_APPOINTMENT_NUMBER_NOT_FOUND, ALEX_ID, 99));
            assertRejected(command + " S1234567A 0", ParseException.class, Appointment.MESSAGE_NUMBER_CONSTRAINTS);
        }
    }

    private void assertRejected(String command, Class<? extends Throwable> exception, String message) throws Exception {
        LaiNurse expected = new LaiNurse(model.getLaiNurse());
        List<Patient> visible = List.copyOf(model.getFilteredPatientList());
        String json = Files.readString(storage.getLaiNurseFilePath());
        assertThrows(exception, message, () -> logic.execute(command));
        assertEquals(expected, model.getLaiNurse());
        assertEquals(visible, model.getFilteredPatientList());
        assertEquals(json, Files.readString(storage.getLaiNurseFilePath()));
    }

    private Patient alex() {
        return model.findPatientById(ALEX_ID).orElseThrow();
    }

    private Appointment appointment(int number) {
        return alex().getSchedule().findByNumber(number).orElseThrow();
    }
}
