package de.iu.likeherotozero.dao;

import de.iu.likeherotozero.configuration.ApplicationSettings;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@ApplicationScoped
public class EntityManagerProvider {

    public static final String PERSISTENCE_UNIT_NAME = "likeherotozero";

    static final String DEFAULT_DATABASE_URL =
            "jdbc:mysql://localhost:3306/likeherotozero?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
    static final String DEFAULT_DATABASE_USER = "likeherotozero";

    private static final Logger LOGGER = Logger.getLogger(EntityManagerProvider.class.getName());

    private EntityManagerFactory entityManagerFactory;

    public EntityManagerProvider() {
    }

    EntityManagerProvider(Map<String, Object> persistenceProperties) {
        this.entityManagerFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME, persistenceProperties);
    }

    @PostConstruct
    void initialize() {
        String databaseUrl = ApplicationSettings.read("DB_URL", DEFAULT_DATABASE_URL);
        String databaseUser = ApplicationSettings.read("DB_USER", DEFAULT_DATABASE_USER);
        String databasePassword = ApplicationSettings.read("DB_PASSWORD", "");

        Map<String, Object> persistenceProperties = new HashMap<>();
        persistenceProperties.put("jakarta.persistence.jdbc.url", databaseUrl);
        persistenceProperties.put("jakarta.persistence.jdbc.user", databaseUser);
        persistenceProperties.put("jakarta.persistence.jdbc.password", databasePassword);

        LOGGER.info("Verbinde mit Datenbank " + databaseUrl + " als Benutzer " + databaseUser);
        entityManagerFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME, persistenceProperties);
    }

    @PreDestroy
    void close() {
        if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
            entityManagerFactory.close();
        }
    }

    public EntityManager createEntityManager() {
        return entityManagerFactory.createEntityManager();
    }
}
