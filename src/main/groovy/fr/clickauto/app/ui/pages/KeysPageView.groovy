package fr.clickauto.app.ui.pages

import fr.clickauto.engine.SequenceExecutor
import fr.clickauto.model.KeyAction
import fr.clickauto.model.Sequence
import groovy.transform.CompileStatic
import javafx.application.Platform
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.ChoiceBox
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.Separator
import javafx.scene.control.TextField
import javafx.scene.control.ToolBar
import javafx.scene.input.ClipboardContent
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.TransferMode
import javafx.scene.layout.BorderPane
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.util.Callback

import java.awt.event.KeyEvent as AwtKeyEvent
import java.util.function.Consumer

@CompileStatic
final class KeysPageView {
    private final Consumer<String> statusUpdater
    private final Sequence sequence = new Sequence('Sequence touches clavier')
    private final ObservableList<KeyAction> keys = FXCollections.observableArrayList()

    private final Label sequenceNameValue = new Label(sequence.getName())
    private final Label keyCountValue = new Label('0')
    private final Label enabledCountValue = new Label('0')
    private final Label selectedActionLabel = new Label('Aucune touche sélectionnée')

    private final TextField keyCodeField = new TextField()
    private final TextField durationField = new TextField()
    private final TextField delayField = new TextField()
    private final CheckBox enabledCheckBox = new CheckBox('Touche active')
    private final Button captureKeyButton = new Button('Capturer touche')

    private final ChoiceBox<String> sequenceModeChoiceBox = new ChoiceBox<>(FXCollections.observableArrayList('One shot', 'Boucle (infini)', 'Par cycles', 'Par duree (ms)'))
    private final TextField seqCyclesField = new TextField()
    private final TextField seqDurationField = new TextField()
    private final TextField seqInitialDelayField = new TextField()

    private Node rootNode
    private ListView<KeyAction> keyList
    private boolean captureKeyPending = false

    private SequenceExecutor executor
    private Button startExecButton
    private Button pauseExecButton
    private Button stopExecButton

    KeysPageView(Consumer<String> statusUpdater) {
        this.statusUpdater = statusUpdater
    }

    Node getView() {
        if (rootNode == null) {
            rootNode = createPage()
        }
        return rootNode
    }

    private Node createPage() {
        keyList = createKeyList()
        executor = new SequenceExecutor(statusUpdater, { Integer idx ->
            Platform.runLater { if (keyList != null) {
                if (idx >= 0 && idx < keys.size()) {
                    keyList.getSelectionModel().select(idx)
                } else {
                    keyList.getSelectionModel().clearSelection()
                }
            } }
        } as Consumer<Integer>)

        BorderPane page = new BorderPane()
        page.setTop(createToolbar())
        page.setCenter(createCenterPane())
        page.setRight(createEditorPane())

        page.addEventFilter(KeyEvent.KEY_PRESSED, { KeyEvent event ->
            if (captureKeyPending) {
                handleCapturedKey(event)
                event.consume()
            }
        } as EventHandler<KeyEvent>)

        BorderPane.setMargin(page.getCenter(), new Insets(0, 12, 0, 0))
        BorderPane.setMargin(page.getRight(), new Insets(0, 0, 0, 12))
        return page
    }

    private Node createToolbar() {
        Button addButton = new Button('Ajouter')
        Button duplicateButton = new Button('Dupliquer')
        Button deleteButton = new Button('Supprimer')
        Button toggleButton = new Button('Activer / désactiver')
        Button upButton = new Button('Monter')
        Button downButton = new Button('Descendre')

        addButton.setOnAction({ ActionEvent ignored -> addKeyFromEditor() } as EventHandler<ActionEvent>)
        duplicateButton.setOnAction({ ActionEvent ignored -> duplicateSelectedKey() } as EventHandler<ActionEvent>)
        deleteButton.setOnAction({ ActionEvent ignored -> deleteSelectedKey() } as EventHandler<ActionEvent>)
        toggleButton.setOnAction({ ActionEvent ignored -> toggleSelectedKey() } as EventHandler<ActionEvent>)
        upButton.setOnAction({ ActionEvent ignored -> moveSelectedKey(-1) } as EventHandler<ActionEvent>)
        downButton.setOnAction({ ActionEvent ignored -> moveSelectedKey(1) } as EventHandler<ActionEvent>)

        return new ToolBar(addButton, duplicateButton, deleteButton, toggleButton, upButton, downButton)
    }

