package de.iu.likeherotozero.controller;

import de.iu.likeherotozero.dao.CountryDAO;
import de.iu.likeherotozero.dao.EmissionRecordDAO;
import de.iu.likeherotozero.dao.UserDAO;
import de.iu.likeherotozero.model.Country;
import de.iu.likeherotozero.model.EmissionRecord;
import de.iu.likeherotozero.model.RecordSource;
import de.iu.likeherotozero.model.User;
import de.iu.likeherotozero.service.DataImportService;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.ValidatorException;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.PersistenceException;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Named
@ViewScoped
public class BackendEmissionController implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int MINIMUM_YEAR = 1750;

    private static final String COUNTRY_FIELD_ID = "emissionForm:country";
    private static final String YEAR_FIELD_ID = "emissionForm:year";
    private static final Logger LOGGER = Logger.getLogger(BackendEmissionController.class.getName());

    @Inject
    private EmissionRecordDAO emissionRecordDAO;

    @Inject
    private CountryDAO countryDAO;

    @Inject
    private UserDAO userDAO;

    @Inject
    private UserSession userSession;

    @Inject
    private DataImportService dataImportService;

    private List<Country> countries = Collections.emptyList();

    private String filterCountryCode;
    private List<EmissionRecord> emissionRecords = Collections.emptyList();

    private Long recordId;
    private EmissionRecord emissionRecord;
    private String countryCode;
    private Long duplicateRecordId;

    public void initializeList() {
        countries = countryDAO.findAll();
        loadEmissionRecords();
    }

    public void loadEmissionRecords() {
        emissionRecords = emissionRecordDAO.findAll(filterCountryCode);
    }

    public void runImport() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        try {
            DataImportService.ImportResult importResult = dataImportService.importFromApi();
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Import abgeschlossen", importResult.summary() + "."));
            countries = countryDAO.findAll();
            loadEmissionRecords();
        } catch (IOException connectionException) {
            LOGGER.log(Level.WARNING, "Manueller Import fehlgeschlagen", connectionException);
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Import fehlgeschlagen", "Die World-Bank-API ist nicht erreichbar: " + connectionException.getMessage()));
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Import abgebrochen", "Der Import wurde unterbrochen."));
        } catch (RuntimeException unexpectedException) {
            LOGGER.log(Level.SEVERE, "Manueller Import fehlgeschlagen", unexpectedException);
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Import fehlgeschlagen", "Beim Speichern der Importdaten ist ein Fehler aufgetreten."));
        }
    }

    public String initializeForm() {
        countries = countryDAO.findAll();

        if (recordId == null) {
            emissionRecord = new EmissionRecord();
            countryCode = filterCountryCode;
            return null;
        }

        Optional<EmissionRecord> foundRecord = emissionRecordDAO.findById(recordId);
        if (foundRecord.isEmpty()) {
            addFlashMessage(FacesMessage.SEVERITY_ERROR, "Datensatz nicht gefunden",
                    "Zur ID " + recordId + " gibt es keinen Datensatz.");
            return "emissions?faces-redirect=true";
        }

        emissionRecord = foundRecord.get();
        countryCode = emissionRecord.getCountry().getIsoCode();
        return null;
    }

    public void validateYear(FacesContext facesContext, UIComponent component, Object value) {
        if (value == null) {
            return;
        }
        int year = ((Number) value).intValue();
        if (year < MINIMUM_YEAR || year > getCurrentYear()) {
            String message = "Jahr: Bitte ein Jahr zwischen " + MINIMUM_YEAR + " und " + getCurrentYear() + " angeben.";
            throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR, message, null));
        }
    }

    public String save() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        duplicateRecordId = null;

        Optional<Country> selectedCountry = countryDAO.findByIsoCode(countryCode);
        if (selectedCountry.isEmpty()) {
            facesContext.addMessage(COUNTRY_FIELD_ID, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Land: Bitte ein Land aus der Liste wählen.", null));
            return null;
        }
        Country country = selectedCountry.get();

        Optional<Long> existingRecordId = emissionRecordDAO.findIdByCountryAndYear(countryCode, emissionRecord.getYear());
        if (isOtherRecord(existingRecordId)) {
            reportDuplicate(facesContext, country, existingRecordId.get());
            return null;
        }

        boolean isNewRecord = emissionRecord.getId() == null;
        Optional<User> currentUser = userDAO.findById(userSession.getUserId());

        emissionRecord.setCountry(country);
        emissionRecord.setSource(RecordSource.RESEARCH);
        emissionRecord.setChangedAt(LocalDateTime.now());
        if (currentUser.isPresent()) {
            emissionRecord.setChangedBy(currentUser.get());
        } else {
            emissionRecord.setChangedBy(null);
        }

        try {
            if (isNewRecord) {
                emissionRecordDAO.create(emissionRecord);
            } else {
                emissionRecordDAO.update(emissionRecord);
            }
        } catch (PersistenceException persistenceException) {
            Optional<Long> concurrentRecordId = emissionRecordDAO.findIdByCountryAndYear(countryCode,
                    emissionRecord.getYear());
            if (!isOtherRecord(concurrentRecordId)) {
                throw persistenceException;
            }
            if (isNewRecord) {
                emissionRecord.setId(null);
            }
            reportDuplicate(facesContext, country, concurrentRecordId.get());
            return null;
        }

        String successMessage;
        if (isNewRecord) {
            successMessage = "Neuer Datensatz für " + country.getName() + " " + emissionRecord.getYear() + " angelegt.";
        } else {
            successMessage = "Datensatz für " + country.getName() + " " + emissionRecord.getYear() + " geändert.";
        }
        addFlashMessage(FacesMessage.SEVERITY_INFO, "Gespeichert", successMessage);
        return "emissions?faces-redirect=true&land=" + countryCode;
    }

    private boolean isOtherRecord(Optional<Long> foundRecordId) {
        if (foundRecordId.isEmpty()) {
            return false;
        }
        return !foundRecordId.get().equals(emissionRecord.getId());
    }

    private void reportDuplicate(FacesContext facesContext, Country country, Long existingRecordId) {
        duplicateRecordId = existingRecordId;
        String message = "Jahr: Für " + country.getName() + " gibt es für " + emissionRecord.getYear()
                         + " bereits einen Datensatz. Bitte bearbeiten Sie den vorhandenen Datensatz.";
        facesContext.addMessage(YEAR_FIELD_ID, new FacesMessage(FacesMessage.SEVERITY_ERROR, message, null));
        facesContext.validationFailed();
    }

    private static void addFlashMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        facesContext.getExternalContext().getFlash().setKeepMessages(true);
        facesContext.addMessage(null, new FacesMessage(severity, summary, detail));
    }

    public boolean isEditMode() {
        return emissionRecord != null && emissionRecord.getId() != null;
    }

    public boolean isEditingImportedValue() {
        return isEditMode() && emissionRecord.getSource() == RecordSource.WORLD_BANK;
    }

    public String getPageTitle() {
        if (isEditMode()) {
            return "Datensatz bearbeiten";
        }
        return "Neuer Datensatz";
    }

    public int getCurrentYear() {
        return Year.now().getValue();
    }

    public int getMinimumYear() {
        return MINIMUM_YEAR;
    }

    public List<Country> getCountries() {
        return countries;
    }

    public String getFilterCountryCode() {
        return filterCountryCode;
    }

    public void setFilterCountryCode(String filterCountryCode) {
        if (filterCountryCode == null || filterCountryCode.isBlank()) {
            this.filterCountryCode = null;
        } else {
            this.filterCountryCode = filterCountryCode.trim().toUpperCase();
        }
    }

    public List<EmissionRecord> getEmissionRecords() {
        return emissionRecords;
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public EmissionRecord getEmissionRecord() {
        return emissionRecord;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public Long getDuplicateRecordId() {
        return duplicateRecordId;
    }

    public String getUnitLabel() {
        return EmissionRecord.UNIT_LABEL;
    }
}
