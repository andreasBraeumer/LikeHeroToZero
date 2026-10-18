package de.iu.likeherotozero.dao;

import de.iu.likeherotozero.model.User;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserDAOTest {

    private EntityManagerProvider entityManagerProvider;
    private UserDAO userDAO;

    @BeforeEach
    void setUp() {
        entityManagerProvider = new EntityManagerProvider(Map.of(
                "jakarta.persistence.jdbc.driver", "org.h2.Driver",
                "jakarta.persistence.jdbc.url",
                "jdbc:h2:mem:likeherotozero-users;MODE=MySQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=YEAR,VALUE",
                "jakarta.persistence.jdbc.user", "sa",
                "jakarta.persistence.jdbc.password", "",
                "hibernate.hbm2ddl.auto", "create-drop",
                "hibernate.show_sql", "false"));
        userDAO = new UserDAO(entityManagerProvider);
    }

    @AfterEach
    void tearDown() {
        entityManagerProvider.close();
    }

    @Test
    void createdUserCanBeFoundByUsername() {
        User newUser = new User("mmuster", "pbkdf2$1$c2FsdA==$aGFzaA==", "Dr. Maria Muster");

        userDAO.create(newUser);

        assertNotNull(newUser.getId());
        Optional<User> foundUser = userDAO.findByUsername("mmuster");
        assertTrue(foundUser.isPresent());
        assertEquals("Dr. Maria Muster", foundUser.get().getDisplayName());
    }

    @Test
    void duplicateUsernameIsRejectedByTheDatabase() {
        userDAO.create(new User("mmuster", "pbkdf2$1$c2FsdA==$aGFzaA==", "Erstes Konto"));

        assertThrows(PersistenceException.class,
                () -> userDAO.create(new User("mmuster", "pbkdf2$1$c2FsdA==$aGFzaA==", "Zweites Konto")));
    }
}
