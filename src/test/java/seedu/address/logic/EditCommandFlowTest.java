package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.CliSyntax.PREFIX_NAME;
import static seedu.address.logic.parser.CliSyntax.PREFIX_NOTES;
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
import seedu.address.logic.commands.EditCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.LaiNurse;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.patient.MedicalHistory;
import seedu.address.model.patient.Notes;
import seedu.address.model.patient.Patient;
import seedu.address.model.patient.PatientId;
import seedu.address.model.patient.Phone;
import seedu.address.storage.JsonLaiNurseStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.Storage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PatientBuilder;
import seedu.address.testutil.SampleData;

/**
 * Tests edit commands from user input through model updates and JSON persistence, starting from the sample data.
 */
public class EditCommandFlowTest {
    private static final String ALEX_ID = "S1234567A"; // appointments 2026-10-05 09:30 and 2026-11-02
    private static final String CHARLOTTE_ID = "T0123456C"; // no medical history or appointments

    private static final String INVALID_FORMAT_MESSAGE =
            String.format(MESSAGE_INVALID_COMMAND_FORMAT, EditCommand.MESSAGE_USAGE);

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
    public void execute_editPhone_onlyPhoneChanged() throws Exception {
        Patient expected = new PatientBuilder(patient(ALEX_ID)).withPhone("91234567").build();
        assertEditPersisted("edit S1234567A --phone 91234567", ALEX_ID, expected);
    }

    @Test
    public void execute_tabSeparatedOptions_edited() throws Exception {
        Patient expected = new PatientBuilder(patient(ALEX_ID)).withPhone("71234567").build();
        assertEditPersisted("edit S1234567A\t--phone\t71234567", ALEX_ID, expected);
    }

    @Test
    public void execute_lowerCaseIdAndExtraSpaces_normalized() throws Exception {
        Patient expected = new PatientBuilder(patient(ALEX_ID)).withName("Alex Tan").build();
        assertEditPersisted("edit   s1234567a   --name   alex    tan  ", ALEX_ID, expected);
    }

    @Test
    public void execute_nameMatchesHiddenArchivedPatient_warnsAndPersistsEdit() throws Exception {
        Patient archived = patient(CHARLOTTE_ID).withArchived(true);
        model.setPatient(patient(CHARLOTTE_ID), archived);
        logic.execute("find --name nobody");
        Patient original = patient(ALEX_ID);
        Patient expected = new PatientBuilder(original).withName("Charlotte Oliveiro").build();
        LaiNurse expectedLaiNurse = new LaiNurse(model.getLaiNurse());
        expectedLaiNurse.setPatient(original, expected);

        CommandResult result = logic.execute("edit " + ALEX_ID + " --name charlotte   oliveiro");

        assertEquals(String.format(EditCommand.MESSAGE_EDIT_PATIENT_SUCCESS, Messages.format(expected))
                + "\nWarning: this name is also used by CHARLOTTE OLIVEIRO (T0123456C) [archived]. "
                + "Check the patient IDs.", result.getFeedbackToUser());
        assertEquals(Optional.of(Navigation.toPatient(expected.getId())), result.getNavigation());
        assertEquals(original.getSchedule(), patient(ALEX_ID).getSchedule());
        assertEquals(expectedLaiNurse, model.getLaiNurse());
        assertTrue(model.getFilteredPatientList().contains(expected));
        assertEquals(expectedLaiNurse, storage.readLaiNurse().orElseThrow());
    }

    @Test
    public void execute_notesOnlyWithSharedName_warnsAndPersistsEdit() throws Exception {
        Patient original = patient(ALEX_ID);
        Patient other = new PatientBuilder(original).withId("SHARED1").build();
        model.addPatient(other);
        Patient expected = new PatientBuilder(original).withNotes("Prefers morning visits").build();
        LaiNurse expectedLaiNurse = new LaiNurse(model.getLaiNurse());
        expectedLaiNurse.setPatient(original, expected);

        CommandResult result = logic.execute("edit " + ALEX_ID + " --notes Prefers morning visits");

        assertEquals(String.format(EditCommand.MESSAGE_EDIT_PATIENT_SUCCESS, Messages.format(expected))
                + "\nWarning: this name is also used by ALEX YEOH (SHARED1). Check the patient IDs.",
                result.getFeedbackToUser());
        assertEquals(Optional.of(Navigation.toPatient(expected.getId())), result.getNavigation());
        assertEquals(original.getSchedule(), patient(ALEX_ID).getSchedule());
        assertEquals(expectedLaiNurse, model.getLaiNurse());
        assertEquals(expectedLaiNurse, storage.readLaiNurse().orElseThrow());
    }

