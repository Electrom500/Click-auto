# Planification - Click-Auto

## 1. Vision du projet

L'application a pour objectif de fournir un outil d'automatisation local pour PC permettant de gérer des séquences de clics et de touches clavier de manière simple, visuelle et configurable.

Le produit doit être pensé en priorité pour Windows. Une version Android peut être envisagée plus tard, mais elle ne fait pas partie du périmètre initial.

L'application doit rester générique et ne pas dépendre d'un jeu ou d'un usage précis.

## 2. Objectifs fonctionnels

- Permettre la création et l'exécution de séquences de clics souris.
- Permettre la création et l'exécution de séquences clavier.
- Permettre de définir l'ordre des actions.
- Permettre de définir la durée, le nombre de cycles et les délais entre actions.
- Permettre de maintenir une touche pendant un temps donné.
- Permettre de sauvegarder et recharger des profils.
- Proposer une interface simple pour organiser les actions par glisser-déposer.

## 3. Périmètre produit

### Inclus
- Automatisation des clics souris.
- Automatisation des touches clavier.
- Gestion de séquences.
- Réglage des délais.
- Réglage du nombre de cycles ou de la durée totale.
- Profils sauvegardables.
- Raccourcis de contrôle.
- Prévisualisation de la séquence.
- Options de variation contrôlée du timing.

### Exclu pour la première version
- Support Android natif.
- Automatisation liée à un jeu spécifique.
- Scénarios complexes conditionnels avancés.
- Système de scripts avancé.
- Reconnaissance visuelle ou IA.
- Synchronisation cloud.

## 4. Partie clics souris

### Fonctionnalités principales
- Création de marqueurs de clic dans une séquence.
- Déplacement des marqueurs par glissement à la souris.
- Réorganisation manuelle de l'ordre des marqueurs.
- Définition d'un délai entre chaque clic.
- Choix du type de clic.
- Définition d'une durée totale de cycle.
- Définition d'un nombre de cycles.
- Délai initial avant démarrage.

### Types de clic
- Clic gauche.
- Clic droit.
- Clic molette.

### Options utiles à ajouter
- Randomisation contrôlée du délai entre les clics.
- Valeur minimale et maximale pour cette randomisation.
- Répétition infinie avec arrêt manuel.
- Séquence en boucle.
- Activation ou désactivation de chaque marqueur.
- Duplication d'un marqueur.
- Suppression rapide d'un marqueur.
- Export et import de profils de clics.

## 5. Partie clavier

### Fonctionnalités principales
- Création d'une séquence de touches.
- Réorganisation de l'ordre des touches.
- Définition de la durée d'appui d'une touche.
- Maintien d'une touche pendant un temps donné.
- Définition d'un délai entre les touches.
- Définition d'un nombre de cycles.
- Définition d'une durée totale d'exécution.
- Possibilité de combiner plusieurs touches.

### Cas d'usage à couvrir
- Appui simple sur une touche.
- Appui prolongé sur une touche.
- Combinaison de touches.
- Séquence de touches répétée.
- Enchaînement texte ou caractères simples si nécessaire.

### Options utiles à ajouter
- Randomisation contrôlée du délai entre appuis.
- Maintien de plusieurs touches dans une séquence.
- Profils séparés pour clavier et souris.
- Séquences mixtes clavier + souris.
- Historique des dernières séquences utilisées.
- Import et export de macros clavier.

## 6. Contrôle d'exécution

### Commandes principales
- Démarrer.
- Arrêter.
- Mettre en pause.
- Reprendre.
- Relancer une séquence sauvegardée.

### Paramètres d'exécution
- Délai initial avant lancement.
- Nombre de cycles.
- Durée totale.
- Répétition continue.
- Déclenchement manuel.
- Raccourci clavier global.
- Touche de lancement globale configurable.
- Touche d'arrêt globale prioritaire (arrêt d'urgence).

### Comportements attendus
- L'arrêt doit être immédiat.
- La pause doit conserver l'état courant de la séquence.
- La reprise doit continuer au bon endroit.
- La fin d'un cycle doit être clairement visible.
- Le lancement involontaire doit être évité par une validation claire.

## 7. Interface utilisateur

L'interface doit être claire, rapide à comprendre et utilisable sans configuration complexe.

### Attentes UX
- Vue liste ou timeline des actions.
- Drag and drop pour réordonner les actions.
- Panneau de réglage pour chaque action.
- Séparation nette entre souris et clavier.
- Boutons visibles pour démarrer, arrêter et mettre en pause.
- Lecture simple des délais et de l'ordre des actions.
- Sauvegarde et chargement rapides des profils.

### Éléments recommandés
- Zone centrale pour la séquence.
- Panneau latéral pour les propriétés.
- Barre supérieure pour les commandes principales.
- Indicateurs d'état pendant l'exécution.
- Retour visuel sur l'action en cours.

## 8. Options complémentaires pertinentes

- Hotkeys globales personnalisables.
- Mode test sans exécution réelle.
- Prévisualisation de la séquence avant lancement.
- Journal d'exécution.
- Sauvegarde automatique des profils.
- Duplication de séquence.
- Désactivation temporaire d'un bloc d'actions.
- Ajustement rapide des délais.
- Mélange de clics et de touches dans une même macro.
- Support multi-profils.

## 9. Options d'anti-régularité

Le produit peut inclure des réglages pour éviter un rythme trop mécanique.

