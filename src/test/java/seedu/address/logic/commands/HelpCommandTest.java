package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandSuccess;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.model.Model;
import seedu.address.model.ModelManager;

public class HelpCommandTest {

    private final Model model = new ModelManager();
    private final Model expectedModel = new ModelManager();

    @Test
    public void execute_helpNoArgs_showsAllCommandsSummary() {
        CommandResult expectedResult = new CommandResult(HelpCommand.SHOWING_HELP_SUMMARY);
        assertCommandSuccess(new HelpCommand(), model, expectedResult, expectedModel);
    }

    @Test
    public void execute_helpValidTargetAdd_showsAddUsage() {
        CommandResult expectedResult = new CommandResult(AddCommand.MESSAGE_USAGE);
        assertCommandSuccess(new HelpCommand("add"), model, expectedResult, expectedModel);
    }

    @Test
    public void execute_notesCommandsHelp_includesNotesOption() {
        for (String commandWord : List.of("add", "edit", "find")) {
            assertTrue(new HelpCommand(commandWord).execute(model).getFeedbackToUser().contains("--notes "));
        }
    }

    @Test
    public void execute_helpValidTargetDelete_showsDeleteUsage() {
        CommandResult expectedResult = new CommandResult(DeleteCommand.MESSAGE_USAGE);
        assertCommandSuccess(new HelpCommand("delete"), model, expectedResult, expectedModel);
    }

    @Test
    public void execute_helpValidTargetViewAppt_showsViewApptUsage() {
        CommandResult expectedResult = new CommandResult(ViewAppointmentCommand.MESSAGE_USAGE);
        assertCommandSuccess(new HelpCommand("viewappt"), model, expectedResult, expectedModel);
    }

    @Test
    public void execute_helpValidTargetHelp_showsHelpUsage() {
        CommandResult expectedResult = new CommandResult(HelpCommand.MESSAGE_USAGE);
        assertCommandSuccess(new HelpCommand("help"), model, expectedResult, expectedModel);
    }

    @Test
    public void execute_everyRegisteredTarget_returnsItsOwnUsage() throws Exception {
        // Expected usages come from each command's own constants, not from the map under test,
        // so a miswired entry in HelpCommand.COMMAND_USAGE_MAP still fails this test.
        assertEquals(AddCommand.MESSAGE_USAGE,
                new HelpCommand(AddCommand.COMMAND_WORD).execute(model).getFeedbackToUser());
        assertEquals(AddAppointmentCommand.MESSAGE_USAGE,
                new HelpCommand(AddAppointmentCommand.COMMAND_WORD).execute(model).getFeedbackToUser());
        assertEquals(ClearCommand.MESSAGE_USAGE,
                new HelpCommand(ClearCommand.COMMAND_WORD).execute(model).getFeedbackToUser());
        assertEquals(DeleteCommand.MESSAGE_USAGE,
                new HelpCommand(DeleteCommand.COMMAND_WORD).execute(model).getFeedbackToUser());
        assertEquals(DeleteAppointmentCommand.MESSAGE_USAGE,
                new HelpCommand(DeleteAppointmentCommand.COMMAND_WORD).execute(model).getFeedbackToUser());
        assertEquals(EditCommand.MESSAGE_USAGE,
                new HelpCommand(EditCommand.COMMAND_WORD).execute(model).getFeedbackToUser());
        assertEquals(EditAppointmentCommand.MESSAGE_USAGE,
                new HelpCommand(EditAppointmentCommand.COMMAND_WORD).execute(model).getFeedbackToUser());
        assertEquals(ExitCommand.MESSAGE_USAGE,
                new HelpCommand(ExitCommand.COMMAND_WORD).execute(model).getFeedbackToUser());
        assertEquals(FindCommand.MESSAGE_USAGE,
                new HelpCommand(FindCommand.COMMAND_WORD).execute(model).getFeedbackToUser());
        assertEquals(HelpCommand.MESSAGE_USAGE,
                new HelpCommand(HelpCommand.COMMAND_WORD).execute(model).getFeedbackToUser());
        assertEquals(MarkAppointmentCommand.MESSAGE_USAGE,
                new HelpCommand(MarkAppointmentCommand.COMMAND_WORD).execute(model).getFeedbackToUser());
        assertEquals(UnmarkAppointmentCommand.MESSAGE_USAGE,
                new HelpCommand(UnmarkAppointmentCommand.COMMAND_WORD).execute(model).getFeedbackToUser());
        assertEquals(ViewCommand.MESSAGE_USAGE,
                new HelpCommand(ViewCommand.COMMAND_WORD).execute(model).getFeedbackToUser());
        assertEquals(ViewAppointmentCommand.MESSAGE_USAGE,
                new HelpCommand(ViewAppointmentCommand.COMMAND_WORD).execute(model).getFeedbackToUser());
    }

    @Test
    public void execute_helpWindowFlag_opensHelpWindow() throws Exception {
        CommandResult result = new HelpCommand("window").execute(model);
        assertTrue(result.isShowHelp());
        assertFalse(result.isExit());
        assertEquals(HelpCommand.SHOWING_HELP_MESSAGE, result.getFeedbackToUser());
    }

    @Test
    public void execute_allCommandsPresentInSummary() {
        for (String commandWord : HelpCommand.COMMAND_USAGE_MAP.keySet()) {
            assertTrue(HelpCommand.SHOWING_HELP_SUMMARY.contains(commandWord));
        }
    }

    @Test
    public void execute_invalidHelpArgumentMessageListsAllTargets() {
        for (String commandWord : HelpCommand.COMMAND_USAGE_MAP.keySet()) {
            assertTrue(HelpCommand.MESSAGE_INVALID_HELP_ARGUMENT.contains(commandWord));
        }
        assertTrue(HelpCommand.MESSAGE_INVALID_HELP_ARGUMENT.contains("window"));
    }

    @Test
    public void equals() {
        HelpCommand defaultHelp = new HelpCommand();
        HelpCommand helpDelete1 = new HelpCommand("delete");
        HelpCommand helpDelete2 = new HelpCommand("delete");
        HelpCommand helpAdd = new HelpCommand("add");
        HelpCommand helpWindow = new HelpCommand("window");

        // Same object -> true
        assertTrue(defaultHelp.equals(defaultHelp));
        assertTrue(helpDelete1.equals(helpDelete1));

        // Same values -> true
        assertTrue(helpDelete1.equals(helpDelete2));

        // Different types -> false
        assertFalse(defaultHelp.equals(1));

        // Null -> false
        assertFalse(defaultHelp.equals(null));

        // Different targets -> false
        assertFalse(helpDelete1.equals(helpAdd));
        assertFalse(helpDelete1.equals(helpWindow));
        assertFalse(defaultHelp.equals(helpDelete1));
    }
}
