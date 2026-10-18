# Navigation zwischen den Seiten

```mermaid
flowchart LR
    index["index.xhtml<br/>öffentlich"] -->|Anmelden| login["login.xhtml"]
    login -->|falsche Daten| login
    index -->|Registrieren| register["register.xhtml"]
    login -->|Jetzt registrieren| register
    register -->|Bereits registriert? Anmelden| login
    register -->|Fehler in der Eingabe| register
    register -->|Erfolg, direkt angemeldet| list
    login -->|Erfolg| list["backend/emissions.xhtml"]
    list -->|Neuer Datensatz / Bearbeiten| edit["backend/emission-edit.xhtml"]
    edit -->|Speichern oder Abbrechen| list
    edit -->|Duplikat: vorhandenen Datensatz bearbeiten| edit
    list -->|Abmelden| index
    list -->|CO₂-Daten| index
    direct(["Direktaufruf /backend/* ohne Session"]) -.->|AuthenticationFilter| login
```

| Übergang | Umsetzung |
|---|---|
| Startseite → Login | `h:link outcome="/login"` in der Kopfzeile (nur ohne Anmeldung sichtbar) |
| Login → Backend | `LoginController.login()` gibt `/backend/emissions?faces-redirect=true` zurück |
| Startseite/Login → Registrierung | `h:link outcome="/register"` in der Kopfzeile (nur ohne Anmeldung) und auf der Login-Seite |
| Registrierung → Backend | `RegistrationController.register()` legt das Konto an, meldet direkt an und gibt `/backend/emissions?faces-redirect=true` zurück (Willkommensmeldung über Flash) |
| Registrierung bei Fehlern | `register()` gibt `null` zurück, die Meldung steht am betroffenen Feld |
| Login bei falschen Daten | `login()` gibt `null` zurück, die Seite bleibt und zeigt die Fehlermeldung |
| Liste → Formular | `h:button outcome="emission-edit"` (neu) bzw. `h:link` mit `f:param name="id"` (bearbeiten) |
| Formular → Liste | `save()` gibt `emissions?faces-redirect=true&land=<ISO>` zurück; „Abbrechen“ ist ein `h:button` ohne Validierung |
| Abmelden | `UserSession.logout()` beendet die Session und leitet auf `/index` um |
| Schutz des Backends | `AuthenticationFilter` (`@WebFilter("/backend/*")`) leitet ohne angemeldete `UserSession` auf `login.xhtml` um, bei Ajax-Anfragen per `<partial-response><redirect/>` |
| Startseite mit Land | GET-Parameter `?land=DEU` über `f:viewParam` (verlinkbar) |
| Fehler | `error.xhtml` für HTTP 404/500, abgelaufene Views (`ViewExpiredException`) führen zur Startseite |

Alle Seiten verwenden das Template `WEB-INF/templates/layout.xhtml` mit Kopfzeile (Navigation), Inhaltsbereich und Fußzeile (Quellenangabe).
