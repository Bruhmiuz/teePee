package seedu.address.model.patient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import seedu.address.testutil.PatientBuilder;

public class PatientContainsSubstringsPredicateTest {

    private final Patient patient = new PatientBuilder().withId("T04123").withName("Mary Jane")
            .withPhone("+65 9876-5432").withAddress("123, Marina Bay, #01-02")
            .withMedicalHistory("Type 2 diabetes; allergy (penicillin)")
            .withNotes("Enjoys gardening; walks at 7am (daily)")
            .withAppointments("2026-10-15 09:30").build();

    @Test
    public void constructor_noCriteria_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> predicate(null, null, null, null, null));
    }

    @Test
    public void constructor_nullOptional_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new PatientContainsSubstringsPredicate(null,
                Optional.of("Mary"), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
        assertThrows(NullPointerException.class, () -> new PatientContainsSubstringsPredicate(Optional.empty(),
                Optional.of("Mary"), Optional.empty(), Optional.empty(), Optional.empty(), null));
    }

    @Test
    public void constructor_blankCriterion_throwsIllegalArgumentException() {
        for (String blank : List.of("", " \t\n ")) {
            assertThrows(IllegalArgumentException.class, () -> predicate(blank, null, null, null, null));
            assertThrows(IllegalArgumentException.class, () -> predicate(null, blank, null, null, null));
            assertThrows(IllegalArgumentException.class, () -> predicate(null, null, blank, null, null));
            assertThrows(IllegalArgumentException.class, () -> predicate(null, null, null, blank, null));
            assertThrows(IllegalArgumentException.class, () -> predicate(null, null, null, null, blank));
            assertThrows(IllegalArgumentException.class, () -> predicate(null, null, null, null, null, blank));
        }
    }

    @Test
    public void constructor_phoneWithoutDigits_throwsIllegalArgumentException() {
        String expectedMessage = "Phone search values must contain at least one digit.";
        for (String value : List.of("+-()", "abc", ".*", "\uFF11\uFF12\uFF13")) {
            assertThrows(IllegalArgumentException.class, expectedMessage, () ->
                    predicate(null, null, value, null, null));
            assertThrows(IllegalArgumentException.class, expectedMessage, () ->
                    predicate(null, "Mary", value, null, null));
        }
    }

    @Test
    public void test_eachFieldContainsSubstring_returnsTrue() {
        List<PatientContainsSubstringsPredicate> predicates = List.of(
                predicate("041", null, null, null, null),
                predicate(null, "arY", null, null, null),
                predicate(null, null, "9876", null, null),
                predicate(null, null, null, "ina Bay", null),
                predicate(null, null, null, null, "diabet"),
                predicate(null, null, null, null, null, "garden"));
        for (PatientContainsSubstringsPredicate predicate : predicates) {
            assertTrue(predicate.test(patient), predicate.toString());
            assertEquals(1, predicate.countMatchingFields(patient), predicate.toString());
        }
    }

    @Test
    public void countMatchingFields_multipleCriteria_countsOnlyMatchingFields() {
        List<PatientContainsSubstringsPredicate> predicates = List.of(
                predicate("Z99", "John", "1111", "Jurong", "asthma"),
                predicate("041", "John", "1111", "Jurong", "asthma"),
                predicate("041", "Mary", "1111", "Jurong", "asthma"),
                predicate("041", "Mary", "9876", "Jurong", "asthma"),
                predicate("041", "Mary", "9876", "Marina", "asthma"),
                predicate("041", "Mary", "9876", "Marina", "diabetes"));
        for (int expectedCount = 0; expectedCount < predicates.size(); expectedCount++) {
            PatientContainsSubstringsPredicate predicate = predicates.get(expectedCount);
            assertEquals(expectedCount, predicate.countMatchingFields(patient));
            assertEquals(expectedCount > 0, predicate.test(patient));
        }
    }

    @Test
    public void countMatchingFields_repeatedOccurrences_countsEachFieldOnce() {
        Patient repeatedValues = new PatientBuilder().withId("123123").withName("Mary Mary Mary")
                .withPhone("123-123").withAddress("Bay Bay").withMedicalHistory("diabetes diabetes").build();
        assertEquals(1, predicate(null, "Mary", null, null, null).countMatchingFields(repeatedValues));
        assertEquals(5, predicate("123", "Mary", "1-2 3", "Bay", "diabetes").countMatchingFields(repeatedValues));
    }

    @Test
    public void countMatchingFields_caseAndWhitespaceDifferences_countsAllMatches() {
        PatientContainsSubstringsPredicate predicate = predicate(" t04 ", "  mArY\t jAnE ", " +65\t 9876-54 ",
                " marina\n  BAY ", " type\t 2 DIABETES ");
        assertEquals(5, predicate.countMatchingFields(patient));
    }

    @Test
    public void countMatchingFields_missingMedicalHistory_doesNotContributeMatch() {
        Patient withoutHistory = new PatientBuilder().build();
        assertEquals(0, predicate(null, null, null, null, "diabetes").countMatchingFields(withoutHistory));
        assertEquals(1, predicate(null, "Amy", null, null, "diabetes").countMatchingFields(withoutHistory));
    }

    @Test
    public void countMatchingFields_nullPatient_throwsNullPointerException() {
        PatientContainsSubstringsPredicate predicate = predicate(null, "Mary", null, null, null);
        assertThrows(NullPointerException.class, () -> predicate.countMatchingFields(null));
        assertThrows(NullPointerException.class, () -> predicate.test(null));
    }

    @Test
    public void test_onlyCorrespondingFieldIsChecked_returnsFalse() {
        assertFalse(predicate("Mary", null, null, null, null).test(patient));
        assertFalse(predicate(null, "Marina", null, null, null).test(patient));
        assertFalse(predicate(null, null, "2026", null, null).test(patient));
        assertFalse(predicate(null, null, null, "diabetes", null).test(patient));
        assertFalse(predicate(null, null, null, null, "09:30").test(patient));
        assertFalse(predicate(null, null, null, null, null, "penicillin").test(patient));
        assertFalse(predicate(null, "gardening", null, null, null).test(patient));
    }

    @Test
    public void test_appointmentsAreNotSearched_returnsFalse() {
        // Appointment searches belong to viewappt, so no field matches the appointment's text
        for (String appointmentText : List.of("2026-10-15", "2026-10", "09:30")) {
            assertFalse(predicate(appointmentText, appointmentText, appointmentText, appointmentText,
                    appointmentText, appointmentText).test(patient));
        }
    }

    @Test
    public void test_caseAndWhitespaceDifferences_returnsTrue() {
        assertTrue(predicate(null, "  mArY\t  jAnE  ", null, null, null).test(patient));
        assertTrue(predicate(null, null, null, "  marina \n BAY ", null).test(patient));
        assertTrue(predicate(null, null, null, null, null, "  WALKS\t at 7am  ").test(patient));
    }

    @Test
    public void test_zeroWidthSpaceInName_foundByNameShown() {
        // the list shows ALEXSMITH, so a search for it finds the patient (#218)
        Patient alex = new PatientBuilder().withName("Alex\u200BSmith").build();
        assertEquals("ALEXSMITH", alex.getName().fullName);
        assertTrue(predicate(null, "alexsmith", null, null, null).test(alex));
        assertTrue(predicate(null, "alex\u200Bsmith", null, null, null).test(alex));
    }

    @Test
    public void test_multiwordValueRequiresContiguousSubstring() {
        assertTrue(predicate(null, null, null, "Marina Bay", null).test(patient));
        assertFalse(predicate(null, null, null, "Bay Marina", null).test(patient));
        assertFalse(predicate(null, null, null, "Marina Street", null).test(patient));
        assertFalse(predicate(null, null, null, null, null, "7am at walks").test(patient));
    }

    @Test
    public void test_multipleCriteria_matchAnyField() {
        assertTrue(predicate(null, "Mary", null, "Jurong", null).test(patient));
        assertTrue(predicate(null, "John", null, "Marina", null).test(patient));
        assertFalse(predicate(null, "John", null, "Jurong", null).test(patient));
        assertTrue(predicate(null, "John", null, null, null, "gardening").test(patient));
        assertTrue(predicate(null, "Mary", null, null, null, "knitting").test(patient));
        assertFalse(predicate(null, "John", null, null, null, "knitting").test(patient));
    }

    @Test
    public void test_missingOptionalFields_doNotMatch() {
        Patient withoutOptionalFields = new PatientBuilder().build();
        assertFalse(predicate(null, null, null, null, "diabetes").test(withoutOptionalFields));
        assertTrue(predicate(null, "Amy", null, null, "diabetes").test(withoutOptionalFields));
        assertFalse(predicate(null, null, null, null, null, "gardening").test(withoutOptionalFields));
        assertTrue(predicate(null, "Amy", null, null, null, "gardening").test(withoutOptionalFields));
    }

    @Test
    public void test_phoneFormattingDifferences_matchesDigitsOnly() {
        for (String storedPhone : List.of("+65 9876-5432", "+6-5 98 765 432", "6598765432")) {
            Patient formattedPatient = new PatientBuilder(patient).withPhone(storedPhone).build();
            for (String search : List.of("987654", "9876-54", "9.8/76 (54)", "+65 9876-5432", "6598765432",
                    "  +65\t 9876 54 ", "4", "phone: 987654")) {
                PatientContainsSubstringsPredicate predicate = predicate(null, null, search, null, null);
                assertTrue(predicate.test(formattedPatient), storedPhone + " searched with " + search);
                assertEquals(1, predicate.countMatchingFields(formattedPatient));
            }
        }
    }

    @Test
    public void test_phoneLeadingZeros_preservesDigits() {
        Patient leadingZeros = new PatientBuilder(patient).withPhone("0012-0345").build();
        assertTrue(predicate(null, null, "00-12", null, null).test(leadingZeros));
        assertTrue(predicate(null, null, "0 3", null, null).test(leadingZeros));
        assertFalse(predicate(null, null, "00012", null, null).test(leadingZeros));
        assertFalse(predicate(null, null, "12345", null, null).test(leadingZeros));
    }

    @Test
    public void test_phoneDifferentDigits_returnsFalse() {
        for (String search : List.of("1111", "98-54", "123987654", "+44 9876-5432")) {
            PatientContainsSubstringsPredicate predicate = predicate(null, null, search, null, null);
            assertFalse(predicate.test(patient), search);
            assertEquals(0, predicate.countMatchingFields(patient));
        }
    }

    @Test
    public void test_punctuationIsLiteral() {
        assertTrue(predicate(null, null, null, "#01-", null).test(patient));
        assertTrue(predicate(null, null, null, null, "(penicillin)").test(patient));
        assertFalse(predicate(null, null, null, null, ".*").test(patient));
        assertTrue(predicate(null, null, null, null, null, "(daily)").test(patient));
        assertFalse(predicate(null, null, null, null, null, ".*").test(patient));
        Patient literalPattern = new PatientBuilder().withMedicalHistory("Recorded .* literally").build();
        assertTrue(predicate(null, null, null, null, ".*").test(literalPattern));
    }

    @Test
    public void equals() {
        PatientContainsSubstringsPredicate firstPredicate = predicate("T04", "Mary Jane", "9876", "Marina Bay",
                "diabetes", "gardening");
        PatientContainsSubstringsPredicate normalizedCopy = predicate("t04", " mary\t jane ", "98-76", "marina bay",
                "DIABETES", " GARDENING ");
        assertTrue(firstPredicate.equals(firstPredicate));
        assertTrue(firstPredicate.equals(normalizedCopy));
        assertTrue(normalizedCopy.equals(firstPredicate));
        assertEquals(firstPredicate.hashCode(), normalizedCopy.hashCode());
        assertFalse(firstPredicate.equals(null));
        assertFalse(firstPredicate.equals(1));

        List<PatientContainsSubstringsPredicate> differentPredicates = List.of(
                predicate("S04", "Mary Jane", "9876", "Marina Bay", "diabetes", "gardening"),
                predicate("T04", "John", "9876", "Marina Bay", "diabetes", "gardening"),
                predicate("T04", "Mary Jane", "1234", "Marina Bay", "diabetes", "gardening"),
                predicate("T04", "Mary Jane", "9876", "Jurong", "diabetes", "gardening"),
                predicate("T04", "Mary Jane", "9876", "Marina Bay", "asthma", "gardening"),
                predicate(null, "Mary Jane", "9876", "Marina Bay", "diabetes", "gardening"),
                predicate("T04", "Mary Jane", "9876", "Marina Bay", "diabetes", "knitting"),
                predicate("T04", "Mary Jane", "9876", "Marina Bay", "diabetes"));
        for (PatientContainsSubstringsPredicate differentPredicate : differentPredicates) {
            assertFalse(firstPredicate.equals(differentPredicate));
        }
    }

    @Test
    public void toStringMethod() {
        PatientContainsSubstringsPredicate predicate = predicate("123", "Mary", "9876", "Marina", "diabetes");
        String expected = PatientContainsSubstringsPredicate.class.getCanonicalName()
                + "{id=Optional[123], name=Optional[MARY], phone=Optional[9876], address=Optional[MARINA], "
                + "medicalHistory=Optional[DIABETES], notes=Optional.empty}";
        assertEquals(expected, predicate.toString());
    }

    private PatientContainsSubstringsPredicate predicate(String id, String name, String phone, String address,
            String medicalHistory) {
        return predicate(id, name, phone, address, medicalHistory, null);
    }

    private PatientContainsSubstringsPredicate predicate(String id, String name, String phone, String address,
            String medicalHistory, String notes) {
        return new PatientContainsSubstringsPredicate(Optional.ofNullable(id), Optional.ofNullable(name),
                Optional.ofNullable(phone), Optional.ofNullable(address), Optional.ofNullable(medicalHistory),
                Optional.ofNullable(notes));
    }
}
