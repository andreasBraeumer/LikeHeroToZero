# Like Hero To Zero

Prototyp einer Webanwendung zu weltweiten CO₂-Emissionen, entstanden als Fallstudie im Kurs
„Programmierung von industriellen Informationssystemen mit Java EE“ (IPWA02-01).

- **Öffentlicher Bereich (US1):** Land auswählen und den aktuellsten Wert im Datensatz sehen (Land, Jahr, Wert, Einheit), dazu den Verlauf der Vorjahre.
- **Backend mit Login (US2):** Registrierte Wissenschaftler:innen legen neue Datensätze an und korrigieren vorhandene.

Technik: Jakarta EE 10 auf **Apache Tomcat 10.1**. Dazu gehören Jakarta Faces 4.0 (Mojarra) mit Facelets und PrimeFaces 15, CDI 4.0 (Weld), Jakarta Persistence 3.1 mit Hibernate 6.6 (`RESOURCE_LOCAL`), MySQL 8, Java 17 und Maven.

Tomcat ist ein reiner Servlet-Container. Faces, CDI, JSON-P und JPA liegen deshalb als Bibliotheken in der WAR-Datei, Servlet-, EL- und Annotations-API liefert Tomcat (`provided`).

Datenquelle: World Bank, World Development Indicators, Indikator `WB_WDI_EN_GHG_CO2_MT_CE_AR5`
(CO₂-Emissionen gesamt ohne Landnutzung, Mt CO₂e) über die Data360 API, Lizenz CC BY 4.0.

---

## Voraussetzungen

| Software | Version |
|---|---|
| JDK | 17 oder neuer (getestet mit 17 und 27) |
| Maven | 3.9 (oder das in IntelliJ gebündelte Maven) |
| Apache Tomcat | 10.1.x (getestet mit 10.1.60) |
| Docker Desktop | für die MySQL-Datenbank (alternativ eine eigene MySQL-8-Installation) |

Beim ersten Start braucht der Server Internetzugang zu `https://data360api.worldbank.org`, um die Datenbank zu befüllen.

## 1. Datenbank starten (MySQL 8 per Docker)

```sh
docker compose up -d --wait # startet MySQL 8 auf localhost:3306 und wartet, bis die Datenbank bereit ist
```

Die Zugangsdaten stehen in der `.env` im Projektordner. Sie ist bei diesem Uni-Projekt bewusst eingecheckt, damit Datenbank und Anwendung nach dem Herunterladen ohne weitere Einrichtung zusammenpassen. Die Passwörter gelten nur für die lokale Docker-Datenbank. Für einen echten Betrieb müssten sie geändert und aus dem Repository entfernt werden. Die Daten liegen im Docker-Volume `lhtz-mysql-data` und überstehen Neustarts. `docker compose down -v` löscht sie.

Mit einer eigenen MySQL-Installation statt Docker sieht die Einrichtung so aus:

```sql
CREATE DATABASE likeherotozero CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'likeherotozero'@'localhost' IDENTIFIED BY '<eigenes-passwort>';
GRANT ALL PRIVILEGES ON likeherotozero.* TO 'likeherotozero'@'localhost';
```

Die Tabellen legt Hibernate beim ersten Start selbst an (`hibernate.hbm2ddl.auto=update`).

## 2. Konfiguration

Die Anwendung liest ihre Einstellungen in dieser Reihenfolge:

1. Umgebungsvariable
2. Java-System-Property (z. B. `-DDB_PASSWORD=...`)
3. Datei `.env` im Projektordner (dieselbe Datei, die auch `docker-compose.yml` verwendet)

Für die lokale Entwicklung reicht deshalb die `.env`. In IntelliJ müssen keine Umgebungsvariablen eingetragen werden. Die Anwendung findet die Datei, weil IntelliJ und Maven die Anwendung im Ordner `target/` des Projekts entpacken. Wird die WAR-Datei auf einem anderen Server deployt, gelten Umgebungsvariablen oder System-Properties.

| Einstellung | Bedeutung | Standardwert |
|---|---|---|
| `DB_URL` | JDBC-URL | `jdbc:mysql://localhost:3306/likeherotozero?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC` |
| `DB_USER` | Datenbankbenutzer | `likeherotozero` |
| `DB_PASSWORD` | Passwort des Datenbankbenutzers | *(leer, muss gesetzt werden)* |
| `DEMO_USER` | Benutzername des Demo-Kontos | `demo` |
| `DEMO_PASSWORD` | Passwort des Demo-Kontos | *(muss gesetzt werden)* |
| `REGISTRATION_CODE` | Code, den neue Wissenschaftler:innen bei der Registrierung angeben müssen | *(nicht gesetzt = Registrierung abgeschaltet)* |

## 3. Bauen und Starten

**IntelliJ:** Maven-Projekt neu laden (die Werte aus `.env` werden automatisch gelesen), in der Run-Configuration „Tomcat 10.1“ unter *Deployment* das Artefakt `LikeHeroToZero:war exploded` eintragen (Application context z. B. `/likeherotozero`) und starten. Die Anwendung ist dann unter <http://localhost:8081/likeherotozero/> erreichbar.

**Kommandozeile:**

```sh
mvn clean package
```

`target/likeherotozero.war` nach `<TOMCAT>/webapps/` kopieren, Tomcat starten und <http://localhost:8080/likeherotozero/> öffnen.

Beim Build laufen auch die Unit-Tests: JSON-Parsing des Imports, Passwort-Hashing, Länderfilter und die DAO-Abfragen gegen eine H2-In-Memory-Datenbank, die nur in den Tests verwendet wird.

Beim ersten Start passiert Folgendes (siehe Server-Log):

