package de.iu.likeherotozero.controller;

import de.iu.likeherotozero.configuration.ApplicationSettings;
import de.iu.likeherotozero.dao.UserDAO;
import de.iu.likeherotozero.model.User;
import de.iu.likeherotozero.service.PasswordHasher;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.ValidatorException;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.PersistenceException;
import jakarta.servlet.http.HttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Named
@RequestScoped
public class RegistrationController {

    static final String REGISTRATION_CODE_SETTING = "REGISTRATION_CODE";
    static final String ALLOWED_USERNAME_PATTERN = "[A-Za-z0-9._-]+";

    private static final String USERNAME_FIELD_ID = "registrationForm:username";
    private static final String PASSWORD_CONFIRMATION_FIELD_ID = "registrationForm:passwordConfirmation";
    private static final String REGISTRATION_CODE_FIELD_ID = "registrationForm:registrationCode";

    @Inject
    private UserDAO userDAO;

    @Inject
    private UserSession userSession;

    private String username;
    private String displayName;
    private String password;
    private String passwordConfirmation;
    private String registrationCode;

    public boolean isRegistrationEnabled() {
        return readConfiguredRegistrationCode() != null;
    }

    public void validateUsername(FacesContext facesContext, UIComponent component, Object value) {
        if (value == null) {
            return;
        }
        String enteredUsername = value.toString().trim();
        if (!enteredUsername.matches(ALLOWED_USERNAME_PATTERN)) {
            throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Benutzername: Bitte nur Buchstaben ohne Umlaute, Ziffern, Punkt, Bindestrich und Unterstrich verwenden.",
                    null));
        }
        if (userDAO.findByUsername(enteredUsername).isPresent()) {
            throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Benutzername: Dieser Name ist bereits vergeben.", null));
        }
    }

    public String register() {
        FacesContext facesContext = FacesContext.getCurrentInstance();

        String configuredRegistrationCode = readConfiguredRegistrationCode();
        if (!isRegistrationCodeValid(registrationCode, configuredRegistrationCode)) {
            facesContext.addMessage(REGISTRATION_CODE_FIELD_ID, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Registrierungscode: Der Code ist nicht gültig.", null));
            clearPasswords();
            return null;
        }

        if (!password.equals(passwordConfirmation)) {
            facesContext.addMessage(PASSWORD_CONFIRMATION_FIELD_ID, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Passwort wiederholen: Die Passwörter stimmen nicht überein.", null));
            clearPasswords();
            return null;
        }

        User newUser = new User(username.trim(), PasswordHasher.hash(password), displayName.trim());
        try {
            userDAO.create(newUser);
        } catch (PersistenceException uniqueConstraintViolation) {
            facesContext.addMessage(USERNAME_FIELD_ID, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Benutzername: Dieser Name ist bereits vergeben.", null));
            clearPasswords();
            return null;
        }

        HttpServletRequest request = (HttpServletRequest) facesContext.getExternalContext().getRequest();
        request.getSession(true);
        request.changeSessionId();
        userSession.login(newUser);

        facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Registrierung erfolgreich", "Willkommen, " + newUser.getDisplayName() + "! Sie sind jetzt angemeldet."));
        facesContext.getExternalContext().getFlash().setKeepMessages(true);
        return "/backend/emissions?faces-redirect=true";
    }

    static boolean isRegistrationCodeValid(String enteredCode, String configuredCode) {
        if (enteredCode == null || configuredCode == null) {
            return false;
        }
        byte[] enteredBytes = enteredCode.trim().getBytes(StandardCharsets.UTF_8);
        byte[] configuredBytes = configuredCode.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(enteredBytes, configuredBytes);
    }

    private static String readConfiguredRegistrationCode() {
        return ApplicationSettings.read(REGISTRATION_CODE_SETTING, null);
    }

    private void clearPasswords() {
        password = null;
        passwordConfirmation = null;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPasswordConfirmation() {
        return passwordConfirmation;
    }

    public void setPasswordConfirmation(String passwordConfirmation) {
        this.passwordConfirmation = passwordConfirmation;
    }

    public String getRegistrationCode() {
        return registrationCode;
    }

    public void setRegistrationCode(String registrationCode) {
        this.registrationCode = registrationCode;
    }
}
