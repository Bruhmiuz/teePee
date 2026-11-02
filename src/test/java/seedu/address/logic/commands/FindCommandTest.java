package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.Messages.MESSAGE_PATIENTS_LISTED_OVERVIEW;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPatients.CARL;
import static seedu.address.testutil.TypicalPatients.ELLE;
import static seedu.address.testutil.TypicalPatients.FIONA;
import static seedu.address.testutil.TypicalPatients.getTypicalLaiNurse;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.CommandResult.Navigation;
import seedu.address.logic.commands.CommandResult.View;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.PatientScope;
import seedu.address.model.UserPrefs;
import seedu.address.model.patient.Patient;
import seedu.address.model.patient.PatientContainsSubstringsPredicate;

/**
 * Contains integration tests (interaction with the Model) for {@code FindCommand}.
 */
public class FindCommandTest {
    private Model model = new ModelManager(getTypicalLaiNurse(), new UserPrefs());
    private Model expectedModel = new ModelManager(getTypicalLaiNurse(), new UserPrefs());

    @Test
    public void constructor_nullPredicate_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new FindCommand(null));
    }

    @Test
    public void execute_nullModel_throwsNullPointerException() {
        FindCommand command = new FindCommand(prepareNamePredicate("Alice"));
        assertThrows(NullPointerException.class, () -> command.execute(null));
    }

    @Test
    public void equals() {
        PatientContainsSubstringsPredicate firstPredicate = prepareNamePredicate("first");
        PatientContainsSubstringsPredicate secondPredicate = prepareNamePredicate("second");

        FindCommand findFirstCommand = new FindCommand(firstPredicate);
        FindCommand findSecondCommand = new FindCommand(secondPredicate);

        // same object -> returns true
        assertTrue(findFirstCommand.equals(findFirstCommand));

        // same values -> returns true
        FindCommand findFirstCommandCopy = new FindCommand(firstPredicate);
        assertTrue(findFirstCommand.equals(findFirstCommandCopy));

        // different types -> returns false
        assertFalse(findFirstCommand.equals(1));

        // null -> returns false
        assertFalse(findFirstCommand.equals(null));

        // different patient -> returns false
        assertFalse(findFirstCommand.equals(findSecondCommand));
    }

    @Test
    public void execute_scope_searchesAndShowsThatScope() {
        Patient archivedElle = ELLE.withArchived(true);
        model.setPatient(ELLE, archivedElle);
        expectedModel.setPatient(ELLE, archivedElle);
        PatientContainsSubstringsPredicate predicate = prepareNamePredicate("Elle");

        // without a scope, the default scope hides Elle
        expectedModel.updateFilteredPatientList(predicate);
        assertCommandSuccess(new FindCommand(predicate), model,
                findResult(String.format(MESSAGE_PATIENTS_LISTED_OVERVIEW, "0 patients")), expectedModel);

        // with --archived, the patient list shows archived patients from then on
        expectedModel.setPatientListScope(PatientScope.ARCHIVED);
        assertCommandSuccess(new FindCommand(predicate, Optional.of(PatientScope.ARCHIVED)), model,
                findResult(String.format(MESSAGE_PATIENTS_LISTED_OVERVIEW, "1 patient")), expectedModel);
        assertEquals(List.of(archivedElle), model.getFilteredPatientList());
        assertEquals(PatientScope.ARCHIVED, model.getPatientListScope().getValue());

        // a find without a scope keeps it
        assertCommandSuccess(new FindCommand(predicate), model,
                findResult(String.format(MESSAGE_PATIENTS_LISTED_OVERVIEW, "1 patient")), expectedModel);
        assertFalse(new FindCommand(predicate).equals(new FindCommand(predicate, Optional.of(PatientScope.ALL))));
    }

    @Test
    public void execute_noMatchingSubstring_noPatientFound() {
        String expectedMessage = String.format(MESSAGE_PATIENTS_LISTED_OVERVIEW, "0 patients");
        PatientContainsSubstringsPredicate predicate = prepareNamePredicate("Nobody");
        FindCommand command = new FindCommand(predicate);
        expectedModel.updateFilteredPatientList(predicate);
        assertCommandSuccess(command, model, findResult(expectedMessage), expectedModel);
        assertEquals(List.of(), model.getFilteredPatientList());
    }

    @Test
    public void execute_multipleFields_multiplePatientsFound() {
        String expectedMessage = String.format(MESSAGE_PATIENTS_LISTED_OVERVIEW, "3 patients");
        PatientContainsSubstringsPredicate predicate = new PatientContainsSubstringsPredicate(Optional.of("3456789"),
                Optional.of("Elle"), Optional.empty(), Optional.of("tokyo"), Optional.empty(), Optional.empty());
        FindCommand command = new FindCommand(predicate);
        expectedModel.updateFilteredPatientList(predicate);
        assertCommandSuccess(command, model, findResult(expectedMessage), expectedModel);
        assertEquals(List.of(CARL, ELLE, FIONA), model.getFilteredPatientList());
    }

    @Test
    public void toStringMethod() {
        PatientContainsSubstringsPredicate predicate = prepareNamePredicate("substring");
        FindCommand findCommand = new FindCommand(predicate);
        String expected = FindCommand.class.getCanonicalName() + "{predicate=" + predicate
                + ", scope=Optional.empty}";
        assertEquals(expected, findCommand.toString());
    }

    /**
     * Creates a predicate that searches for a substring in patient names.
     */
    private PatientContainsSubstringsPredicate prepareNamePredicate(String substring) {
        return new PatientContainsSubstringsPredicate(Optional.empty(), Optional.of(substring), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty());
    }

    /**
     * Returns the result of a find command with {@code message}, which selects the first match.
     */
    private static CommandResult findResult(String message) {
        return new CommandResult(message).withNavigation(Navigation.toView(View.PATIENTS));
    }
}
