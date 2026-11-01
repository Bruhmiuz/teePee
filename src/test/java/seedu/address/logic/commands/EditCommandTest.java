package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.commands.CommandTestUtil.DESC_AMY;
import static seedu.address.logic.commands.CommandTestUtil.DESC_BOB;
import static seedu.address.logic.commands.CommandTestUtil.VALID_ADDRESS_BOB;
import static seedu.address.logic.commands.CommandTestUtil.VALID_ID_AMY;
import static seedu.address.logic.commands.CommandTestUtil.VALID_ID_BOB;
import static seedu.address.logic.commands.CommandTestUtil.VALID_MEDICAL_HISTORY_BOB;
import static seedu.address.logic.commands.CommandTestUtil.VALID_NAME_BOB;
import static seedu.address.logic.commands.CommandTestUtil.VALID_PHONE_BOB;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandFailure;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.address.logic.commands.CommandTestUtil.showPatientAtIndex;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_PATIENT;
import static seedu.address.testutil.TypicalIndexes.INDEX_SECOND_PATIENT;
import static seedu.address.testutil.TypicalPatients.BENSON;
import static seedu.address.testutil.TypicalPatients.getTypicalLaiNurse;

import org.junit.jupiter.api.Test;

import seedu.address.logic.Messages;
import seedu.address.logic.commands.CommandResult.Navigation;
import seedu.address.logic.commands.EditCommand.EditPatientDescriptor;
import seedu.address.model.LaiNurse;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.PatientScope;
import seedu.address.model.UserPrefs;
import seedu.address.model.patient.Patient;
import seedu.address.model.patient.PatientId;
import seedu.address.testutil.EditPatientDescriptorBuilder;
import seedu.address.testutil.PatientBuilder;

/**
 * Contains integration tests (interaction with the Model) and unit tests for EditCommand.
 */
public class EditCommandTest {

    private Model model = new ModelManager(getTypicalLaiNurse(), new UserPrefs());

    @Test
    public void execute_allFieldsSpecified_success() {
        // The descriptor copies no appointments and the ID cannot be edited, so these are kept from the first patient
        Patient firstPatient = model.getFilteredPatientList().get(INDEX_FIRST_PATIENT);
        Patient editedPatient = new PatientBuilder(firstPatient).withName(VALID_NAME_BOB)
                .withPhone(VALID_PHONE_BOB).withAddress(VALID_ADDRESS_BOB).build();
        EditPatientDescriptor descriptor = new EditPatientDescriptorBuilder(editedPatient).build();
        EditCommand editCommand = new EditCommand(firstPatient.getId(), descriptor);

        String expectedMessage = String.format(EditCommand.MESSAGE_EDIT_PATIENT_SUCCESS,
                Messages.format(editedPatient));

        Model expectedModel = new ModelManager(new LaiNurse(model.getLaiNurse()), new UserPrefs());
        expectedModel.setPatient(firstPatient, editedPatient);

        assertCommandSuccess(editCommand, model, editResult(expectedMessage, firstPatient.getId()), expectedModel);
    }

    @Test
    public void execute_archivedPatient_staysArchivedAndShown() {
        Patient archivedBenson = BENSON.withArchived(true);
        model.setPatient(BENSON, archivedBenson);
        Patient editedPatient = new PatientBuilder(archivedBenson).withPhone(VALID_PHONE_BOB).build();
        EditPatientDescriptor descriptor = new EditPatientDescriptorBuilder().withPhone(VALID_PHONE_BOB).build();

        String expectedMessage = String.format(EditCommand.MESSAGE_EDIT_PATIENT_SUCCESS,
                Messages.format(editedPatient));
        Model expectedModel = new ModelManager(new LaiNurse(model.getLaiNurse()), new UserPrefs());
        expectedModel.setPatient(archivedBenson, editedPatient);
        // The scope shows archived patients too, so that the patient's row is in the list to select
        expectedModel.setPatientListScope(PatientScope.ALL);

        assertCommandSuccess(new EditCommand(BENSON.getId(), descriptor), model,
                new CommandResult(expectedMessage).withNavigation(Navigation.toPatient(BENSON.getId())),
                expectedModel);
    }

