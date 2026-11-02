package seedu.address.logic;

import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;
import static seedu.address.model.patient.PatientNameComparator.PATIENT_NAME_ORDER;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import seedu.address.logic.parser.CliSyntax;
import seedu.address.logic.parser.Prefix;
import seedu.address.model.Model;
import seedu.address.model.Settings;
import seedu.address.model.patient.Appointment;
import seedu.address.model.patient.AppointmentClashes;
import seedu.address.model.patient.Patient;
import seedu.address.model.patient.PatientAppointment;

/**
 * Container for user visible messages.
 */
public class Messages {

    public static final String MESSAGE_UNKNOWN_COMMAND = "Unknown command. Type 'help' to view all available commands.";
    public static final String MESSAGE_INVALID_COMMAND_FORMAT = "Invalid command format!\n%1$s";
    public static final String MESSAGE_PATIENT_NOT_FOUND = "No patient has the ID %1$s.";
    public static final String MESSAGE_PATIENTS_LISTED_OVERVIEW = "%1$s listed!";
    public static final String MESSAGE_DUPLICATE_OPTION = "%1$s can be given only once.";
    public static final String MESSAGE_DUPLICATE_OPTIONS = "Each of these options can be given only once: %1$s.";
    public static final String MESSAGE_APPOINTMENT_NUMBER_NOT_FOUND = "Patient %1$s has no appointment #%2$d.";
    public static final String MESSAGE_CONFLICTING_SCOPES = CliSyntax.PREFIX_ALL.toString().trim() + " and "
            + CliSyntax.PREFIX_ARCHIVED.toString().trim() + " cannot be given together.";
    public static final String MESSAGE_SCOPE_WITH_ID = "An ID shows that patient, archived or not, so "
            + CliSyntax.PREFIX_ALL.toString().trim() + " and " + CliSyntax.PREFIX_ARCHIVED.toString().trim()
            + " cannot be given with it.";
    public static final String MESSAGE_ARCHIVED_PATIENT_NOTE = "\nNote: this patient is archived, so their "
            + "appointments do not clash. To unarchive them: 'unarchive %1$s'";
    public static final String MESSAGE_CLASHING_ARCHIVED_PATIENT_NOTE = "\nNote: this patient is archived. "
            + "To unarchive them: 'unarchive %1$s'";
    /** What the user types to confirm a command that deletes much at once (#217). */
    public static final String CONFIRMATION_PHRASE = "confirm";
    public static final String MESSAGE_CONFIRM = "%1$s\nType \"" + CONFIRMATION_PHRASE + "\" to confirm, "
            + "or anything else to cancel.";
    public static final String MESSAGE_CANCELLED = "Cancelled. Nothing was deleted.";
    public static final String MESSAGE_CONFLICTING_FIELD_EDITS = "%1$s and %2$s cannot be given together. "
            + "%1$s replaces the value, or clears it if given no value, and %2$s adds to the end of it.";

    /**
     * Returns an error message naming the options of {@code duplicatePrefixes}, which were each given more than once,
     * in the given order. Parsers give them in the order of the command's usage.
     */
    public static String getErrorMessageForDuplicatePrefixes(Prefix... duplicatePrefixes) {
        assert duplicatePrefixes.length > 0;

        List<String> duplicateOptions = Stream.of(duplicatePrefixes)
                .map(prefix -> prefix.toString().trim())
                .distinct()
                .toList();

        return duplicateOptions.size() == 1
                ? String.format(MESSAGE_DUPLICATE_OPTION, duplicateOptions.get(0))
                : String.format(MESSAGE_DUPLICATE_OPTIONS, String.join(", ", duplicateOptions));
    }

    /**
     * Returns {@code count} with {@code noun}, in the singular if {@code count} is 1 and in the plural otherwise,
     * such as "1 patient" or "2 patients".
     */
    public static String formatCount(long count, String noun) {
        return count + " " + noun + (count == 1 ? "" : "s");
    }

