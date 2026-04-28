package fr.clickauto.model

import groovy.transform.CompileStatic
import groovy.transform.ToString

/**
 * Représente une action de clic souris dans une séquence.
 */
@CompileStatic
@ToString(includes = 'clickType,delayMs,x,y')
final class ClickAction implements Action {
    private final ClickType clickType
    private final long delayMs
    private final int x
    private final int y
    private boolean enabled

    /**
     * Crée une action de clic.
     *
     * @param clickType type de clic (LEFT, RIGHT, MIDDLE)
     * @param delayMs délai en millisecondes avant ce clic
     * @param x coordonnée X absolue du clic
     * @param y coordonnée Y absolue du clic
     */
    ClickAction(ClickType clickType, long delayMs, int x, int y) {
        this.clickType = clickType
        this.delayMs = delayMs
        this.x = x
        this.y = y
        this.enabled = true
    }

    ClickType getClickType() {
        return clickType
    }

    @Override
    long getDelayMs() {
        return delayMs
    }

    int getX() {
        return x
    }

    int getY() {
        return y
    }

    boolean isEnabled() {
        return enabled
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled
    }

    @Override
    String getDescription() {
        return "${clickType.label} à (${x}, ${y})"
    }

    @Override
    String getActionType() {
        return 'CLICK'
    }
}

