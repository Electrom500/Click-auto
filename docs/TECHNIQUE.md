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
- Note: ce choix a ensuite été remplacé par une base JavaFX quand l'UI s'est enrichie.

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
- Statut: decision historique, remplacee ensuite par JavaFX pour l'application courante.

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

## Decision D-011 - UX avancée page clicks (capture coordonnees + raccourcis)

- Date: 2026-04-29
- Contexte: besoin d'édition rapide et de capture de positions sans saisie manuelle.
- Choix:
  - corriger la sélection index `0` (bug lié à l'opérateur Elvis sur `getSelectedIndex()`),
  - ajouter le déplacement `Shift+Haut` / `Shift+Bas`,
  - ajouter un bouton `Enregistrer coordonnees` qui capte le prochain clic gauche via overlay JavaFX plein écran.
- Pourquoi: améliorer la vitesse d'édition et éviter les erreurs de coordonnées.
- Impact: meilleure ergonomie immédiate; pas de dépendance native supplémentaire.

## Decision D-012 - Affichage hors application: stratégie progressive

- Date: 2026-04-29
- Contexte: besoin d'un retour visuel des points de clic en dehors de la page d'édition.
- Choix: prioriser un overlay JavaFX transparent (capture de coordonnées) comme base technique.
- Pourquoi: faisable sans SDK natif externe, déjà compatible avec l'architecture actuelle.
- Impact: les marqueurs visuels "croix rouges" sur applications tierces restent planifiés pour une étape dédiée.

## Decision D-013 - Persistance JSON des profils de clics

- Date: 2026-04-30
- Contexte: besoin de sauvegarder et recharger rapidement des séquences de clics depuis l'onglet `clicks`.
- Choix: utiliser `groovy.json.JsonOutput` / `JsonSlurper` et un petit service dédié `SequenceProfileStore`.
- Pourquoi: zéro dépendance lourde, format lisible, maintenable et facile à exporter.
- Impact: les profils peuvent être sérialisés en JSON avec les cycles, délais et actions.

## Decision D-014 - Confirmation obligatoire du mode infini

- Date: 2026-04-30
- Contexte: un démarrage involontaire d'une boucle infinie peut bloquer l'utilisateur.
- Choix: afficher une confirmation avant lancement lorsque `Sequence.cycles == 0`.
- Pourquoi: réduire le risque d'erreur et rappeler explicitement la touche d'arrêt `F9`.
- Impact: lancement plus sûr, surtout pour les séquences répétées sans limite.

## Decision D-015 - Click-through overlay Windows

- Date: 2026-04-30
- Contexte: l'overlay visuel ne devait pas bloquer les interactions avec les autres applications.
- Choix: implémenter un support Windows via JNA avec un fallback JavaFX best-effort.
- Pourquoi: rester Windows-first tout en gardant une solution de secours si l'accès au handle natif échoue.
- Impact: l'overlay peut fonctionner en mode pass-through et laisser la souris au système sous-jacent.

## Decision D-016 - Hotkeys globales et drag & drop

- Date: 2026-05-04
- Contexte: besoin de lancer/stopper la séquence hors focus et réordonner rapidement les clics.
- Choix: activer des hotkeys globales (F8/F7/F9) via `com.github.kwhat.jnativehook` et ajouter le drag & drop dans la liste des clics.
- Pourquoi: améliorer l'efficacité d'exécution et l'ergonomie de la page `clicks`.
- Impact: le contrôle global devient optionnel et la liste de clics est réordonnable à la souris.

## Decision D-017 - Prévisualisation des marqueurs visuels

- Date: 2026-05-04
- Contexte: besoin de visualiser rapidement les positions de clics sur l'écran.
- Choix: overlay transparent qui affiche les marqueurs pour quelques secondes, avec click-through best-effort.
- Pourquoi: éviter les erreurs de coordonnées sans exécuter la séquence.
- Impact: prévisualisation rapide via bouton dédié.

## Decision D-013 - Capture de coordonnées globale avec fallback overlay

- Date: 2026-04-30
- Contexte: le bouton `Enregistrer coordonnees` devait viser d'autres applications, pas seulement la fenêtre courante.
- Choix: tenter d'abord une capture globale via `com.github.kwhat.jnativehook`, puis basculer sur un overlay plein écran si le hook natif n'est pas disponible.
- Pourquoi: conserver une UX simple sans bloquer le projet sur une dépendance native obligatoire.
- Impact: la capture de coordonnées devient plus utile pour cibler d'autres applications, avec mode secours intégré.

## Decision D-014 - Sécurisation des callbacks UI du moteur

- Date: 2026-04-30
- Contexte: des erreurs `Not on FX application thread` sont apparues lors du lancement d'une séquence.
- Choix: marshaller systématiquement les messages de statut du moteur vers le thread JavaFX.
- Pourquoi: éviter les accès UI hors thread et stabiliser les mises à jour de statut.
- Impact: `SequenceExecutor` peut rester sur un thread dédié tout en mettant à jour l'interface sans exception.

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

### Etat actuel de lancement de routine

- Le moteur d'exécution (`SequenceExecutor`) est branché sur la page `clicks`.
- Les boutons `Démarrer / Pause / Arrêter` contrôlent réellement l'exécution.
- Les hotkeys locales `F8 / F7 / F9` sont disponibles dans la page et l'overlay.
- Les messages de statut sont renvoyés sur le thread JavaFX pour éviter les exceptions de thread.
- Le mode infini déclenche une confirmation de sécurité avant démarrage.
- Le fichier de profil JSON est géré directement depuis l'onglet `clicks`.
- L'overlay peut passer en click-through pour ne pas bloquer les clics dans les autres applications.
- Les hotkeys globales peuvent être activées depuis l'UI.
- La liste des clics accepte le drag & drop.
- Un overlay de prévisualisation affiche les marqueurs visuels des clics.

## 5. Regles d'evolution technique

- Toute decision impactant architecture, perfs, securite ou UX doit ajouter une entree `Decision D-xxx`.
- En cas de changement demande pendant le dev, documenter:
  - l'etat avant,
  - la demande,
  - la solution retenue,
  - les impacts.

## 6. Backlog technique court terme

- Créer un service d'execution `SequenceExecutor` pour start/stop/pause/resume.
- Finaliser la persistance JSON des profils (sauvegarde/chargement).
- Ajouter des tests Spock sur `Sequence`, `ClickAction`, `KeyAction` et `SequenceExecutor`.
- Ajouter des hotkeys globales si le besoin hors focus devient prioritaire.
- Compléter l'affichage visuel externe des points de clic avec click-through natif Windows si nécessaire.


