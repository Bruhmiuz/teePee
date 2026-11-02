package seedu.address.logic.commands;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;

/**
 * A command that deletes what cannot be recovered, so that LaiNurse asks the user to confirm it before it runs (#217).
 */
public interface Confirmable {

    /**
     * Returns what the command would delete from {@code model}, such as "This deletes 2 patients and their 3
     * appointments.", without deleting anything.
     *
     * @throws CommandException if there is nothing to delete, or the command cannot run.
     */
    String describeDeletion(Model model) throws CommandException;
}