- Das Demo-Konto wird mit `DEMO_PASSWORD` angelegt. In der Datenbank liegt nur der PBKDF2-Hash des Passworts.
- Ist die Tabelle der Emissionsdaten leer, importiert die Anwendung alle Werte des Indikators aus der Data360 API: 14 Aufrufe à 1000 Einträge, etwa 15 bis 25 Sekunden. Übernommen werden nur echte Länder (ISO 3166-1 alpha-3 plus Kosovo `XKX`), Regionen und Aggregate werden aussortiert. Ergebnis: 203 Länder mit 11.165 Jahreswerten.
- Ist die API nicht erreichbar, startet die Anwendung trotzdem. Der Import lässt sich später im Backend über „Weltbank-Daten aktualisieren“ nachholen.

Danach liest und schreibt die Anwendung nur noch die eigene Datenbank.

## Demo-Konto

| Benutzername | Passwort |
|---|---|
| `demo` (bzw. `DEMO_USER`) | der Wert von `DEMO_PASSWORD` aus der `.env` |

`DEMO_PASSWORD` ist die einzige Quelle für das Passwort des Demo-Kontos. Bei jedem Start prüft die Anwendung das Konto:

- Fehlt es, wird es mit `DEMO_PASSWORD` angelegt.
- Passt das gespeicherte Passwort nicht zu `DEMO_PASSWORD`, wird es einmalig angeglichen.
- Passt es, bleibt das Konto unverändert.
- Ist `DEMO_PASSWORD` nicht gesetzt, wird kein Konto angelegt und ein Fehler ins Log geschrieben. Es wird nie ein Zufallspasswort erzeugt.

Um das Passwort zu ändern, den Wert in `.env` anpassen und Tomcat neu starten.

## Registrierung

Weitere Wissenschaftler:innen legen sich unter „Registrieren“ (`register.xhtml`) selbst ein Konto an. Dafür brauchen sie den Registrierungscode aus `REGISTRATION_CODE`, den die Projektleitung weitergibt. So kann nicht jede:r Besucher:in Daten ändern. Ist `REGISTRATION_CODE` nicht gesetzt, zeigt die Seite nur einen Hinweis und nimmt keine Registrierungen an.

Nach erfolgreicher Registrierung ist man sofort angemeldet und landet in der Datenpflege. Das Passwort wird wie beim Demo-Konto nur als PBKDF2-Hash gespeichert.

## Bedienung

| Seite | Zugriff | Funktion |
|---|---|---|
| `index.xhtml` | öffentlich | Land wählen (Suche im Auswahlfeld). Der aktuellste Wert wird immer aus der Datenbank berechnet (höchstes Jahr je Land). Darunter stehen die Veränderung zum Vorjahr und der Verlauf. Das Land lässt sich auch per URL setzen (`?land=DEU`), ohne Parameter wird es aus der Browsersprache vorgeschlagen. |
| `login.xhtml` | öffentlich | Anmeldung, Fehlermeldung bei falschen Zugangsdaten |
| `register.xhtml` | öffentlich | Registrierung mit Benutzername (3 bis 50 Zeichen, eindeutig), Anzeigename, Passwort (mindestens 8 Zeichen, zweimal) und Registrierungscode |
| `backend/emissions.xhtml` | Login | alle Datensätze, Filter nach Land, Sortierung, „Neuer Datensatz“, „Bearbeiten“, manueller Import |
| `backend/emission-edit.xhtml` | Login | Land (Pflicht), Jahr (Pflicht, ganzzahlig, 1750 bis aktuelles Jahr), Wert in Mt CO₂e (Pflicht, 0 bis 100.000). Doppelte Kombination aus Land und Jahr wird abgefangen, mit Link zum vorhandenen Datensatz. |

Selbst erfasste oder korrigierte Werte bekommen die Quelle `RESEARCH` und werden bei späteren Importen nie überschrieben.
Backend-URLs ohne Anmeldung leiten auf die Login-Seite um. Logout beendet die Session.

## Projektstruktur

```
pom.xml, docker-compose.yml, .env
src/main/java/de/iu/likeherotozero/
  configuration/ ApplicationSettings
  model/        Country, EmissionRecord, User, RecordSource
  dao/          CountryDAO, EmissionRecordDAO, UserDAO, EntityManagerProvider
  controller/   PublicEmissionController, LoginController, RegistrationController, UserSession,
                BackendEmissionController
  service/      DataImportService, Data360Client, PasswordHasher, ApplicationInitializer
  web/          AuthenticationFilter
src/main/resources/
  META-INF/persistence.xml
  de/iu/likeherotozero/messages.properties   deutsche Validierungsmeldungen
src/main/webapp/
  index.xhtml, login.xhtml, register.xhtml, error.xhtml
  backend/emissions.xhtml, backend/emission-edit.xhtml
  WEB-INF/web.xml, beans.xml, faces-config.xml, templates/layout.xhtml
  resources/css/style.css
docs/           Entwürfe und Diagramme für den Fallstudienbericht
```

## Dokumentation

- [INSTALLATION.md](INSTALLATION.md): Schritt-für-Schritt-Anleitung für die Installation auf einem neuen Rechner
- [docs/projektdokumentation.md](docs/projektdokumentation.md): ausführliche Erklärung aller Prozesse, Daten und Komponenten
- [docs/wireframes.md](docs/wireframes.md): Skizzen der Seiten
- [docs/navigation.md](docs/navigation.md): Navigation zwischen den Seiten
- [docs/datenmodell.md](docs/datenmodell.md): ER-Diagramm und Tabellen
- [docs/architektur.md](docs/architektur.md): UML-Klassendiagramm aller Beans
- [docs/screenshots/](docs/screenshots/): Screenshots der umgesetzten Seiten (aufgenommen auf Tomcat 10.1 mit Testdaten)