    private Node createCenterPane() {
        Label title = new Label('Gestion des touches clavier')
        title.setFont(Font.font('System', FontWeight.BOLD, 16))

        Label subtitle = new Label('Créer, dupliquer, désactiver et réordonner les touches clavier.')
        subtitle.setStyle('-fx-text-fill: #6b7280;')

        VBox header = new VBox(4, title, subtitle)

        GridPane summary = new GridPane()
        summary.setHgap(10)
        summary.setVgap(8)
        summary.setPadding(new Insets(12))
        summary.setStyle('-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 8; -fx-background-radius: 8;')
        summary.getColumnConstraints().addAll(new ColumnConstraints(120), new ColumnConstraints(200))
        addRow(summary, 0, 'Sequence', sequenceNameValue)
        addRow(summary, 1, 'Total touches', keyCountValue)
        addRow(summary, 2, 'Touches actives', enabledCountValue)

        StackPane listWrapper = new StackPane(keyList)
        listWrapper.setPadding(new Insets(12))
        listWrapper.setStyle('-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 8; -fx-background-radius: 8;')

        VBox center = new VBox(12, header, summary, listWrapper)
        VBox.setVgrow(listWrapper, Priority.ALWAYS)
        return center
    }

    private Node createEditorPane() {
        Label title = new Label('Proprietes de la touche')
        title.setFont(Font.font('System', FontWeight.BOLD, 16))

        GridPane grid = new GridPane()
        grid.setHgap(10)
        grid.setVgap(10)
        grid.setPadding(new Insets(12))
        grid.setStyle('-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 8; -fx-background-radius: 8;')
        grid.getColumnConstraints().addAll(new ColumnConstraints(120), new ColumnConstraints(220))

        keyCodeField.setPromptText('KeyCode AWT')
        durationField.setPromptText('Duree en ms')
        delayField.setPromptText('Delai en ms')

        addRow(grid, 0, 'KeyCode', keyCodeField)
        addRow(grid, 1, 'Duree', durationField)
        addRow(grid, 2, 'Delai', delayField)
        grid.add(enabledCheckBox, 1, 3)
        captureKeyButton.setOnAction({ ActionEvent ignored -> startKeyCapture() } as EventHandler<ActionEvent>)
        grid.add(captureKeyButton, 1, 4)

        Button applyButton = new Button('Appliquer')
        applyButton.setOnAction({ ActionEvent ignored -> applyEditorToSelected() } as EventHandler<ActionEvent>)

        sequenceModeChoiceBox.getSelectionModel().select('One shot')
        seqCyclesField.setPromptText('Nombre de cycles')
        seqDurationField.setPromptText('Duree totale en ms')
        seqInitialDelayField.setPromptText('Delai initial en ms')

        GridPane execGrid = new GridPane()
        execGrid.setHgap(10)
        execGrid.setVgap(8)
        execGrid.getColumnConstraints().addAll(new ColumnConstraints(120), new ColumnConstraints(220))
        addRow(execGrid, 0, 'Mode sequence', sequenceModeChoiceBox)
        addRow(execGrid, 1, 'Cycles', seqCyclesField)
        addRow(execGrid, 2, 'Duree totale (ms)', seqDurationField)
        addRow(execGrid, 3, 'Delai initial (ms)', seqInitialDelayField)

        startExecButton = new Button('Démarrer')
        pauseExecButton = new Button('Pause')
        stopExecButton = new Button('Arrêter')
        pauseExecButton.setDisable(true)
        stopExecButton.setDisable(true)

        HBox execButtons = new HBox(8, startExecButton, pauseExecButton, stopExecButton)

        startExecButton.setOnAction({ ActionEvent ignored -> startExecution() } as EventHandler<ActionEvent>)
        pauseExecButton.setOnAction({ ActionEvent ignored -> togglePause() } as EventHandler<ActionEvent>)
        stopExecButton.setOnAction({ ActionEvent ignored -> stopExecution() } as EventHandler<ActionEvent>)

        selectedActionLabel.setWrapText(true)

        VBox editor = new VBox(10, title, grid, applyButton, new Separator(), execGrid, execButtons, new Separator(), selectedActionLabel)
        editor.setPrefWidth(380)
        return editor
    }

