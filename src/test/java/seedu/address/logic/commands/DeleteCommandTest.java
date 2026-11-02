package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandFailure;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.address.logic.commands.CommandTestUtil.showPatientAtIndex;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_PATIENT;
import static seedu.address.testutil.TypicalIndexes.INDEX_SECOND_PATIENT;
import static seedu.address.testutil.TypicalPatients.getTypicalLaiNurse;

import org.junit.jupiter.api.Test;

import seedu.address.logic.Messages;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.patient.Patient;
import seedu.address.model.patient.PatientId;
import seedu.address.testutil.PatientBuilder;

/**
 * Contains integration tests (interaction with the Model) and unit tests for
 * {@code DeleteCommand}.
 */
public class DeleteCommandTest {

    private Model model = new ModelManager(getTypicalLaiNurse(), new UserPrefs());

    @Test
    public void execute_validIdUnfilteredList_success() {
        Patient patientToDelete = model.getFilteredPatientList().get(INDEX_FIRST_PATIENT);
        DeleteCommand deleteCommand = new DeleteCommand(patientToDelete.getId());

        String expectedMessage = String.format(DeleteCommand.MESSAGE_DELETE_PATIENT_SUCCESS,
                Messages.format(patientToDelete));

        ModelManager expectedModel = new ModelManager(model.getLaiNurse(), new UserPrefs());
        expectedModel.deletePatient(patientToDelete);

        // A command that removes patients selects nothing
        assertCommandSuccess(deleteCommand, model, new CommandResult(expectedMessage).withSelectionCleared(),
                expectedModel);
    }

    @Test
    public void describeDeletion_validId_namesPatientAndAppointments() throws Exception {
        Patient patient = new PatientBuilder().withId("D1").withName("Test Name")
                .withAppointments("2026-10-05", "2026-10-06").build();
        model.addPatient(patient);

        assertEquals("This deletes TEST NAME (D1) and their 2 appointments.",
                new DeleteCommand(patient.getId()).describeDeletion(model));
        assertTrue(model.hasPatient(patient));
    }

    @Test
    public void describeDeletion_nonExistentId_throwsCommandException() {
        PatientId nonExistentId = new PatientId("T9999999Z");
        assertThrows(CommandException.class, String.format(Messages.MESSAGE_PATIENT_NOT_FOUND, nonExistentId), () ->
                new DeleteCommand(nonExistentId).describeDeletion(model));
    }

    @Test
    public void execute_nonExistentId_throwsCommandException() {
        PatientId nonExistentId = new PatientId("T9999999Z");
        DeleteCommand deleteCommand = new DeleteCommand(nonExistentId);

        assertCommandFailure(deleteCommand, model, String.format(Messages.MESSAGE_PATIENT_NOT_FOUND, nonExistentId));
    }

    @Test
    public void execute_validIdFilteredListNoRestrict_success() {
        showPatientAtIndex(model, INDEX_FIRST_PATIENT);

        Patient hiddenFromListPatient = model.getLaiNurse().getPatientList()
                .get(INDEX_SECOND_PATIENT);
        DeleteCommand deleteCommand = new DeleteCommand(hiddenFromListPatient.getId());

        String expectedMessage = String.format(DeleteCommand.MESSAGE_DELETE_PATIENT_SUCCESS,
                Messages.format(hiddenFromListPatient));

        Model expectedModel = new ModelManager(model.getLaiNurse(), new UserPrefs());
        expectedModel.deletePatient(hiddenFromListPatient);
        showPatientAtIndex(expectedModel, INDEX_FIRST_PATIENT);

        // A command that removes patients selects nothing
        assertCommandSuccess(deleteCommand, model, new CommandResult(expectedMessage).withSelectionCleared(),
                expectedModel);
    }

    @Test
    public void equals() {
        PatientId firstId = new PatientId("T0123456A");
        PatientId secondId = new PatientId("T0123456B");

        DeleteCommand deleteFirstCommand = new DeleteCommand(firstId);
        DeleteCommand deleteSecondCommand = new DeleteCommand(secondId);

        // same object -> returns true
        assertTrue(deleteFirstCommand.equals(deleteFirstCommand));

        // same values -> returns true
        DeleteCommand deleteFirstCommandCopy = new DeleteCommand(firstId);
        assertTrue(deleteFirstCommand.equals(deleteFirstCommandCopy));

        // different types -> returns false
        assertFalse(deleteFirstCommand.equals(1));

        // null -> returns false
        assertFalse(deleteFirstCommand.equals(null));

        // different patient -> returns false
        assertFalse(deleteFirstCommand.equals(deleteSecondCommand));
    }

    @Test
    public void toStringMethod() {
        PatientId targetId = new PatientId("T0123456B");
        DeleteCommand deleteCommand = new DeleteCommand(targetId);
        String expected = DeleteCommand.class.getCanonicalName() + "{targetId=" + targetId + "}";
        assertEquals(expected, deleteCommand.toString());
    }

    /**
     * Updates {@code model}'s filtered list to show no one.
     */
    private void showNoPatient(Model model) {
        model.updateFilteredPatientList(p -> false);

        assertTrue(model.getFilteredPatientList().isEmpty());
    }
}
