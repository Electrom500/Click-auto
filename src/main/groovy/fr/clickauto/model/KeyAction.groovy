package fr.clickauto.model

import groovy.transform.CompileStatic
import groovy.transform.ToString

/**
 * Représente une action de touche clavier dans une séquence.
 */
@CompileStatic
@ToString(includes = 'keyCode,durationMs,delayMs')
final class KeyAction implements Action {
    private final int keyCode
    private final long durationMs
    private final long delayMs
    private boolean enabled

    /**
     * Crée une action de touche clavier.
     *
     * @param keyCode code de la touche (ex: KeyEvent.VK_A)
     * @param durationMs durée du maintien de la touche en ms
     * @param delayMs délai avant cette action en ms
     */
    KeyAction(int keyCode, long durationMs, long delayMs) {
        this.keyCode = keyCode
        this.durationMs = durationMs
        this.delayMs = delayMs
        this.enabled = true
    }

    int getKeyCode() {
        return keyCode
    }

    long getDurationMs() {
        return durationMs
    }

    @Override
    long getDelayMs() {
        return delayMs
    }

    boolean isEnabled() {
        return enabled
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled
    }

    @Override
    String getDescription() {
        return "Touche 0x${Integer.toHexString(keyCode)} (${durationMs}ms)"
    }

    @Override
    String getActionType() {
        return 'KEY'
    }
}

