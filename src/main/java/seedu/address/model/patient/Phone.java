package seedu.address.model.patient;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

import seedu.address.commons.util.TextNormalizer;

/**
 * Represents a Patient's phone number in LaiNurse.
 * The number is stored in upper case, with leading, trailing and repeated whitespace removed.
 * Guarantees: immutable; is valid as declared in {@link #isValidPhone(String)}
 */
public class Phone {


    public static final String MESSAGE_CONSTRAINTS =
            "Phone numbers accept any non-blank text containing at least three digits, and cannot contain "
                    + "common invisible characters such as the zero-width space.";

    /*
     * The first lookahead requires at least three ASCII digits anywhere in the value. The rest accepts any visible
     * character, so that annotated numbers such as "1234 5678 (HP) 1111-3333" are accepted. Format characters
     * (which include the invisible characters that pasted text can carry, such as the zero-width space) and
     * control characters are rejected.
     */
    public static final String VALIDATION_REGEX = "(?=(?:\\D*\\d){3})(?:(?!\\p{Cf}|\\p{Cc}).)+";
    public final String value;

    /**
     * Constructs a {@code Phone}.
     *
     * @param phone A valid phone number.
     */
    public Phone(String phone) {
        requireNonNull(phone);
        checkArgument(isValidPhone(phone), MESSAGE_CONSTRAINTS);
        value = TextNormalizer.normalize(phone);
    }

    /**
     * Returns true if a given string is a valid phone number.
     * Leading, trailing and repeated whitespace is ignored.
     */
    public static boolean isValidPhone(String test) {
        return TextNormalizer.normalize(test).matches(VALIDATION_REGEX);
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof Phone otherPhone)) {
            return false;
        }

        return value.equals(otherPhone.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

}
