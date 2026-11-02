package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.Messages.MESSAGE_PATIENT_NOT_FOUND;
import static seedu.address.logic.Messages.MESSAGE_UNKNOWN_COMMAND;
import static seedu.address.logic.commands.CommandTestUtil.ADDRESS_DESC_AMY;
import static seedu.address.logic.commands.CommandTestUtil.ID_DESC_AMY;
import static seedu.address.logic.commands.CommandTestUtil.NAME_DESC_AMY;
import static seedu.address.logic.commands.CommandTestUtil.PHONE_DESC_AMY;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPatients.AMY;
import static seedu.address.testutil.TypicalPatients.BOB;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.core.FeatureFlags;
import seedu.address.logic.commands.AddCommand;
import seedu.address.logic.commands.ClearAppointmentCommand;
import seedu.address.logic.commands.ClearCommand;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.CommandResult.AppointmentRange;
import seedu.address.logic.commands.CommandResult.Navigation;
import seedu.address.logic.commands.CommandResult.View;
import seedu.address.logic.commands.DeleteCommand;
import seedu.address.logic.commands.ListCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.Completion;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.PatientScope;
import seedu.address.model.ReadOnlyLaiNurse;
import seedu.address.model.UserPrefs;
import seedu.address.model.patient.Patient;
import seedu.address.model.patient.PatientAppointment;
import seedu.address.model.patient.PatientId;
import seedu.address.storage.BackupFailedException;
import seedu.address.storage.JsonLaiNurseStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PatientBuilder;

public class LogicManagerTest {
    private static final IOException DUMMY_IO_EXCEPTION = new IOException("dummy IO exception");
    private static final IOException DUMMY_AD_EXCEPTION = new AccessDeniedException("dummy access denied exception");

    @TempDir
    public Path temporaryFolder;

    private Model model = new ModelManager();
    private Logic logic;

    @BeforeEach
    public void setUp() {
        JsonLaiNurseStorage laiNurseStorage =
                new JsonLaiNurseStorage(temporaryFolder.resolve("laiNurse.json"));
        JsonUserPrefsStorage userPrefsStorage = new JsonUserPrefsStorage(temporaryFolder.resolve("userPrefs.json"));
        StorageManager storage = new StorageManager(laiNurseStorage, userPrefsStorage);
        logic = new LogicManager(model, storage);
    }

    @AfterEach
    public void tearDown() {
        FeatureFlags.setSettingsEnabled(false);
        FeatureFlags.setSelectiveClearEnabled(false);
    }

    @Test
    public void execute_clearBeforeRelease_clearsAtOnce() throws Exception {
        model.addPatient(new PatientBuilder(AMY).build());
        // selective clearing is not released, so clear works as before, and clearappt is unknown (#217)
        assertParseException("clearappt S1", MESSAGE_UNKNOWN_COMMAND);
        logic.execute("clear");
        assertTrue(model.getLaiNurse().getPatientList().isEmpty());
    }

    @Test
    public void execute_clearAfterRelease_asksForThePhrase() throws Exception {
        FeatureFlags.setSelectiveClearEnabled(true);
        Patient amy = new PatientBuilder(AMY).withAppointments("2026-10-05", "2026-10-06").build();
        model.addPatient(amy);

        // the first time, clear only says what would go
        CommandResult prompt = logic.execute("clear");
        assertEquals(String.format(Messages.MESSAGE_CONFIRM, String.format(ClearCommand.MESSAGE_DELETES, "1 patient",
                "2 appointments")),
                prompt.getFeedbackToUser());
        assertEquals(1, model.getLaiNurse().getPatientList().size());

        // any other input cancels it, and runs no command
        assertEquals(Messages.MESSAGE_CANCELLED, logic.execute("view").getFeedbackToUser());
        assertEquals(1, model.getLaiNurse().getPatientList().size());

        // the phrase is case-sensitive
        logic.execute("clear");
        assertEquals(Messages.MESSAGE_CANCELLED, logic.execute("CONFIRM").getFeedbackToUser());
        assertEquals(1, model.getLaiNurse().getPatientList().size());

        // the phrase confirms it
        logic.execute("clearappt " + amy.getId().value + " --to 2026-10-05");
        assertEquals(ClearAppointmentCommand.MESSAGE_SUCCESS.formatted("1 appointment"),
                logic.execute(Messages.CONFIRMATION_PHRASE).getFeedbackToUser());
        assertEquals(1, model.findPatientById(amy.getId()).orElseThrow().getSchedule().asList().size());
        logic.execute("clear");
        logic.execute("  " + Messages.CONFIRMATION_PHRASE + " ");
        assertTrue(model.getLaiNurse().getPatientList().isEmpty());

        // with nothing to delete, there is nothing to confirm
        assertCommandException("clear --archived", ClearCommand.MESSAGE_NO_ARCHIVED_PATIENTS);
        assertEquals(ListCommand.MESSAGE_SUCCESS, logic.execute("view").getFeedbackToUser());
    }