    @Test
    public void execute_addMedicalHistoryToPatientWithout_persisted() throws Exception {
        Patient expected = new PatientBuilder(patient(CHARLOTTE_ID)).withMedicalHistory("Asthma").build();
        assertEditPersisted("edit T0123456C --medical-history Asthma", CHARLOTTE_ID, expected);
    }

    @Test
    public void execute_addNotesToPatientWithout_persisted() throws Exception {
        Patient expected = new PatientBuilder(patient(CHARLOTTE_ID)).withNotes("Enjoys gardening").build();
        assertEditPersisted("edit T0123456C --notes Enjoys\t gardening", CHARLOTTE_ID, expected);
    }

    @Test
    public void execute_replaceNotes_persisted() throws Exception {
        Patient expected = new PatientBuilder(patient(ALEX_ID)).withNotes("Walks every morning").build();
        assertEditPersisted("edit S1234567A --notes Walks   every morning", ALEX_ID, expected);
    }

    @Test
    public void execute_clearMedicalHistoryAndNotes_persisted() throws Exception {
        Patient expected = new PatientBuilder(patient(ALEX_ID)).withoutMedicalHistory().withoutNotes().build();
        assertEditPersisted("edit S1234567A --medical-history --notes", ALEX_ID, expected);

        // clearing a field that is already empty changes nothing, and is allowed
        assertEditPersisted("edit S1234567A\t--notes\t \t--name Alex Yeoh", ALEX_ID, expected);
    }

    @Test
    public void execute_addToMedicalHistoryAndNotes_persisted() throws Exception {
        Patient alex = patient(ALEX_ID);
        Patient expected = new PatientBuilder(alex)
                .withMedicalHistory(alex.getMedicalHistory().orElseThrow().value + "; Asthma")
                .withNotes(alex.getNotes().orElseThrow().value + "; Prefers   morning visits").build();
        assertEditPersisted("edit S1234567A --add-medical-history Asthma --add-notes Prefers   morning visits",
                ALEX_ID, expected);
    }

    @Test
    public void execute_addToEmptyMedicalHistory_setsIt() throws Exception {
        Patient expected = new PatientBuilder(patient(CHARLOTTE_ID)).withMedicalHistory("Asthma").build();
        assertEditPersisted("edit T0123456C --add-medical-history asthma", CHARLOTTE_ID, expected);
    }

    @Test
    public void execute_notesAfterReload_foundAndViewed() throws Exception {
        logic.execute("edit T0123456C --notes Enjoys gardening");
        Model reloadedModel = new ModelManager(storage.readLaiNurse().orElseThrow(), new UserPrefs());
        Logic reloadedLogic = new LogicManager(reloadedModel, storage);
        reloadedLogic.execute("find --notes \"enjoys   garden\"");
        assertEquals(List.of(patient(CHARLOTTE_ID)), reloadedModel.getFilteredPatientList());
        CommandResult viewResult = reloadedLogic.execute("view T0123456C");
        assertEquals(Optional.of(new PatientId(CHARLOTTE_ID)), viewResult.getNavigation().flatMap(Navigation::patient));
        Patient reloadedCharlotte = reloadedModel.findPatientById(new PatientId(CHARLOTTE_ID)).orElseThrow();
        assertEquals("ENJOYS GARDENING", reloadedCharlotte.getNotes().orElseThrow().value);
    }

    @Test
    public void execute_editPhone_keepsNotesAndAppointmentDetails() throws Exception {
        // Omitting --notes preserves the notes, appointments are kept as-is,
        // and their details survive unrelated edits
        Patient before = patient(ALEX_ID);
        logic.execute("edit S1234567A --phone 91234567");
        Patient after = patient(ALEX_ID);
        assertTrue(before.getNotes().isPresent());
        assertEquals(before.getNotes(), after.getNotes());
        assertEquals(before.getSchedule(), after.getSchedule());
        assertTrue(after.getSchedule().asList().get(0).getDetails().isPresent());
    }

    @Test
    public void execute_unknownId_rejectedWithoutChanges() throws Exception {
        assertRejectedCommandLeavesDataUnchanged("edit S0000000Z --phone 91234567", CommandException.class,
                String.format(Messages.MESSAGE_PATIENT_NOT_FOUND, "S0000000Z"));

        // a list number is read as an ID, which no patient has
        assertRejectedCommandLeavesDataUnchanged("edit 1 --name Test", CommandException.class,
                String.format(Messages.MESSAGE_PATIENT_NOT_FOUND, "1"));
    }