    private ListView<KeyAction> createKeyList() {
        ListView<KeyAction> listView = new ListView<>(keys)
        listView.setCellFactory(keyCellFactory())
        listView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<KeyAction>() {
            @Override
            void changed(ObservableValue<? extends KeyAction> observable, KeyAction oldValue, KeyAction newValue) {
                onSelectedKeyChanged(newValue)
            }
        })
        listView.setPlaceholder(new Label('Aucune touche dans la sequence.'))

        if (!keys.isEmpty()) {
            listView.getSelectionModel().select(0)
        }
        refreshCounters()
        return listView
    }

    private Callback<ListView<KeyAction>, ListCell<KeyAction>> keyCellFactory() {
        return { ListView<KeyAction> ignored ->
            ListCell<KeyAction> cell = new ListCell<KeyAction>() {
                @Override
                protected void updateItem(KeyAction item, boolean empty) {
                    super.updateItem(item, empty)
                    if (empty || item == null) {
                        setText(null)
                    } else {
                        String state = item.isEnabled() ? 'ON' : 'OFF'
                        setText("[${state}] 0x${Integer.toHexString(item.getKeyCode())} | duree ${item.getDurationMs()} ms | delai ${item.getDelayMs()} ms")
                    }
                }
            }
            wireDragAndDrop(cell)
            return cell
        }
    }

    private void wireDragAndDrop(ListCell<KeyAction> cell) {
        cell.setOnDragDetected { event ->
            if (cell.isEmpty()) {
                return
            }
            def dragboard = cell.startDragAndDrop(TransferMode.MOVE)
            ClipboardContent content = new ClipboardContent()
            content.putString(Integer.toString(cell.getIndex()))
            dragboard.setContent(content)
            event.consume()
        }

        cell.setOnDragOver { event ->
            if (event.getGestureSource() != cell && !cell.isEmpty()) {
                event.acceptTransferModes(TransferMode.MOVE)
            }
            event.consume()
        }

        cell.setOnDragDropped { event ->
            def dragboard = event.getDragboard()
            if (dragboard.hasString()) {
                int fromIndex = Integer.parseInt(dragboard.getString())
                int toIndex = cell.getIndex()
                moveKeyForDrag(fromIndex, toIndex)
                event.setDropCompleted(true)
            } else {
                event.setDropCompleted(false)
            }
            event.consume()
        }
    }

    private void onSelectedKeyChanged(KeyAction key) {
        if (key == null) {
            clearEditor()
            selectedActionLabel.setText('Aucune touche selectionnee')
            return
        }
        keyCodeField.setText(Integer.toString(key.getKeyCode()))
        durationField.setText(Long.toString(key.getDurationMs()))
        delayField.setText(Long.toString(key.getDelayMs()))
        enabledCheckBox.setSelected(key.isEnabled())
        selectedActionLabel.setText("Selection: ${key.getDescription()}".toString())
    }

    private void addKeyFromEditor() {
        KeyAction key = readEditorAsKey()
        if (key == null) {
            return
        }
        keys.add(key)
        sequence.addAction(key)
        keyList.refresh()
        keyList.getSelectionModel().select(keys.size() - 1)
        refreshCounters()
        statusUpdater.accept('Touche ajoutee')
    }

    private void duplicateSelectedKey() {
        int index = getSelectedIndex()
        if (index < 0 || index >= keys.size()) {
            statusUpdater.accept('Aucune touche selectionnee pour duplication')
            return
        }

        KeyAction source = keys.get(index)
        KeyAction duplicate = new KeyAction(source.getKeyCode(), source.getDurationMs(), source.getDelayMs())
        duplicate.setEnabled(source.isEnabled())
        keys.add(index + 1, duplicate)
        sequence.insertAction(index + 1, duplicate)
        keyList.refresh()
        keyList.getSelectionModel().select(index + 1)
        refreshCounters()
        statusUpdater.accept('Touche dupliquee')
    }

    private void deleteSelectedKey() {
        int index = getSelectedIndex()
        if (index < 0 || index >= keys.size()) {
            statusUpdater.accept('Aucune touche selectionnee pour suppression')
            return
        }

        keys.remove(index)
        sequence.removeAction(index)
        keyList.refresh()
        if (!keys.isEmpty()) {
            keyList.getSelectionModel().select(Math.min(index, keys.size() - 1))
        }
        refreshCounters()
        statusUpdater.accept('Touche supprimee')
    }

    private void toggleSelectedKey() {
        KeyAction selected = keyList == null ? null : keyList.getSelectionModel().getSelectedItem()
        if (selected == null) {
            statusUpdater.accept('Aucune touche selectionnee pour activation')
            return
        }

        selected.setEnabled(!selected.isEnabled())
        keyList.refresh()
        refreshCounters()
        statusUpdater.accept(selected.isEnabled() ? 'Touche activee' : 'Touche desactivee')
    }

    private void moveSelectedKey(int delta) {
        int index = getSelectedIndex()
        int target = index + delta
        if (index < 0 || target < 0 || target >= keys.size()) {
            statusUpdater.accept('Deplacement impossible')
            return
        }

        KeyAction item = keys.remove(index)
        keys.add(target, item)
        sequence.removeAction(index)
        sequence.insertAction(target, item)
        keyList.refresh()
        keyList.getSelectionModel().select(target)
        refreshCounters()
        statusUpdater.accept(delta < 0 ? 'Touche montee' : 'Touche descendue')
    }

    private void moveKeyForDrag(int fromIndex, int toIndex) {
        if (fromIndex == toIndex) {
            return
        }
        if (fromIndex < 0 || fromIndex >= keys.size() || toIndex < 0 || toIndex >= keys.size()) {
            return
        }
        KeyAction item = keys.remove(fromIndex)
        keys.add(toIndex, item)
        sequence.removeAction(fromIndex)
        sequence.insertAction(toIndex, item)
        keyList.refresh()
        keyList.getSelectionModel().select(toIndex)
        refreshCounters()
        statusUpdater.accept('Touche réordonnée')
    }

    private void applyEditorToSelected() {
        int index = getSelectedIndex()
        if (index < 0 || index >= keys.size()) {
            statusUpdater.accept('Selectionne une touche pour appliquer les modifications')
            return
        }

        KeyAction updated = readEditorAsKey()
        if (updated == null) {
            return
        }
        updated.setEnabled(enabledCheckBox.isSelected())

        keys.set(index, updated)
        sequence.removeAction(index)
        sequence.insertAction(index, updated)
        keyList.refresh()
        keyList.getSelectionModel().select(index)
        refreshCounters()
        statusUpdater.accept('Touche mise a jour')
    }

    private KeyAction readEditorAsKey() {
        try {
            int keyCode = Integer.parseInt(keyCodeField.getText().trim())
            long duration = Long.parseLong(durationField.getText().trim())
            long delay = Long.parseLong(delayField.getText().trim())

            KeyAction key = new KeyAction(keyCode, duration, delay)
            key.setEnabled(enabledCheckBox.isSelected())
            return key
        } catch (Exception ex) {
            statusUpdater.accept('Champs invalides pour la touche: verifier keyCode, duree et delai')
            return null
        }
    }

    private void clearEditor() {
        keyCodeField.clear()
        durationField.clear()
        delayField.clear()
        enabledCheckBox.setSelected(true)
    }

    private void refreshCounters() {
        keyCountValue.setText(Integer.toString(keys.size()))
        int enabledCount = 0
        for (KeyAction key : keys) {
            if (key.isEnabled()) {
                enabledCount++
            }
        }
        enabledCountValue.setText(Integer.toString(enabledCount))
    }

    private int getSelectedIndex() {
        if (keyList == null) {
            return -1
        }
        return keyList.getSelectionModel().getSelectedIndex()
    }

    private void startKeyCapture() {
        captureKeyPending = true
        statusUpdater.accept('Capture active: appuie sur une touche')
    }

    private void handleCapturedKey(KeyEvent event) {
        int awtCode = mapKeyCodeToAwt(event.getCode())
        if (awtCode == AwtKeyEvent.VK_UNDEFINED) {
            statusUpdater.accept('Touche non supportee pour capture')
            captureKeyPending = false
            return
        }
        keyCodeField.setText(Integer.toString(awtCode))
        captureKeyPending = false
        statusUpdater.accept('Touche capturee: 0x' + Integer.toHexString(awtCode))
    }

    private static int mapKeyCodeToAwt(KeyCode code) {
        if (code == null) {
            return AwtKeyEvent.VK_UNDEFINED
        }
        if (code.isLetterKey()) {
            String name = code.getName().toUpperCase()
            if (name.length() == 1) {
                char c = name.charAt(0)
                return (int) c
            }
        }
        if (code.isDigitKey()) {
            String name = code.getName()
            if (name.length() == 1) {
                return (int) name.charAt(0)
            }
        }
        switch (code) {
            case KeyCode.ENTER: return AwtKeyEvent.VK_ENTER
            case KeyCode.SPACE: return AwtKeyEvent.VK_SPACE
            case KeyCode.ESCAPE: return AwtKeyEvent.VK_ESCAPE
            case KeyCode.TAB: return AwtKeyEvent.VK_TAB
            case KeyCode.BACK_SPACE: return AwtKeyEvent.VK_BACK_SPACE
            case KeyCode.DELETE: return AwtKeyEvent.VK_DELETE
            case KeyCode.UP: return AwtKeyEvent.VK_UP
            case KeyCode.DOWN: return AwtKeyEvent.VK_DOWN
            case KeyCode.LEFT: return AwtKeyEvent.VK_LEFT
            case KeyCode.RIGHT: return AwtKeyEvent.VK_RIGHT
            default:
                return AwtKeyEvent.VK_UNDEFINED
        }
    }

    private void startExecution() {
        applySequenceSettingsToModel()
        if (sequence.getCycles() == 0) {
            statusUpdater.accept('Mode infini: utilise F9 pour arreter')
        }
        if (executor == null) {
            statusUpdater.accept('Moteur indisponible')
            return
        }
        executor.start(sequence)
        startExecButton.setDisable(true)
        pauseExecButton.setDisable(false)
        stopExecButton.setDisable(false)
    }

    private void togglePause() {
        if (executor == null) {
            return
        }
        if (executor.getState() == SequenceExecutor.State.RUNNING) {
            executor.pause()
            pauseExecButton.setText('Reprendre')
        } else if (executor.getState() == SequenceExecutor.State.PAUSED) {
            executor.resume()
            pauseExecButton.setText('Pause')
        }
    }

    private void stopExecution() {
        if (executor == null) {
            return
        }
        executor.stop()
        startExecButton.setDisable(false)
        pauseExecButton.setDisable(true)
        pauseExecButton.setText('Pause')
        stopExecButton.setDisable(true)
    }

    private void applySequenceSettingsToModel() {
        try {
            long init = 0L
            if (seqInitialDelayField.getText() != null && !seqInitialDelayField.getText().trim().isEmpty()) {
                init = Long.parseLong(seqInitialDelayField.getText().trim())
            }
            sequence.setInitialDelayMs(init)
        } catch (Exception ignored) {}

        String mode = sequenceModeChoiceBox.getSelectionModel().getSelectedItem()
        try {
            if (mode == 'Boucle (infini)') {
                sequence.setCycles(0)
                sequence.setTotalDurationMs(0L)
            } else if (mode == 'One shot') {
                sequence.setCycles(1)
                sequence.setTotalDurationMs(0L)
            } else if (mode == 'Par cycles') {
                int c = 1
                if (seqCyclesField.getText() != null && !seqCyclesField.getText().trim().isEmpty()) {
                    c = Integer.parseInt(seqCyclesField.getText().trim())
                }
                sequence.setCycles(Math.max(1, c))
                sequence.setTotalDurationMs(0L)
            } else if (mode == 'Par duree (ms)') {
                long d = 0L
                if (seqDurationField.getText() != null && !seqDurationField.getText().trim().isEmpty()) {
                    d = Long.parseLong(seqDurationField.getText().trim())
                }
                sequence.setTotalDurationMs(Math.max(0L, d))
                sequence.setCycles(0)
            }
        } catch (Exception ignored) {}
    }

    private static void addRow(GridPane grid, int rowIndex, String labelText, Node editor) {
        Label label = new Label(labelText)
        label.setStyle('-fx-text-fill: #6b7280;')
        grid.add(label, 0, rowIndex)
        grid.add(editor, 1, rowIndex)
    }
}
