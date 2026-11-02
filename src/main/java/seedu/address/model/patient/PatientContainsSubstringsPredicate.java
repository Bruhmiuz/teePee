package seedu.address.model.patient;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import seedu.address.commons.util.TextNormalizer;
import seedu.address.commons.util.ToStringBuilder;

/**
 * Tests whether any specified patient field contains its corresponding substring.
 * Matching ignores case and repeated whitespace. Phone matching compares digits only.
 */
public final class PatientContainsSubstringsPredicate implements Predicate<Patient> {
    public static final String MESSAGE_PHONE_SEARCH_CONSTRAINTS =
            "Phone search values must contain at least one digit.";

    private final Optional<String> id;
    private final Optional<String> name;
    private final Optional<String> phone;
    private final Optional<String> address;
    private final Optional<String> medicalHistory;
    private final Optional<String> notes;

    /**
     * Creates a predicate with at least one non-blank search criterion.
     *
     * @throws NullPointerException if any optional is null.
     * @throws IllegalArgumentException if no criterion is supplied, any supplied criterion is blank,
     *     or the phone criterion contains no digits.
     */
    public PatientContainsSubstringsPredicate(Optional<String> id, Optional<String> name, Optional<String> phone,
            Optional<String> address, Optional<String> medicalHistory, Optional<String> notes) {
        requireAllNonNull(id, name, phone, address, medicalHistory, notes);
        checkArgument(Stream.of(id, name, phone, address, medicalHistory, notes)
                .anyMatch(Optional::isPresent), "At least one search criterion must be supplied.");
        this.id = normalizeCriterion(id);
        this.name = normalizeCriterion(name);
        this.phone = normalizeCriterion(phone).map(value -> value.replaceAll("[^0-9]", ""));
        checkArgument(this.phone.filter(String::isEmpty).isEmpty(), MESSAGE_PHONE_SEARCH_CONSTRAINTS);
        assert this.phone.map(value -> value.matches("[0-9]+")).orElse(true)
                : "Phone search criteria must contain only digits.";
        this.address = normalizeCriterion(address);
        this.medicalHistory = normalizeCriterion(medicalHistory);
        this.notes = normalizeCriterion(notes);
    }

    @Override
    public boolean test(Patient patient) {
        return countMatchingFields(patient) > 0;
    }

    /**
     * Returns the number of supplied criteria that match their corresponding patient fields.
     * Each field contributes at most one match, regardless of repeated occurrences of the substring.
     *
     * @throws NullPointerException if {@code patient} is null.
     */
    public int countMatchingFields(Patient patient) {
        requireNonNull(patient);
        int matchCount = 0;
        if (matches(id, patient.getId().value)) {
            matchCount++;
        }
        if (matches(name, patient.getName().fullName)) {
            matchCount++;
        }
        if (matches(phone, patient.getPhone().value.replaceAll("[^0-9]", ""))) {
            matchCount++;
        }
        if (matches(address, patient.getAddress().value)) {
            matchCount++;
        }
        if (patient.getMedicalHistory().map(history -> matches(medicalHistory, history.value)).orElse(false)) {
            matchCount++;
        }
        if (patient.getNotes().map(patientNotes -> matches(notes, patientNotes.value)).orElse(false)) {
            matchCount++;
        }
        assert matchCount >= 0 && matchCount <= 6 : "Each of the six searchable fields contributes at most one match.";
        return matchCount;
    }

    private static Optional<String> normalizeCriterion(Optional<String> criterion) {
        return criterion.map(value -> {
            String normalizedValue = TextNormalizer.normalizeText(value);
            checkArgument(!normalizedValue.isEmpty(), "Search values must not be blank.");
            return normalizedValue;
        });
    }

    private static boolean matches(Optional<String> criterion, String fieldValue) {
        return criterion.map(fieldValue::contains).orElse(false);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof PatientContainsSubstringsPredicate otherPredicate)) {
            return false;
        }

        return id.equals(otherPredicate.id)
                && name.equals(otherPredicate.name)
                && phone.equals(otherPredicate.phone)
                && address.equals(otherPredicate.address)
                && medicalHistory.equals(otherPredicate.medicalHistory)
                && notes.equals(otherPredicate.notes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, phone, address, medicalHistory, notes);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("id", id)
                .add("name", name)
                .add("phone", phone)
                .add("address", address)
                .add("medicalHistory", medicalHistory)
                .add("notes", notes)
                .toString();
    }
}
