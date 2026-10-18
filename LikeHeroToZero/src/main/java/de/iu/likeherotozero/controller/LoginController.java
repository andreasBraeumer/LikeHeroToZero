package de.iu.likeherotozero.controller;

import de.iu.likeherotozero.dao.UserDAO;
import de.iu.likeherotozero.model.User;
import de.iu.likeherotozero.service.ApplicationInitializer;
import de.iu.likeherotozero.service.PasswordHasher;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

@Named
@RequestScoped
public class LoginController {

    @Inject
    private UserDAO userDAO;

    @Inject
    private UserSession userSession;

    @Inject
    private ApplicationInitializer applicationInitializer;

    private String username;
    private String password;

    public String login() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        applicationInitializer.ensureInitialized();

        String trimmedUsername = null;
        if (username != null) {
            trimmedUsername = username.trim();
        }

        Optional<User> foundUser = userDAO.findByUsername(trimmedUsername);
        String storedPasswordHash = null;
        if (foundUser.isPresent()) {
            storedPasswordHash = foundUser.get().getPasswordHash();
        }

        if (!PasswordHasher.verify(password, storedPasswordHash)) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Anmeldung fehlgeschlagen", "Benutzername oder Passwort ist falsch."));
            password = null;
            return null;
        }

        HttpServletRequest request = (HttpServletRequest) facesContext.getExternalContext().getRequest();
        request.getSession(true);
        request.changeSessionId();
        userSession.login(foundUser.get());
        return "/backend/emissions?faces-redirect=true";
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
