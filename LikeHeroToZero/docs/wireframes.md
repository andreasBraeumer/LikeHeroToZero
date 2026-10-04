# Wireframes (Entwurf der Webseiten)

Alle Seiten teilen sich das Template `WEB-INF/templates/layout.xhtml`: Kopfzeile mit Logo und Navigation, Inhaltsbereich, Fußzeile mit Quellenangabe.
Die umgesetzten Seiten zeigen die Screenshots in [screenshots/](screenshots/).

## Gemeinsames Layout

```
+----------------------------------------------------------------------+
| [CO₂] Like Hero To Zero              CO₂-Daten   [Anmelden]          |  ohne Login
| [CO₂] Like Hero To Zero    CO₂-Daten  Datenpflege  Name  Abmelden    |  mit Login
+----------------------------------------------------------------------+
|                                                                      |
|                        <Inhalt der Seite>                            |
|                                                                      |
+----------------------------------------------------------------------+
| Datenquelle: World Bank, WDI, Indikator ..., Data360 API, CC BY 4.0  |
+----------------------------------------------------------------------+
```

## 1. Startseite `index.xhtml` (öffentlich, US1)

```
 Wie viel CO₂ stößt Ihr Land aus?
 Wählen Sie ein Land und sehen Sie den aktuellsten Wert ...

 +-------------------------------------------+
 | Land                                      |
 | [ Deutschland                        v ]  |   <- Auswahl mit Suchfeld, Ajax
 +-------------------------------------------+

 +-------------------------------------------+
 || Aktuellster Wert für Deutschland         |
 ||  579,94  Mt CO₂e                         |   <- großer Wert
 || im Jahr 2024 · Mio. t CO₂-Äquivalent ·   |
 ||   Quelle: Weltbank                       |
 || -1,6 % gegenüber 2023                    |
 +-------------------------------------------+

 +-------------------------------------------+
 | Verlauf                                   |
 | Jahr ^v | CO₂-Ausstoß (Mt CO₂e) ^v | Quelle|
 | 2024    |                 579,94 | Weltbank|
 | 2023    |                 589,66 | Weltbank|
 | ...                                       |
 |           < 1 2 3 4 5 6 >                 |
 +-------------------------------------------+

 Land ohne Werte:
 +-------------------------------------------+
 | Für <Land> liegen in unserem Datensatz    |
 | leider noch keine CO₂-Werte vor.          |
 +-------------------------------------------+
```

## 2. Login `login.xhtml` (öffentlich)

```
 +-------------------------------------------+
 | Anmeldung für Wissenschaftler:innen       |
 | [x] Anmeldung fehlgeschlagen              |  <- nur bei falschen Daten
 |     Benutzername oder Passwort ist falsch.|
 |                                           |
 | Benutzername                              |
 | [______________________]                  |
 | Passwort                                  |
 | [______________________]                  |
 |                                           |
 | [Anmelden]  Zurück zur Startseite         |
 +-------------------------------------------+
```

## 3. Datenpflege `backend/emissions.xhtml` (Login, US2)

```
 Datenpflege                                     [Neuer Datensatz]
 Emissionsdaten anlegen und korrigieren ...

 [i] Gespeichert – Neuer Datensatz für Deutschland 2025 angelegt.

 +------------------------------------------------------------------+
 | Land                                                              |
 | [ Alle Länder          v ]    11166 Datensätze  [⟳ Weltbank-Daten |
 |                                                   aktualisieren]  |
 +------------------------------------------------------------------+
 | Land ^v | Jahr ^v | Wert ^v | Quelle   | Geändert von | am   |    |
 | Deutschl| 2025    | 560,25  | Forschung| Demo         | ...  |[Bearbeiten]|
 | Deutschl| 2024    | 579,94  | Weltbank |              | ...  |[Bearbeiten]|
 | ...                                                              |
 |   1–25 von 11166   << < 1 2 3 ... > >>   [25 v]                  |
 +------------------------------------------------------------------+
```

## 4. Formular `backend/emission-edit.xhtml` (Login, US2)

```
 +-------------------------------------------+
 | Neuer Datensatz / Datensatz bearbeiten    |
 | (Hinweis bei Weltbank-Wert: wird nach dem |
 |  Speichern zum Forschungsbeitrag)         |
 |                                           |
 | Land *                                    |
 | [ – Land auswählen –                 v ]  |
 | Land: Bitte einen Wert angeben.           |  <- Meldung am Feld
 |                                           |
 | Jahr *                                    |
 | [2024]                                    |
 | Jahr: Für Deutschland gibt es für 2024    |
 | bereits einen Datensatz ...               |
 | Vorhandenen Datensatz bearbeiten          |  <- Link bei Duplikat
 | Ganzzahl von 1750 bis 2026.               |
 |                                           |
 | CO₂-Ausstoß in Mt CO₂e *                  |
 | [ 579,9356 ]                              |
 | Millionen Tonnen CO₂-Äquivalent ...       |
 |                                           |
 | [Speichern] [Abbrechen]                   |
 +-------------------------------------------+
```