    @Test
    public void execute_set_unknownUntilReleasedThenSavesPrefs() throws Exception {
        // the settings are not released, so set is an unknown command (#216)
        assertParseException("set past-clash off", MESSAGE_UNKNOWN_COMMAND);

        FeatureFlags.setSettingsEnabled(true);
        logic.execute("set past-clash off");
        assertFalse(logic.getSettings().getValue().isPastClashing());
        // saved at once, not only when the app closes
        UserPrefs saved = new JsonUserPrefsStorage(temporaryFolder.resolve("userPrefs.json")).readUserPrefs()
                .orElseThrow();
        assertFalse(saved.getSettings().isPastClashing());
    }

    @Test
    public void execute_invalidCommandFormat_throwsParseException() {
        String invalidCommand = "uicfhmowqewca";
        assertParseException(invalidCommand, MESSAGE_UNKNOWN_COMMAND);
    }

    @Test
    public void execute_delete_asksForConfirmationFirst() throws Exception {
        FeatureFlags.setSelectiveClearEnabled(true);
        Patient amy = new PatientBuilder(AMY).withAppointments("2026-10-05", "2026-10-06").build();
        model.addPatient(amy);
        String command = "delete " + amy.getId().value;

        // the first time, delete only says what would go
        assertEquals(String.format(Messages.MESSAGE_CONFIRM,
                "This deletes " + amy.getName() + " (" + amy.getId() + ") and their 2 appointments."),
                logic.execute(command).getFeedbackToUser());
        assertTrue(model.hasPatient(amy));

        // any other input cancels it
        assertEquals(Messages.MESSAGE_CANCELLED, logic.execute("yes").getFeedbackToUser());
        assertTrue(model.hasPatient(amy));

        // the phrase confirms it
        logic.execute(command);
        assertEquals(String.format(DeleteCommand.MESSAGE_DELETE_PATIENT_SUCCESS, Messages.format(amy)),
                logic.execute(Messages.CONFIRMATION_PHRASE).getFeedbackToUser());
        assertFalse(model.hasPatient(amy));
    }

    @Test
    public void execute_commandExecutionError_throwsCommandException() {
        String deleteCommand = "delete T9999999Z";
        assertCommandException(deleteCommand, String.format(MESSAGE_PATIENT_NOT_FOUND, "T9999999Z"));
    }

    @Test
    public void execute_validCommand_success() throws Exception {
        assertCommandSuccess("view", ListCommand.MESSAGE_SUCCESS, model);
    }

    @Test
    public void execute_storageThrowsIoException_throwsCommandException() {
        assertCommandFailureForExceptionFromStorage(DUMMY_IO_EXCEPTION, String.format(
                LogicManager.FILE_OPS_ERROR_FORMAT, DUMMY_IO_EXCEPTION.getMessage()));
    }

    @Test
    public void execute_storageThrowsAdException_throwsCommandException() {
        assertCommandFailureForExceptionFromStorage(DUMMY_AD_EXCEPTION, String.format(
                LogicManager.FILE_OPS_PERMISSION_ERROR_FORMAT, DUMMY_AD_EXCEPTION.getMessage()));
    }

