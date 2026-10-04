package de.iu.likeherotozero.controller;

import de.iu.likeherotozero.model.User;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

import java.io.Serializable;

@Named
@SessionScoped
public class UserSession implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private String displayName;

    public UserSession() {
    }

    void login(User user) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.displayName = user.getDisplayName();
    }

    public String logout() {
        userId = null;
        username = null;
        displayName = null;
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        externalContext.invalidateSession();
        return "/index?faces-redirect=true";
    }

    public boolean isLoggedIn() {
        return userId != null;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }
}
