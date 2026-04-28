# Click-auto

Application desktop Windows-first pour automatiser des sequences de clics et de touches clavier.

## Etat actuel

Le projet est initialise techniquement avec Gradle, Groovy et JavaFX.
- Point d'entrée exécutable en Groovy avec `@CompileStatic`.
- Page principale JavaFX (base UI en 4 zones).
- Modèles métier créés : `Action`, `ClickAction`, `KeyAction`, `Sequence`.
- Tous les tests de compilation passent.

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
  - édition des coordonnées, du type et du délai.
- Modèles métier complets:
  - `Action` : interface de base pour toute action.
  - `ClickAction` : représente un clic souris (LEFT, RIGHT, MIDDLE).
  - `KeyAction` : représente une touche clavier avec durée de maintien.
  - `ClickType` : énumération des types de clics.
  - `Sequence` : conteneur de séquence d'actions avec cycles et délais.
- Structure de base prete pour separer UI, moteur d'execution et persistance.

## Bugs connus

- Aucun bug fonctionnel liste a ce stade (MVP architecture en place).
- Le projet ne peut pas etre lance sans Java et Gradle correctement installes dans le PATH.

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

1. Creer le service d'execution (`SequenceExecutor`).
2. Rendre la page JavaFX interactive (édition de séquence, sélection, duplication).
3. Ajouter un premier moteur de clic souris minimal avec `start/stop/pause`.
4. Introduire la sauvegarde JSON d'un profil.
5. Ajouter des tests Groovy Spock sur la logique de sequence.

