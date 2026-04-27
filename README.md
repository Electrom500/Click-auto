# Click-auto

Application desktop Windows-first pour automatiser des sequences de clics et de touches clavier.

## Etat actuel

Le projet est initialise techniquement avec Gradle et Groovy.
La premiere version executable ouvre une fenetre vide (base UI).

## Installation (environnement entreprise)

### Prerequis

- JDK 21 recommande (LTS).
- Java 26 possible en local, mais la cible de build est Java 21 via toolchain Gradle.
- Gradle installe localement (une fois) pour generer le wrapper.

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

Puis, utiliser uniquement le wrapper :

```powershell
.\gradlew.bat clean build
.\gradlew.bat run
```

## Fonctionnalites principales developpees

- Initialisation du projet Gradle (`application` + `groovy`).
- Point d'entree Groovy: `fr.clickauto.app.MainApp`.
- Ouverture d'une fenetre desktop vide (Swing).
- Compilation statique activee via `@CompileStatic` sur l'entree applicative.
- Structure de base prete pour separer UI, moteur d'execution et persistance.

## Bugs connus

- Aucun bug fonctionnel liste a ce stade (MVP non demarre).
- Le projet ne peut pas etre lance sans Java et Gradle correctement installes dans le PATH.

## Architecture du projet (actuelle)

- `build.gradle`: configuration build et runtime.
- `settings.gradle`: nom du projet Gradle.
- `src/main/groovy/fr/clickauto/app/MainApp.groovy`: point d'entree UI.
- `docs/TECHNIQUE.md`: decisions techniques et historique des choix.
- `Plannification.md`: vision produit, perimetre, evolutions.

## Prochaines etapes conseillees

1. Ajouter un squelette UI (zones: sequence, proprietes, commandes).
2. Creer le modele de sequence (`Action`, `ClickAction`, `KeyAction`).
3. Ajouter un premier moteur d'execution minimal avec `start/stop`.
4. Introduire la sauvegarde JSON d'un profil.
