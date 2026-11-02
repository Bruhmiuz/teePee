package seedu.address.logic.parser;

import static java.util.Objects.requireNonNull;
import static seedu.address.logic.parser.CliSyntax.PREFIX_ALL;
import static seedu.address.logic.parser.CliSyntax.PREFIX_ARCHIVED;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.logging.Logger;

import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.util.DateUtil;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.OptionalFieldEdit;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.PatientScope;
import seedu.address.model.patient.Address;
import seedu.address.model.patient.Appointment;
import seedu.address.model.patient.AppointmentDetails;
import seedu.address.model.patient.MedicalHistory;
import seedu.address.model.patient.Name;
import seedu.address.model.patient.Notes;
import seedu.address.model.patient.PatientId;
import seedu.address.model.patient.Phone;

/**
 * Contains utility methods used for parsing strings in the various *Parser classes.
 */
public class ParserUtil {

    private static final Logger logger = LogsCenter.getLogger(ParserUtil.class);

    /**
     * Parses the value of an option into a field, such as {@link #parseNotes(String)}.
     *
     * @param <T> The type of the field.
     */
    @FunctionalInterface
    public interface FieldParser<T> {
        T parse(String value) throws ParseException;
    }

    /**
     * Returns the edit of an optional field that {@code argMultimap} gives with {@code replacePrefix} or
     * {@code appendPrefix}, or {@code Optional.empty()} if it gives neither.
     * {@code replacePrefix} with a value replaces the field, and with no value clears it.
     * {@code appendPrefix} adds its value, which must not be blank, to the end of the field.
     *
     * @param parseValue Parses a value given to either prefix, and rejects a blank one.
     * @throws ParseException if both prefixes are given, or a value is invalid.
     */
    public static <T> Optional<OptionalFieldEdit<T>> parseOptionalFieldEdit(ArgumentMultimap argMultimap,
            Prefix replacePrefix, Prefix appendPrefix, FieldParser<T> parseValue) throws ParseException {
        requireNonNull(argMultimap);
        Optional<String> replaceValue = argMultimap.getValue(replacePrefix);
        Optional<String> appendValue = argMultimap.getValue(appendPrefix);
        if (replaceValue.isPresent() && appendValue.isPresent()) {
            throw new ParseException(String.format(Messages.MESSAGE_CONFLICTING_FIELD_EDITS,
                    replacePrefix.toString().trim(), appendPrefix.toString().trim()));
        }
        if (replaceValue.isPresent()) {
            return Optional.of(replaceValue.get().isBlank()
                    ? OptionalFieldEdit.clear()
                    : OptionalFieldEdit.replace(parseValue.parse(replaceValue.get())));
        }
        if (appendValue.isPresent()) {
            return Optional.of(OptionalFieldEdit.append(parseValue.parse(appendValue.get())));
        }
        return Optional.empty();
    }

    /**
     * Returns the scope that {@code PREFIX_ALL} or {@code PREFIX_ARCHIVED} gives in {@code argMultimap}, or
     * {@code Optional#empty()} if neither is given. Neither option takes a value.
     *
     * @param invalidFormatMessage Message of the exception thrown if an option has a value.
     * @throws ParseException if both options are given, either is given twice, or either has a value.
     */
    public static Optional<PatientScope> parseScope(ArgumentMultimap argMultimap, String invalidFormatMessage)
            throws ParseException {
        requireNonNull(argMultimap);
        argMultimap.verifyNoDuplicatePrefixesFor(PREFIX_ALL, PREFIX_ARCHIVED);
        Optional<String> all = argMultimap.getValue(PREFIX_ALL);
        Optional<String> archived = argMultimap.getValue(PREFIX_ARCHIVED);
        if (all.isPresent() && archived.isPresent()) {
            throw new ParseException(Messages.MESSAGE_CONFLICTING_SCOPES);
        }
        if (all.or(() -> archived).filter(value -> !value.isEmpty()).isPresent()) {
            throw new ParseException(invalidFormatMessage);
        }
        if (all.isPresent()) {
            return Optional.of(PatientScope.ALL);
        }
        return archived.isPresent() ? Optional.of(PatientScope.ARCHIVED) : Optional.empty();
    }

    /**
     * Parses a {@code String id} into a {@code PatientId}.
     * Leading and trailing whitespaces will be trimmed.
     *
     * @throws ParseException if the given {@code id} is invalid.
     */
    public static PatientId parseId(String id) throws ParseException {
        requireNonNull(id);
        String trimmedId = id.trim();
        if (!PatientId.isValidId(trimmedId)) {
            throw new ParseException(PatientId.MESSAGE_CONSTRAINTS);
        }
        return new PatientId(trimmedId);
    }

    /**
     * Parses a {@code String name} into a {@code Name}.
     * Leading and trailing whitespaces will be trimmed.
     *
     * @throws ParseException if the given {@code name} is invalid.
     */
    public static Name parseName(String name) throws ParseException {
        requireNonNull(name);
        String trimmedName = name.trim();
        if (!Name.isValidName(trimmedName)) {
            throw new ParseException(Name.MESSAGE_CONSTRAINTS);
        }
        return new Name(trimmedName);
    }

