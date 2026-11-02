package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;
import static seedu.address.logic.parser.CliSyntax.PREFIX_ADDRESS;
import static seedu.address.logic.parser.CliSyntax.PREFIX_ALL;
import static seedu.address.logic.parser.CliSyntax.PREFIX_ARCHIVED;
import static seedu.address.logic.parser.CliSyntax.PREFIX_ID;
import static seedu.address.logic.parser.CliSyntax.PREFIX_MEDICAL_HISTORY;
import static seedu.address.logic.parser.CliSyntax.PREFIX_NAME;
import static seedu.address.logic.parser.CliSyntax.PREFIX_NOTES;
import static seedu.address.logic.parser.CliSyntax.PREFIX_PHONE;

import java.util.Comparator;
import java.util.Optional;

import seedu.address.commons.util.ToStringBuilder;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.CommandResult.Navigation;
import seedu.address.logic.commands.CommandResult.View;
import seedu.address.model.Model;
import seedu.address.model.PatientScope;
import seedu.address.model.patient.Patient;
import seedu.address.model.patient.PatientContainsSubstringsPredicate;
import seedu.address.model.patient.PatientNameComparator;

/**
 * Finds and lists patients whose fields contain any of the supplied substrings.
 * Matching ignores case and repeated whitespace. Patients matching more fields appear first, but archived patients
 * always come last. Phone matching compares digits only.
 */
public class FindCommand extends Command {

    public static final String COMMAND_WORD = "find";

    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Finds patients matching any supplied field substring "
            + "(case-insensitive) and displays them as a list with index numbers.\n"
            + "Patients matching more fields appear first, with archived ones last; ties are in name order. "
            + "The first is selected.\n"
            + "Parameters: [" + PREFIX_ID + "ID] [" + PREFIX_NAME + "NAME] [" + PREFIX_ADDRESS + "ADDRESS] "
            + "[" + PREFIX_PHONE + "PHONE] [" + PREFIX_MEDICAL_HISTORY + "HISTORY] [" + PREFIX_NOTES + "NOTES] "
            + "[" + PREFIX_ALL.toString().trim() + " or " + PREFIX_ARCHIVED.toString().trim() + "]\n"
            + "Supply at least one non-blank value. Double quotes around values are optional.\n"
            + "Phone searches compare digits only and require at least one digit.\n"
            + "Searches the patients in the scope that the patient list shows, or, with "
            + PREFIX_ALL.toString().trim() + ", all patients, or, with " + PREFIX_ARCHIVED.toString().trim()
            + ", only archived ones, which the list then shows.\n"
            + "Example: " + COMMAND_WORD + " " + PREFIX_NAME + "\"John\" " + PREFIX_ADDRESS + "\"Marina Bay\"";

    private final PatientContainsSubstringsPredicate predicate;
    private final Optional<PatientScope> scope; // empty to search the scope that the patient list shows

    /**
     * Creates a find command with the supplied patient-field predicate, which searches the patients in the scope
     * that the patient list shows.
     */
    public FindCommand(PatientContainsSubstringsPredicate predicate) {
        this(predicate, Optional.empty());
    }

    /**
     * Creates a find command with the supplied patient-field predicate, which searches the patients in
     * {@code scope}, or if it is empty, in the scope that the patient list shows.
     */
    public FindCommand(PatientContainsSubstringsPredicate predicate, Optional<PatientScope> scope) {
        requireAllNonNull(predicate, scope);
        this.predicate = predicate;
        this.scope = scope;
    }

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        Comparator<Patient> comparator = Comparator.comparingInt(predicate::countMatchingFields).reversed()
                .thenComparing(PatientNameComparator.PATIENT_NAME_ORDER);
        scope.ifPresent(model::setPatientListScope);
        model.updateFilteredPatientList(predicate, comparator);
        return new CommandResult(
                String.format(Messages.MESSAGE_PATIENTS_LISTED_OVERVIEW,
                        Messages.formatCount(model.getFilteredPatientList().size(), "patient")))
                .withNavigation(Navigation.toView(View.PATIENTS));
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
        if (!(other instanceof FindCommand otherFindCommand)) {
            return false;
        }

        return predicate.equals(otherFindCommand.predicate) && scope.equals(otherFindCommand.scope);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("predicate", predicate)
                .add("scope", scope)
                .toString();
    }
}