    @Test
    public void execute_invalidValues_rejectedWithoutChanges() throws Exception {
        assertRejectedCommandLeavesDataUnchanged("edit S1234567A --phone 12", ParseException.class,
                Phone.MESSAGE_CONSTRAINTS);
        // text to add cannot be blank
        assertRejectedCommandLeavesDataUnchanged("edit S1234567A --add-medical-history  --name Alex",
                ParseException.class, MedicalHistory.MESSAGE_CONSTRAINTS);
        assertRejectedCommandLeavesDataUnchanged("edit S1234567A --add-notes \t ", ParseException.class,
                Notes.MESSAGE_CONSTRAINTS);
        assertRejectedCommandLeavesDataUnchanged("edit S1234567A --add-notes --phone 91234567", ParseException.class,
                Notes.MESSAGE_CONSTRAINTS);

        // a field cannot be both replaced and added to
        assertRejectedCommandLeavesDataUnchanged("edit S1234567A --notes Walks --add-notes Swims",
                ParseException.class, String.format(Messages.MESSAGE_CONFLICTING_FIELD_EDITS, "--notes",
                        "--add-notes"));
        assertRejectedCommandLeavesDataUnchanged("edit S1234567A --add-medical-history Asthma --medical-history",
                ParseException.class, String.format(Messages.MESSAGE_CONFLICTING_FIELD_EDITS, "--medical-history",
                        "--add-medical-history"));

        // option with no value at the end of the command
        assertRejectedCommandLeavesDataUnchanged("edit S1234567A --phone", ParseException.class,
                Phone.MESSAGE_CONSTRAINTS);
    }

    @Test
    public void execute_invalidFormat_rejectedWithoutChanges() throws Exception {
        // nothing to edit
        assertRejectedCommandLeavesDataUnchanged("edit S1234567A", ParseException.class,
                EditCommand.MESSAGE_NOT_EDITED);

        // missing or invalid ID
        assertRejectedCommandLeavesDataUnchanged("edit --name Alex", ParseException.class, INVALID_FORMAT_MESSAGE);
        assertRejectedCommandLeavesDataUnchanged("edit S12345678901 --name Alex", ParseException.class,
                INVALID_FORMAT_MESSAGE);

        // the ID cannot be edited, and the retired appointment options are rejected like any
        // other unknown option
        assertRejectedCommandLeavesDataUnchanged("edit S1234567A --id S9999999Z", ParseException.class,
                INVALID_FORMAT_MESSAGE);
        assertRejectedCommandLeavesDataUnchanged("edit S1234567A --appt 2026-10-10", ParseException.class,
                INVALID_FORMAT_MESSAGE);
        assertRejectedCommandLeavesDataUnchanged("edit S1234567A --add-appt 2026-10-20", ParseException.class,
                INVALID_FORMAT_MESSAGE);
        assertRejectedCommandLeavesDataUnchanged("edit S1234567A --remove-appt 2026-10-20 09:30", ParseException.class,
                INVALID_FORMAT_MESSAGE);

        // single-valued field repeated
        assertRejectedCommandLeavesDataUnchanged("edit S1234567A --name Alex --name Bob", ParseException.class,
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_NAME));
        assertRejectedCommandLeavesDataUnchanged("edit S1234567A --notes First --notes Second", ParseException.class,
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_NOTES));
    }

    /**
     * Returns the patient with the given ID from the model.
     */
    private Patient patient(String id) {
        PatientId patientId = new PatientId(id);
        return model.getLaiNurse().getPatientList().stream()
                .filter(patient -> patient.getId().equals(patientId)).findFirst().orElseThrow();
    }

    /**
     * Executes {@code command} and asserts that only the patient with {@code id} changed, to {@code expected},
     * and that the change is saved to the data file.
     */
    private void assertEditPersisted(String command, String id, Patient expected) throws Exception {
        LaiNurse expectedLaiNurse = new LaiNurse(model.getLaiNurse());
        expectedLaiNurse.setPatient(patient(id), expected);

        CommandResult result = logic.execute(command);

        assertEquals(String.format(EditCommand.MESSAGE_EDIT_PATIENT_SUCCESS, Messages.format(expected)),
                result.getFeedbackToUser());
        assertEquals(expectedLaiNurse, model.getLaiNurse());
        assertEquals(model.getLaiNurse().getPatientList(), model.getFilteredPatientList());
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
}
