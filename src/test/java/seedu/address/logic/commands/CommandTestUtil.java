package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.parser.CliSyntax.PREFIX_ADDRESS;
import static seedu.address.logic.parser.CliSyntax.PREFIX_ID;
import static seedu.address.logic.parser.CliSyntax.PREFIX_MEDICAL_HISTORY;
import static seedu.address.logic.parser.CliSyntax.PREFIX_NAME;
import static seedu.address.logic.parser.CliSyntax.PREFIX_PHONE;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.ArrayList;
import java.util.List;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.LaiNurse;
import seedu.address.model.Model;
import seedu.address.model.patient.NameContainsKeywordsPredicate;
import seedu.address.model.patient.Patient;
import seedu.address.testutil.EditPatientDescriptorBuilder;

/**
 * Contains helper methods for testing commands.
 */
public class CommandTestUtil {

    public static final String VALID_ID_AMY = "S1111111A";
    public static final String VALID_ID_BOB = "S2222222B";
    public static final String VALID_NAME_AMY = "Amy Bee";
    public static final String VALID_NAME_BOB = "Bob Choo";
    public static final String VALID_PHONE_AMY = "11111111";
    public static final String VALID_PHONE_BOB = "22222222";
    public static final String VALID_ADDRESS_AMY = "Block 312, Amy Street 1";
    public static final String VALID_ADDRESS_BOB = "Block 123, Bobby Street 3";
    public static final String VALID_MEDICAL_HISTORY_BOB = "dementia";
    public static final String VALID_APPOINTMENT_BOB = "2026-11-30 09:00";

    public static final String ID_DESC_AMY = " " + PREFIX_ID + VALID_ID_AMY;
    public static final String ID_DESC_BOB = " " + PREFIX_ID + VALID_ID_BOB;
    public static final String NAME_DESC_AMY = " " + PREFIX_NAME + VALID_NAME_AMY;
    public static final String NAME_DESC_BOB = " " + PREFIX_NAME + VALID_NAME_BOB;
    public static final String PHONE_DESC_AMY = " " + PREFIX_PHONE + VALID_PHONE_AMY;
    public static final String PHONE_DESC_BOB = " " + PREFIX_PHONE + VALID_PHONE_BOB;
    public static final String ADDRESS_DESC_AMY = " " + PREFIX_ADDRESS + VALID_ADDRESS_AMY;
    public static final String ADDRESS_DESC_BOB = " " + PREFIX_ADDRESS + VALID_ADDRESS_BOB;
    public static final String MEDICAL_HISTORY_DESC_BOB = " " + PREFIX_MEDICAL_HISTORY + VALID_MEDICAL_HISTORY_BOB;

    public static final String INVALID_ID = "S1234-567"; // '-' not allowed in IDs
    public static final String INVALID_ID_DESC = " " + PREFIX_ID + INVALID_ID;
    public static final String INVALID_NAME_DESC = " " + PREFIX_NAME + " "; // blank names not allowed
    public static final String INVALID_PHONE_DESC = " " + PREFIX_PHONE + "abc"; // no digits
    public static final String INVALID_ADDRESS_DESC = " " + PREFIX_ADDRESS; // empty string not allowed for addresses
    public static final String INVALID_MEDICAL_HISTORY_DESC = " " + PREFIX_MEDICAL_HISTORY; // similar to address

    public static final String PREAMBLE_WHITESPACE = "\t  \r  \n";
    public static final String PREAMBLE_NON_EMPTY = "NonEmptyPreamble";

    public static final EditCommand.EditPatientDescriptor DESC_AMY;
    public static final EditCommand.EditPatientDescriptor DESC_BOB;

    static {
        DESC_AMY = new EditPatientDescriptorBuilder().withName(VALID_NAME_AMY)
                .withPhone(VALID_PHONE_AMY).withAddress(VALID_ADDRESS_AMY).build();
        DESC_BOB = new EditPatientDescriptorBuilder().withName(VALID_NAME_BOB)
                .withPhone(VALID_PHONE_BOB).withAddress(VALID_ADDRESS_BOB).build();
    }

    /**
     * Executes the given {@code command}, confirms that <br>
     * - the returned {@link CommandResult} matches {@code expectedCommandResult} <br>
     * - the {@code actualModel} matches {@code expectedModel}
     */
    public static void assertCommandSuccess(Command command, Model actualModel, CommandResult expectedCommandResult,
            Model expectedModel) {
        try {
            CommandResult result = command.execute(actualModel);
            assertEquals(expectedCommandResult, result);
            assertEquals(expectedModel, actualModel);
        } catch (CommandException ce) {
            throw new AssertionError("Execution of command should not fail.", ce);
        }
    }

    /**
     * Convenience wrapper to {@link #assertCommandSuccess(Command, Model, CommandResult, Model)}
     * that takes a string {@code expectedMessage}.
     */
    public static void assertCommandSuccess(Command command, Model actualModel, String expectedMessage,
            Model expectedModel) {
        CommandResult expectedCommandResult = new CommandResult(expectedMessage);
        assertCommandSuccess(command, actualModel, expectedCommandResult, expectedModel);
    }

    /**
     * Executes the given {@code command}, confirms that <br>
     * - a {@code CommandException} is thrown <br>
     * - the CommandException message matches {@code expectedMessage} <br>
     * - the {@code LaiNurse}, filtered patient list and selected patient in {@code actualModel} remain unchanged
     */
    public static void assertCommandFailure(Command command, Model actualModel, String expectedMessage) {
        // we are unable to defensively copy the model for comparison later, so we can
        // only do so by copying its components.
        LaiNurse expectedLaiNurse = new LaiNurse(actualModel.getLaiNurse());
        List<Patient> expectedFilteredList = new ArrayList<>(actualModel.getFilteredPatientList());

        assertThrows(CommandException.class, expectedMessage, () -> command.execute(actualModel));
        assertEquals(expectedLaiNurse, actualModel.getLaiNurse());
        assertEquals(expectedFilteredList, actualModel.getFilteredPatientList());
    }
    /**
     * Updates {@code model}'s filtered list to show only the patient at the zero-based {@code targetIndex} in the
     * {@code model}'s filtered list.
     */
    public static void showPatientAtIndex(Model model, int targetIndex) {
        assertTrue(targetIndex < model.getFilteredPatientList().size());

        Patient patient = model.getFilteredPatientList().get(targetIndex);
        final String[] splitName = patient.getName().fullName.split("\\s+");
        model.updateFilteredPatientList(new NameContainsKeywordsPredicate(List.of(splitName[0])));

        assertEquals(1, model.getFilteredPatientList().size());
    }

}
