package seedu.address.model.patient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

public class PhoneTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Phone(null));
    }

    @Test
    public void constructor_invalidPhone_throwsIllegalArgumentException() {
        String invalidPhone = "";
        assertThrows(IllegalArgumentException.class, () -> new Phone(invalidPhone));
    }

    @Test
    public void isValidPhone() {
        // null phone number
        assertThrows(NullPointerException.class, () -> Phone.isValidPhone(null));

        // invalid phone numbers
        assertFalse(Phone.isValidPhone("")); // empty string
        assertFalse(Phone.isValidPhone(" ")); // spaces only
        assertFalse(Phone.isValidPhone("91")); // less than 3 numbers
        assertFalse(Phone.isValidPhone("+6-5")); // less than 3 numbers, with symbols
        assertFalse(Phone.isValidPhone("phone")); // no digits
        assertFalse(Phone.isValidPhone("９３１２")); // full-width digits are not ASCII digits
        assertFalse(Phone.isValidPhone("9312\u200B1534")); // invisible characters are not allowed

        // valid phone numbers
        assertTrue(Phone.isValidPhone("911")); // exactly 3 numbers
        assertTrue(Phone.isValidPhone("93121534"));
        assertTrue(Phone.isValidPhone("124293842033123")); // long phone numbers
        assertTrue(Phone.isValidPhone("9312 1534")); // spaces within digits
        assertTrue(Phone.isValidPhone("+65 9312-1534")); // country code and hyphen
        assertTrue(Phone.isValidPhone(" 93121534 ")); // surrounding whitespace is ignored
        assertTrue(Phone.isValidPhone("9011p041")); // letters alongside three digits
        assertTrue(Phone.isValidPhone("(65) 9312")); // brackets are allowed
        assertTrue(Phone.isValidPhone("9312.1534")); // dots are allowed
        assertTrue(Phone.isValidPhone("#9312 1534")); // other symbols are allowed
        assertTrue(Phone.isValidPhone("1234 5678 (HP) 1111-3333")); // annotations are allowed
    }

    @Test
    public void constructor_normalizesValue() {
        assertEquals("+65 9312-1534", new Phone("  +65   9312-1534 ").value);
        assertEquals("1234 (HP)", new Phone("1234 (hp)").value); // letters are stored in upper case
    }

    @Test
    public void equals() {
        Phone phone = new Phone("999");

        // same values -> returns true
        assertTrue(phone.equals(new Phone("999")));

        // same object -> returns true
        assertTrue(phone.equals(phone));

        // null -> returns false
        assertFalse(phone.equals(null));

        // different types -> returns false
        assertFalse(phone.equals(5.0f));

        // different values -> returns false
        assertFalse(phone.equals(new Phone("995")));
    }
}
