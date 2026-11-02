package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.address.logic.parser.CliSyntax.PREFIX_ADDRESS;
import static seedu.address.logic.parser.CliSyntax.PREFIX_ID;
import static seedu.address.logic.parser.CliSyntax.PREFIX_MEDICAL_HISTORY;
import static seedu.address.logic.parser.CliSyntax.PREFIX_NAME;
import static seedu.address.logic.parser.CliSyntax.PREFIX_NOTES;
import static seedu.address.logic.parser.CliSyntax.PREFIX_PHONE;

import java.util.Optional;
import java.util.logging.Logger;

import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.util.ToStringBuilder;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.CommandResult.Navigation;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.patient.Patient;

/**
 * Adds a patient to LaiNurse.
 */
public class AddCommand extends Command {

    public static final String COMMAND_WORD = "add";

    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Adds a patient to LaiNurse. "
            + "Parameters: "
            + PREFIX_ID + "ID "
            + PREFIX_NAME + "NAME "
            + PREFIX_PHONE + "PHONE "
            + PREFIX_ADDRESS + "ADDRESS "
            + "[" + PREFIX_MEDICAL_HISTORY + "MEDICAL_HISTORY] "
            + "[" + PREFIX_NOTES + "NOTES]\n"
            + "Example: " + COMMAND_WORD + " "
            + PREFIX_ID + "T0123456B "
            + PREFIX_NAME + "Taron Ban "
            + PREFIX_PHONE + "98765432 "
            + PREFIX_ADDRESS + "311, Clementi Ave 2, #02-25 "
            + PREFIX_MEDICAL_HISTORY + "dementia "
            + PREFIX_NOTES + "Enjoys gardening";

    public static final String MESSAGE_SUCCESS = "New patient added: %1$s";
    public static final String MESSAGE_DUPLICATE_PATIENT = "%1$s (%2$s) already exists in LaiNurse.";
    public static final String MESSAGE_DUPLICATE_ARCHIVED_PATIENT = "%1$s (%2$s) already exists in LaiNurse, and is "
            + "archived. To unarchive them: 'unarchive %2$s'";

    private static final Logger logger = LogsCenter.getLogger(AddCommand.class);

    private final Patient toAdd;

    /**
     * Creates an AddCommand to add the specified {@code Patient}
     */
    public AddCommand(Patient patient) {
        requireNonNull(patient);
        toAdd = patient;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);

        Optional<Patient> existingPatient = model.findPatientById(toAdd.getId());
        if (existingPatient.isPresent()) {
            Patient existing = existingPatient.get();
            throw new CommandException(String.format(existing.isArchived() ? MESSAGE_DUPLICATE_ARCHIVED_PATIENT
                    : MESSAGE_DUPLICATE_PATIENT, existing.getName(), existing.getId()));
        }

        model.addPatient(toAdd);
        assert model.findPatientById(toAdd.getId()).filter(toAdd::equals).isPresent()
                : "The added patient must be stored with all their details.";
        model.showInPatientList(toAdd);
        String warning = Messages.formatSameNameWarning(model.getLaiNurse().getPatientList(), toAdd);
        logger.fine("Added patient " + toAdd.getId() + (warning.isEmpty() ? "." : " with a same-name warning."));
        CommandResult result = new CommandResult(String.format(MESSAGE_SUCCESS, Messages.format(toAdd)))
                .withNavigation(Navigation.toPatient(toAdd.getId()));
        return warning.isEmpty() ? result : result.withNoteAppended(warning);
    }

    @Override
    public Optional<CommandResult.View> getView() {
        return Optional.of(CommandResult.View.PATIENTS);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof AddCommand otherAddCommand)) {
            return false;
        }

        return toAdd.equals(otherAddCommand.toAdd);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("toAdd", toAdd)
                .toString();
    }
}
