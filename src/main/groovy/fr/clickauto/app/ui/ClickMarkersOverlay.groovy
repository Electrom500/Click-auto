package fr.clickauto.app.ui

import fr.clickauto.model.ClickAction
import groovy.transform.CompileStatic
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Rectangle2D
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.stage.Screen
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.util.Duration

/**
 * Transparent overlay to preview click markers on screen.
 */
@CompileStatic
final class ClickMarkersOverlay {
    private Stage stage
    private Timeline hideTimeline

    void show(List<ClickAction> clicks, long durationMs) {
        if (clicks == null || clicks.isEmpty()) {
            return
        }
        Platform.runLater {
            ensureStage()
            Pane root = new Pane()
            root.setMouseTransparent(true)

            int index = 1
            for (ClickAction click : clicks) {
                Circle circle = new Circle(12, Color.rgb(239, 68, 68, 0.8))
                circle.setStroke(Color.WHITE)
                circle.setStrokeWidth(2)

                Label label = new Label(Integer.toString(index++))
                label.setTextFill(Color.WHITE)
                label.setStyle('-fx-font-weight: bold; -fx-font-size: 11;')

                Pane marker = new Pane(circle, label)
                marker.setLayoutX(click.getX() - 12)
                marker.setLayoutY(click.getY() - 12)
                label.setLayoutX(7)
                label.setLayoutY(4)

                root.getChildren().add(marker)
            }

            Scene scene = new Scene(root, stage.getWidth(), stage.getHeight(), Color.TRANSPARENT)
            scene.setFill(Color.TRANSPARENT)
            stage.setScene(scene)
            stage.show()
            WindowsClickThroughSupport.apply(stage, true)

            if (hideTimeline != null) {
                hideTimeline.stop()
            }
            hideTimeline = new Timeline(new KeyFrame(Duration.millis(durationMs), { ActionEvent e -> stage.hide() } as EventHandler<ActionEvent>))
            hideTimeline.setCycleCount(1)
            hideTimeline.play()
        }
    }

    private void ensureStage() {
        if (stage != null) {
            return
        }
        Rectangle2D bounds = Screen.getPrimary().getBounds()
        stage = new Stage(StageStyle.TRANSPARENT)
        stage.setAlwaysOnTop(true)
        stage.setX(bounds.getMinX())
        stage.setY(bounds.getMinY())
        stage.setWidth(bounds.getWidth())
        stage.setHeight(bounds.getHeight())
    }
}


