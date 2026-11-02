package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import seedu.address.logic.Messages;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.patient.Appointment;
import seedu.address.model.patient.Patient;
import seedu.address.model.patient.PatientId;
import seedu.address.testutil.LaiNurseBuilder;
import seedu.address.testutil.PatientBuilder;

public class ClearAppointmentCommandTest {

    private static final Patient ALEX = new PatientBuilder().withId("S1234567A").withName("Alex")
            .withAppointments("2026-10-05", "2026-10-05 09:30", "2026-10-06 10:00").build();
    private static final Patient BEN = new PatientBuilder().withId("S2345678B").withName("Ben")
            .withAppointments("2026-10-05 12:00", "2026-11-01").build();

    private final Model model = new ModelManager(new LaiNurseBuilder().withPatient(ALEX).withPatient(BEN).build(),
            new UserPrefs());

    @Test
    public void constructor_fromAfterTo_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new ClearAppointmentCommand(Optional.empty(),
                at("2026-10-06"), at("2026-10-05")));
    }

    @Test
    public void execute_nothingGiven_deletesEveryAppointment() throws Exception {
        ClearAppointmentCommand command = new ClearAppointmentCommand(Optional.empty(), Optional.empty(),
                Optional.empty());
        assertEquals(String.format(ClearAppointmentCommand.MESSAGE_DELETES, "5 appointments", "2 patients"),
                command.describeDeletion(model));
        assertEquals(String.format(ClearAppointmentCommand.MESSAGE_SUCCESS, "5 appointments"), command.execute(model)
                .getFeedbackToUser());
        assertEquals(List.of(), appointmentsOf(ALEX.getId()));
        assertEquals(List.of(), appointmentsOf(BEN.getId()));
        // the patients stay
        assertEquals(2, model.getLaiNurse().getPatientList().size());
    }

    @Test
    public void execute_patientOnly_deletesAllOfTheirs() throws Exception {
        ClearAppointmentCommand command = new ClearAppointmentCommand(Optional.of(ALEX.getId()), Optional.empty(),
                Optional.empty());
        assertEquals(String.format(ClearAppointmentCommand.MESSAGE_DELETES_OF_PATIENT, "3 appointments", ALEX.getName(),
                ALEX.getId()), command.describeDeletion(model));
        assertEquals(new CommandResult(String.format(ClearAppointmentCommand.MESSAGE_SUCCESS, "3 appointments"))
                .withSelectionCleared(), command.execute(model));
        assertEquals(0, appointmentsOf(ALEX.getId()).size());
        assertEquals(2, appointmentsOf(BEN.getId()).size());
    }

    @Test
    public void execute_dateRange_wholeDaysOfEveryPatient() throws Exception {
        // a date without a time covers the whole day, and an appointment with no time is on it
        ClearAppointmentCommand command = new ClearAppointmentCommand(Optional.empty(), at("2026-10-05"),
                at("2026-10-05"));
        assertEquals(String.format(ClearAppointmentCommand.MESSAGE_DELETES, "3 appointments", "2 patients"),
                command.describeDeletion(model));
        command.execute(model);
        assertEquals(List.of("2026-10-06 10:00"), appointmentsOf(ALEX.getId()));
        assertEquals(List.of("2026-11-01"), appointmentsOf(BEN.getId()));
    }

    @Test
    public void execute_timeRangeOfOnePatient_keepsTheRest() throws Exception {
        // an appointment with no time counts as at the start of its day, so it is before 09:00
        ClearAppointmentCommand command = new ClearAppointmentCommand(Optional.of(ALEX.getId()),
                at("2026-10-05 09:00"), at("2026-10-06 10:00"));
        assertEquals(String.format(ClearAppointmentCommand.MESSAGE_SUCCESS, "2 appointments"), command.execute(model)
                .getFeedbackToUser());
        assertEquals(List.of("2026-10-05"), appointmentsOf(ALEX.getId()));
        assertEquals(2, appointmentsOf(BEN.getId()).size());

        // an open range
        new ClearAppointmentCommand(Optional.empty(), at("2026-10-31"), Optional.empty()).execute(model);
        assertEquals(List.of("2026-10-05 12:00"), appointmentsOf(BEN.getId()));
    }

    @Test
    public void describeDeletion_nothingToDelete_throwsCommandException() {
        assertThrows(CommandException.class, ClearAppointmentCommand.MESSAGE_NO_APPOINTMENTS, () ->
                new ClearAppointmentCommand(Optional.empty(), at("2030-01-01"), Optional.empty())
                        .describeDeletion(model));
        PatientId unknown = new PatientId("T9999999Z");
        assertThrows(CommandException.class, String.format(Messages.MESSAGE_PATIENT_NOT_FOUND, unknown), () ->
                new ClearAppointmentCommand(Optional.of(unknown), Optional.empty(), Optional.empty())
                        .describeDeletion(model));
    }

    @Test
    public void equals() {
        ClearAppointmentCommand command = new ClearAppointmentCommand(Optional.of(ALEX.getId()), at("2026-10-05"),
                Optional.empty());
        assertEquals(command, new ClearAppointmentCommand(Optional.of(ALEX.getId()), at("2026-10-05"),
                Optional.empty()));
        assertNotEquals(command, new ClearAppointmentCommand(Optional.of(BEN.getId()), at("2026-10-05"),
                Optional.empty()));
        assertNotEquals(command, new ClearAppointmentCommand(Optional.of(ALEX.getId()), Optional.empty(),
                at("2026-10-05")));
        assertNotEquals(command, null);
    }

    private static Optional<Appointment> at(String dateTime) {
        return Optional.of(new Appointment(dateTime));
    }

    private List<String> appointmentsOf(PatientId id) {
        return model.findPatientById(id).orElseThrow().getSchedule().asList().stream()
                .map(Appointment::toString).toList();
    }
}
