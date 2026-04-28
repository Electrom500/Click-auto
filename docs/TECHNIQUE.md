# Documentation technique - Click-auto

## 1. Objectif de ce document

Ce document complete `Plannification.md` avec une vue technique plus detaillee:

- structure du code,
- choix techniques,
- compromis,
- historique des decisions importantes.

## 2. Stack technique actuelle

- Langage: Groovy (application) sur JVM
- Build: Gradle (Groovy DSL)
- UI de depart: JavaFX
- Version cible Java: 21 (LTS) via toolchain Gradle

## 3. Choix techniques (journal)

## Decision D-001 - Build Gradle + plugin application

- Date: 2026-04-27
- Contexte: projet vide, besoin d'un socle executable rapidement.
- Choix: utiliser Gradle avec le plugin `application`.
- Pourquoi: execution simple (`run`), convention standard, evolutif.
- Impact: pipeline build/run unifiee, integration IDE facilitee.

## Decision D-002 - Cible Java 21 avec usage local possible de Java 26

- Date: 2026-04-27
- Contexte: Java 26 disponible cote utilisateur, mais risque de compatibilite ecosysteme.
- Choix: fixer la toolchain Gradle sur Java 21 LTS.
- Pourquoi: meilleure stabilite des dependances et de l'outillage.
- Impact: necessite un JDK 21 present sur la machine de build.

## Decision D-003 - UI initiale en Swing

- Date: 2026-04-27
- Contexte: besoin d'une premiere fenetre executable avec setup minimal.
- Choix: demarrer avec Swing.
- Pourquoi: inclus dans le JDK, zero dependance UI externe.
- Impact: mise en route rapide; migration JavaFX possible plus tard si besoin UX.

## Decision D-004 - Migration applicative en 100% Groovy

- Date: 2026-04-27
- Contexte: usage de Groovy attendu comme exigence principale du projet.
- Choix: migrer le point d'entree de `src/main/java` vers `src/main/groovy` et activer le plugin `groovy`.
- Pourquoi: aligner la stack applicative avec l'objectif de technologie retenue.
- Impact: l'application compile et s'execute sans source Java applicative.

## Decision D-005 - Usage cible de @CompileStatic

- Date: 2026-04-27
- Contexte: besoin de fiabilite proche Java en restant en Groovy.
- Choix: utiliser `@CompileStatic` sur les classes de coeur (entrypoint, domaine, moteur, persistance), sauf zones necessitant du dynamique.
- Pourquoi: verification compile-time plus stricte, performance et lisibilite des erreurs.
- Impact: code Groovy plus predictible et maintenable, avec exceptions explicites si besoin de dynamique.

## Decision D-006 - Swing au lieu de JavaFX pour MVP

- Date: 2026-04-27
- Contexte: JavaFX sur Java 21 requiert une configuration du module system complexe.
- Choix: rester sur Swing inclus dans le JDK pour MVP.
- Pourquoi: zero dependance externe, executable immediatement, robuste.
- Impact: JavaFX peut etre adopte plus tard si besoin UX plus avancee; pour MVP, Swing suffit largement.
- Note: migration vers JavaFX reste simple a faire une fois la logique stabilisee.

## Decision D-007 - Passage de la page principale en JavaFX

- Date: 2026-04-27
- Contexte: besoin d'une UI plus moderne et structurée pour la page principale.
- Choix: construire la fenêtre principale avec JavaFX en Groovy statique, sans FXML pour garder la base légère.
- Pourquoi: page principale plus lisible, séparation claire des zones (actions, propriétés, statut).
- Impact: dépendances JavaFX ajoutées au build Gradle avec classifier Windows; base prête pour enrichir l'écran sans toucher au modèle métier.

## Decision D-008 - Navigation header + onglet Test

- Date: 2026-04-28
- Contexte: besoin de naviguer rapidement entre blocs fonctionnels et valider les clics souris.
- Choix: ajouter un header avec 4 sections (`clicks`, `touches clavier`, `record and replay`, `test`).
- Pourquoi: préparer l'architecture UI modulaire tout en conservant un MVP simple.
- Impact: l'onglet `test` permet un clic souris aux coordonnées X/Y via `java.awt.Robot` et un popup de validation.

## Decision D-009 - Decoupage des pages UI en fichiers distincts

- Date: 2026-04-28
- Contexte: `MainView` devenait trop volumineux et melangeait navigation + contenu des pages.
- Choix: conserver `MainView` comme shell (header, routage, statut) et extraire chaque page dans `ui/pages`.
- Pourquoi: meilleure lisibilite, maintenance plus simple, evolution independante par page.
- Impact: architecture UI modulaire avec partage du statut via callback `Consumer<String>`.

## Decision D-010 - Page de gestion de clics dédiée

- Date: 2026-04-28
- Contexte: l'onglet clicks devait devenir une vraie zone de gestion des clics souris.
- Choix: centraliser la création, duplication, suppression, activation et reordonnancement dans `ClicksPageView`.
- Pourquoi: rendre l'édition des clics explicite et préparer l'execution future sans mélanger les touches clavier.
- Impact: la page clicks devient spécifique aux `ClickAction`, avec édition par champs et compteur de clics actifs.

## 4. Structure de code actuelle

```
src/main/groovy/
├── fr/clickauto/app/
│   ├── MainApp.groovy              # Point d'entrée JavaFX
│   ├── MainLauncher.groovy         # Lanceur JavaFX recommandé pour IDE
│   └── ui/
│       ├── MainView.groovy         # Shell: header + navigation + statut
│       └── pages/
│           ├── ClicksPageView.groovy
│           ├── KeysPageView.groovy
│           ├── RecordReplayPageView.groovy
│           └── TestPageView.groovy
└── fr/clickauto/model/
    ├── Action.groovy               # Interface de base pour les actions
    ├── ClickAction.groovy          # Clic souris (avec type, position, délai)
    ├── KeyAction.groovy            # Touche clavier (avec durée, délai)
    ├── ClickType.groovy            # Énumération: LEFT, RIGHT, MIDDLE
    └── Sequence.groovy             # Conteneur de séquence (liste d'actions + cycles)
```

### Annotations Groovy utilisées

- `@CompileStatic` : activée sur toutes les classes du modèle, l'entrypoint et la vue principale.
- `@ToString` : génère les `toString()` avec champs selectionnés pour debug.

### Notes sur le choix de package

- `model/` : domaine purement metier, sans dependances UI.
- `app/ui` : shell de navigation JavaFX.
- `app/ui/pages` : pages fonctionnelles independantes (gestion clics, touches, record/replay, test).
- `service/` ou `engine/` : viendra plus tard pour l'exécution.

## 5. Regles d'evolution technique

- Toute decision impactant architecture, perfs, securite ou UX doit ajouter une entree `Decision D-xxx`.
- En cas de changement demande pendant le dev, documenter:
  - l'etat avant,
  - la demande,
  - la solution retenue,
  - les impacts.

## 6. Backlog technique court terme

- Créer un service d'execution `SequenceExecutor` pour start/stop/pause/resume.
- Implémenter la logique de clic souris (Robot, délais, cycles).
- Implémenter la logique de touche clavier (Robot, durées).
- Tester les modèles métier avec Spock (tests Groovy).
- Ajouter des services dédiés par page (ex: clicks/test) pour sortir la logique des vues.
- Ajouter la persistance JSON des profils (sauvegarde/chargement).
- Intégrer les raccourcis clavier globaux (hotkeys).


