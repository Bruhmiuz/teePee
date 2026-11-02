package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.commands.CommandTestUtil.ADDRESS_DESC_AMY;
import static seedu.address.logic.commands.CommandTestUtil.ADDRESS_DESC_BOB;
import static seedu.address.logic.commands.CommandTestUtil.ID_DESC_AMY;
import static seedu.address.logic.commands.CommandTestUtil.ID_DESC_BOB;
import static seedu.address.logic.commands.CommandTestUtil.MEDICAL_HISTORY_DESC_BOB;
import static seedu.address.logic.commands.CommandTestUtil.NAME_DESC_AMY;
import static seedu.address.logic.commands.CommandTestUtil.NAME_DESC_BOB;
import static seedu.address.logic.commands.CommandTestUtil.PHONE_DESC_AMY;
import static seedu.address.logic.commands.CommandTestUtil.PHONE_DESC_BOB;
import static seedu.address.logic.commands.CommandTestUtil.VALID_MEDICAL_HISTORY_BOB;
import static seedu.address.logic.parser.CliSyntax.PREFIX_NAME;
import static seedu.address.logic.parser.CliSyntax.PREFIX_NOTES;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPatients.AMY;
import static seedu.address.testutil.TypicalPatients.BOB;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.AddCommand;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.CommandResult.Navigation;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.LaiNurse;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.patient.MedicalHistory;
import seedu.address.model.patient.Notes;
import seedu.address.model.patient.Patient;
import seedu.address.storage.JsonLaiNurseStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.Storage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PatientBuilder;
import seedu.address.testutil.PatientUtil;

/**
 * Tests add commands from user input through model updates and JSON persistence.
 */
public class AddCommandFlowTest {
    @TempDir
    public Path temporaryFolder;

    private Model model;
    private Storage storage;
    private Logic logic;

    @BeforeEach
    public void setUp() {
        model = new ModelManager();
        storage = new StorageManager(new JsonLaiNurseStorage(temporaryFolder.resolve("laiNurse.json")),
                new JsonUserPrefsStorage(temporaryFolder.resolve("userPrefs.json")));
        logic = new LogicManager(model, storage);
    }

    @Test
    public void execute_requiredFieldsOnly_normalizedAndPersisted() throws Exception {
        assertAddedPatientPersisted("add --address Block 312, Amy Street 1 --name Amy   Bee"
                + " --phone 11111111 --id s1111111a", AMY);
    }

    @Test
    public void execute_medicalHistoryOnly_persisted() throws Exception {
        Patient expectedPatient = new PatientBuilder(AMY).withMedicalHistory(VALID_MEDICAL_HISTORY_BOB).build();
        assertAddedPatientPersisted(AddCommand.COMMAND_WORD + MEDICAL_HISTORY_DESC_BOB + PHONE_DESC_AMY
                + ADDRESS_DESC_AMY + ID_DESC_AMY + NAME_DESC_AMY, expectedPatient);
    }

    @Test
    public void execute_notesOnly_normalizedAndPersisted() throws Exception {
        Patient expectedPatient = new PatientBuilder(AMY).withNotes("Enjoys gardening; walks at 7am").build();
        assertAddedPatientPersisted("add --notes Enjoys\t gardening; walks at 7am"
                + NAME_DESC_AMY + ADDRESS_DESC_AMY + ID_DESC_AMY + PHONE_DESC_AMY, expectedPatient);
    }

    @Test
    public void execute_allFields_persisted() throws Exception {
        Patient expectedPatient = new PatientBuilder(BOB).withAppointments().withNotes("Dislikes loud noises").build();
        assertAddedPatientPersisted(AddCommand.COMMAND_WORD + ADDRESS_DESC_BOB
                + NAME_DESC_BOB + MEDICAL_HISTORY_DESC_BOB + PHONE_DESC_BOB + ID_DESC_BOB
                + " --notes Dislikes loud noises", expectedPatient);
    }

