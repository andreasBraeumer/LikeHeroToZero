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

    private static final Logger LOGGER = Logger.getLogger(ApplicationInitializer.class.getName());

    @Inject
    private UserDAO userDAO;

    @Inject
    private DataImportService dataImportService;

    void onApplicationStart(@Observes @Initialized(ApplicationScoped.class) Object startEvent) {
        try {
            synchronizeDemoAccount();
            dataImportService.importIfEmpty();
        } catch (RuntimeException initializationException) {
            LOGGER.log(Level.SEVERE, "Initialisierung der Datenbank fehlgeschlagen. Sind MySQL und die "
                                     + "Einstellungen DB_URL, DB_USER, DB_PASSWORD korrekt eingerichtet?",
                    initializationException);
        }
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
