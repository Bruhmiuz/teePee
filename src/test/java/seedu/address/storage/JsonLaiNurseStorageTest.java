package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPatients.ALICE;
import static seedu.address.testutil.TypicalPatients.HOON;
import static seedu.address.testutil.TypicalPatients.IDA;
import static seedu.address.testutil.TypicalPatients.getTypicalLaiNurse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.model.LaiNurse;
import seedu.address.model.ReadOnlyLaiNurse;

public class JsonLaiNurseStorageTest {
    private static final Path TEST_DATA_FOLDER = Paths.get("src", "test", "data", "JsonLaiNurseStorageTest");

    @TempDir
    public Path testFolder;

    @Test
    public void readLaiNurse_nullFilePath_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> readLaiNurse(null));
    }

    private java.util.Optional<ReadOnlyLaiNurse> readLaiNurse(String filePath) throws Exception {
        return new JsonLaiNurseStorage(Paths.get(filePath)).readLaiNurse(addToTestDataPathIfNotNull(filePath));
    }

    private Path addToTestDataPathIfNotNull(String prefsFileInTestDataFolder) {
        return prefsFileInTestDataFolder != null
                ? TEST_DATA_FOLDER.resolve(prefsFileInTestDataFolder)
                : null;
    }

    @Test
    public void read_missingFile_emptyResult() throws Exception {
        assertFalse(readLaiNurse("NonExistentFile.json").isPresent());
    }

    @Test
    public void read_notJsonFormat_exceptionThrown() {
        assertThrows(DataLoadingException.class, () -> readLaiNurse("notJsonFormatLaiNurse.json"));
    }

    @Test
    public void read_onlyNull_exceptionThrown() {
        assertThrows(DataLoadingException.class, () -> readLaiNurse("nullLaiNurse.json"));
    }

    @Test
    public void readLaiNurse_invalidPatientLaiNurse_throwDataLoadingException() {
        assertThrows(DataLoadingException.class, () -> readLaiNurse("invalidPatientLaiNurse.json"));
    }

    @Test
    public void readLaiNurse_invalidAndValidPatientLaiNurse_throwDataLoadingException() {
        assertThrows(DataLoadingException.class, () -> readLaiNurse("invalidAndValidPatientLaiNurse.json"));
    }

    @Test
    public void readAndSaveLaiNurse_allInOrder_success() throws Exception {
        Path filePath = testFolder.resolve("TempLaiNurse.json");
        LaiNurse original = getTypicalLaiNurse();
        JsonLaiNurseStorage jsonLaiNurseStorage = new JsonLaiNurseStorage(filePath);

        // Save in new file and read back
        jsonLaiNurseStorage.saveLaiNurse(original, filePath);
        ReadOnlyLaiNurse readBack = jsonLaiNurseStorage.readLaiNurse(filePath).get();
        assertEquals(original, new LaiNurse(readBack));

        // Modify data, overwrite existing file, and read back
        original.addPatient(HOON);
        original.removePatient(ALICE);
        jsonLaiNurseStorage.saveLaiNurse(original, filePath);
        readBack = jsonLaiNurseStorage.readLaiNurse(filePath).get();
        assertEquals(original, new LaiNurse(readBack));

        // Save and read without specifying file path
        original.addPatient(IDA);
        jsonLaiNurseStorage.saveLaiNurse(original); // file path not specified
        readBack = jsonLaiNurseStorage.readLaiNurse().get(); // file path not specified
        assertEquals(original, new LaiNurse(readBack));

    }

    @Test
    public void saveLaiNurse_nullLaiNurse_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> saveLaiNurse(null, "SomeFile.json"));
    }

    /**
     * Saves {@code laiNurse} at the specified {@code filePath}.
     */
    private void saveLaiNurse(ReadOnlyLaiNurse laiNurse, String filePath) {
        try {
            new JsonLaiNurseStorage(Paths.get(filePath))
                    .saveLaiNurse(laiNurse, addToTestDataPathIfNotNull(filePath));
        } catch (IOException ioe) {
            throw new AssertionError("There should not be an error writing to the file.", ioe);
        }
    }

    @Test
    public void saveLaiNurse_nullFilePath_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> saveLaiNurse(new LaiNurse(), null));
    }

    @Test
    public void constructor_dataFile_backupAndCopiesNextToIt() {
        JsonLaiNurseStorage storage = new JsonLaiNurseStorage(Paths.get("data", "lainurse.json"));
        assertEquals(Paths.get("data", "lainurse.backup.json"), storage.getLaiNurseBackupFilePath());
        assertEquals(Paths.get("data", "lainurse.corrupt.json"), storage.getLaiNurseCorruptFilePath());
        assertEquals(Paths.get("data", "lainurse.previous.json"), storage.getLaiNursePreviousFilePath());
    }

    @Test
    public void withSuffix() {
        // suffix goes before the last extension
        assertEquals(Paths.get("a", "b.c.backup.json"), JsonLaiNurseStorage.withSuffix(Paths.get("a", "b.c.json"),
                "backup"));

        // no extension, or a name that only starts with a dot: suffix goes at the end
        assertEquals(Paths.get("lainurse.backup"), JsonLaiNurseStorage.withSuffix(Paths.get("lainurse"), "backup"));
        assertEquals(Paths.get(".lainurse.backup"), JsonLaiNurseStorage.withSuffix(Paths.get(".lainurse"),
                "backup"));
    }

    @Test
    public void saveLaiNurse_dataFileAndBackup_bothHoldSavedData() throws Exception {
        JsonLaiNurseStorage storage = new JsonLaiNurseStorage(testFolder.resolve("data").resolve("lainurse.json"));
        LaiNurse original = getTypicalLaiNurse();
        storage.saveLaiNurse(original);
        assertEquals(original, new LaiNurse(storage.readLaiNurse().get()));
        assertEquals(original, new LaiNurse(storage.readLaiNurseBackup().get()));

        // the backup follows every save
        original.removePatient(ALICE);
        storage.saveLaiNurse(original);
        assertEquals(original, new LaiNurse(storage.readLaiNurseBackup().get()));
        assertEquals(Files.readString(storage.getLaiNurseFilePath()),
                Files.readString(storage.getLaiNurseBackupFilePath()));
    }

    @Test
    public void saveLaiNurse_withFilePath_backupNotWritten() throws Exception {
        JsonLaiNurseStorage storage = new JsonLaiNurseStorage(testFolder.resolve("lainurse.json"));
        storage.saveLaiNurse(getTypicalLaiNurse(), storage.getLaiNurseFilePath());
        assertFalse(Files.exists(storage.getLaiNurseBackupFilePath()));
    }

    @Test
    public void saveLaiNurse_backupCannotBeWritten_dataFileSavedAndBackupFailedExceptionThrown() throws Exception {
        JsonLaiNurseStorage storage = new JsonLaiNurseStorage(testFolder.resolve("lainurse.json"));
        // a folder where the backup should be cannot be written as a file
        Files.createDirectories(storage.getLaiNurseBackupFilePath());

        LaiNurse original = getTypicalLaiNurse();
        // a distinct exception, so that the caller keeps the change that the data file holds
        assertThrows(BackupFailedException.class, () -> storage.saveLaiNurse(original));
        assertEquals(original, new LaiNurse(storage.readLaiNurse().get()));
    }

    @Test
    public void readLaiNurseBackup_missingBackup_emptyResult() throws Exception {
        assertFalse(new JsonLaiNurseStorage(testFolder.resolve("lainurse.json")).readLaiNurseBackup().isPresent());
    }

    @Test
    public void readLaiNurseBackup_invalidBackup_throwsDataLoadingException() throws Exception {
        JsonLaiNurseStorage storage = new JsonLaiNurseStorage(testFolder.resolve("lainurse.json"));
        Files.copy(TEST_DATA_FOLDER.resolve("invalidPatientLaiNurse.json"), storage.getLaiNurseBackupFilePath());
        assertThrows(DataLoadingException.class, storage::readLaiNurseBackup);
    }

    @Test
    public void keepCorruptLaiNurseFile_earlierCopy_replaced() throws Exception {
        JsonLaiNurseStorage storage = new JsonLaiNurseStorage(testFolder.resolve("lainurse.json"));
        Files.writeString(storage.getLaiNurseCorruptFilePath(), "earlier copy");
        Files.writeString(storage.getLaiNurseFilePath(), "{ not json");

        storage.keepCorruptLaiNurseFile();
        assertEquals("{ not json", Files.readString(storage.getLaiNurseCorruptFilePath()));
        assertEquals("{ not json", Files.readString(storage.getLaiNurseFilePath()));
    }

    @Test
    public void keepCorruptLaiNurseFile_missingDataFile_throwsIoException() {
        JsonLaiNurseStorage storage = new JsonLaiNurseStorage(testFolder.resolve("lainurse.json"));
        assertThrows(IOException.class, storage::keepCorruptLaiNurseFile);
    }

    @Test
    public void keepLaiNurseBackup_earlierCopy_replaced() throws Exception {
        JsonLaiNurseStorage storage = new JsonLaiNurseStorage(testFolder.resolve("lainurse.json"));
        Files.writeString(storage.getLaiNursePreviousFilePath(), "earlier copy");
        Files.writeString(storage.getLaiNurseBackupFilePath(), "the backup");

        storage.keepLaiNurseBackup();
        assertEquals("the backup", Files.readString(storage.getLaiNursePreviousFilePath()));
        assertEquals("the backup", Files.readString(storage.getLaiNurseBackupFilePath()));
    }

    @Test
    public void keepLaiNurseBackup_missingBackup_throwsIoException() {
        JsonLaiNurseStorage storage = new JsonLaiNurseStorage(testFolder.resolve("lainurse.json"));
        assertThrows(IOException.class, storage::keepLaiNurseBackup);
    }
}
