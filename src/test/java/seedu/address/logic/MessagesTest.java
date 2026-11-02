package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.logic.parser.CliSyntax.PREFIX_FROM;
import static seedu.address.logic.parser.CliSyntax.PREFIX_PHONE;
import static seedu.address.logic.parser.CliSyntax.PREFIX_TO;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.model.patient.Patient;
import seedu.address.model.patient.PatientAppointment;
import seedu.address.testutil.PatientBuilder;

public class MessagesTest {

    @Test
    public void format_patientWithNotes_includesNormalizedNotes() {
        Patient patient = new PatientBuilder().withMedicalHistory("Asthma").withNotes("Enjoys gardening")
                .withAppointments("2026-11-30").build();
        assertEquals("AMY BEE; ID: S9876543Z; Phone: 85355255; Address: 123, JURONG WEST AVE 6, #08-111"
                + "; Medical History: ASTHMA; Notes: ENJOYS GARDENING; Appointments: 2026-11-30",
                Messages.format(patient));
    }

    @Test
    public void format_patientWithoutNotes_omitsNotes() {
        assertEquals("AMY BEE; ID: S9876543Z; Phone: 85355255; Address: 123, JURONG WEST AVE 6, #08-111",
                Messages.format(new PatientBuilder().build()));
    }

    @Test
    public void formatSameNameWarning_nullArguments_throwsNullPointerException() {
        Patient target = new PatientBuilder().build();
        List<Patient> patientsWithNull = Collections.singletonList(null);
        assertThrows(NullPointerException.class, () -> Messages.formatSameNameWarning(null, target));
        assertThrows(NullPointerException.class, () -> Messages.formatSameNameWarning(List.of(), null));
        assertThrows(NullPointerException.class, () -> Messages.formatSameNameWarning(patientsWithNull, target));
    }

    @Test
    public void formatSameNameWarning_noExactMatches_returnsEmptyString() {
        Patient target = new PatientBuilder().withName("Alex Yeoh").build();
        Patient partialMatch = new PatientBuilder().withId("S1").withName("Alex").build();
        Patient longerName = new PatientBuilder().withId("S2").withName("Alex Yeoh Tan").build();
        assertEquals("", Messages.formatSameNameWarning(List.of(), target));
        assertEquals("", Messages.formatSameNameWarning(List.of(partialMatch, longerName), target));
    }

    @Test
    public void formatSameNameWarning_sameIdWithDifferentDetails_excludesSelf() {
        Patient target = new PatientBuilder().build();
        Patient previousVersion = new PatientBuilder(target).withPhone("11112222").build();
        assertEquals("", Messages.formatSameNameWarning(List.of(target), target));
        assertEquals("", Messages.formatSameNameWarning(List.of(previousVersion), target));
    }

    @Test
    public void formatSameNameWarning_caseAndWhitespaceDifferences_warns() {
        Patient target = new PatientBuilder().withName("  alex\t  yeoh  ").build();
        Patient other = new PatientBuilder().withId("S1234567A").withName("Alex Yeoh").build();
        assertEquals("Warning: this name is also used by ALEX YEOH (S1234567A). Check the patient IDs.",
                Messages.formatSameNameWarning(List.of(target, other), target));
    }

    @Test
    public void formatSameNameWarning_multipleMatches_ordersByIdAndLabelsArchived() {
        Patient target = new PatientBuilder().withId("S3").build();
        Patient active = new PatientBuilder(target).withId("S2").build();
        Patient archived = new PatientBuilder(target).withId("S1").withArchived(true).build();
        Patient differentName = new PatientBuilder().withId("S4").withName("Bob").build();
        String expected = "Warning: this name is also used by AMY BEE (S1) [archived]; AMY BEE (S2). "
                + "Check the patient IDs.";
        assertEquals(expected,
                Messages.formatSameNameWarning(List.of(active, target, differentName, archived), target));
        assertEquals(expected, Messages.formatSameNameWarning(List.of(archived, active), target));
    }

    @Test
    public void getErrorMessageForDuplicatePrefixes_oneOption_namesIt() {
        assertEquals("--phone can be given only once.", Messages.getErrorMessageForDuplicatePrefixes(PREFIX_PHONE));
    }

    @Test
    public void getErrorMessageForDuplicatePrefixes_severalOptions_namesThemInOrder() {
        assertEquals("Each of these options can be given only once: --from, --to.",
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_FROM, PREFIX_TO));
        assertEquals("Each of these options can be given only once: --to, --from.",
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_TO, PREFIX_FROM));
    }

    @Test
    public void formatClashWarning_samePatientAndOther_namesOnlyTheOtherPatient() {
        Patient alex = new PatientBuilder().withId("S1234567A").withName("Alex Yeoh")
                .withAppointments("2026-10-05 09:30", "2026-10-05 09:30").build();
        Patient bernice = new PatientBuilder().withId("S2345678B").withName("Bernice Yu")
                .withAppointments("2026-10-05 09:30").build();
        List<PatientAppointment> appointments = List.of(entry(alex, 1), entry(alex, 2), entry(bernice, 1));
        assertEquals("\nWarning: it clashes with this patient's appointment #1; BERNICE YU (S2345678B), "
                + "appointment #1.", Messages.formatClashWarning(appointments, entry(alex, 2)));
        assertEquals("", Messages.formatClashWarning(List.of(entry(alex, 1)), entry(alex, 1)));
    }

    private static PatientAppointment entry(Patient patient, int number) {
        return new PatientAppointment(patient, patient.getSchedule().findByNumber(number).orElseThrow());
    }

    @Test
    public void formatCount_oneIsSingular_othersArePlural() {
        assertEquals("1 patient", Messages.formatCount(1, "patient"));
        assertEquals("0 patients", Messages.formatCount(0, "patient"));
        assertEquals("2 archived patients", Messages.formatCount(2, "archived patient"));
    }
}