    @Test
    public void execute_someFieldsSpecified_success() {
        Patient lastPatient = model.getFilteredPatientList().get(model.getFilteredPatientList().size() - 1);

        PatientBuilder patientInList = new PatientBuilder(lastPatient);
        Patient editedPatient = patientInList.withName(VALID_NAME_BOB).withPhone(VALID_PHONE_BOB).build();

        EditPatientDescriptor descriptor = new EditPatientDescriptorBuilder().withName(VALID_NAME_BOB)
                .withPhone(VALID_PHONE_BOB).build();
        EditCommand editCommand = new EditCommand(lastPatient.getId(), descriptor);

        String expectedMessage = String.format(EditCommand.MESSAGE_EDIT_PATIENT_SUCCESS,
                Messages.format(editedPatient));

        Model expectedModel = new ModelManager(new LaiNurse(model.getLaiNurse()), new UserPrefs());
        expectedModel.setPatient(lastPatient, editedPatient);

        assertCommandSuccess(editCommand, model, editResult(expectedMessage, lastPatient.getId()), expectedModel);
    }

    @Test
    public void execute_medicalHistorySpecified_success() {
        // Only the medical history changes; the other fields are kept
        Patient firstPatient = model.getFilteredPatientList().get(INDEX_FIRST_PATIENT);
        Patient editedPatient = new PatientBuilder(firstPatient).withMedicalHistory(VALID_MEDICAL_HISTORY_BOB).build();
        EditPatientDescriptor descriptor = new EditPatientDescriptorBuilder()
                .withMedicalHistory(VALID_MEDICAL_HISTORY_BOB).build();
        EditCommand editCommand = new EditCommand(firstPatient.getId(), descriptor);

        String expectedMessage = String.format(EditCommand.MESSAGE_EDIT_PATIENT_SUCCESS,
                Messages.format(editedPatient));

        Model expectedModel = new ModelManager(new LaiNurse(model.getLaiNurse()), new UserPrefs());
        expectedModel.setPatient(firstPatient, editedPatient);

        assertCommandSuccess(editCommand, model, editResult(expectedMessage, firstPatient.getId()), expectedModel);
    }

    @Test
    public void execute_notesSpecified_success() {
        EditPatientDescriptor descriptor = new EditPatientDescriptorBuilder().withNotes("Enjoys gardening").build();
        Patient editedPatient = new PatientBuilder(BENSON).withNotes("Enjoys gardening").build();
        assertEditSuccess(BENSON, descriptor, editedPatient);
    }

    @Test
    public void execute_clearMedicalHistoryAndNotes_success() {
        Patient bensonWithNotes = new PatientBuilder(BENSON).withNotes("Enjoys gardening").build();
        model.setPatient(BENSON, bensonWithNotes);
        EditPatientDescriptor descriptor = new EditPatientDescriptorBuilder().withoutMedicalHistory().withoutNotes()
                .build();
        Patient editedPatient = new PatientBuilder(BENSON).withoutMedicalHistory().withoutNotes().build();
        assertEditSuccess(bensonWithNotes, descriptor, editedPatient);
    }

    @Test
    public void execute_clearEmptyNotes_success() {
        // BENSON has no notes, so clearing them changes nothing
        EditPatientDescriptor descriptor = new EditPatientDescriptorBuilder().withoutNotes().build();
        assertEditSuccess(BENSON, descriptor, BENSON);
    }

    @Test
    public void execute_addToMedicalHistory_addedAfterSeparator() {
        // BENSON's medical history is HYPERTENSION, ALLERGIC TO PENICILLIN
        EditPatientDescriptor descriptor = new EditPatientDescriptorBuilder().withMedicalHistoryAdded("Asthma")
                .build();
        Patient editedPatient = new PatientBuilder(BENSON)
                .withMedicalHistory("Hypertension, allergic to penicillin; Asthma").build();
        assertEditSuccess(BENSON, descriptor, editedPatient);
    }

    @Test
    public void execute_addToEmptyNotes_setsNotes() {
        EditPatientDescriptor descriptor = new EditPatientDescriptorBuilder().withNotesAdded("Enjoys gardening")
                .build();
        Patient editedPatient = new PatientBuilder(BENSON).withNotes("Enjoys gardening").build();
        assertEditSuccess(BENSON, descriptor, editedPatient);
    }

    @Test
    public void execute_noFieldSpecified_success() {
        Patient firstPatient = model.getFilteredPatientList().get(INDEX_FIRST_PATIENT);
        EditCommand editCommand = new EditCommand(firstPatient.getId(), new EditPatientDescriptor());

        String expectedMessage = String.format(EditCommand.MESSAGE_EDIT_PATIENT_SUCCESS,
                Messages.format(firstPatient));

        Model expectedModel = new ModelManager(new LaiNurse(model.getLaiNurse()), new UserPrefs());

        assertCommandSuccess(editCommand, model, editResult(expectedMessage, firstPatient.getId()), expectedModel);
    }

