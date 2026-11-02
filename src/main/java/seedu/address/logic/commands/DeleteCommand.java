package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.List;

import seedu.address.commons.util.ToStringBuilder;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.patient.Patient;
import seedu.address.model.patient.PatientId;

/**
 * Deletes the patient with the given ID from LaiNurse.
 */
public class DeleteCommand extends Command implements Confirmable {

    public static final String COMMAND_WORD = "delete";

    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Deletes the patient with the given ID and all their appointments, after you confirm it.\n"
            + "Parameters: ID\n"
            + "Example: " + COMMAND_WORD + " T0123456B";

    public static final String MESSAGE_DELETE_PATIENT_SUCCESS = "Deleted patient: %1$s";
    public static final String MESSAGE_DELETES = "This deletes %1$s (%2$s) and their %3$s.";

    private final PatientId targetId;

    /**
     * Creates a DeleteCommand to delete the patient with the specified {@code targetId}.
     */
    public DeleteCommand(PatientId targetId) {
        requireNonNull(targetId);
        this.targetId = targetId;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        Patient patientToDelete = findPatientToDelete(model);
        model.deletePatient(patientToDelete);
        return new CommandResult(String.format(MESSAGE_DELETE_PATIENT_SUCCESS, Messages.format(patientToDelete)))
                .withSelectionCleared();
    }

    @Override
    public String describeDeletion(Model model) throws CommandException {
        requireNonNull(model);
        Patient patientToDelete = findPatientToDelete(model);
        return String.format(MESSAGE_DELETES, patientToDelete.getName(), patientToDelete.getId(),
                Messages.formatCount(patientToDelete.getSchedule().asList().size(), "appointment"));
    }

    private Patient findPatientToDelete(Model model) throws CommandException {
        List<Patient> listAllPatients = model.getLaiNurse().getPatientList();

        return listAllPatients.stream().filter(patient -> patient.getId().equals(targetId))
                        .findFirst().orElseThrow(() -> new CommandException(
                                String.format(Messages.MESSAGE_PATIENT_NOT_FOUND, targetId)));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof DeleteCommand otherDeleteCommand)) {
            return false;
        }

        return targetId.equals(otherDeleteCommand.targetId);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("targetId", targetId)
                .toString();
    }
}
