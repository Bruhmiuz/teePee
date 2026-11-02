package seedu.address.logic.commands;

import static seedu.address.logic.commands.CommandTestUtil.assertCommandFailure;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.address.testutil.TypicalPatients.getTypicalLaiNurse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import seedu.address.logic.Messages;
import seedu.address.logic.commands.CommandResult.Navigation;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.patient.Patient;
import seedu.address.testutil.PatientBuilder;

/**
 * Contains integration tests (interaction with the Model) for {@code AddCommand}.
 */
public class AddCommandIntegrationTest {

    private Model model;

    @BeforeEach
    public void setUp() {
        model = new ModelManager(getTypicalLaiNurse(), new UserPrefs());
    }

    @Test
    public void execute_newPatient_success() {
        Patient validPatient = new PatientBuilder().build();

        Model expectedModel = new ModelManager(model.getLaiNurse(), new UserPrefs());
        expectedModel.addPatient(validPatient);

        // The new patient is selected, as view does
        CommandResult expectedResult = new CommandResult(
                String.format(AddCommand.MESSAGE_SUCCESS, Messages.format(validPatient)))
                .withNavigation(Navigation.toPatient(validPatient.getId()));
        assertCommandSuccess(new AddCommand(validPatient), model, expectedResult, expectedModel);
    }

    @Test
    public void execute_sameNameAsFilteredOutPatient_warnsAndAddsPatient() {
        Patient existing = model.getLaiNurse().getPatientList().get(0);
        Patient toAdd = new PatientBuilder().withName(existing.getName().fullName).build();
        model.updateFilteredPatientList(patient -> false);

        Model expectedModel = new ModelManager(model.getLaiNurse(), new UserPrefs());
        expectedModel.addPatient(toAdd);
        CommandResult expectedResult = new CommandResult(
                String.format(AddCommand.MESSAGE_SUCCESS, Messages.format(toAdd))
                + "\nWarning: this name is also used by " + existing.getName() + " (" + existing.getId()
                + "). Check the patient IDs.").withNavigation(Navigation.toPatient(toAdd.getId()));

        assertCommandSuccess(new AddCommand(toAdd), model, expectedResult, expectedModel);
    }

    @Test
    public void execute_sameNameAsArchivedPatient_warnsAndAddsPatient() {
        Patient existing = model.getLaiNurse().getPatientList().get(0);
        Patient archived = existing.withArchived(true);
        model.setPatient(existing, archived);
        Patient toAdd = new PatientBuilder().withName(archived.getName().fullName).build();

        Model expectedModel = new ModelManager(model.getLaiNurse(), new UserPrefs());
        expectedModel.addPatient(toAdd);
        CommandResult expectedResult = new CommandResult(
                String.format(AddCommand.MESSAGE_SUCCESS, Messages.format(toAdd))
                + "\nWarning: this name is also used by " + archived.getName() + " (" + archived.getId()
                + ") [archived]. Check the patient IDs.").withNavigation(Navigation.toPatient(toAdd.getId()));

        assertCommandSuccess(new AddCommand(toAdd), model, expectedResult, expectedModel);
    }

    @Test
    public void execute_duplicatePatient_throwsCommandException() {
        Patient patientInList = model.getLaiNurse().getPatientList().get(0);
        assertCommandFailure(new AddCommand(patientInList), model,
                String.format(AddCommand.MESSAGE_DUPLICATE_PATIENT, patientInList.getName(), patientInList.getId()));
    }

}
