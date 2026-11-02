package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPatients.BENSON;
import static seedu.address.testutil.TypicalPatients.getTypicalLaiNurse;

import org.junit.jupiter.api.Test;

import seedu.address.logic.Messages;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.LaiNurse;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.patient.Patient;

public class ClearCommandTest {

    @Test
    public void execute_emptyLaiNurse_success() {
        Model model = new ModelManager();
        Model expectedModel = new ModelManager();

        // A command that removes patients selects nothing
        assertCommandSuccess(new ClearCommand(), model,
                new CommandResult(ClearCommand.MESSAGE_SUCCESS).withSelectionCleared(), expectedModel);
    }

    @Test
    public void execute_nonEmptyLaiNurse_success() {
        Model model = new ModelManager(getTypicalLaiNurse(), new UserPrefs());
        Model expectedModel = new ModelManager(getTypicalLaiNurse(), new UserPrefs());
        expectedModel.setLaiNurse(new LaiNurse());

        // A command that removes patients selects nothing
        assertCommandSuccess(new ClearCommand(), model,
                new CommandResult(ClearCommand.MESSAGE_SUCCESS).withSelectionCleared(), expectedModel);
    }

    @Test
    public void execute_archivedOnly_deletesOnlyArchivedPatients() throws Exception {
        Patient archivedBenson = BENSON.withArchived(true);
        Model model = new ModelManager(getTypicalLaiNurse(), new UserPrefs());
        model.setPatient(BENSON, archivedBenson);
        Model expectedModel = new ModelManager(model.getLaiNurse(), new UserPrefs());
        expectedModel.deletePatient(archivedBenson);

        ClearCommand command = new ClearCommand(true);
        assertEquals(String.format(ClearCommand.MESSAGE_DELETES_ARCHIVED, "1 archived patient",
                Messages.formatCount(archivedBenson.getSchedule().asList().size(), "appointment")),
                        command.describeDeletion(model));
        assertCommandSuccess(command, model, new CommandResult(String.format(ClearCommand.MESSAGE_ARCHIVED_SUCCESS,
                "1 archived patient")).withSelectionCleared(), expectedModel);
        // nothing left to delete
        assertThrows(CommandException.class, ClearCommand.MESSAGE_NO_ARCHIVED_PATIENTS, () ->
                command.describeDeletion(model));
    }

    @Test
    public void describeDeletion_allPatients_countsTheirAppointments() throws Exception {
        Model model = new ModelManager(getTypicalLaiNurse(), new UserPrefs());
        int appointmentCount = model.getLaiNurse().getPatientList().stream()
                .mapToInt(patient -> patient.getSchedule().asList().size()).sum();
        assertEquals(String.format(ClearCommand.MESSAGE_DELETES,
                Messages.formatCount(model.getLaiNurse().getPatientList().size(), "patient"),
                Messages.formatCount(appointmentCount, "appointment")), new ClearCommand().describeDeletion(model));
        assertThrows(CommandException.class, ClearCommand.MESSAGE_NO_PATIENTS, () ->
                new ClearCommand().describeDeletion(new ModelManager()));
        assertEquals(new ClearCommand(), new ClearCommand(false));
        assertNotEquals(new ClearCommand(), new ClearCommand(true));
    }
}