    @Test
    public void execute_patientHiddenByFilter_success() {
        // Patients are found by ID, so a patient hidden by a filter can still be edited,
        // and the list is reset to show every patient afterwards
        showPatientAtIndex(model, INDEX_FIRST_PATIENT);
        Patient secondPatient = model.getLaiNurse().getPatientList().get(INDEX_SECOND_PATIENT);
        Patient editedPatient = new PatientBuilder(secondPatient).withName(VALID_NAME_BOB).build();
        EditCommand editCommand = new EditCommand(secondPatient.getId(),
                new EditPatientDescriptorBuilder().withName(VALID_NAME_BOB).build());

        String expectedMessage = String.format(EditCommand.MESSAGE_EDIT_PATIENT_SUCCESS,
                Messages.format(editedPatient));

        Model expectedModel = new ModelManager(new LaiNurse(model.getLaiNurse()), new UserPrefs());
        expectedModel.setPatient(secondPatient, editedPatient);

        assertCommandSuccess(editCommand, model, editResult(expectedMessage, secondPatient.getId()), expectedModel);
    }

    @Test
    public void execute_sameDetailsAsOtherPatient_successAndKeepsId() {
        // Patients are identified by ID, which edit cannot change, so copying another patient's
        // name, phone, address and medical history does not create a duplicate
        Patient firstPatient = model.getFilteredPatientList().get(INDEX_FIRST_PATIENT);
        Patient secondPatient = model.getFilteredPatientList().get(INDEX_SECOND_PATIENT);
        EditPatientDescriptor descriptor = new EditPatientDescriptorBuilder(firstPatient).build();
        EditCommand editCommand = new EditCommand(secondPatient.getId(), descriptor);

        PatientBuilder editedPatientBuilder = new PatientBuilder(secondPatient)
                .withName(firstPatient.getName().fullName).withPhone(firstPatient.getPhone().value)
                .withAddress(firstPatient.getAddress().value);
        firstPatient.getMedicalHistory().ifPresent(history -> editedPatientBuilder.withMedicalHistory(history.value));
        Patient editedPatient = editedPatientBuilder.build();
        String expectedMessage = String.format(EditCommand.MESSAGE_EDIT_PATIENT_SUCCESS,
                Messages.format(editedPatient)) + "\nWarning: this name is also used by " + firstPatient.getName()
                + " (" + firstPatient.getId() + "). Check the patient IDs.";

        Model expectedModel = new ModelManager(new LaiNurse(model.getLaiNurse()), new UserPrefs());
        expectedModel.setPatient(secondPatient, editedPatient);

        assertCommandSuccess(editCommand, model, editResult(expectedMessage, secondPatient.getId()), expectedModel);
    }

    @Test
    public void execute_sharedNameWithoutNameChange_warns() {
        Patient other = new PatientBuilder(BENSON).withId("SHARED1").build();
        model.addPatient(other);
        Patient editedPatient = new PatientBuilder(BENSON).withPhone(VALID_PHONE_BOB).build();
        EditPatientDescriptor descriptor = new EditPatientDescriptorBuilder().withPhone(VALID_PHONE_BOB).build();
        String expectedMessage = String.format(EditCommand.MESSAGE_EDIT_PATIENT_SUCCESS, Messages.format(editedPatient))
                + "\nWarning: this name is also used by BENSON MEIER (SHARED1). Check the patient IDs.";

        Model expectedModel = new ModelManager(model.getLaiNurse(), new UserPrefs());
        expectedModel.setPatient(BENSON, editedPatient);
        assertCommandSuccess(new EditCommand(BENSON.getId(), descriptor), model,
                editResult(expectedMessage, BENSON.getId()), expectedModel);

        // Explicitly giving the same normalized name still warns, even though the edit changes no details.
        EditPatientDescriptor sameName = new EditPatientDescriptorBuilder().withName("  benson   meier  ").build();
        assertCommandSuccess(new EditCommand(BENSON.getId(), sameName), model,
                editResult(expectedMessage, BENSON.getId()), expectedModel);
    }