    @Test
    public void resetPatientList_filterThenScope_undoesOneStepWithoutSaving() {
        Patient archivedAmy = AMY.withArchived(true);
        model.addPatient(BOB);
        model.addPatient(archivedAmy);
        model.setPatientListScope(PatientScope.ALL);
        model.updateFilteredPatientList(patient -> patient.isSamePatient(BOB));

        // the first step removes the filter and keeps the scope, with archived patients last
        logic.resetPatientList();
        assertEquals(List.of(BOB, archivedAmy), logic.getFilteredPatientList());
        assertEquals(PatientScope.ALL, logic.getPatientListScope().getValue());

        // the second goes back to the default scope
        logic.resetPatientList();
        assertEquals(List.of(BOB), logic.getFilteredPatientList());
        assertEquals(PatientScope.DEFAULT, logic.getPatientListScope().getValue());

        // no command ran, so nothing was saved
        assertFalse(Files.exists(temporaryFolder.resolve("laiNurse.json")));
    }

    @Test
    public void getFilteredPatientList_modifyList_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> logic.getFilteredPatientList().remove(0));
    }

    @Test
    public void getPatientList_modifyList_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> logic.getPatientList().remove(0));
    }

    @Test
    public void execute_eachCommand_resultNamesTheViewItShows() throws Exception {
        // commands that list or change patients show the patient view
        assertEquals(Optional.of(View.PATIENTS), logic.execute("view").getView());
        assertEquals(Optional.of(View.PATIENTS), logic.execute("find --name Amy").getView());
        // a command word that ends in "appt" shows the appointment view
        assertEquals(Optional.of(View.APPOINTMENTS), logic.execute("viewappt 2026-10-06").getView());
        // other commands keep the view that is shown
        assertEquals(Optional.empty(), logic.execute("help").getView());
        assertEquals(Optional.empty(), logic.execute("clear").getView());
    }

    @Test
    public void checkNavigation_entryShown_success() {
        Patient patient = new PatientBuilder(AMY).withAppointments("2026-10-06").build();
        model.addPatient(patient);
        PatientAppointment entry = new PatientAppointment(patient, patient.getSchedule().asList().getFirst());
        CommandResult result = new CommandResult("feedback");
        CommandResult rangeResult = new CommandResult("feedback",
                new AppointmentRange(LocalDate.of(2026, 10, 6), LocalDate.of(2026, 10, 6)));

        // no navigation, or no entry to select
        LogicManager.checkNavigation(result, model);
        LogicManager.checkNavigation(result.withNavigation(Navigation.toView(View.APPOINTMENTS)), model);
        // a patient in the filtered list
        LogicManager.checkNavigation(result.withNavigation(Navigation.toPatient(patient.getId())), model);
        // an appointment with no range, and one in the range
        LogicManager.checkNavigation(result.withNavigation(Navigation.toAppointment(entry)), model);
        LogicManager.checkNavigation(rangeResult.withNavigation(Navigation.toAppointment(entry)), model);
    }

    @Test
    public void checkNavigation_patientFilteredOut_throwsIllegalStateException() {
        Patient patient = new PatientBuilder(AMY).build();
        model.addPatient(patient);
        model.updateFilteredPatientList(unused -> false);
        CommandResult result = new CommandResult("feedback").withNavigation(Navigation.toPatient(patient.getId()));

        assertThrows(IllegalStateException.class, () -> LogicManager.checkNavigation(result, model));
    }

    @Test
    public void checkNavigation_appointmentOutsideRange_throwsIllegalStateException() {
        Patient patient = new PatientBuilder(AMY).withAppointments("2026-10-06").build();
        model.addPatient(patient);
        PatientAppointment entry = new PatientAppointment(patient, patient.getSchedule().asList().getFirst());
        CommandResult result = new CommandResult("feedback",
                new AppointmentRange(LocalDate.of(2026, 10, 7), LocalDate.of(2026, 10, 31)))
                .withNavigation(Navigation.toAppointment(entry));

        assertThrows(IllegalStateException.class, () -> LogicManager.checkNavigation(result, model));
    }

    @Test
    public void checkNavigation_rangeOfOnePatient_checksPatient() {
        Patient patient = new PatientBuilder(AMY).withAppointments("2026-10-06").build();
        model.addPatient(patient);
        PatientAppointment entry = new PatientAppointment(patient, patient.getSchedule().asList().getFirst());
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 31);
        Navigation navigation = Navigation.toAppointment(entry);

        // the range's patient
        LogicManager.checkNavigation(new CommandResult("feedback",
                new AppointmentRange(start, end, patient.getId())).withNavigation(navigation), model);
        // another patient's range, although the date is in it
        CommandResult otherPatient = new CommandResult("feedback",
                new AppointmentRange(start, end, new PatientId("S2222222B"))).withNavigation(navigation);
        assertThrows(IllegalStateException.class, () -> LogicManager.checkNavigation(otherPatient, model));
    }

    @Test
    public void checkNavigation_appointmentNotInModel_throwsIllegalStateException() {
        Patient patient = new PatientBuilder(AMY).withAppointments("2026-10-06").build();
        PatientAppointment entry = new PatientAppointment(patient, patient.getSchedule().asList().getFirst());
        CommandResult result = new CommandResult("feedback").withNavigation(Navigation.toAppointment(entry));

        assertThrows(IllegalStateException.class, () -> LogicManager.checkNavigation(result, model));
    }

    @Test
    public void getAppointmentList_followsModel() {
        Patient patient = new PatientBuilder(AMY).withAppointments("2026-10-06").build();
        model.addPatient(patient);
        assertEquals(List.of(new PatientAppointment(patient, patient.getSchedule().asList().getFirst())),
                logic.getAppointmentList());
        assertThrows(UnsupportedOperationException.class, () -> logic.getAppointmentList().remove(0));
    }

    @Test
    public void getPatientList_filteredList_stillHasAllPatients() throws Exception {
        Patient expectedPatient = new PatientBuilder(AMY).build();
        model.addPatient(expectedPatient);
        model.updateFilteredPatientList(patient -> false);

        assertEquals(0, logic.getFilteredPatientList().size());
        assertEquals(List.of(expectedPatient), logic.getPatientList());
    }

    @Test
    public void getCompletions_filteredList_listsDisplayedPatientsFirst() {
        model.addPatient(new PatientBuilder().withId("S1000001A").withName("Alpha").build());
        model.addPatient(new PatientBuilder().withId("S1000002B").withName("Bravo").build());
        model.addPatient(new PatientBuilder().withId("S1000003C").withName("Charlie").build());
        model.updateFilteredPatientList(patient -> patient.getId().value.equals("S1000003C"));

        assertEquals(List.of("view S1000003C", "view S1000001A", "view S1000002B"),
                logic.getCompletions("view S1").stream().map(Completion::text).toList());
        assertEquals(List.of("S1000003C", "S1000001A", "S1000002B"),
                logic.getCompletions("viewappt --id s").stream().map(Completion::word).toList());
        assertEquals(List.of(), logic.getCompletions("exit"));
    }

    @Test
    public void getCompletions_archivedPatients_listsThemLast() {
        model.addPatient(new PatientBuilder().withId("S1000001A").withName("Alpha").withArchived(true).build());
        model.addPatient(new PatientBuilder().withId("S1000002B").withName("Bravo").build());
        model.setPatientListScope(PatientScope.ALL);

        assertEquals(List.of("S1000002B", "S1000001A"),
                logic.getCompletions("archive S1").stream().map(Completion::word).toList());
    }

    /**
     * Executes the command and confirms that
     * - no exceptions are thrown <br>
     * - the feedback message is equal to {@code expectedMessage} <br>
     * - the internal model manager state is the same as that in {@code expectedModel} <br>
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertCommandSuccess(String inputCommand, String expectedMessage,
            Model expectedModel) throws CommandException, ParseException {
        CommandResult result = logic.execute(inputCommand);
        assertEquals(expectedMessage, result.getFeedbackToUser());
        assertEquals(expectedModel, model);
    }

    /**
     * Executes the command, confirms that a ParseException is thrown and that the result message is correct.
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertParseException(String inputCommand, String expectedMessage) {
        assertCommandFailure(inputCommand, ParseException.class, expectedMessage);
    }

    /**
     * Executes the command, confirms that a CommandException is thrown and that the result message is correct.
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertCommandException(String inputCommand, String expectedMessage) {
        assertCommandFailure(inputCommand, CommandException.class, expectedMessage);
    }

    /**
     * Executes the command, confirms that the exception is thrown and that the result message is correct.
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertCommandFailure(String inputCommand, Class<? extends Throwable> expectedException,
            String expectedMessage) {
        Model expectedModel = new ModelManager(model.getLaiNurse(), new UserPrefs());
        assertCommandFailure(inputCommand, expectedException, expectedMessage, expectedModel);
    }

    /**
     * Executes the command and confirms that
     * - the {@code expectedException} is thrown <br>
     * - the resulting error message is equal to {@code expectedMessage} <br>
     * - the internal model manager state is the same as that in {@code expectedModel} <br>
     * @see #assertCommandSuccess(String, String, Model)
     */
    private void assertCommandFailure(String inputCommand, Class<? extends Throwable> expectedException,
            String expectedMessage, Model expectedModel) {
        assertThrows(expectedException, expectedMessage, () -> logic.execute(inputCommand));
        assertEquals(expectedModel, model);
    }

    /**
     * Tests the Logic component's handling of an {@code IOException} thrown by the Storage component.
     *
     * @param e the exception to be thrown by the Storage component
     * @param expectedMessage the message expected inside exception thrown by the Logic component
     */
    private void assertCommandFailureForExceptionFromStorage(IOException e, String expectedMessage) {
        Path prefPath = temporaryFolder.resolve("ExceptionUserPrefs.json");

        // Inject LogicManager with a JsonLaiNurseStorage that throws the IOException e when saving
        JsonLaiNurseStorage laiNurseStorage = new JsonLaiNurseStorage(prefPath) {
            @Override
            public void saveLaiNurse(ReadOnlyLaiNurse laiNurse) throws IOException {
                throw e;
            }
        };

        JsonUserPrefsStorage userPrefsStorage =
                new JsonUserPrefsStorage(temporaryFolder.resolve("ExceptionUserPrefs.json"));
        StorageManager storage = new StorageManager(laiNurseStorage, userPrefsStorage);

        logic = new LogicManager(model, storage);

        // Triggers the saveLaiNurse method by executing an add command, which the failed save undoes
        String addCommand = AddCommand.COMMAND_WORD + ID_DESC_AMY + NAME_DESC_AMY + PHONE_DESC_AMY
                + ADDRESS_DESC_AMY;
        assertCommandFailure(addCommand, CommandException.class, expectedMessage, new ModelManager());
    }

    @Test
    public void execute_saveFailsThenWorks_retryAppliesChangeOnce() throws Exception {
        Patient amy = new PatientBuilder(AMY).withNotes("Original").build();
        model.addPatient(amy);
        List<IOException> failures = new ArrayList<>(List.of(DUMMY_AD_EXCEPTION));
        logic = new LogicManager(model, storageFailingWith(failures));
        String addNotes = "edit " + amy.getId().value + " --add-notes followup";

        // the failed save puts the notes back as they were (#198)
        assertThrows(CommandException.class, () -> logic.execute(addNotes));
        assertEquals("ORIGINAL", notesOf(amy));

        // so running the command again adds the text once
        logic.execute(addNotes);
        assertEquals("ORIGINAL; FOLLOWUP", notesOf(amy));
    }

    @Test
    public void execute_setWhenSaveFails_settingsUnchanged() throws Exception {
        FeatureFlags.setSettingsEnabled(true);
        List<IOException> failures = new ArrayList<>(List.of(DUMMY_AD_EXCEPTION));
        logic = new LogicManager(model, storageFailingWith(failures));

        // the error says that nothing was changed, so the setting must stay as it was (#261)
        assertThrows(CommandException.class, () -> logic.execute("set past-clash off"));
        assertTrue(model.getUserPrefs().getSettings().isPastClashing());
        assertTrue(logic.getSettings().getValue().isPastClashing());

        // so running the command again changes it
        logic.execute("set past-clash off");
        assertFalse(model.getUserPrefs().getSettings().isPastClashing());
    }

    @Test
    public void execute_backupFails_keepsChangeAndWarns() throws Exception {
        Path backupPath = temporaryFolder.resolve("lainurse.backup.json");
        List<IOException> failures = new ArrayList<>(List.of(
                new BackupFailedException(backupPath, new IOException("disk full"))));
        logic = new LogicManager(model, storageFailingWith(failures));

        // the data file holds the change, so the command succeeds, and only says that the backup failed
        String addCommand = AddCommand.COMMAND_WORD + ID_DESC_AMY + NAME_DESC_AMY + PHONE_DESC_AMY
                + ADDRESS_DESC_AMY;
        CommandResult result = logic.execute(addCommand);
        assertTrue(result.getFeedbackToUser().endsWith(String.format(LogicManager.BACKUP_WARNING_FORMAT, backupPath)));
        assertTrue(model.hasPatient(new PatientBuilder(AMY).build()));
    }

    @Test
    public void execute_sameNameAndBackupFails_keepsBothWarningsAndPatients() throws Exception {
        Patient existing = new PatientBuilder(AMY).withId("EXISTING1").build();
        model.addPatient(existing);
        Path backupPath = temporaryFolder.resolve("lainurse.backup.json");
        JsonLaiNurseStorage laiNurseStorage = new JsonLaiNurseStorage(temporaryFolder.resolve("lainurse.json")) {
            @Override
            public void saveLaiNurse(ReadOnlyLaiNurse laiNurse, Path filePath) throws IOException {
                if (filePath.equals(backupPath)) {
                    throw new IOException("disk full");
                }
                super.saveLaiNurse(laiNurse, filePath);
            }
        };
        StorageManager failingStorage = new StorageManager(laiNurseStorage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json")));
        logic = new LogicManager(model, failingStorage);

        CommandResult result = logic.execute(AddCommand.COMMAND_WORD + ID_DESC_AMY + NAME_DESC_AMY
                + PHONE_DESC_AMY + ADDRESS_DESC_AMY);

        assertEquals(String.format(AddCommand.MESSAGE_SUCCESS, Messages.format(AMY))
                + "\nWarning: this name is also used by AMY BEE (EXISTING1). Check the patient IDs.\n"
                + String.format(LogicManager.BACKUP_WARNING_FORMAT, backupPath), result.getFeedbackToUser());
        assertEquals(Optional.of(Navigation.toPatient(AMY.getId())), result.getNavigation());
        assertEquals(List.of(existing, AMY), model.getLaiNurse().getPatientList());
        assertEquals(model.getLaiNurse(), failingStorage.readLaiNurse().orElseThrow());
        assertFalse(Files.exists(backupPath));
    }

    /**
     * Returns a storage that saves to the temporary folder, but fails the next saves with {@code failures}, in order,
     * until there are none left.
     */
    private StorageManager storageFailingWith(List<IOException> failures) {
        JsonLaiNurseStorage laiNurseStorage = new JsonLaiNurseStorage(temporaryFolder.resolve("lainurse.json")) {
            @Override
            public void saveLaiNurse(ReadOnlyLaiNurse laiNurse) throws IOException {
                if (!failures.isEmpty()) {
                    throw failures.remove(0);
                }
                super.saveLaiNurse(laiNurse);
            }
        };
        return new StorageManager(laiNurseStorage, new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json")));
    }

    private String notesOf(Patient patient) {
        return model.findPatientById(patient.getId()).orElseThrow().getNotes().orElseThrow().value;
    }
}
