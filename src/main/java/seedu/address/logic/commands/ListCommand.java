package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.address.model.Model.PREDICATE_SHOW_ALL_PATIENTS;

import java.util.Optional;

import seedu.address.commons.util.ToStringBuilder;
import seedu.address.logic.commands.CommandResult.Navigation;
import seedu.address.logic.commands.CommandResult.View;
import seedu.address.model.Model;
import seedu.address.model.PatientScope;

/**
 * Lists all patients in a scope to the user, and selects the first. {@code view} without an ID runs it.
 */
public class ListCommand extends Command {

    public static final String MESSAGE_SUCCESS = "Listed all patients who are not archived.";
    public static final String MESSAGE_SUCCESS_ALL = "Listed all patients, with archived ones last.";
    public static final String MESSAGE_SUCCESS_ARCHIVED = "Listed archived patients.";

    private final PatientScope scope;

    /**
     * Creates a ListCommand that shows the patients in the default scope.
     */
    public ListCommand() {
        this(PatientScope.DEFAULT);
    }

    /**
     * Creates a ListCommand that shows the patients in {@code scope}.
     */
    public ListCommand(PatientScope scope) {
        requireNonNull(scope);
        this.scope = scope;
    }

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        model.setPatientListScope(scope);
        model.updateFilteredPatientList(PREDICATE_SHOW_ALL_PATIENTS);
        String message = switch (scope) {
            case UNARCHIVED -> MESSAGE_SUCCESS;
            case ALL -> MESSAGE_SUCCESS_ALL;
            case ARCHIVED -> MESSAGE_SUCCESS_ARCHIVED;
        };
        return new CommandResult(message).withNavigation(Navigation.toView(View.PATIENTS));
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
        if (!(other instanceof ListCommand otherListCommand)) {
            return false;
        }

        return scope == otherListCommand.scope;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("scope", scope)
                .toString();
    }
}
