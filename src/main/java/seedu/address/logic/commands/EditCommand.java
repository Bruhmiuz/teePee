package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.address.logic.parser.CliSyntax.PREFIX_ADDRESS;
import static seedu.address.logic.parser.CliSyntax.PREFIX_ADD_MEDICAL_HISTORY;
import static seedu.address.logic.parser.CliSyntax.PREFIX_ADD_NOTES;
import static seedu.address.logic.parser.CliSyntax.PREFIX_MEDICAL_HISTORY;
import static seedu.address.logic.parser.CliSyntax.PREFIX_NAME;
import static seedu.address.logic.parser.CliSyntax.PREFIX_NOTES;
import static seedu.address.logic.parser.CliSyntax.PREFIX_PHONE;
import static seedu.address.model.Model.PREDICATE_SHOW_ALL_PATIENTS;

import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.util.CollectionUtil;
import seedu.address.commons.util.ToStringBuilder;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.CommandResult.Navigation;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.patient.Address;
import seedu.address.model.patient.MedicalHistory;
import seedu.address.model.patient.Name;
import seedu.address.model.patient.Notes;
import seedu.address.model.patient.Patient;
import seedu.address.model.patient.PatientId;
import seedu.address.model.patient.Phone;

/**
 * Edits the details of an existing patient in LaiNurse.
 */
public class EditCommand extends Command {

    public static final String COMMAND_WORD = "edit";

    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Edits the details of the patient with the given ID. "
            + "Existing values will be overwritten by the input values. The ID itself cannot be edited. "
            + PREFIX_MEDICAL_HISTORY.toString().trim() + " or " + PREFIX_NOTES.toString().trim()
            + " with no value clears that field, and " + PREFIX_ADD_MEDICAL_HISTORY.toString().trim() + " or "
            + PREFIX_ADD_NOTES.toString().trim() + " adds text to the end of it, after \""
            + OptionalFieldEdit.APPEND_SEPARATOR + "\". A field cannot be both replaced and added to.\n"
            + "Parameters: ID "
            + "[" + PREFIX_NAME + "NAME] "
            + "[" + PREFIX_PHONE + "PHONE] "
            + "[" + PREFIX_ADDRESS + "ADDRESS] "
            + "[" + PREFIX_MEDICAL_HISTORY + "[MEDICAL_HISTORY]] "
            + "[" + PREFIX_ADD_MEDICAL_HISTORY + "MORE_HISTORY] "
            + "[" + PREFIX_NOTES + "[NOTES]] "
            + "[" + PREFIX_ADD_NOTES + "MORE_NOTES]\n"
            + "Examples: " + COMMAND_WORD + " T0123456B " + PREFIX_PHONE + "91234567, "
            + COMMAND_WORD + " T0123456B " + PREFIX_ADD_MEDICAL_HISTORY + "Asthma " + PREFIX_NOTES.toString().trim();

    public static final String MESSAGE_EDIT_PATIENT_SUCCESS = "Edited patient: %1$s";
    public static final String MESSAGE_NOT_EDITED = "At least one of " + PREFIX_NAME.toString().trim() + ", "
            + PREFIX_PHONE.toString().trim() + ", " + PREFIX_ADDRESS.toString().trim() + ", "
            + PREFIX_MEDICAL_HISTORY.toString().trim() + ", " + PREFIX_ADD_MEDICAL_HISTORY.toString().trim() + ", "
            + PREFIX_NOTES.toString().trim() + " and " + PREFIX_ADD_NOTES.toString().trim() + " must be provided.";

    private static final Logger logger = LogsCenter.getLogger(EditCommand.class);

    private final PatientId targetId;
    private final EditPatientDescriptor editPatientDescriptor;

    /**
     * @param targetId ID of the patient to edit
     * @param editPatientDescriptor details to edit the patient with
     */
    public EditCommand(PatientId targetId, EditPatientDescriptor editPatientDescriptor) {
        requireNonNull(targetId);
        requireNonNull(editPatientDescriptor);

        this.targetId = targetId;
        this.editPatientDescriptor = new EditPatientDescriptor(editPatientDescriptor);
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        Optional<Patient> foundPatient = model.findPatientById(targetId);
        if (foundPatient.isEmpty()) {
            logger.fine("Rejected edit: no patient has the given ID.");
            throw new CommandException(String.format(Messages.MESSAGE_PATIENT_NOT_FOUND, targetId));
        }
        Patient patientToEdit = foundPatient.get();
        Patient editedPatient = createEditedPatient(patientToEdit, editPatientDescriptor);
        assert editedPatient.getId().equals(patientToEdit.getId()) : "An edit never changes the patient's ID.";

        model.setPatient(patientToEdit, editedPatient);
        assert model.findPatientById(targetId).filter(editedPatient::equals).isPresent()
                : "The edited patient must be stored with all their details.";
        model.updateFilteredPatientList(PREDICATE_SHOW_ALL_PATIENTS);
        model.showInPatientList(editedPatient);
        String warning = Messages.formatSameNameWarning(model.getLaiNurse().getPatientList(), editedPatient);
        logger.fine("Edited patient " + targetId + (warning.isEmpty() ? "." : " with a same-name warning."));
        CommandResult result = new CommandResult(
                String.format(MESSAGE_EDIT_PATIENT_SUCCESS, Messages.format(editedPatient)))
                .withNavigation(Navigation.toPatient(editedPatient.getId()));
        return warning.isEmpty() ? result : result.withNoteAppended(warning);
    }

