package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPatients.ALICE;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import seedu.address.commons.core.GuiSettings;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.CommandResult.Navigation;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.LaiNurse;
import seedu.address.model.Model;
import seedu.address.model.PatientScope;
import seedu.address.model.ReadOnlyLaiNurse;
import seedu.address.model.ReadOnlyUserPrefs;
import seedu.address.model.Settings;
import seedu.address.model.patient.Patient;
import seedu.address.model.patient.PatientAppointment;
import seedu.address.model.patient.PatientId;
import seedu.address.testutil.PatientBuilder;

public class AddCommandTest {

    @Test
    public void constructor_nullPatient_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new AddCommand(null));
    }

    @Test
    public void execute_patientAcceptedByModel_addSuccessful() throws Exception {
        ModelStubAcceptingPatientAdded modelStub = new ModelStubAcceptingPatientAdded();
        Patient validPatient = new PatientBuilder().build();

        CommandResult commandResult = new AddCommand(validPatient).execute(modelStub);

        assertEquals(String.format(AddCommand.MESSAGE_SUCCESS, Messages.format(validPatient)),
                commandResult.getFeedbackToUser());
        assertEquals(Optional.of(Navigation.toPatient(validPatient.getId())), commandResult.getNavigation());
        assertEquals(List.of(validPatient), modelStub.patientsAdded);
    }

    @Test
    public void execute_sameNameWithDifferentId_warnsAndAddsPatient() throws Exception {
        ModelStubAcceptingPatientAdded modelStub = new ModelStubAcceptingPatientAdded();
        Patient existing = new PatientBuilder().withId("S1").withName("Alex Yeoh").build();
        modelStub.addPatient(existing);
        Patient toAdd = new PatientBuilder().withId("S2").withName("  alex   yeoh  ").build();

        CommandResult result = new AddCommand(toAdd).execute(modelStub);

        assertEquals(String.format(AddCommand.MESSAGE_SUCCESS, Messages.format(toAdd))
                + "\nWarning: this name is also used by ALEX YEOH (S1). Check the patient IDs.",
                result.getFeedbackToUser());
        assertEquals(Optional.of(Navigation.toPatient(toAdd.getId())), result.getNavigation());
        assertEquals(List.of(existing, toAdd), modelStub.patientsAdded);
    }

    @Test
    public void execute_duplicatePatient_throwsCommandException() {
        Patient validPatient = new PatientBuilder().build();
        AddCommand addCommand = new AddCommand(validPatient);
        ModelStub modelStub = new ModelStubWithPatient(validPatient);

        assertThrows(CommandException.class, String.format(AddCommand.MESSAGE_DUPLICATE_PATIENT,
                validPatient.getName(), validPatient.getId()), () -> addCommand.execute(modelStub));
    }

    @Test
    public void execute_duplicateArchivedPatient_suggestsUnarchive() {
        Patient archivedPatient = new PatientBuilder().withArchived(true).build();
        AddCommand addCommand = new AddCommand(new PatientBuilder().build());
        ModelStub modelStub = new ModelStubWithPatient(archivedPatient);

        assertThrows(CommandException.class, String.format(AddCommand.MESSAGE_DUPLICATE_ARCHIVED_PATIENT,
                archivedPatient.getName(), archivedPatient.getId()), () -> addCommand.execute(modelStub));
    }

    @Test
    public void equals() {
        Patient alice = new PatientBuilder().withName("Alice").build();
        Patient bob = new PatientBuilder().withName("Bob").build();
        AddCommand addAliceCommand = new AddCommand(alice);
        AddCommand addBobCommand = new AddCommand(bob);

        // same object -> returns true
        assertTrue(addAliceCommand.equals(addAliceCommand));

        // same values -> returns true
        AddCommand addAliceCommandCopy = new AddCommand(alice);
        assertTrue(addAliceCommand.equals(addAliceCommandCopy));

        // different types -> returns false
        assertFalse(addAliceCommand.equals(1));

        // null -> returns false
        assertFalse(addAliceCommand.equals(null));

        // different patient -> returns false
        assertFalse(addAliceCommand.equals(addBobCommand));
    }

    @Test
    public void toStringMethod() {
        AddCommand addCommand = new AddCommand(ALICE);
        String expected = AddCommand.class.getCanonicalName() + "{toAdd=" + ALICE + "}";
        assertEquals(expected, addCommand.toString());
    }

    /**
     * A default model stub that has all of the methods failing.
     */
    private class ModelStub implements Model {
        @Override
        public ReadOnlyUserPrefs getUserPrefs() {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public GuiSettings getGuiSettings() {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public void setGuiSettings(GuiSettings guiSettings) {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public void addPatient(Patient patient) {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public void setLaiNurse(ReadOnlyLaiNurse newData) {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public ReadOnlyLaiNurse getLaiNurse() {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public boolean hasPatient(Patient patient) {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public Optional<Patient> findPatientById(PatientId id) {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public boolean hasPatientFilter() {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public ObservableValue<Settings> getSettings() {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public void setSettings(Settings settings) {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public ObservableValue<Boolean> isPatientListFiltered() {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public ObservableValue<PatientScope> getPatientListScope() {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public void setPatientListScope(PatientScope scope) {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public void showInPatientList(Patient patient) {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public void deletePatient(Patient target) {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public void setPatient(Patient target, Patient editedPatient) {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public ObservableList<Patient> getFilteredPatientList() {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public void updateFilteredPatientList(Predicate<Patient> predicate) {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public void updateFilteredPatientList(Predicate<Patient> predicate, Comparator<Patient> comparator) {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public ObservableList<PatientAppointment> getAppointmentList() {
            throw new AssertionError("This method should not be called.");
        }
    }

    /**
     * A Model stub that contains a single patient.
     */
    private class ModelStubWithPatient extends ModelStub {
        private final Patient patient;

        ModelStubWithPatient(Patient patient) {
            requireNonNull(patient);
            this.patient = patient;
        }

        @Override
        public boolean hasPatient(Patient patient) {
            requireNonNull(patient);
            return this.patient.isSamePatient(patient);
        }

        @Override
        public Optional<Patient> findPatientById(PatientId id) {
            requireNonNull(id);
            return patient.getId().equals(id) ? Optional.of(patient) : Optional.empty();
        }
    }

    /**
     * A Model stub that always accepts the patient being added.
     */
    private class ModelStubAcceptingPatientAdded extends ModelStub {
        final ArrayList<Patient> patientsAdded = new ArrayList<>();

        @Override
        public boolean hasPatient(Patient patient) {
            requireNonNull(patient);
            return patientsAdded.stream().anyMatch(patient::isSamePatient);
        }

        @Override
        public Optional<Patient> findPatientById(PatientId id) {
            requireNonNull(id);
            return patientsAdded.stream().filter(patient -> patient.getId().equals(id)).findFirst();
        }

        @Override
        public void addPatient(Patient patient) {
            requireNonNull(patient);
            patientsAdded.add(patient);
        }

        @Override
        public void showInPatientList(Patient patient) {
            requireNonNull(patient);
        }

        @Override
        public ReadOnlyLaiNurse getLaiNurse() {
            LaiNurse laiNurse = new LaiNurse();
            laiNurse.setPatients(patientsAdded);
            return laiNurse;
        }
    }

}
