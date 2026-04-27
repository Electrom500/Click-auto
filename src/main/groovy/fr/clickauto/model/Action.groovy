package fr.clickauto.model

import groovy.transform.CompileStatic

/**
 * Interface de base pour une action dans une séquence.
 * Peut être un clic, une touche clavier, etc.
 */
@CompileStatic
interface Action {
    /**
     * Retourne la description de l'action.
     */
    String getDescription()

    /**
     * Retourne le délai (en ms) avant l'exécution de cette action.
     */
    long getDelayMs()

    /**
     * Retourne le type d'action pour identifier sa catégorie.
     */
    String getActionType()
}