    /**
     * Creates and returns a {@code Patient} with the details of {@code patientToEdit}
     * edited with {@code editPatientDescriptor}. The patient ID is never changed.
     * The patient's appointments are kept.
     */
    private static Patient createEditedPatient(Patient patientToEdit, EditPatientDescriptor editPatientDescriptor) {
        assert patientToEdit != null;
        assert editPatientDescriptor != null;

        Name updatedName = editPatientDescriptor.getName().orElse(patientToEdit.getName());
        Phone updatedPhone = editPatientDescriptor.getPhone().orElse(patientToEdit.getPhone());
        Address updatedAddress = editPatientDescriptor.getAddress().orElse(patientToEdit.getAddress());
        Optional<MedicalHistory> updatedMedicalHistory = editPatientDescriptor.getMedicalHistory()
                .map(edit -> edit.applyTo(patientToEdit.getMedicalHistory(), MedicalHistory::new))
                .orElse(patientToEdit.getMedicalHistory());
        Optional<Notes> updatedNotes = editPatientDescriptor.getNotes()
                .map(edit -> edit.applyTo(patientToEdit.getNotes(), Notes::new))
                .orElse(patientToEdit.getNotes());

        return new Patient(patientToEdit.getId(), updatedName, updatedPhone, updatedAddress,
                updatedMedicalHistory, updatedNotes, patientToEdit.getSchedule(), patientToEdit.isArchived());
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
        if (!(other instanceof EditCommand otherEditCommand)) {
            return false;
        }

        return targetId.equals(otherEditCommand.targetId)
                && editPatientDescriptor.equals(otherEditCommand.editPatientDescriptor);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("targetId", targetId)
                .add("editPatientDescriptor", editPatientDescriptor)
                .toString();
    }

    /**
     * Stores the details to edit the patient with. Each non-empty field value will replace the
     * corresponding field value of the patient. The medical history and notes are stored as edits, which can also
     * clear the field or add text to the end of it.
     */
    public static class EditPatientDescriptor {
        private Name name;
        private Phone phone;
        private Address address;
        private OptionalFieldEdit<MedicalHistory> medicalHistory;
        private OptionalFieldEdit<Notes> notes;

        public EditPatientDescriptor() {}

        /**
         * Copy constructor.
         */
        public EditPatientDescriptor(EditPatientDescriptor toCopy) {
            setName(toCopy.name);
            setPhone(toCopy.phone);
            setAddress(toCopy.address);
            setMedicalHistory(toCopy.medicalHistory);
            setNotes(toCopy.notes);
        }

        /**
         * Returns true if at least one field is edited.
         */
        public boolean isAnyFieldEdited() {
            return CollectionUtil.isAnyNonNull(name, phone, address, medicalHistory, notes);
        }

        public void setName(Name name) {
            this.name = name;
        }

        public Optional<Name> getName() {
            return Optional.ofNullable(name);
        }

        public void setPhone(Phone phone) {
            this.phone = phone;
        }

        public Optional<Phone> getPhone() {
            return Optional.ofNullable(phone);
        }

        public void setAddress(Address address) {
            this.address = address;
        }

        public Optional<Address> getAddress() {
            return Optional.ofNullable(address);
        }

        public void setMedicalHistory(OptionalFieldEdit<MedicalHistory> medicalHistory) {
            this.medicalHistory = medicalHistory;
        }

        public Optional<OptionalFieldEdit<MedicalHistory>> getMedicalHistory() {
            return Optional.ofNullable(medicalHistory);
        }

        public void setNotes(OptionalFieldEdit<Notes> notes) {
            this.notes = notes;
        }

        public Optional<OptionalFieldEdit<Notes>> getNotes() {
            return Optional.ofNullable(notes);
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            }

            // instanceof handles nulls
            if (!(other instanceof EditPatientDescriptor otherEditPatientDescriptor)) {
                return false;
            }

            return Objects.equals(name, otherEditPatientDescriptor.name)
                    && Objects.equals(phone, otherEditPatientDescriptor.phone)
                    && Objects.equals(address, otherEditPatientDescriptor.address)
                    && Objects.equals(medicalHistory, otherEditPatientDescriptor.medicalHistory)
                    && Objects.equals(notes, otherEditPatientDescriptor.notes);
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .add("name", name)
                    .add("phone", phone)
                    .add("address", address)
                    .add("medicalHistory", medicalHistory)
                    .add("notes", notes)
                    .toString();
        }
    }
}
