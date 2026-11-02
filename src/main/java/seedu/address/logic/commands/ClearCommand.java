package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.List;

import seedu.address.commons.core.FeatureFlags;
import seedu.address.commons.util.ToStringBuilder;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.CliSyntax;
import seedu.address.model.LaiNurse;
import seedu.address.model.Model;
import seedu.address.model.patient.Patient;

/**
 * Clears LaiNurse, or with {@code --archived}, only its archived patients (#217).
 */
public class ClearCommand extends Command implements Confirmable {

    public static final String COMMAND_WORD = "clear";
    public static final String MESSAGE_SUCCESS = "LaiNurse has been cleared!";
    public static final String MESSAGE_ARCHIVED_SUCCESS = "Deleted %1$s.";
    public static final String MESSAGE_DELETES = "This deletes %1$s and their %2$s.";
    public static final String MESSAGE_DELETES_ARCHIVED =
            "This deletes %1$s and their %2$s.";
    public static final String MESSAGE_NO_PATIENTS = "There are no patients to delete.";
    public static final String MESSAGE_NO_ARCHIVED_PATIENTS = "There are no archived patients to delete.";
    public static final String MESSAGE_USAGE = FeatureFlags.isSelectiveClearEnabled()
            ? COMMAND_WORD + ": Deletes all patients, or with " + CliSyntax.PREFIX_ARCHIVED.toString().trim()
                    + ", only archived ones, after you confirm it.\n"
                    + "Parameters: [" + CliSyntax.PREFIX_ARCHIVED.toString().trim() + "]\n"
                    + "Example: " + COMMAND_WORD + " " + CliSyntax.PREFIX_ARCHIVED.toString().trim()
            : COMMAND_WORD + ": Clears all entries from LaiNurse.\n"
                    + "Example: " + COMMAND_WORD;

    private final boolean isArchivedOnly;

    /**
     * Creates a ClearCommand that deletes every patient.
     */
    public ClearCommand() {
        this(false);
    }

    /**
     * Creates a ClearCommand that deletes only archived patients if {@code isArchivedOnly}, and every patient
     * otherwise.
     */
    public ClearCommand(boolean isArchivedOnly) {
        this.isArchivedOnly = isArchivedOnly;
    }

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        if (!isArchivedOnly) {
            model.setLaiNurse(new LaiNurse());
            return new CommandResult(MESSAGE_SUCCESS).withSelectionCleared();
        }
        List<Patient> archived = patientsToDelete(model);
        archived.forEach(model::deletePatient);
        return new CommandResult(String.format(MESSAGE_ARCHIVED_SUCCESS,
                Messages.formatCount(archived.size(), "archived patient"))).withSelectionCleared();
    }

    @Override
    public String describeDeletion(Model model) throws CommandException {
        requireNonNull(model);
        List<Patient> patients = patientsToDelete(model);
        if (patients.isEmpty()) {
            throw new CommandException(isArchivedOnly ? MESSAGE_NO_ARCHIVED_PATIENTS : MESSAGE_NO_PATIENTS);
        }
        int appointmentCount = patients.stream().mapToInt(patient -> patient.getSchedule().asList().size()).sum();
        String patientCount = Messages.formatCount(patients.size(), isArchivedOnly ? "archived patient" : "patient");
        return String.format(isArchivedOnly ? MESSAGE_DELETES_ARCHIVED : MESSAGE_DELETES, patientCount,
                Messages.formatCount(appointmentCount, "appointment"));
    }

    private List<Patient> patientsToDelete(Model model) {
        return model.getLaiNurse().getPatientList().stream()
                .filter(patient -> !isArchivedOnly || patient.isArchived())
                .toList();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof ClearCommand otherClearCommand)) {
            return false;
        }

        return isArchivedOnly == otherClearCommand.isArchivedOnly;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("isArchivedOnly", isArchivedOnly)
                .toString();
    }
}
