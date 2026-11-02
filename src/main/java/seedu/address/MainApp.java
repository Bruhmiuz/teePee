package seedu.address;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.stage.Stage;
import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.commons.util.StringUtil;
import seedu.address.logic.Logic;
import seedu.address.logic.LogicManager;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyUserPrefs;
import seedu.address.model.UserPrefs;
import seedu.address.storage.JsonLaiNurseStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.LaiNurseLoader;
import seedu.address.storage.Storage;
import seedu.address.storage.StorageManager;
import seedu.address.ui.Ui;
import seedu.address.ui.UiManager;

/**
 * Runs the application.
 */
public class MainApp extends Application {

    public static final String VERSION = "V1.6";

    private static final Logger logger = LogsCenter.getLogger(MainApp.class);
    private static final Path USER_PREFS_FILE_PATH = Paths.get("preferences.json");
    private static final Path LAI_NURSE_FILE_PATH = Paths.get("data", "lainurse.json");

    protected Ui ui;
    protected Logic logic;
    protected Storage storage;
    protected Model model;

    /** The message to show the user when the window opens, if the data did not come from the data file. */
    private Optional<String> startupMessage = Optional.empty();

    @Override
    public void init() throws Exception {
        logger.info("=============================[ Initializing LaiNurse ]===========================");
        super.init();

        JsonUserPrefsStorage userPrefsStorage = new JsonUserPrefsStorage(USER_PREFS_FILE_PATH);
        UserPrefs userPrefs = initPrefs(userPrefsStorage);
        JsonLaiNurseStorage laiNurseStorage = new JsonLaiNurseStorage(LAI_NURSE_FILE_PATH);
        storage = new StorageManager(laiNurseStorage, userPrefsStorage);

        model = initModelManager(storage, userPrefs);

        logic = new LogicManager(model, storage);

        ui = new UiManager(logic, storage.getLaiNurseFilePath(), startupMessage);
    }

    /**
     * Returns a {@code ModelManager} with the data from {@code storage}'s data file and {@code userPrefs}. <br>
     * The sample data will be used instead if {@code storage}'s data file is not found,
     * or the data from its backup will be used instead if errors occur when reading {@code storage}'s data file,
     * as {@link LaiNurseLoader#load(Storage)} describes.
     * Any message about where the data came from is kept in {@link #startupMessage}.
     */
    private Model initModelManager(Storage storage, ReadOnlyUserPrefs userPrefs) {
        LaiNurseLoader.Result loaded = LaiNurseLoader.load(storage);
        startupMessage = loaded.message();
        return new ModelManager(loaded.data(), userPrefs);
    }

    /**
     * Returns a {@code UserPrefs} using the file at {@code storage}'s user prefs file path,
     * or a new {@code UserPrefs} with default configuration if errors occur when
     * reading from the file.
     */
    protected UserPrefs initPrefs(JsonUserPrefsStorage storage) {
        Path prefsFilePath = storage.getUserPrefsFilePath();
        logger.info("Using preference file : " + prefsFilePath);

        UserPrefs initializedPrefs;
        try {
            Optional<UserPrefs> prefsOptional = storage.readUserPrefs();
            if (prefsOptional.isEmpty()) {
                logger.info("Creating new preference file " + prefsFilePath);
            }
            initializedPrefs = prefsOptional.orElse(new UserPrefs());
        } catch (DataLoadingException e) {
            logger.warning("Preference file at " + prefsFilePath + " could not be loaded."
                    + " Using default preferences.");
            initializedPrefs = new UserPrefs();
        }

        //Update prefs file in case it was missing to begin with or there are new/unused fields
        try {
            storage.saveUserPrefs(initializedPrefs);
        } catch (IOException e) {
            logger.warning("Failed to save preference file : " + StringUtil.getDetails(e));
        }

        return initializedPrefs;
    }

    @Override
    public void start(Stage primaryStage) {
        logger.info("Starting LaiNurse " + MainApp.VERSION);
        ui.start(primaryStage);
    }

    @Override
    public void stop() {
        logger.info("============================ [ Stopping LaiNurse ] =============================");
        try {
            storage.saveUserPrefs(model.getUserPrefs());
        } catch (IOException e) {
            logger.severe("Failed to save preferences " + StringUtil.getDetails(e));
        }
    }
}
