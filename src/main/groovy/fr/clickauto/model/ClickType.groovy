package fr.clickauto.model

import groovy.transform.CompileStatic
import groovy.transform.ToString

/**
 * Enum des types de clics souris.
 */
@CompileStatic
enum ClickType {
    LEFT('Clic gauche'),
    RIGHT('Clic droit'),
    MIDDLE('Clic molette')

    final String label

    ClickType(String label) {
        this.label = label
    }
}

