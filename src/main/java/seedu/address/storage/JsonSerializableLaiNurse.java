package seedu.address.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;

import seedu.address.commons.exceptions.IllegalValueException;
import seedu.address.model.LaiNurse;
import seedu.address.model.ReadOnlyLaiNurse;
import seedu.address.model.patient.Patient;

/**
 * An Immutable LaiNurse that is serializable to JSON format.
 */
@JsonRootName(value = "lainurse")
class JsonSerializableLaiNurse {

    public static final String MESSAGE_DUPLICATE_PATIENT = "Patients list contains duplicate patients.";
    public static final String MESSAGE_MISSING_PATIENTS = "The data file has no \"patients\" list.";
    public static final String MESSAGE_NULL_PATIENT = "Patients list contains a null entry.";

    // Jackson uses the field name as the key in the data file. Null if the file has no "patients" key.
    private final List<JsonAdaptedPatient> patients;

    /**
     * Constructs a {@code JsonSerializableLaiNurse} with the given patients.
     * {@code patients} is null if the data file has no {@code "patients"} key, such as a file from v1.3 or earlier,
     * which saved them under {@code "persons"}.
     */
    @JsonCreator
    public JsonSerializableLaiNurse(@JsonProperty("patients") List<JsonAdaptedPatient> patients) {
        this.patients = patients == null ? null : new ArrayList<>(patients);
    }

    /**
     * Converts a given {@code ReadOnlyLaiNurse} into this class for Jackson use.
     *
     * @param source future changes to this will not affect the created {@code JsonSerializableLaiNurse}.
     */
    public JsonSerializableLaiNurse(ReadOnlyLaiNurse source) {
        patients = source.getPatientList().stream().map(JsonAdaptedPatient::new).collect(Collectors.toList());
    }

    /**
     * Converts this LaiNurse data into the model's {@code LaiNurse} object.
     *
     * @throws IllegalValueException if the data file has no patient list, or there were any data constraints violated.
     */
    public LaiNurse toModelType() throws IllegalValueException {
        if (patients == null) {
            throw new IllegalValueException(MESSAGE_MISSING_PATIENTS);
        }
        LaiNurse laiNurse = new LaiNurse();
        for (JsonAdaptedPatient jsonAdaptedPatient : patients) {
            if (jsonAdaptedPatient == null) {
                throw new IllegalValueException(MESSAGE_NULL_PATIENT);
            }
            Patient patient = jsonAdaptedPatient.toModelType();
            if (laiNurse.hasPatient(patient)) {
                throw new IllegalValueException(MESSAGE_DUPLICATE_PATIENT);
            }
            laiNurse.addPatient(patient);
        }
        return laiNurse;
    }

}
