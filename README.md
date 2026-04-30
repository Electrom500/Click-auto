# Click-auto

Application desktop Windows-first pour automatiser des sequences de clics et de touches clavier.

## Etat actuel

Le projet est maintenant structure autour d'une UI JavaFX et d'un moteur d'execution basique.
- Point d'entrée exécutable en Groovy avec `@CompileStatic`.
- Shell JavaFX avec navigation entre 4 sections.
- Page `clicks` dédiée à la gestion des clics.
- Moteur `SequenceExecutor` pour start / pause / resume / stop.
- Capture des coordonnées via clic global quand disponible, avec fallback overlay.
- Hotkeys locales sur la page et l'overlay : F8 démarrer, F7 pause/reprendre, F9 arrêter.
- Modèles métier créés : `Action`, `ClickAction`, `KeyAction`, `Sequence`.
- Compilation Gradle validée.

## Installation (environnement entreprise)

### Prerequis

- JDK 21 recommande (LTS).
- Java 26 possible en local, mais la cible de build est Java 21 via toolchain Gradle.
- Gradle installe localement (une fois) pour generer le wrapper.
- JavaFX est fourni via les dépendances Gradle (profil Windows).

### Verification des outils

```powershell
java -version
gradle -v
```

### Initialisation du wrapper Gradle

A executer une fois apres installation de Gradle :

```powershell
gradle wrapper
```

### Construction et execution

```powershell
.\gradlew.bat clean build
.\gradlew.bat run
```

### Execution dans IntelliJ (erreur JavaFX runtime manquant)

Si IntelliJ affiche `JavaFX runtime components are missing`, lance l'app via Gradle ou via le launcher:

- Classe a lancer: `fr.clickauto.app.MainLauncher`
- SDK du projet: Java 21
- Type de configuration recommande: `Gradle` (task `run`)

Option `Application` (si necessaire):

- Main class: `fr.clickauto.app.MainLauncher`
- VM options:

```text
--module-path "<chemin-vers-javafx-lib>" --add-modules=javafx.controls,javafx.graphics,javafx.base
```

## Fonctionnalites principales developpees

- Initialisation du projet Gradle (`application` + `groovy`).
- Point d'entree Groovy: `fr.clickauto.app.MainApp` avec `@CompileStatic`.
- Fenetre principale JavaFX exécutable.
- Header de navigation: `clicks` - `touches clavier` - `record and replay` - `test`.
- Onglet `test` disponible:
  - champs X / Y,
  - bouton `Creer un clic` (clic souris reel via Robot),
  - bouton `Afficher popup`.
- Onglet `clicks` dédié à la gestion de clics:
  - ajout,
  - duplication,
  - suppression,
  - activation / désactivation,
  - déplacement haut / bas,
  - déplacement clavier `Shift + Fleche Haut/Bas`,
  - édition des coordonnées, du type et du délai.
  - bouton `Enregistrer coordonnees` pour remplir `X/Y` avec le prochain clic gauche capturé.
  - panneau d'execution avec mode de séquence (`One shot`, boucle infinie, cycles, durée), délai initial, et boutons `Démarrer / Pause / Arrêter`.
  - hotkeys locales pour piloter l'execution (`F8`, `F7`, `F9`).
- Modèles métier complets:
  - `Action` : interface de base pour toute action.
  - `ClickAction` : représente un clic souris (LEFT, RIGHT, MIDDLE).
  - `KeyAction` : représente une touche clavier avec durée de maintien.
  - `ClickType` : énumération des types de clics.
  - `Sequence` : conteneur de séquence d'actions avec cycles et délais.
    - Structure de base prête pour séparer UI, moteur d'execution et persistance.

## Bugs connus

- Aucun bug fonctionnel bloquant liste a ce stade (MVP architecture en place).
- Le projet ne peut pas etre lance sans Java et Gradle correctement installes dans le PATH.

## Etat de la routine de clics

- Le lancement réel d'une routine est branché sur `SequenceExecutor`.
- Les clics souris et les touches clavier sont exécutés via `java.awt.Robot`.
- Le moteur gère `start/stop/pause/resume` sur un thread dédié.
- Les callbacks UI sont sérialisés sur le thread JavaFX pour éviter les erreurs de thread.
- Les hotkeys globales restent une évolution possible, au-delà des raccourcis locaux actuels.

## Architecture du projet (actuelle)

```
src/main/groovy/
├── fr/clickauto/app/
│   ├── MainApp.groovy          # Point d'entrée JavaFX
│   ├── MainLauncher.groovy     # Lanceur pour runtime JavaFX
│   └── ui/
│       ├── MainView.groovy                    # Shell (header + navigation + statut)
│       └── pages/
│           ├── ClicksPageView.groovy          # Page clicks
│           ├── KeysPageView.groovy            # Page touches clavier
│           ├── RecordReplayPageView.groovy    # Page record and replay
│           └── TestPageView.groovy            # Page test (clic XY + popup)
└── fr/clickauto/model/
    ├── Action.groovy            # Interface de base
    ├── ClickAction.groovy       # Action clic souris
    ├── KeyAction.groovy         # Action touche clavier
    ├── ClickType.groovy         # Énumération des types de clics
    └── Sequence.groovy          # Conteneur de séquence
```

- `build.gradle`: configuration build et runtime.
- `settings.gradle`: nom du projet Gradle.
- `docs/TECHNIQUE.md`: decisions techniques et historique des choix.
- `Plannification.md`: vision produit, perimetre, evolutions.

## Prochaines etapes conseillees

1. Finaliser la persistance JSON des profils.
2. Ajouter des tests Groovy / Spock sur `Sequence` et `SequenceExecutor`.
3. Ajouter la configuration des hotkeys globales si besoin.
4. Compléter la partie visuelle d'exécution (overlay click-through natif sous Windows si nécessaire).

