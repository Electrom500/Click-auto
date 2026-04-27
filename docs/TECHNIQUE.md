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
- UI de depart: Swing
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

## 4. Structure de code actuelle

- `src/main/groovy/fr/clickauto/app/MainApp.groovy`
  - Point d'entree applicatif.
  - Ouvre une fenetre vide pour valider la chaine build -> run.

## 5. Regles d'evolution technique

- Toute decision impactant architecture, perfs, securite ou UX doit ajouter une entree `Decision D-xxx`.
- En cas de changement demande pendant le dev, documenter:
  - l'etat avant,
  - la demande,
  - la solution retenue,
  - les impacts.

## 6. Backlog technique court terme

- Ajouter le wrapper Gradle (`gradlew`, `gradlew.bat`, `gradle/wrapper/*`).
- Creer un module de domaine pour les actions (clic/clavier).
- Introduire un service de persistance JSON des profils.
- Ajouter des tests unitaires sur la logique de sequence.

