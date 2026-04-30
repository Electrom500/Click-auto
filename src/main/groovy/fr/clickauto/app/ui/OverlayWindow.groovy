package fr.clickauto.app.ui

import fr.clickauto.engine.SequenceExecutor
import fr.clickauto.model.Sequence
import groovy.transform.CompileStatic
import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.HBox
import javafx.scene.paint.Color
import javafx.stage.Stage
import javafx.stage.StageStyle

/**
 * Petite fenêtre flottante toujours au dessus pour contrôler l'exécution.
 * Draggable. Ne gère pas le "click-through" natif (pour ça il faut JNA/JNativeHook).
 */
@CompileStatic
final class OverlayWindow {
    private final Stage stage
    private final SequenceExecutor executor
    private final Label statusLabel = new Label('Idle')

    private double dragOffsetX = 0
    private double dragOffsetY = 0

    OverlayWindow(SequenceExecutor executor, Sequence sequence) {
        this.executor = executor
        stage = new Stage(StageStyle.TRANSPARENT)
        stage.setAlwaysOnTop(true)

        Button start = new Button('▶')
        Button pause = new Button('⏸')
        Button stop = new Button('■')
        start.setOnAction({ ActionEvent e ->
            if (executor != null) executor.start(sequence)
            updateStatus()
        } as EventHandler<ActionEvent>)
        pause.setOnAction({ ActionEvent e ->
            if (executor != null) {
                if (executor.getState() == SequenceExecutor.State.RUNNING) executor.pause() else if (executor.getState() == SequenceExecutor.State.PAUSED) executor.resume()
            }
            updateStatus()
        } as EventHandler<ActionEvent>)
        stop.setOnAction({ ActionEvent e ->
            if (executor != null) executor.stop()
            updateStatus()
        } as EventHandler<ActionEvent>)

        statusLabel.setStyle('-fx-text-fill: white; -fx-font-size: 11;')
        HBox root = new HBox(8, start, pause, stop, statusLabel)
        root.setPrefSize(240, 48)
        stage.setWidth(240)
        stage.setHeight(48)
        root.setPadding(new Insets(8))
        root.setBackground(new Background(new BackgroundFill(Color.rgb(30,30,30, 0.75 as double), new CornerRadii(8), Insets.EMPTY)))

        // drag support
        root.setOnMousePressed { event ->
            dragOffsetX = event.getSceneX()
            dragOffsetY = event.getSceneY()
        }
        root.setOnMouseDragged { event ->
            stage.setX(event.getScreenX() - dragOffsetX)
            stage.setY(event.getScreenY() - dragOffsetY)
        }

        Scene scene = new Scene(root)
        scene.setFill(Color.TRANSPARENT)
        // local hotkeys on overlay: F8 start, F7 pause/resume, F9 stop
        scene.setOnKeyPressed({ KeyEvent event ->
            if (executor == null) return
            if (event.getCode() == KeyCode.F8) {
                executor.start(sequence)
                updateStatus()
            } else if (event.getCode() == KeyCode.F9) {
                executor.stop()
                updateStatus()
            } else if (event.getCode() == KeyCode.F7) {
                if (executor.getState() == SequenceExecutor.State.RUNNING) {
                    executor.pause()
                } else if (executor.getState() == SequenceExecutor.State.PAUSED) {
                    executor.resume()
                }
                updateStatus()
            }
        } as EventHandler<KeyEvent>)
        stage.setScene(scene)
    }

    void show() {
        if (!stage.isShowing()) {
            Platform.runLater { stage.show(); updateStatus() }
        }
    }

    void hide() {
        if (stage.isShowing()) {
            Platform.runLater { stage.hide() }
        }
    }

    void toggle() {
        if (stage.isShowing()) hide() else show()
    }

    void updateStatus() {
        if (executor == null) return
        def s = executor.getState().toString()
        Platform.runLater { statusLabel.setText(s) }
    }
}