    @Test
    public void execute_renameAwayFromSharedName_successWithoutWarning() {
        model.addPatient(new PatientBuilder(BENSON).withId("SHARED1").build());
        EditPatientDescriptor descriptor = new EditPatientDescriptorBuilder().withName("Unique Name").build();
        Patient editedPatient = new PatientBuilder(BENSON).withName("Unique Name").build();

        assertEditSuccess(BENSON, descriptor, editedPatient);
    }

    @Test
    public void execute_archivedPatientWithSharedName_warnsAndStaysArchived() {
        Patient archivedBenson = BENSON.withArchived(true);
        model.setPatient(BENSON, archivedBenson);
        model.addPatient(new PatientBuilder(BENSON).withId("SHARED1").build());
        Patient editedPatient = new PatientBuilder(archivedBenson).withPhone(VALID_PHONE_BOB).build();
        EditPatientDescriptor descriptor = new EditPatientDescriptorBuilder().withPhone(VALID_PHONE_BOB).build();
        String expectedMessage = String.format(EditCommand.MESSAGE_EDIT_PATIENT_SUCCESS, Messages.format(editedPatient))
                + "\nWarning: this name is also used by BENSON MEIER (SHARED1). Check the patient IDs.";

        Model expectedModel = new ModelManager(model.getLaiNurse(), new UserPrefs());
        expectedModel.setPatient(archivedBenson, editedPatient);
        expectedModel.setPatientListScope(PatientScope.ALL);

        assertCommandSuccess(new EditCommand(BENSON.getId(), descriptor), model,
                editResult(expectedMessage, BENSON.getId()), expectedModel);
    }

    @Test
    public void execute_idNotFound_failure() {
        // VALID_ID_AMY is not one of the typical patients
        EditPatientDescriptor descriptor = new EditPatientDescriptorBuilder().withName(VALID_NAME_BOB).build();
        EditCommand editCommand = new EditCommand(new PatientId(VALID_ID_AMY), descriptor);

        assertCommandFailure(editCommand, model, String.format(Messages.MESSAGE_PATIENT_NOT_FOUND, VALID_ID_AMY));
    }

    /**
     * Asserts that editing {@code patientToEdit} with {@code descriptor} succeeds and gives {@code editedPatient}.
     */
    private void assertEditSuccess(Patient patientToEdit, EditPatientDescriptor descriptor, Patient editedPatient) {
        EditCommand editCommand = new EditCommand(patientToEdit.getId(), descriptor);

        String expectedMessage = String.format(EditCommand.MESSAGE_EDIT_PATIENT_SUCCESS,
                Messages.format(editedPatient));

        Model expectedModel = new ModelManager(new LaiNurse(model.getLaiNurse()), new UserPrefs());
        expectedModel.setPatient(patientToEdit, editedPatient);

        assertCommandSuccess(editCommand, model, editResult(expectedMessage, patientToEdit.getId()), expectedModel);
    }

    /**
     * Returns the result of an edit that succeeds with {@code message}, which selects the patient with {@code id}.
     */
    private static CommandResult editResult(String message, PatientId id) {
        return new CommandResult(message).withNavigation(Navigation.toPatient(id));
    }

    @Test
    public void equals() {
        PatientId firstId = new PatientId(VALID_ID_AMY);
        PatientId secondId = new PatientId(VALID_ID_BOB);
        final EditCommand standardCommand = new EditCommand(firstId, DESC_AMY);

        // same values -> returns true
        EditPatientDescriptor copyDescriptor = new EditPatientDescriptor(DESC_AMY);
        EditCommand commandWithSameValues = new EditCommand(firstId, copyDescriptor);
        assertTrue(standardCommand.equals(commandWithSameValues));

        // same object -> returns true
        assertTrue(standardCommand.equals(standardCommand));

        // null -> returns false
        assertFalse(standardCommand.equals(null));

        // different types -> returns false
        assertFalse(standardCommand.equals(new ClearCommand()));

        // different ID -> returns false
        assertFalse(standardCommand.equals(new EditCommand(secondId, DESC_AMY)));

        // different descriptor -> returns false
        assertFalse(standardCommand.equals(new EditCommand(firstId, DESC_BOB)));
    }

    @Test
    public void toStringMethod() {
        PatientId id = new PatientId(VALID_ID_AMY);
        EditPatientDescriptor editPatientDescriptor = new EditPatientDescriptor();
        EditCommand editCommand = new EditCommand(id, editPatientDescriptor);
        String expected = EditCommand.class.getCanonicalName() + "{targetId=" + id + ", editPatientDescriptor="
                + editPatientDescriptor + "}";
        assertEquals(expected, editCommand.toString());
    }

}
