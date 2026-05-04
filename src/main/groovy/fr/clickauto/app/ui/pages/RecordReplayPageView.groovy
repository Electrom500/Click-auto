package fr.clickauto.app.ui.pages

import fr.clickauto.engine.SequenceExecutor
import fr.clickauto.model.ClickAction
import fr.clickauto.model.ClickType
import fr.clickauto.model.Sequence
import groovy.transform.CompileStatic
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import javafx.scene.text.FontWeight

import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.NativeHookException
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent
import com.github.kwhat.jnativehook.mouse.NativeMouseListener

import java.util.function.Consumer

@CompileStatic
final class RecordReplayPageView {
    private final Consumer<String> statusUpdater
    private final Sequence recordedSequence = new Sequence('Sequence enregistrée')
    private final ObservableList<String> recordedItems = FXCollections.observableArrayList()

    private SequenceExecutor executor
    private NativeMouseListener mouseListener
    private boolean recording = false
    private boolean hookRegistered = false
    private long lastEventTime = 0L

    private Node rootNode

    RecordReplayPageView(Consumer<String> statusUpdater) {
        this.statusUpdater = statusUpdater
    }

    Node getView() {
        if (rootNode == null) {
            rootNode = createPage()
        }
        return rootNode
    }

    private Node createPage() {
        executor = new SequenceExecutor(statusUpdater, { Integer idx -> } as Consumer<Integer>)

        Label title = new Label('Record and replay')
        title.setFont(Font.font('System', FontWeight.BOLD, 18))

        Label subtitle = new Label('Enregistre les clics globaux puis rejoue la séquence.')
        subtitle.setStyle('-fx-text-fill: #6b7280;')

        CheckBox recordClicksCheck = new CheckBox('Enregistrer les clics')
        recordClicksCheck.setSelected(true)

        CheckBox recordKeysCheck = new CheckBox('Enregistrer les touches (bientôt)')
        recordKeysCheck.setDisable(true)

        Button startRecord = new Button('Démarrer enregistrement')
        Button stopRecord = new Button('Arrêter enregistrement')
        Button clearRecord = new Button('Effacer')
        Button replayButton = new Button('Rejouer')
        Button stopReplay = new Button('Stop')

        stopRecord.setDisable(true)
        replayButton.setDisable(true)
        stopReplay.setDisable(true)

        startRecord.setOnAction({ ActionEvent ignored ->
            if (!recordClicksCheck.isSelected()) {
                statusUpdater.accept('Active au moins les clics pour enregistrer')
                return
            }
            startRecording()
            startRecord.setDisable(true)
            stopRecord.setDisable(false)
            replayButton.setDisable(true)
        } as EventHandler<ActionEvent>)

        stopRecord.setOnAction({ ActionEvent ignored ->
            stopRecording()
            startRecord.setDisable(false)
            stopRecord.setDisable(true)
            replayButton.setDisable(recordedSequence.getActions().isEmpty())
        } as EventHandler<ActionEvent>)

        clearRecord.setOnAction({ ActionEvent ignored ->
            recordedSequence.clearActions()
            recordedItems.clear()
            replayButton.setDisable(true)
            statusUpdater.accept('Enregistrement effacé')
        } as EventHandler<ActionEvent>)

        replayButton.setOnAction({ ActionEvent ignored ->
            if (recordedSequence.getActions().isEmpty()) {
                statusUpdater.accept('Aucun élément à rejouer')
                return
            }
            executor.start(recordedSequence)
            replayButton.setDisable(true)
            stopReplay.setDisable(false)
        } as EventHandler<ActionEvent>)

        stopReplay.setOnAction({ ActionEvent ignored ->
            executor.stop()
            replayButton.setDisable(false)
            stopReplay.setDisable(true)
        } as EventHandler<ActionEvent>)

        HBox controls = new HBox(10, startRecord, stopRecord, clearRecord, replayButton, stopReplay)
        HBox options = new HBox(12, recordClicksCheck, recordKeysCheck)

        ListView<String> listView = new ListView<>(recordedItems)
        listView.setPlaceholder(new Label('Aucune action enregistrée'))

        VBox card = new VBox(10, title, subtitle, options, controls, listView)
        card.setPadding(new Insets(16))
        card.setStyle('-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 8; -fx-background-radius: 8;')

        BorderPane root = new BorderPane(card)
        BorderPane.setMargin(card, new Insets(8, 0, 0, 0))
        return root
    }

    private void startRecording() {
        if (recording) {
            return
        }
        recordedSequence.clearActions()
        recordedItems.clear()
        lastEventTime = System.currentTimeMillis()

        mouseListener = new NativeMouseListener() {
            @Override
            void nativeMouseClicked(NativeMouseEvent event) {
                long now = System.currentTimeMillis()
                long delay = Math.max(0L, now - lastEventTime)
                lastEventTime = now

                ClickType type = ClickType.LEFT
                if (event.getButton() == NativeMouseEvent.BUTTON2) {
                    type = ClickType.MIDDLE
                } else if (event.getButton() == NativeMouseEvent.BUTTON3) {
                    type = ClickType.RIGHT
                }

                ClickAction action = new ClickAction(type, delay, event.getX(), event.getY())
                recordedSequence.addAction(action)
                Platform.runLater {
                    recordedItems.add(type.name() + ' @ (' + event.getX() + ', ' + event.getY() + ') +' + delay + 'ms')
                }
            }

            @Override void nativeMousePressed(NativeMouseEvent event) {}
            @Override void nativeMouseReleased(NativeMouseEvent event) {}
        }

        try {
            if (!hookRegistered) {
                try {
                    GlobalScreen.registerNativeHook()
                    hookRegistered = true
                } catch (NativeHookException ignored) {
                    // If already registered elsewhere, continue and try to add the listener.
                }
            }
            GlobalScreen.addNativeMouseListener(mouseListener)
            recording = true
            statusUpdater.accept('Enregistrement en cours...')
        } catch (Exception ex) {
            statusUpdater.accept('Impossible de démarrer l\'enregistrement: ' + ex.getMessage())
        }
    }

    private void stopRecording() {
        if (!recording) {
            return
        }
        try {
            if (mouseListener != null) {
                GlobalScreen.removeNativeMouseListener(mouseListener)
            }
        } catch (Exception ignored) {}
        recording = false
        statusUpdater.accept('Enregistrement terminé')
    }
}