### Paramètres possibles
- Décalage aléatoire sur le délai entre actions.
- Plage min/max configurable.
- Variation légère par cycle.
- Activation ou désactivation globale de la variation.
- Valeurs séparées pour souris et clavier.

### Objectif
Ces options servent à empècher la détecter d'un clicker classique. 
## 10. Exigences techniques

### Plateforme cible
- PC en priorité.
- Windows en premier.
- Android plus tard si le projet évolue.

### Contraintes générales
- Application locale.
- Réponse rapide de l'interface.
- Automatisation stable.
- Architecture modulaire.
- Facilité de maintenance.
- Possibilité d'étendre les types d'actions plus tard.

### Architecture recommandée
- Un module de séquence pour stocker les actions.
- Un moteur d'exécution pour lancer les clics et touches.
- Un système de profil pour sauvegarder les configurations.
- Une couche UI séparée du moteur d'automatisation.
- Un système de raccourcis globaux.
- Une gestion des délais et de la répétition isolée du reste.

## 11. Technologies possibles

### Java
- Très bon choix pour un premier MVP desktop.
- Cohérent si le but est d'aller vite avec une base sérieuse.
- Très adapté à une application structurée et maintenable.
- Bonne option si l'objectif est de capitaliser sur un bon niveau de maîtrise.

### Python
- Très bon choix pour prototyper rapidement.
- Intéressant pour tester l'UX et les séquences.
- Moins idéal si l'on veut une application desktop très structurée à long terme.
- Bon candidat pour une version expérimentale.

### Go
- Intéressant pour produire un exécutable simple.
- Bon compromis entre lisibilité et distribution.
- Plus pertinent si l'on vise un outil léger.
- Moins naturel que Java pour une UI desktop riche.

### Rust
- Excellent pour la robustesse et la performance.
- Intéressant si l'objectif est aussi d'apprendre une technologie bas niveau.
- Plus exigeant en courbe d'apprentissage.
- Demande plus d'effort pour une interface desktop complète.

## 12. Recommandation technique

Pour ce projet, Java est la meilleure recommandation de départ.

### Pourquoi
- Tu maîtrises déjà bien Java.
- La cible principale est une application desktop.
- Java convient bien à une architecture claire et évolutive.
- Le projet peut être structuré proprement dès le départ.

### Position sur les autres options
- Python peut servir pour prototyper rapidement.
- Go devient intéressant si tu veux un binaire plus léger.
- Rust est pertinent si tu veux prioriser la robustesse et l'apprentissage, mais il est moins direct pour un premier MVP.

### Pour Android plus tard
Il sera probablement préférable de traiter Android comme un second projet ou une seconde phase, plutôt que de forcer une réutilisation directe du code desktop.

## 13. Évolution possible

- Support Android.
- Profils avancés.
- Éditeur visuel plus riche.
- Séquences conditionnelles.
- Gestion multi-écran.
- Raccourcis personnalisés avancés.
- Import/export de profils au format fichier.
- Mode avancé pour utilisateurs expérimentés.

## 14. Critères de réussite

L'application sera considérée comme réussie si elle permet de :

- Créer rapidement une séquence de clics.
- Réorganiser facilement les actions.
- Définir les délais et les répétitions.
- Maintenir une touche pendant un temps donné.
- Sauvegarder et recharger des profils.
- Démarrer et arrêter proprement l'exécution.
- Rester simple à comprendre pour un utilisateur PC.

## 15. Suivi d'implémentation technique

### État courant (2026-04-27)

- ✅ Initialisation Gradle opérationnelle.
- ✅ Migration complète en 100% Groovy (`@CompileStatic` activé).
- ✅ UI principale en JavaFX avec navigation header.
- ✅ Pages UI séparées en fichiers distincts (`clicks`, `touches clavier`, `record and replay`, `test`).
- ✅ Page de gestion de clics dédiée avec ajout, édition, duplication, suppression et activation.
- ✅ Réordonnancement clavier dans la page clics (`Shift + Haut/Bas`).
- ✅ Capture guidée des coordonnées du prochain clic gauche via bouton dédié.
- ✅ Modèles métier créés et testés à la compilation :
  - `Action` : interface générique.
  - `ClickAction` : clic souris (type, position, délai, activable).
  - `KeyAction` : touche clavier (code, durée, délai, activable).
  - `ClickType` : énumération LEFT/RIGHT/MIDDLE.
  - `Sequence` : conteneur d'actions (cycles, durée totale, délai initial, gestion d'ordre).
- ✅ Build avec Gradle en 100% succès.

### Décisions techniques actées

- Plugin `groovy` activé dans `build.gradle`.
- Dépendances JavaFX ajoutées pour Windows via Gradle.
- Cible JVM maintenue en Java 21 (toolchain Gradle) pour stabilité.
- Utilisation de `@CompileStatic` sur domaine + entrypoint.
- JavaFX choisi pour la page principale et la base d'interface.
- Les pages UI sont découplées pour permettre une evolution independante.
- L'onglet `clicks` est maintenant centré sur la gestion des `ClickAction`.
- Le lancement réel de routine reste à brancher sur un moteur dédié (`SequenceExecutor`).

### Prochain lot technique (sprint 2)

1. Créer `SequenceExecutor` pour exécuter les séquences.
2. Implémenter les hooks de clic/touche avec `java.awt.Robot`.
3. Brancher la page de gestion de clics sur une logique métier/service dédié.
4. Ajouter tests Spock Groovy sur la logique métier.
5. Implémenter la persistance JSON (save/load profils).


