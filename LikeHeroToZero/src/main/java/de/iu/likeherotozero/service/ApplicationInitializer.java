package de.iu.likeherotozero.service;

import de.iu.likeherotozero.configuration.ApplicationSettings;
import de.iu.likeherotozero.dao.UserDAO;
import de.iu.likeherotozero.model.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class ApplicationInitializer {

    static final String DEFAULT_DEMO_USERNAME = "demo";
    static final String DEMO_DISPLAY_NAME = "Demo-Wissenschaftler:in";

    private static final int MAXIMUM_STARTUP_ATTEMPTS = 30;
    private static final long WAIT_BETWEEN_ATTEMPTS_IN_MILLISECONDS = 3000;

    private static final Logger LOGGER = Logger.getLogger(ApplicationInitializer.class.getName());

    @Inject
    private UserDAO userDAO;

    @Inject
    private DataImportService dataImportService;

    private volatile boolean initialized;

    void onApplicationStart(@Observes @Initialized(ApplicationScoped.class) Object startEvent) {
        initializeWithRetries();
    }

    public void ensureInitialized() {
        if (initialized) {
            return;
        }
        synchronized (this) {
            if (initialized) {
                return;
            }
            try {
                initialize();
            } catch (RuntimeException initializationException) {
                LOGGER.log(Level.WARNING, "Datenbank weiterhin nicht erreichbar, Initialisierung wird beim nächsten "
                                          + "Aufruf erneut versucht.", initializationException);
            }
        }
    }

    private synchronized void initializeWithRetries() {
        for (int attempt = 1; attempt <= MAXIMUM_STARTUP_ATTEMPTS; attempt++) {
            try {
                initialize();
                return;
            } catch (RuntimeException initializationException) {
                if (attempt == MAXIMUM_STARTUP_ATTEMPTS) {
                    LOGGER.log(Level.SEVERE, "Datenbank nach " + MAXIMUM_STARTUP_ATTEMPTS + " Versuchen nicht erreichbar. "
                                             + "Sind MySQL und die Einstellungen DB_URL, DB_USER, DB_PASSWORD korrekt? "
                                             + "Die Initialisierung wird beim ersten Seitenaufruf erneut versucht.",
                            initializationException);
                    return;
                }
                LOGGER.warning("Datenbank noch nicht erreichbar (Versuch " + attempt + " von " + MAXIMUM_STARTUP_ATTEMPTS
                               + "), neuer Versuch in " + (WAIT_BETWEEN_ATTEMPTS_IN_MILLISECONDS / 1000) + " Sekunden.");
                if (!waitBeforeNextAttempt()) {
                    return;
                }
            }
        }
    }

    private boolean waitBeforeNextAttempt() {
        try {
            Thread.sleep(WAIT_BETWEEN_ATTEMPTS_IN_MILLISECONDS);
            return true;
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            LOGGER.warning("Warten auf die Datenbank wurde unterbrochen.");
            return false;
        }
    }

    private void initialize() {
        synchronizeDemoAccount();
        dataImportService.importIfEmpty();
        initialized = true;
        LOGGER.info("Initialisierung abgeschlossen.");
    }

    void synchronizeDemoAccount() {
        String username = ApplicationSettings.read("DEMO_USER", DEFAULT_DEMO_USERNAME);
        String password = ApplicationSettings.read("DEMO_PASSWORD", null);
        if (password == null) {
            LOGGER.severe("DEMO_PASSWORD ist weder als Umgebungsvariable noch in der .env-Datei gesetzt. "
                          + "Das Demo-Konto '" + username + "' wird nicht angelegt oder aktualisiert.");
            return;
        }

        Optional<User> existingUser = userDAO.findByUsername(username);
        if (existingUser.isEmpty()) {
            userDAO.create(new User(username, PasswordHasher.hash(password), DEMO_DISPLAY_NAME));
            LOGGER.info("Demo-Konto '" + username + "' mit Passwort aus DEMO_PASSWORD angelegt.");
            return;
        }

        User demoUser = existingUser.get();
        if (PasswordHasher.verify(password, demoUser.getPasswordHash())) {
            return;
        }

        demoUser.setPasswordHash(PasswordHasher.hash(password));
        userDAO.update(demoUser);
        LOGGER.info("Passwort des Demo-Kontos '" + username + "' an DEMO_PASSWORD angeglichen.");
    }
}