    @Test
    public void execute_sameNameAsHiddenArchivedPatient_warnsAndPersistsBoth() throws Exception {
        Patient archived = new PatientBuilder(AMY).withArchived(true)
                .withAppointment("2026-11-30 09:00", "Checkup").build();
        model.addPatient(archived);
        logic.execute("find --name nobody");
        Patient toAdd = new PatientBuilder().withId("NEW1").withName("amy bee").build();

        CommandResult result = logic.execute("add --id NEW1 --name amy   bee --phone 85355255"
                + " --address 123, Jurong West Ave 6, #08-111");

        assertEquals(String.format(AddCommand.MESSAGE_SUCCESS, Messages.format(toAdd))
                + "\nWarning: this name is also used by AMY BEE (S1111111A) [archived]. Check the patient IDs.",
                result.getFeedbackToUser());
        assertEquals(Optional.of(Navigation.toPatient(toAdd.getId())), result.getNavigation());
        assertEquals(List.of(archived, toAdd), model.getLaiNurse().getPatientList());
        assertEquals(List.of(toAdd), model.getFilteredPatientList());
        assertEquals(model.getLaiNurse(), storage.readLaiNurse().orElseThrow());
    }

    @Test
    public void execute_caseInsensitiveDuplicateIdOutsideFilteredList_rejectedWithoutChanges() throws Exception {
        logic.execute(PatientUtil.getAddCommand(AMY));
        model.updateFilteredPatientList(patient -> false);

        assertRejectedCommandLeavesDataUnchanged("add --name Different Patient --phone 98765432"
                + " --address Different Address --id s1111111a", CommandException.class,
                String.format(AddCommand.MESSAGE_DUPLICATE_PATIENT, AMY.getName(), AMY.getId()));
    }

    @Test
    public void execute_unknownOption_rejectedWithoutChanges() throws Exception {
        logic.execute(PatientUtil.getAddCommand(AMY));
        String command = PatientUtil.getAddCommand(new PatientBuilder().build()) + " --medical-histroy asthma";

        assertRejectedCommandLeavesDataUnchanged(command, ParseException.class,
                String.format(MESSAGE_INVALID_COMMAND_FORMAT, AddCommand.MESSAGE_USAGE));
    }

    @Test
    public void execute_emptyOptionalFlags_rejectedWithoutChanges() throws Exception {
        logic.execute(PatientUtil.getAddCommand(AMY));
        String command = PatientUtil.getAddCommand(new PatientBuilder().build());

        assertRejectedCommandLeavesDataUnchanged(command + " --medical-history", ParseException.class,
                MedicalHistory.MESSAGE_CONSTRAINTS);
        assertRejectedCommandLeavesDataUnchanged(command + " --notes \t ", ParseException.class,
                Notes.MESSAGE_CONSTRAINTS);
    }

    @Test
    public void execute_repeatedFinalFlag_rejectedWithoutChanges() throws Exception {
        logic.execute(PatientUtil.getAddCommand(AMY));
        String command = PatientUtil.getAddCommand(new PatientBuilder().build()) + " --name";

        assertRejectedCommandLeavesDataUnchanged(command, ParseException.class,
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_NAME));
    }

    @Test
    public void execute_repeatedNotes_rejectedWithoutChanges() throws Exception {
        logic.execute(PatientUtil.getAddCommand(AMY));
        String command = PatientUtil.getAddCommand(new PatientBuilder().build()) + " --notes First --notes Second";
        assertRejectedCommandLeavesDataUnchanged(command, ParseException.class,
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_NOTES));
    }

    private void assertAddedPatientPersisted(String command, Patient expectedPatient) throws Exception {
        CommandResult result = logic.execute(command);

        assertEquals(String.format(AddCommand.MESSAGE_SUCCESS, Messages.format(expectedPatient)),
                result.getFeedbackToUser());
        assertEquals(List.of(expectedPatient), model.getLaiNurse().getPatientList());
        assertEquals(List.of(expectedPatient), model.getFilteredPatientList());
        assertEquals(model.getLaiNurse(), storage.readLaiNurse().orElseThrow());
    }

    private void assertRejectedCommandLeavesDataUnchanged(String command, Class<? extends Throwable> expectedException,
            String expectedMessage) throws Exception {
        LaiNurse expectedLaiNurse = new LaiNurse(model.getLaiNurse());
        List<Patient> expectedFilteredList = List.copyOf(model.getFilteredPatientList());
        String expectedJson = Files.readString(storage.getLaiNurseFilePath());

        assertThrows(expectedException, expectedMessage, () -> logic.execute(command));

        assertEquals(expectedLaiNurse, model.getLaiNurse());
        assertEquals(expectedFilteredList, model.getFilteredPatientList());
        assertEquals(expectedJson, Files.readString(storage.getLaiNurseFilePath()));
        assertEquals(expectedLaiNurse, storage.readLaiNurse().orElseThrow());
    }
}
