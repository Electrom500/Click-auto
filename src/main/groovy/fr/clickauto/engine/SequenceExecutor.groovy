package fr.clickauto.engine

import fr.clickauto.model.Action
import fr.clickauto.model.ClickAction
import fr.clickauto.model.ClickType
import fr.clickauto.model.KeyAction
import fr.clickauto.model.Sequence
import groovy.transform.CompileStatic
import javafx.application.Platform

import java.awt.Robot
import java.awt.AWTException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer
import static java.awt.event.InputEvent.BUTTON1_DOWN_MASK
import static java.awt.event.InputEvent.BUTTON2_DOWN_MASK
import static java.awt.event.InputEvent.BUTTON3_DOWN_MASK

/**
 * Moteur d'exécution d'une Sequence. Exécute ClickAction et KeyAction via java.awt.Robot.
 * Supporte start / pause / resume / stop et notifie l'UI via callbacks.
 */
@CompileStatic
final class SequenceExecutor {
    enum State { IDLE, RUNNING, PAUSED, STOPPED }

    private volatile State state = State.IDLE
    private final Consumer<String> statusUpdater
    private final Consumer<Integer> actionHighlighter // index de l'action courante
    private Thread worker
    private Robot robot
    private final AtomicBoolean stopRequested = new AtomicBoolean(false)

    SequenceExecutor(Consumer<String> statusUpdater, Consumer<Integer> actionHighlighter) {
        this.statusUpdater = statusUpdater
        this.actionHighlighter = actionHighlighter
        try {
            this.robot = new Robot()
        } catch (AWTException e) {
            this.robot = null
            notifyStatus('Impossible d\'initialiser Robot: ' + e.getMessage())
        }
    }

    private void notifyStatus(String message) {
        if (Platform.isFxApplicationThread()) {
            statusUpdater.accept(message)
        } else {
            Platform.runLater({ -> statusUpdater.accept(message) } as Runnable)
        }
    }

    private void clearHighlight() {
        try {
            actionHighlighter.accept(-1)
        } catch (Exception ignored) {
            // ignore UI highlight cleanup failures
        }
    }

    State getState() {
        return state
    }

    void start(Sequence sequence) {
        if (state == State.RUNNING) {
            notifyStatus('Sequence deja en cours')
            return
        }
        stopRequested.set(false)
        worker = new Thread({ ->
            runSequence(sequence)
        } as Runnable)
        worker.setDaemon(true)
        state = State.RUNNING
        notifyStatus('Execution demarree')
        worker.start()
    }

    void pause() {
        if (state != State.RUNNING) {
            return
        }
        state = State.PAUSED
        notifyStatus('Execution en pause')
    }

    void resume() {
        if (state != State.PAUSED) {
            return
        }
        state = State.RUNNING
        notifyStatus('Execution reprise')
    }

    void stop() {
        stopRequested.set(true)
        state = State.STOPPED
        notifyStatus('Arret demande')
        if (worker != null) {
            worker.interrupt()
        }
    }

    private void runSequence(Sequence sequence) {
        long startTime = System.currentTimeMillis()
        try {
            // initial delay
            long initial = sequence.getInitialDelayMs()
            if (initial > 0) {
                sleepInterruptibly(initial)
                if (stopRequested.get()) return
            }

            long totalDuration = sequence.getTotalDurationMs()
            boolean useTotalDuration = totalDuration > 0

            int cycles = sequence.getCycles()
            boolean infinite = cycles == 0

            int performedCycles = 0
            while (!stopRequested.get() && (infinite || performedCycles < cycles)) {
                if (useTotalDuration && (System.currentTimeMillis() - startTime) >= totalDuration) {
                    break
                }
                List<Action> actions = sequence.getActions()
                for (int i = 0; i < actions.size(); i++) {
                    if (stopRequested.get()) break
                    // handle pause
                    while (state == State.PAUSED && !stopRequested.get()) {
                        Thread.sleep(50)
                    }
                    if (stopRequested.get()) break

                    Action action = actions.get(i)
                    if (action == null) continue
                    // highlight in UI
                    try {
                        actionHighlighter.accept(i)
                    } catch (Exception ignored) {}

                    if (action instanceof ClickAction) {
                        ClickAction ca = (ClickAction) action
                        if (ca.isEnabled()) {
                            doClick(ca)
                        }
                        sleepInterruptibly(ca.getDelayMs())
                    } else if (action instanceof KeyAction) {
                        KeyAction ka = (KeyAction) action
                        if (ka.isEnabled()) {
                            doKey(ka)
                        }
                        sleepInterruptibly(ka.getDelayMs())
                    } else {
                        // unknown action: respect its delay
                        sleepInterruptibly(action.getDelayMs())
                    }
                }
                performedCycles++
            }
        } catch (InterruptedException ignored) {
            // thread interrupted -> stop
        } catch (Exception ex) {
            notifyStatus('Erreur lors de l\'execution: ' + ex.getMessage())
        } finally {
            state = stopRequested.get() ? State.STOPPED : State.IDLE
            notifyStatus(state == State.STOPPED ? 'Execution arrete' : 'Execution terminee')
            clearHighlight()
        }
    }

    private void doClick(ClickAction ca) {
        if (robot == null) return
        int x = ca.getX()
        int y = ca.getY()
        robot.mouseMove(x, y)
        int mask = BUTTON1_DOWN_MASK
        switch (ca.getClickType()) {
            case RIGHT:
                mask = BUTTON3_DOWN_MASK
                break
            case MIDDLE:
                mask = BUTTON2_DOWN_MASK
                break
            default:
                mask = BUTTON1_DOWN_MASK
        }
        robot.mousePress(mask)
        robot.mouseRelease(mask)
    }

    private void doKey(KeyAction ka) {
        if (robot == null) return
        int key = ka.getKeyCode()
        robot.keyPress(key)
        sleepInterruptibly(ka.getDurationMs())
        robot.keyRelease(key)
    }

    private void sleepInterruptibly(long ms) throws InterruptedException {
        long remaining = ms
        long end = System.currentTimeMillis() + ms
        while (remaining > 0 && !stopRequested.get()) {
            long toSleep = Math.min(remaining, 200)
            Thread.sleep(toSleep)
            remaining = end - System.currentTimeMillis()
        }
    }
}