    /**
     * Parses a {@code String phone} into a {@code Phone}.
     * Leading and trailing whitespaces will be trimmed.
     *
     * @throws ParseException if the given {@code phone} is invalid.
     */
    public static Phone parsePhone(String phone) throws ParseException {
        requireNonNull(phone);
        String trimmedPhone = phone.trim();
        if (!Phone.isValidPhone(trimmedPhone)) {
            throw new ParseException(Phone.MESSAGE_CONSTRAINTS);
        }
        return new Phone(trimmedPhone);
    }

    /**
     * Parses a {@code String address} into an {@code Address}.
     * Leading and trailing whitespaces will be trimmed.
     *
     * @throws ParseException if the given {@code address} is invalid.
     */
    public static Address parseAddress(String address) throws ParseException {
        requireNonNull(address);
        String trimmedAddress = address.trim();
        if (!Address.isValidAddress(trimmedAddress)) {
            throw new ParseException(Address.MESSAGE_CONSTRAINTS);
        }
        return new Address(trimmedAddress);
    }

    /**
     * Parses {@code String medicalHistory} into a {@code MedicalHistory}.
     * Leading and trailing whitespaces will be trimmed.
     *
     * @throws ParseException if the given {@code medicalHistory} is invalid.
     */
    public static MedicalHistory parseMedicalHistory(String medicalHistory) throws ParseException {
        requireNonNull(medicalHistory);
        String trimmedMedicalHistory = medicalHistory.trim();
        if (!MedicalHistory.isValidMedicalHistory(trimmedMedicalHistory)) {
            throw new ParseException(MedicalHistory.MESSAGE_CONSTRAINTS);
        }
        return new MedicalHistory(trimmedMedicalHistory);
    }

    /**
     * Parses {@code String notes} into {@code Notes}.
     * Leading and trailing whitespaces will be trimmed.
     *
     * @throws ParseException if the given {@code notes} are invalid.
     */
    public static Notes parseNotes(String notes) throws ParseException {
        requireNonNull(notes);
        String trimmedNotes = notes.trim();
        if (!Notes.isValidNotes(trimmedNotes)) {
            logger.fine("Rejected blank patient notes.");
            throw new ParseException(Notes.MESSAGE_CONSTRAINTS);
        }
        Notes parsedNotes = new Notes(trimmedNotes);
        logger.fine("Parsed patient notes.");
        return parsedNotes;
    }

    /**
     * Parses {@code String appointment} into an {@code Appointment}.
     * Leading and trailing whitespaces will be trimmed.
     *
     * @throws ParseException if the given {@code Appointment} is invalid.
     */
    public static Appointment parseAppointment(String appointment) throws ParseException {
        requireNonNull(appointment);
        String trimmedAppointment = appointment.trim();
        if (!Appointment.isValidAppointment(trimmedAppointment)) {
            throw new ParseException(Appointment.MESSAGE_CONSTRAINTS);
        }
        return new Appointment(trimmedAppointment);
    }

    /**
     * Parses a patient-local appointment number, which is a positive whole number with no sign or fraction.
     * A number too large for any appointment is kept, so that the command reports that the patient has no such
     * appointment.
     *
     * @throws ParseException if {@code number} is not a positive whole number, or is too large for a {@code long}.
     */
    public static long parseAppointmentNumber(String number) throws ParseException {
        requireNonNull(number);
        if (!number.matches("[0-9]+") || number.matches("0+")) {
            logger.fine("Rejected invalid appointment number.");
            throw new ParseException(Appointment.MESSAGE_NUMBER_CONSTRAINTS);
        }
        try {
            return Long.parseLong(number);
        } catch (NumberFormatException e) {
            logger.fine("Rejected appointment number that is too large.");
            throw new ParseException(String.format(Appointment.MESSAGE_NUMBER_TOO_LARGE, number));
        }
    }

    /**
     * Parses {@code String details} into {@code AppointmentDetails}.
     * Leading and trailing whitespaces will be trimmed.
     *
     * @throws ParseException if the given {@code details} are blank.
     */
    public static AppointmentDetails parseAppointmentDetails(String details) throws ParseException {
        requireNonNull(details);
        String trimmedDetails = details.trim();
        if (!AppointmentDetails.isValidAppointmentDetails(trimmedDetails)) {
            logger.fine("Rejected blank appointment details.");
            throw new ParseException(AppointmentDetails.MESSAGE_CONSTRAINTS);
        }
        AppointmentDetails parsedDetails = new AppointmentDetails(trimmedDetails);
        logger.fine("Parsed appointment details.");
        return parsedDetails;
    }

    /**
     * Parses {@code String date} into a {@code LocalDate}.
     * Leading and trailing whitespaces will be trimmed.
     *
     * @throws ParseException if the given {@code date} is invalid.
     */
    public static LocalDate parseDate(String date) throws ParseException {
        requireNonNull(date);
        String trimmedDate = date.trim();
        try {
            return DateUtil.parseDate(trimmedDate);
        } catch (DateTimeParseException e) {
            throw new ParseException(DateUtil.MESSAGE_CONSTRAINTS);
        }
    }
}