    /**
     * Formats the {@code patient} for display to the user.
     */
    public static String format(Patient patient) {
        final StringBuilder builder = new StringBuilder();
        builder.append(patient.getName())
                .append("; ID: ")
                .append(patient.getId())
                .append("; Phone: ")
                .append(patient.getPhone())
                .append("; Address: ")
                .append(patient.getAddress());
        patient.getMedicalHistory().ifPresent(history -> builder.append("; Medical History: ").append(history));
        patient.getNotes().ifPresent(notes -> builder.append("; Notes: ").append(notes));
        if (!patient.getSchedule().isEmpty()) {
            builder.append("; Appointments: ").append(patient.getSchedule());
        }
        return builder.toString();
    }

    /**
     * Returns {@code appointment} as shown in command messages: its date and time, and its details, if any.
     */
    public static String format(Appointment appointment) {
        return appointment + appointment.getDetails().map(details -> "; Details: " + details).orElse("");
    }

    /**
     * Returns a warning naming the other patients in {@code patients} who share {@code target}'s normalized name,
     * in patient name and ID order, or an empty string if there are none. The target is excluded by ID, and archived
     * matches are labeled. The warning has no leading newline, for {@code CommandResult#withNoteAppended}.
     *
     * @throws NullPointerException if {@code patients}, any patient in it, or {@code target} is null.
     */
    public static String formatSameNameWarning(Collection<Patient> patients, Patient target) {
        requireAllNonNull(patients, target);

        String matches = patients.stream()
                .filter(patient -> !patient.getId().equals(target.getId()))
                .filter(patient -> patient.getName().equals(target.getName()))
                .sorted(PATIENT_NAME_ORDER)
                .map(patient -> patient.getName() + " (" + patient.getId() + ")"
                        + (patient.isArchived() ? " [archived]" : ""))
                .collect(Collectors.joining("; "));
        return matches.isEmpty() ? "" : "Warning: this name is also used by " + matches + ". Check the patient IDs.";
    }

    /**
     * Returns a note that {@code patient} is archived, to add to the result of a change to their appointments, or an
     * empty string if they are not archived.
     */
    public static String formatArchivedNote(Patient patient) {
        return formatArchivedNote(patient, Settings.DEFAULT);
    }

    /**
     * Returns a note that {@code patient} is archived, as {@link #formatArchivedNote(Patient)} does, which says that
     * their appointments do not clash only if {@code settings} keep archived patients from clashing (#216).
     */
    public static String formatArchivedNote(Patient patient, Settings settings) {
        if (!patient.isArchived()) {
            return "";
        }
        return String.format(settings.isArchivedClashing() ? MESSAGE_CLASHING_ARCHIVED_PATIENT_NOTE
                : MESSAGE_ARCHIVED_PATIENT_NOTE, patient.getId());
    }

    /**
     * Describes the clashes of {@code target} among the appointments of {@code model}, under its settings, as
     * {@link #formatClashWarning(Collection, PatientAppointment)} does.
     */
    public static String formatClashWarning(Model model, PatientAppointment target) {
        return formatClashWarning(AppointmentClashes.find(model.getAppointmentList(), target,
                model.getSettings().getValue(), LocalDateTime.now()), target);
    }

    /**
     * Describes clashes after a successful appointment change. Another patient's appointment is named with the
     * patient, and another visit of the same patient as "this patient's appointment", since the result already names
     * the patient.
     */
    public static String formatClashWarning(Collection<PatientAppointment> appointments, PatientAppointment target) {
        return formatClashWarning(AppointmentClashes.find(appointments, target), target);
    }

    private static String formatClashWarning(List<PatientAppointment> clashes, PatientAppointment target) {
        if (clashes.isEmpty()) {
            return "";
        }
        return "\nWarning: it clashes with " + clashes.stream()
                .map(entry -> entry.getPatient().getId().equals(target.getPatient().getId())
                        ? "this patient's appointment #" + entry.getAppointment().getNumber()
                        : entry.getPatient().getName() + " (" + entry.getPatient().getId()
                                + "), appointment #" + entry.getAppointment().getNumber())
                .collect(Collectors.joining("; ")) + ".";
    }

}
