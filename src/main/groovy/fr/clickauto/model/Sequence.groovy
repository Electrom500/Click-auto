package fr.clickauto.model

import groovy.transform.CompileStatic
import groovy.transform.ToString

/**
 * Représente une séquence configurable d'actions (clics et touches).
 */
@CompileStatic
@ToString(includes = 'name,actions,cycles,totalDurationMs')
final class Sequence {
    private String name
    private final List<Action> actions
    private int cycles
    private long totalDurationMs
    private long initialDelayMs

    /**
     * Crée une nouvelle séquence.
     *
     * @param name nom de la séquence
     */
    Sequence(String name) {
        this.name = name
        this.actions = new ArrayList<>()
        this.cycles = 1
        this.totalDurationMs = 0
        this.initialDelayMs = 0
    }

    String getName() {
        return name
    }

    void setName(String name) {
        if (name != null && !name.trim().isEmpty()) {
            this.name = name.trim()
        }
    }

    List<Action> getActions() {
        return new ArrayList<>(actions)
    }

    void addAction(Action action) {
        if (action != null) {
            actions.add(action)
        }
    }

    void clearActions() {
        actions.clear()
    }

    void removeAction(int index) {
        if (index >= 0 && index < actions.size()) {
            actions.remove(index)
        }
    }

    void insertAction(int index, Action action) {
        if (action != null && index >= 0 && index <= actions.size()) {
            actions.add(index, action)
        }
    }

    int getCycles() {
        return cycles
    }

    void setCycles(int cycles) {
        // Allow 0 to mean "infinite" repetition. Keep positive values as-is.
        this.cycles = Math.max(0, cycles)
    }

    long getTotalDurationMs() {
        return totalDurationMs
    }

    void setTotalDurationMs(long totalDurationMs) {
        this.totalDurationMs = Math.max(0, totalDurationMs)
    }

    long getInitialDelayMs() {
        return initialDelayMs
    }

    void setInitialDelayMs(long initialDelayMs) {
        this.initialDelayMs = Math.max(0, initialDelayMs)
    }

    /**
     * Retourne le nombre d'actions dans cette séquence.
     */
    int getActionCount() {
        return actions.size()
    }

    /**
     * Retourne la durée totale estimée pour un cycle (en ms).
     */
    long calculateCycleDurationMs() {
        if (actions.isEmpty()) {
            return 0
        }
        return actions.stream()
                .mapToLong { Action action -> action.getDelayMs() }
                .max()
                .orElse(0)
    }
}
