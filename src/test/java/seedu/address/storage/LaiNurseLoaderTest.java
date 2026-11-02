package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPatients.ALICE;
import static seedu.address.testutil.TypicalPatients.getTypicalLaiNurse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.model.LaiNurse;
import seedu.address.model.util.SampleDataUtil;

public class LaiNurseLoaderTest {
    private static final Path TEST_DATA_FOLDER = Paths.get("src", "test", "data", "JsonLaiNurseStorageTest");
    private static final String NOT_JSON = "{ not json";

    @TempDir
    public Path testFolder;

    private JsonLaiNurseStorage laiNurseStorage;
    private StorageManager storage;

    @BeforeEach
    public void setUp() {
        laiNurseStorage = new JsonLaiNurseStorage(testFolder.resolve("data").resolve("lainurse.json"));
        storage = new StorageManager(laiNurseStorage, new JsonUserPrefsStorage(testFolder.resolve("prefs.json")));
    }

    @Test
    public void load_nullStorage_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> LaiNurseLoader.load(null));
    }

    @Test
    public void load_validDataFile_dataFileLoadedWithoutMessage() throws Exception {
        LaiNurse saved = getTypicalLaiNurse();
        storage.saveLaiNurse(saved);

        // a different backup shows that the data file is the one loaded
        LaiNurse backup = getTypicalLaiNurse();
        backup.removePatient(ALICE);
        laiNurseStorage.saveLaiNurse(backup, storage.getLaiNurseBackupFilePath());

        LaiNurseLoader.Result result = LaiNurseLoader.load(storage);
        assertEquals(saved, new LaiNurse(result.data()));
        assertEquals(Optional.empty(), result.message());
        assertFalse(Files.exists(storage.getLaiNurseCorruptFilePath()));
    }

    @Test
    public void load_missingDataFileNoBackup_sampleDataLoadedWithoutMessage() throws Exception {
        LaiNurseLoader.Result result = LaiNurseLoader.load(storage);
        assertEquals(new LaiNurse(SampleDataUtil.getSampleLaiNurse()), new LaiNurse(result.data()));
        assertEquals(Optional.empty(), result.message());
        assertFalse(Files.exists(storage.getLaiNursePreviousFilePath()));
    }

    @Test
    public void load_missingDataFileWithBackup_sampleDataLoadedAndBackupKept() throws Exception {
        LaiNurse saved = getTypicalLaiNurse();
        storage.saveLaiNurse(saved);
        Files.delete(storage.getLaiNurseFilePath());

        LaiNurseLoader.Result result = LaiNurseLoader.load(storage);
        assertEquals(new LaiNurse(SampleDataUtil.getSampleLaiNurse()), new LaiNurse(result.data()));
        assertEquals(Optional.of(String.format(LaiNurseLoader.MESSAGE_SAMPLE_DATA_BACKUP_KEPT,
                storage.getLaiNurseFilePath(), storage.getLaiNursePreviousFilePath())), result.message());
        assertFalse(Files.exists(storage.getLaiNurseCorruptFilePath()));

        // the next save replaces the backup with the sample data, but the copy keeps the data
        storage.saveLaiNurse(result.data());
        assertEquals(new LaiNurse(SampleDataUtil.getSampleLaiNurse()),
                new LaiNurse(storage.readLaiNurseBackup().orElseThrow()));
        Files.copy(storage.getLaiNursePreviousFilePath(), storage.getLaiNurseFilePath(),
                StandardCopyOption.REPLACE_EXISTING);
        assertEquals(saved, new LaiNurse(LaiNurseLoader.load(storage).data()));
    }

    @Test
    public void load_backupCopyFails_userTold() throws Exception {
        // Inject a JsonLaiNurseStorage that cannot copy the backup
        JsonLaiNurseStorage failingStorage = new JsonLaiNurseStorage(laiNurseStorage.getLaiNurseFilePath()) {
            @Override
            public void keepLaiNurseBackup() throws IOException {
                throw new IOException("dummy IO exception");
            }
        };
        storage = new StorageManager(failingStorage, new JsonUserPrefsStorage(testFolder.resolve("prefs.json")));
        storage.saveLaiNurse(getTypicalLaiNurse());
        Files.delete(storage.getLaiNurseFilePath());

        LaiNurseLoader.Result result = LaiNurseLoader.load(storage);
        assertEquals(new LaiNurse(SampleDataUtil.getSampleLaiNurse()), new LaiNurse(result.data()));
        assertEquals(Optional.of(String.format(LaiNurseLoader.MESSAGE_SAMPLE_DATA_BACKUP_NOT_KEPT,
                storage.getLaiNurseFilePath(), storage.getLaiNursePreviousFilePath(),
                storage.getLaiNurseBackupFilePath())), result.message());
    }

    @Test
    public void load_invalidDataFileValidBackup_backupLoaded() throws Exception {
        LaiNurse saved = getTypicalLaiNurse();
        storage.saveLaiNurse(saved);
        Files.copy(TEST_DATA_FOLDER.resolve("invalidPatientLaiNurse.json"), storage.getLaiNurseFilePath(),
                StandardCopyOption.REPLACE_EXISTING);
        String invalidContent = Files.readString(storage.getLaiNurseFilePath());

        LaiNurseLoader.Result result = LaiNurseLoader.load(storage);
        assertEquals(saved, new LaiNurse(result.data()));
        assertEquals(Optional.of(String.format(LaiNurseLoader.MESSAGE_BACKUP_LOADED, storage.getLaiNurseFilePath(),
                storage.getLaiNurseBackupFilePath()) + "\n" + getCorruptFileKeptMessage()), result.message());
        assertEquals(invalidContent, Files.readString(storage.getLaiNurseCorruptFilePath()));
    }

    @Test
    public void load_nullPatientInDataFile_backupLoaded() throws Exception {
        LaiNurse saved = getTypicalLaiNurse();
        storage.saveLaiNurse(saved);
        String nullPatient = "{ \"patients\" : [ null ] }";
        Files.writeString(storage.getLaiNurseFilePath(), nullPatient);

        LaiNurseLoader.Result result = LaiNurseLoader.load(storage);
        assertEquals(saved, new LaiNurse(result.data()));
        assertEquals(nullPatient, Files.readString(storage.getLaiNurseCorruptFilePath()));
    }

    @Test
    public void load_invalidDataFileMissingBackup_emptyDataLoaded() throws Exception {
        Files.createDirectories(storage.getLaiNurseFilePath().getParent());
        Files.writeString(storage.getLaiNurseFilePath(), NOT_JSON);

        LaiNurseLoader.Result result = LaiNurseLoader.load(storage);
        assertEquals(new LaiNurse(), new LaiNurse(result.data()));
        assertEquals(Optional.of(String.format(LaiNurseLoader.MESSAGE_BACKUP_MISSING, storage.getLaiNurseFilePath(),
                storage.getLaiNurseBackupFilePath()) + "\n" + getCorruptFileKeptMessage()), result.message());
        assertEquals(NOT_JSON, Files.readString(storage.getLaiNurseCorruptFilePath()));
    }

    @Test
    public void load_invalidDataFileInvalidBackup_emptyDataLoaded() throws Exception {
        Files.createDirectories(storage.getLaiNurseFilePath().getParent());
        Files.writeString(storage.getLaiNurseFilePath(), NOT_JSON);
        Files.copy(TEST_DATA_FOLDER.resolve("invalidPatientLaiNurse.json"), storage.getLaiNurseBackupFilePath());

        LaiNurseLoader.Result result = LaiNurseLoader.load(storage);
        assertEquals(new LaiNurse(), new LaiNurse(result.data()));
        assertEquals(Optional.of(String.format(LaiNurseLoader.MESSAGE_BACKUP_NOT_LOADED,
                storage.getLaiNurseFilePath(), storage.getLaiNurseBackupFilePath()) + "\n"
                + getCorruptFileKeptMessage()), result.message());
        assertEquals(NOT_JSON, Files.readString(storage.getLaiNurseCorruptFilePath()));
    }

    @Test
    public void load_corruptCopyFails_userTold() throws Exception {
        // Inject a JsonLaiNurseStorage that cannot copy the data file
        JsonLaiNurseStorage failingStorage = new JsonLaiNurseStorage(laiNurseStorage.getLaiNurseFilePath()) {
            @Override
            public void keepCorruptLaiNurseFile() throws IOException {
                throw new IOException("dummy IO exception");
            }
        };
        storage = new StorageManager(failingStorage, new JsonUserPrefsStorage(testFolder.resolve("prefs.json")));
        LaiNurse saved = getTypicalLaiNurse();
        storage.saveLaiNurse(saved);
        Files.writeString(storage.getLaiNurseFilePath(), NOT_JSON);

        LaiNurseLoader.Result result = LaiNurseLoader.load(storage);
        assertEquals(saved, new LaiNurse(result.data()));
        assertEquals(Optional.of(String.format(LaiNurseLoader.MESSAGE_BACKUP_LOADED, storage.getLaiNurseFilePath(),
                storage.getLaiNurseBackupFilePath()) + "\n" + String.format(
                LaiNurseLoader.MESSAGE_CORRUPT_FILE_NOT_KEPT, storage.getLaiNurseCorruptFilePath())),
                result.message());
    }

    @Test
    public void messages_dataAtRisk_warnThatExitAlsoSaves() {
        // exit is a command that succeeds, so it saves too: only the window's close button leaves the files alone
        for (String message : List.of(LaiNurseLoader.MESSAGE_BACKUP_NOT_LOADED, LaiNurseLoader.MESSAGE_BACKUP_MISSING,
                LaiNurseLoader.MESSAGE_CORRUPT_FILE_NOT_KEPT, LaiNurseLoader.MESSAGE_SAMPLE_DATA_BACKUP_NOT_KEPT)) {
            assertTrue(message.contains("even exit"), message);
            assertTrue(message.contains("close button"), message);
        }
        assertTrue(LaiNurseLoader.MESSAGE_CORRUPT_FILE_KEPT.contains("close button"));
    }

    private String getCorruptFileKeptMessage() {
        return String.format(LaiNurseLoader.MESSAGE_CORRUPT_FILE_KEPT, storage.getLaiNurseCorruptFilePath(),
                storage.getLaiNurseFilePath());
    }
}
