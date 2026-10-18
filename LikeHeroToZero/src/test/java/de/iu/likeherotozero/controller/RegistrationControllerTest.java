package de.iu.likeherotozero.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistrationControllerTest {

    @Test
    void matchingRegistrationCodeIsAccepted() {
        assertTrue(RegistrationController.isRegistrationCodeValid("klima-2026", "klima-2026"));
        assertTrue(RegistrationController.isRegistrationCodeValid("  klima-2026 ", "klima-2026"));
    }

    @Test
    void wrongRegistrationCodeIsRejected() {
        assertFalse(RegistrationController.isRegistrationCodeValid("Klima-2026", "klima-2026"));
        assertFalse(RegistrationController.isRegistrationCodeValid("klima", "klima-2026"));
        assertFalse(RegistrationController.isRegistrationCodeValid("", "klima-2026"));
        assertFalse(RegistrationController.isRegistrationCodeValid(null, "klima-2026"));
    }

    @Test
    void registrationIsClosedWithoutConfiguredCode() {
        assertFalse(RegistrationController.isRegistrationCodeValid("klima-2026", null));
        assertFalse(RegistrationController.isRegistrationCodeValid(null, null));
    }
}
