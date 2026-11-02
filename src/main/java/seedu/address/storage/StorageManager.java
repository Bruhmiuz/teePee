package seedu.address.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;

import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.model.ReadOnlyLaiNurse;
import seedu.address.model.ReadOnlyUserPrefs;
import seedu.address.model.UserPrefs;

/**
 * Manages storage of LaiNurse data in local storage.
 */
public class StorageManager implements Storage {

    private static final Logger logger = LogsCenter.getLogger(StorageManager.class);
    private JsonLaiNurseStorage laiNurseStorage;
    private JsonUserPrefsStorage userPrefsStorage;

    /**
     * Creates a {@code StorageManager} with the given LaiNurse storage and user prefs storage.
     */
    public StorageManager(JsonLaiNurseStorage laiNurseStorage, JsonUserPrefsStorage userPrefsStorage) {
        this.laiNurseStorage = laiNurseStorage;
        this.userPrefsStorage = userPrefsStorage;
    }

    // ================ UserPrefs methods ==============================

    @Override
    public Path getUserPrefsFilePath() {
        return userPrefsStorage.getUserPrefsFilePath();
    }

    @Override
    public Optional<UserPrefs> readUserPrefs() throws DataLoadingException {
        return userPrefsStorage.readUserPrefs();
    }

    @Override
    public void saveUserPrefs(ReadOnlyUserPrefs userPrefs) throws IOException {
        userPrefsStorage.saveUserPrefs(userPrefs);
    }


    // ================ LaiNurse methods ==============================

    @Override
    public Path getLaiNurseFilePath() {
        return laiNurseStorage.getLaiNurseFilePath();
    }

    @Override
    public Path getLaiNurseBackupFilePath() {
        return laiNurseStorage.getLaiNurseBackupFilePath();
    }

    @Override
    public Path getLaiNurseCorruptFilePath() {
        return laiNurseStorage.getLaiNurseCorruptFilePath();
    }

    @Override
    public Path getLaiNursePreviousFilePath() {
        return laiNurseStorage.getLaiNursePreviousFilePath();
    }

    @Override
    public Optional<ReadOnlyLaiNurse> readLaiNurse() throws DataLoadingException {
        logger.fine("Attempting to read data from file: " + laiNurseStorage.getLaiNurseFilePath());
        return laiNurseStorage.readLaiNurse();
    }

    @Override
    public void saveLaiNurse(ReadOnlyLaiNurse laiNurse) throws IOException {
        logger.fine("Attempting to write to data file: " + laiNurseStorage.getLaiNurseFilePath());
        laiNurseStorage.saveLaiNurse(laiNurse);
    }

    @Override
    public Optional<ReadOnlyLaiNurse> readLaiNurseBackup() throws DataLoadingException {
        logger.fine("Attempting to read data from backup file: " + laiNurseStorage.getLaiNurseBackupFilePath());
        return laiNurseStorage.readLaiNurseBackup();
    }

    @Override
    public void keepCorruptLaiNurseFile() throws IOException {
        logger.fine("Attempting to copy data file to: " + laiNurseStorage.getLaiNurseCorruptFilePath());
        laiNurseStorage.keepCorruptLaiNurseFile();
    }

    @Override
    public void keepLaiNurseBackup() throws IOException {
        logger.fine("Attempting to copy backup file to: " + laiNurseStorage.getLaiNursePreviousFilePath());
        laiNurseStorage.keepLaiNurseBackup();
    }

}
