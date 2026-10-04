package de.iu.likeherotozero.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    @Test
    void correctPasswordIsAccepted() {
        String passwordHash = PasswordHasher.hash("geheim123");

        assertTrue(PasswordHasher.verify("geheim123", passwordHash));
        assertFalse(passwordHash.contains("geheim123"));
    }

    @Test
    void wrongPasswordIsRejected() {
        String passwordHash = PasswordHasher.hash("geheim123");

        assertFalse(PasswordHasher.verify("Geheim123", passwordHash));
        assertFalse(PasswordHasher.verify("", passwordHash));
        assertFalse(PasswordHasher.verify(null, passwordHash));
    }

    @Test
    void samePasswordGetsDifferentSalt() {
        assertNotEquals(PasswordHasher.hash("abc"), PasswordHasher.hash("abc"));
    }

    @Test
    void unknownUserOrBrokenHashIsRejected() {
        assertFalse(PasswordHasher.verify("placeholder-password-for-unknown-users", null));
        assertFalse(PasswordHasher.verify("x", "kein-gueltiger-hash"));
        assertFalse(PasswordHasher.verify("x", "pbkdf2$abc$def$ghi"));
    }
}
