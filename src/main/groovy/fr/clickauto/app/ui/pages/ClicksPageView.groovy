package fr.clickauto.app.ui.pages

import fr.clickauto.app.ui.OverlayWindow
import fr.clickauto.app.ui.ClickMarkersOverlay
import fr.clickauto.model.ClickAction
import fr.clickauto.model.ClickType
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
import javafx.geometry.Rectangle2D
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.ButtonType
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
import javafx.scene.input.MouseButton
import javafx.scene.layout.BorderPane
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.util.Callback
import javafx.stage.FileChooser
import javafx.stage.Screen
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.stage.Window

import java.io.File
import java.io.FileFilter
import java.util.function.Consumer
import fr.clickauto.engine.SequenceExecutor
import fr.clickauto.persistence.SequenceProfileStore
import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.NativeHookException
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent
import com.github.kwhat.jnativehook.mouse.NativeMouseListener
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import java.util.logging.Level
import java.util.logging.Logger

@CompileStatic
final class ClicksPageView {
    private final Consumer<String> statusUpdater
    private final Sequence sequence = createDemoSequence()
    private final ObservableList<ClickAction> clicks = FXCollections.observableArrayList()

    private final Label sequenceNameValue = new Label(sequence.getName())
    private final Label clickCountValue = new Label('0')
    private final Label enabledCountValue = new Label('0')
    private final Label selectedActionLabel = new Label('Aucun clic sélectionne')
    private final ChoiceBox<ClickType> typeChoiceBox = new ChoiceBox<>(FXCollections.observableArrayList(ClickType.values()))
    private final TextField xField = new TextField()
    private final TextField yField = new TextField()
    private final TextField delayField = new TextField()
    private final CheckBox enabledCheckBox = new CheckBox('Clic actif')
    private final Button captureCoordinatesButton = new Button('Enregistrer coordonnees')

    // Execution UI controls (moved under details)
    private final ChoiceBox<String> sequenceModeChoiceBox = new ChoiceBox<>(FXCollections.observableArrayList('One shot', 'Boucle (infini)', 'Par cycles', 'Par duree (ms)'))
    private final TextField seqCyclesField = new TextField()
    private final TextField seqDurationField = new TextField()
    private final TextField seqInitialDelayField = new TextField()
    private final SequenceProfileStore profileStore = new SequenceProfileStore()
    private final CheckBox confirmBeforeStartCheckBox = new CheckBox('Confirmer avant lancement')
    private final CheckBox globalHotkeysCheckBox = new CheckBox('Hotkeys globales (F8/F7/F9)')

    private final ChoiceBox<String> profileChoiceBox = new ChoiceBox<>(FXCollections.observableArrayList())
    private final TextField profileNameField = new TextField()

    private Node rootNode
    private ListView<ClickAction> clickList
    private boolean capturePending = false
    private Stage captureOverlay
    private OverlayWindow overlayWindow
    private final ClickMarkersOverlay markersOverlay = new ClickMarkersOverlay()

    private boolean globalHookRegistered = false
    private NativeKeyListener globalHotkeyListener

    // Execution controller and buttons
    private SequenceExecutor executor
    private Button startExecButton
    private Button pauseExecButton
    private Button stopExecButton

    ClicksPageView(Consumer<String> statusUpdater) {
        this.statusUpdater = statusUpdater
        for (Object action : sequence.getActions()) {
            clicks.add((ClickAction) action)
        }
        typeChoiceBox.getSelectionModel().select(ClickType.LEFT)
        refreshProfileList()
    }

    Node getView() {
        if (rootNode == null) {
            rootNode = createPage()
        }
        return rootNode
    }

    private Node createPage() {
        clickList = createClickList()

        // create executor after clickList is available so highlighting can select items
        executor = new SequenceExecutor(statusUpdater, { Integer idx ->
            Platform.runLater { if (clickList != null) {
                if (idx >= 0 && idx < clicks.size()) {
                    clickList.getSelectionModel().select(idx)
                } else {
                    clickList.getSelectionModel().clearSelection()
                }
            } }
        } as Consumer<Integer>)

        BorderPane page = new BorderPane()
        page.setTop(createToolbar())
        page.setCenter(createCenterPane())
        page.setRight(createEditorPane())

        // local hotkeys when this page has focus: F8 start, F9 stop, F7 pause/resume
        page.setOnKeyPressed({ KeyEvent event ->
            if (executor == null) return
            if (event.getCode() == KeyCode.F8) {
                requestStartExecution()
            } else if (event.getCode() == KeyCode.F9) {
                handleStopExecution()
            } else if (event.getCode() == KeyCode.F7) {
                handlePauseExecution()
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
        Button saveButton = new Button('Sauvegarder')
        Button loadButton = new Button('Charger')

        addButton.setOnAction({ ActionEvent ignored -> addClickFromEditor() } as EventHandler<ActionEvent>)
        duplicateButton.setOnAction({ ActionEvent ignored -> duplicateSelectedClick() } as EventHandler<ActionEvent>)
        deleteButton.setOnAction({ ActionEvent ignored -> deleteSelectedClick() } as EventHandler<ActionEvent>)
        toggleButton.setOnAction({ ActionEvent ignored -> toggleSelectedClick() } as EventHandler<ActionEvent>)
        upButton.setOnAction({ ActionEvent ignored -> moveSelectedClick(-1) } as EventHandler<ActionEvent>)
        downButton.setOnAction({ ActionEvent ignored -> moveSelectedClick(1) } as EventHandler<ActionEvent>)
        saveButton.setOnAction({ ActionEvent ignored -> saveCurrentSequenceProfile() } as EventHandler<ActionEvent>)
        loadButton.setOnAction({ ActionEvent ignored -> loadSequenceProfile() } as EventHandler<ActionEvent>)

        Button overlayToggle = new Button('Overlay')
        overlayToggle.setOnAction({ ActionEvent ignored ->
            try {
                if (executor == null) return
                // lazy create overlay
                if (overlayWindow == null) {
                    overlayWindow = new OverlayWindow(
                            executor,
                            ({ -> requestStartExecution() } as Runnable),
                            ({ -> handlePauseExecution() } as Runnable),
                            ({ -> handleStopExecution() } as Runnable)
                    )
                }
                overlayWindow.toggle()
            } catch (Exception ex) {
                statusUpdater.accept('Impossible d\'afficher l\'overlay: ' + ex.getMessage())
            }
        } as EventHandler<ActionEvent>)

        return new ToolBar(addButton, duplicateButton, deleteButton, toggleButton, upButton, downButton, new Separator(), saveButton, loadButton, new Separator(), overlayToggle)
    }

    private Node createCenterPane() {
        Label title = new Label('Gestion des clics')
        title.setFont(Font.font('System', FontWeight.BOLD, 16))

        Label subtitle = new Label('Créer, dupliquer, désactiver et réordonner les clics souris.')
        subtitle.setStyle('-fx-text-fill: #6b7280;')

        Label hint = new Label('Astuce: Shift + Fleche Haut/Bas deplace le clic selectionne.')
        hint.setStyle('-fx-text-fill: #6b7280;')

        VBox header = new VBox(4, title, subtitle, hint)

        GridPane summary = new GridPane()
        summary.setHgap(10)
        summary.setVgap(8)
        summary.setPadding(new Insets(12))
        summary.setStyle('-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 8; -fx-background-radius: 8;')
        summary.getColumnConstraints().addAll(new ColumnConstraints(120), new ColumnConstraints(200))
        addRow(summary, 0, 'Sequence', sequenceNameValue)
        addRow(summary, 1, 'Total clics', clickCountValue)
        addRow(summary, 2, 'Clics actifs', enabledCountValue)

        StackPane listWrapper = new StackPane(clickList)
        listWrapper.setPadding(new Insets(12))
        listWrapper.setStyle('-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 8; -fx-background-radius: 8;')

        VBox center = new VBox(12, header, summary, listWrapper)
        VBox.setVgrow(listWrapper, Priority.ALWAYS)
        return center
    }

    private Node createEditorPane() {
        Label title = new Label('Proprietes du clic')
        title.setFont(Font.font('System', FontWeight.BOLD, 16))

        GridPane grid = new GridPane()
        grid.setHgap(10)
        grid.setVgap(10)
        grid.setPadding(new Insets(12))
        grid.setStyle('-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 8; -fx-background-radius: 8;')
        grid.getColumnConstraints().addAll(new ColumnConstraints(120), new ColumnConstraints(220))

        xField.setPromptText('X')
        yField.setPromptText('Y')
        delayField.setPromptText('Delai en ms')

        addRow(grid, 0, 'Type', typeChoiceBox)
        addRow(grid, 1, 'X', xField)
        addRow(grid, 2, 'Y', yField)
        addRow(grid, 3, 'Delai', delayField)
        grid.add(enabledCheckBox, 1, 4)
        captureCoordinatesButton.setOnAction({ ActionEvent ignored -> startCoordinateCapture() } as EventHandler<ActionEvent>)
        grid.add(captureCoordinatesButton, 1, 5)

        Button applyButton = new Button('Appliquer')
        applyButton.setOnAction({ ActionEvent ignored -> applyEditorToSelected() } as EventHandler<ActionEvent>)

        // Sequence execution settings
        seqCyclesField.setPromptText('Nombre de cycles')
        seqDurationField.setPromptText('Duree totale en ms')
        seqInitialDelayField.setPromptText('Delai initial en ms')
        sequenceModeChoiceBox.getSelectionModel().select('One shot')

        GridPane execGrid = new GridPane()
        execGrid.setHgap(10)
        execGrid.setVgap(8)
        execGrid.getColumnConstraints().addAll(new ColumnConstraints(120), new ColumnConstraints(220))
        addRow(execGrid, 0, 'Mode sequence', sequenceModeChoiceBox)
        addRow(execGrid, 1, 'Cycles', seqCyclesField)
        addRow(execGrid, 2, 'Duree totale (ms)', seqDurationField)
        addRow(execGrid, 3, 'Delai initial (ms)', seqInitialDelayField)

        confirmBeforeStartCheckBox.setSelected(true)
        globalHotkeysCheckBox.setSelected(false)

        GridPane safetyGrid = new GridPane()
        safetyGrid.setHgap(10)
        safetyGrid.setVgap(6)
        safetyGrid.getColumnConstraints().addAll(new ColumnConstraints(120), new ColumnConstraints(220))
        addRow(safetyGrid, 0, 'Sécurité', confirmBeforeStartCheckBox)
        addRow(safetyGrid, 1, 'Hotkeys', globalHotkeysCheckBox)

        globalHotkeysCheckBox.setOnAction({ ActionEvent ignored ->
            if (globalHotkeysCheckBox.isSelected()) {
                enableGlobalHotkeys()
            } else {
                disableGlobalHotkeys()
            }
        } as EventHandler<ActionEvent>)

        // Execution buttons below sequence settings
        startExecButton = new Button('Démarrer')
        pauseExecButton = new Button('Pause')
        stopExecButton = new Button('Arrêter')
        Button previewButton = new Button('Prévisualiser')
        pauseExecButton.setDisable(true)
        stopExecButton.setDisable(true)

        HBox execButtons = new HBox(8, startExecButton, pauseExecButton, stopExecButton, previewButton)

        startExecButton.setOnAction({ ActionEvent ignored ->
            requestStartExecution()
        } as EventHandler<ActionEvent>)

        pauseExecButton.setOnAction({ ActionEvent ignored ->
            if (executor != null) {
                if (executor.getState() == SequenceExecutor.State.RUNNING) {
                    executor.pause()
                    pauseExecButton.setText('Reprendre')
                } else if (executor.getState() == SequenceExecutor.State.PAUSED) {
                    executor.resume()
                    pauseExecButton.setText('Pause')
                }
            }
        } as EventHandler<ActionEvent>)

        stopExecButton.setOnAction({ ActionEvent ignored ->
            handleStopExecution()
        } as EventHandler<ActionEvent>)

        previewButton.setOnAction({ ActionEvent ignored ->
            showClickMarkersPreview()
        } as EventHandler<ActionEvent>)

        selectedActionLabel.setWrapText(true)

        Label hotkeysHint = new Label('Raccourcis (si la page a le focus): F8 = Démarrer · F7 = Pause/Reprendre · F9 = Arrêter')
        hotkeysHint.setStyle('-fx-text-fill: #6b7280; -fx-font-size: 11;')

        GridPane profileGrid = new GridPane()
        profileGrid.setHgap(10)
        profileGrid.setVgap(6)
        profileGrid.getColumnConstraints().addAll(new ColumnConstraints(120), new ColumnConstraints(220))
        profileNameField.setPromptText('Nom du profil')
        profileNameField.setText(sequence.getName())
        addRow(profileGrid, 0, 'Profil', profileChoiceBox)
        addRow(profileGrid, 1, 'Nom', profileNameField)

        Button saveProfileButton = new Button('Enregistrer profil')
        Button loadProfileButton = new Button('Charger profil')
        Button deleteProfileButton = new Button('Supprimer profil')
        HBox profileButtons = new HBox(8, saveProfileButton, loadProfileButton, deleteProfileButton)

        saveProfileButton.setOnAction({ ActionEvent ignored -> saveProfileToLibrary() } as EventHandler<ActionEvent>)
        loadProfileButton.setOnAction({ ActionEvent ignored -> loadProfileFromLibrary() } as EventHandler<ActionEvent>)
        deleteProfileButton.setOnAction({ ActionEvent ignored -> deleteProfileFromLibrary() } as EventHandler<ActionEvent>)

        VBox editor = new VBox(10, title, grid, applyButton, new Separator(), execGrid, safetyGrid, execButtons, hotkeysHint, new Separator(), profileGrid, profileButtons, new Separator(), selectedActionLabel)
        editor.setPrefWidth(380)
        return editor
    }

    private void applySequenceSettingsToModel() {
        // read initial delay
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
                // run until duration -> set cycles to 0 (infinite), executor will stop by duration
                sequence.setCycles(0)
            }
        } catch (Exception ignored) {}
    }

    private void requestStartExecution() {
        applySequenceSettingsToModel()
        if (confirmBeforeStartCheckBox.isSelected()) {
            if (sequence.getCycles() == 0) {
                if (!confirmInfiniteExecution()) {
                    return
                }
            } else if (!confirmRegularExecution()) {
                return
            }
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

    private void handlePauseExecution() {
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

    private void handleStopExecution() {
        if (executor == null) {
            return
        }
        executor.stop()
        startExecButton.setDisable(false)
        pauseExecButton.setDisable(true)
        pauseExecButton.setText('Pause')
        stopExecButton.setDisable(true)
    }

    private boolean confirmInfiniteExecution() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION)
        alert.setTitle('Confirmation de lancement')
        alert.setHeaderText('Mode infini détecté')
        alert.setContentText('Cette séquence est configurée en boucle infinie.\n' +
                'Elle s\'arrêtera uniquement avec la touche F9.\n\nContinuer ?')
        def result = alert.showAndWait()
        return result.isPresent() && result.get() == ButtonType.OK
    }

    private boolean confirmRegularExecution() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION)
        alert.setTitle('Confirmation de lancement')
        alert.setHeaderText('Lancer la séquence ?')
        alert.setContentText('Confirmer le démarrage de la séquence sélectionnée.')
        def result = alert.showAndWait()
        return result.isPresent() && result.get() == ButtonType.OK
    }

    private void showClickMarkersPreview() {
        List<ClickAction> enabledClicks = clicks.findAll { ClickAction click -> click.isEnabled() }
        if (enabledClicks.isEmpty()) {
            statusUpdater.accept('Aucun clic actif à prévisualiser')
            return
        }
        markersOverlay.show(enabledClicks, 2000L)
        statusUpdater.accept('Prévisualisation des clics (2s)')
    }

    private void enableGlobalHotkeys() {
        if (globalHotkeyListener == null) {
            globalHotkeyListener = new NativeKeyListener() {
                @Override
                void nativeKeyPressed(NativeKeyEvent event) {
                    if (event.getKeyCode() == NativeKeyEvent.VC_F8) {
                        Platform.runLater { requestStartExecution() }
                    } else if (event.getKeyCode() == NativeKeyEvent.VC_F9) {
                        Platform.runLater { handleStopExecution() }
                    } else if (event.getKeyCode() == NativeKeyEvent.VC_F7) {
                        Platform.runLater { handlePauseExecution() }
                    }
                }
                @Override void nativeKeyReleased(NativeKeyEvent event) {}
                @Override void nativeKeyTyped(NativeKeyEvent event) {}
            }
        }

        if (!globalHookRegistered) {
            try {
                GlobalScreen.registerNativeHook()
                globalHookRegistered = true
            } catch (NativeHookException ex) {
                globalHotkeysCheckBox.setSelected(false)
                statusUpdater.accept('Impossible d\'activer les hotkeys globales: ' + ex.getMessage())
                return
            }
        }
        try { GlobalScreen.removeNativeKeyListener(globalHotkeyListener) } catch (Exception ignored) {}
        GlobalScreen.addNativeKeyListener(globalHotkeyListener)
        statusUpdater.accept('Hotkeys globales actives')
    }

    private void disableGlobalHotkeys() {
        if (globalHotkeyListener != null) {
            try { GlobalScreen.removeNativeKeyListener(globalHotkeyListener) } catch (Exception ignored) {}
        }
        if (globalHookRegistered && !capturePending) {
            try {
                GlobalScreen.unregisterNativeHook()
                globalHookRegistered = false
            } catch (NativeHookException ignored) {}
        }
        statusUpdater.accept('Hotkeys globales desactivées')
    }

    private File getProfilesDirectory() {
        File dir = new File(System.getProperty('user.home'), 'Click-auto/profiles')
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private void refreshProfileList() {
        File dir = getProfilesDirectory()
        List<String> names = []
        File[] files = dir.listFiles({ File f -> f.isFile() && f.name.toLowerCase().endsWith('.json') } as FileFilter)
        if (files != null) {
            for (File file : files) {
                names.add(file.name)
            }
        }
        names.sort()
        profileChoiceBox.setItems(FXCollections.observableArrayList(names))
        if (!names.isEmpty()) {
            profileChoiceBox.getSelectionModel().select(0)
        }
    }

    private void saveProfileToLibrary() {
        String name = profileNameField.getText() == null ? '' : profileNameField.getText().trim()
        if (name.isEmpty()) {
            name = sequence.getName()
        }
        if (!name.toLowerCase().endsWith('.json')) {
            name = name + '.json'
        }
        File file = new File(getProfilesDirectory(), name)
        try {
            profileStore.save(file, sequence)
            refreshProfileList()
            profileChoiceBox.getSelectionModel().select(name)
            statusUpdater.accept('Profil enregistré: ' + name)
        } catch (Exception ex) {
            statusUpdater.accept('Erreur de sauvegarde du profil: ' + ex.getMessage())
        }
    }

    private void loadProfileFromLibrary() {
        String selected = profileChoiceBox.getSelectionModel().getSelectedItem()
        if (selected == null || selected.trim().isEmpty()) {
            statusUpdater.accept('Aucun profil sélectionné')
            return
        }
        File file = new File(getProfilesDirectory(), selected)
        if (!file.exists()) {
            statusUpdater.accept('Profil introuvable: ' + selected)
            refreshProfileList()
            return
        }
        loadProfileFromFile(file)
    }

    private void deleteProfileFromLibrary() {
        String selected = profileChoiceBox.getSelectionModel().getSelectedItem()
        if (selected == null || selected.trim().isEmpty()) {
            statusUpdater.accept('Aucun profil sélectionné')
            return
        }
        File file = new File(getProfilesDirectory(), selected)
        if (!file.exists()) {
            statusUpdater.accept('Profil introuvable: ' + selected)
            refreshProfileList()
            return
        }
        if (file.delete()) {
            refreshProfileList()
            statusUpdater.accept('Profil supprimé: ' + selected)
        } else {
            statusUpdater.accept('Impossible de supprimer le profil')
        }
    }

    private void saveCurrentSequenceProfile() {
        FileChooser chooser = new FileChooser()
        chooser.setTitle('Sauvegarder le profil de clics')
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter('Profil JSON', '*.json'))
        chooser.setInitialFileName(defaultProfileFileName())

        File file = chooser.showSaveDialog(getDialogOwner())
        if (file == null) {
            return
        }
        if (!file.name.toLowerCase().endsWith('.json')) {
            file = new File(file.parentFile, file.name + '.json')
        }
        try {
            profileStore.save(file, sequence)
            statusUpdater.accept('Profil sauvegarde: ' + file.name)
        } catch (Exception ex) {
            statusUpdater.accept('Erreur de sauvegarde du profil: ' + ex.getMessage())
        }
    }

    private void loadSequenceProfile() {
        FileChooser chooser = new FileChooser()
        chooser.setTitle('Charger un profil de clics')
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter('Profil JSON', '*.json'))

        File file = chooser.showOpenDialog(getDialogOwner())
        if (file == null) {
            return
        }
        loadProfileFromFile(file)
    }

    private String defaultProfileFileName() {
        return sequence.getName().replaceAll('[^a-zA-Z0-9._-]+', '_') + '.json'
    }

    private void syncExecutionControlsFromSequence(Sequence loadedSequence) {
        if (loadedSequence.getTotalDurationMs() > 0) {
            sequenceModeChoiceBox.getSelectionModel().select('Par duree (ms)')
            seqDurationField.setText(Long.toString(loadedSequence.getTotalDurationMs()))
            seqCyclesField.clear()
        } else if (loadedSequence.getCycles() == 0) {
            sequenceModeChoiceBox.getSelectionModel().select('Boucle (infini)')
            seqCyclesField.clear()
            seqDurationField.clear()
        } else if (loadedSequence.getCycles() == 1) {
            sequenceModeChoiceBox.getSelectionModel().select('One shot')
            seqCyclesField.clear()
            seqDurationField.clear()
        } else {
            sequenceModeChoiceBox.getSelectionModel().select('Par cycles')
            seqCyclesField.setText(Integer.toString(loadedSequence.getCycles()))
            seqDurationField.clear()
        }
        seqInitialDelayField.setText(Long.toString(loadedSequence.getInitialDelayMs()))
    }

    private Window getDialogOwner() {
        if (rootNode != null && rootNode.getScene() != null) {
            return rootNode.getScene().getWindow()
        }
        return null
    }

    private void loadProfileFromFile(File file) {
        if (executor != null && (executor.getState() == SequenceExecutor.State.RUNNING || executor.getState() == SequenceExecutor.State.PAUSED)) {
            handleStopExecution()
        }
        try {
            Sequence loaded = profileStore.load(file)
            for (Object action : loaded.getActions()) {
                if (!(action instanceof ClickAction)) {
                    statusUpdater.accept('Profil refuse: cet onglet charge uniquement les profils de clics')
                    return
                }
            }
            applyLoadedSequence(loaded)
            statusUpdater.accept('Profil charge: ' + file.name)
        } catch (Exception ex) {
            statusUpdater.accept('Erreur de chargement du profil: ' + ex.getMessage())
        }
    }

    private void applyLoadedSequence(Sequence loaded) {
        sequence.setName(loaded.getName())
        sequence.setCycles(loaded.getCycles())
        sequence.setTotalDurationMs(loaded.getTotalDurationMs())
        sequence.setInitialDelayMs(loaded.getInitialDelayMs())
        sequence.clearActions()
        clicks.clear()

        for (Object action : loaded.getActions()) {
            ClickAction click = (ClickAction) action
            sequence.addAction(click)
            clicks.add(click)
        }

        sequenceNameValue.setText(sequence.getName())
        profileNameField.setText(sequence.getName())
        syncExecutionControlsFromSequence(sequence)
        refreshCounters()
        if (clickList != null) {
            clickList.refresh()
            if (!clicks.isEmpty()) {
                clickList.getSelectionModel().select(0)
            }
        }
    }

    private ListView<ClickAction> createClickList() {
        ListView<ClickAction> listView = new ListView<>(clicks)
        listView.setCellFactory(clickCellFactory())
        listView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<ClickAction>() {
            @Override
            void changed(ObservableValue<? extends ClickAction> observable, ClickAction oldValue, ClickAction newValue) {
                onSelectedClickChanged(newValue)
            }
        })
        listView.addEventFilter(KeyEvent.KEY_PRESSED, { KeyEvent event ->
            if (handleShiftReorder(event)) {
                event.consume()
            }
        } as EventHandler<KeyEvent>)
        listView.addEventFilter(KeyEvent.KEY_RELEASED, { KeyEvent event ->
            if (isShiftArrow(event)) {
                event.consume()
            }
        } as EventHandler<KeyEvent>)
        listView.setPlaceholder(new Label('Aucun clic dans la sequence.'))

        if (!clicks.isEmpty()) {
            listView.getSelectionModel().select(0)
        }
        refreshCounters()
        return listView
    }

    private boolean handleShiftReorder(KeyEvent event) {
        if (!event.isShiftDown()) {
            return false
        }
        if (event.getCode() == KeyCode.UP) {
            moveSelectedClick(-1)
            return true
        }
        if (event.getCode() == KeyCode.DOWN) {
            moveSelectedClick(1)
            return true
        }
        return false
    }

    private static boolean isShiftArrow(KeyEvent event) {
        if (!event.isShiftDown()) {
            return false
        }
        return event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN
    }

    private Callback<ListView<ClickAction>, ListCell<ClickAction>> clickCellFactory() {
        return { ListView<ClickAction> ignored ->
            ListCell<ClickAction> cell = new ListCell<ClickAction>() {
                @Override
                protected void updateItem(ClickAction item, boolean empty) {
                    super.updateItem(item, empty)
                    if (empty || item == null) {
                        setText(null)
                    } else {
                        String state = item.isEnabled() ? 'ON' : 'OFF'
                        setText("[${state}] ${item.getClickType().label} - (${item.getX()}, ${item.getY()}) | delai ${item.getDelayMs()} ms")
                    }
                }
            }
            wireDragAndDrop(cell)
            return cell
        }
    }

    private void wireDragAndDrop(ListCell<ClickAction> cell) {
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
                moveClickForDrag(fromIndex, toIndex)
                event.setDropCompleted(true)
            } else {
                event.setDropCompleted(false)
            }
            event.consume()
        }
    }

    private void onSelectedClickChanged(ClickAction click) {
        if (click == null) {
            clearEditor()
            selectedActionLabel.setText('Aucun clic selectionne')
            return
        }

        typeChoiceBox.getSelectionModel().select(click.getClickType())
        xField.setText(Integer.toString(click.getX()))
        yField.setText(Integer.toString(click.getY()))
        delayField.setText(Long.toString(click.getDelayMs()))
        enabledCheckBox.setSelected(click.isEnabled())
        selectedActionLabel.setText("Selection: ${click.getDescription()}".toString())
    }

    private void addClickFromEditor() {
        ClickAction click = readEditorAsClick()
        if (click == null) {
            return
        }

        clicks.add(click)
        sequence.addAction(click)
        clickList.refresh()
        clickList.getSelectionModel().select(clicks.size() - 1)
        refreshCounters()
        statusUpdater.accept('Clic ajoute')
    }

    private void duplicateSelectedClick() {
        int index = getSelectedIndex()
        if (index < 0 || index >= clicks.size()) {
            statusUpdater.accept('Aucun clic selectionne pour duplication')
            return
        }

        ClickAction source = clicks.get(index)
        ClickAction duplicate = new ClickAction(source.getClickType(), source.getDelayMs(), source.getX(), source.getY())
        duplicate.setEnabled(source.isEnabled())
        clicks.add(index + 1, duplicate)
        sequence.insertAction(index + 1, duplicate)
        clickList.refresh()
        clickList.getSelectionModel().select(index + 1)
        refreshCounters()
        statusUpdater.accept('Clic duplique')
    }

    private void deleteSelectedClick() {
        int index = getSelectedIndex()
        if (index < 0 || index >= clicks.size()) {
            statusUpdater.accept('Aucun clic selectionne pour suppression')
            return
        }

        clicks.remove(index)
        sequence.removeAction(index)
        clickList.refresh()
        if (!clicks.isEmpty()) {
            clickList.getSelectionModel().select(Math.min(index, clicks.size() - 1))
        }
        refreshCounters()
        statusUpdater.accept('Clic supprime')
    }

    private void toggleSelectedClick() {
        ClickAction selected = clickList == null ? null : clickList.getSelectionModel().getSelectedItem()
        if (selected == null) {
            statusUpdater.accept('Aucun clic selectionne pour activation')
            return
        }

        selected.setEnabled(!selected.isEnabled())
        clickList.refresh()
        refreshCounters()
        statusUpdater.accept(selected.isEnabled() ? 'Clic active' : 'Clic desactive')
    }

    private void moveSelectedClick(int delta) {
        int index = getSelectedIndex()
        int target = index + delta
        if (index < 0 || target < 0 || target >= clicks.size()) {
            statusUpdater.accept('Deplacement impossible')
            return
        }

        ClickAction item = clicks.remove(index)
        clicks.add(target, item)
        sequence.removeAction(index)
        sequence.insertAction(target, item)
        clickList.refresh()
        clickList.getSelectionModel().select(target)
        refreshCounters()
        statusUpdater.accept(delta < 0 ? 'Clic monte' : 'Clic descendu')
    }

    private void moveClickForDrag(int fromIndex, int toIndex) {
        if (fromIndex == toIndex) {
            return
        }
        if (fromIndex < 0 || fromIndex >= clicks.size() || toIndex < 0 || toIndex >= clicks.size()) {
            return
        }

        ClickAction item = clicks.remove(fromIndex)
        clicks.add(toIndex, item)
        sequence.removeAction(fromIndex)
        sequence.insertAction(toIndex, item)
        clickList.refresh()
        clickList.getSelectionModel().select(toIndex)
        refreshCounters()
        statusUpdater.accept('Clic réordonné')
    }

    private void applyEditorToSelected() {
        int index = getSelectedIndex()
        if (index < 0 || index >= clicks.size()) {
            statusUpdater.accept('Selectionne un clic pour appliquer les modifications')
            return
        }

        ClickAction updated = readEditorAsClick()
        if (updated == null) {
            return
        }
        updated.setEnabled(enabledCheckBox.isSelected())

        clicks.set(index, updated)
        sequence.removeAction(index)
        sequence.insertAction(index, updated)
        clickList.refresh()
        clickList.getSelectionModel().select(index)
        refreshCounters()
        statusUpdater.accept('Clic mis a jour')
    }

    private ClickAction readEditorAsClick() {
        try {
            ClickType type = typeChoiceBox.getSelectionModel().getSelectedItem()
            if (type == null) {
                type = ClickType.LEFT
            }
            int x = Integer.parseInt(xField.getText().trim())
            int y = Integer.parseInt(yField.getText().trim())
            long delay = Long.parseLong(delayField.getText().trim())

            ClickAction click = new ClickAction(type, delay, x, y)
            click.setEnabled(enabledCheckBox.isSelected())
            return click
        } catch (Exception ignored) {
            statusUpdater.accept('Champs invalides pour le clic: verifier X, Y et delai')
            return null
        }
    }

    private void clearEditor() {
        typeChoiceBox.getSelectionModel().select(ClickType.LEFT)
        xField.clear()
        yField.clear()
        delayField.clear()
        enabledCheckBox.setSelected(true)
    }

    private void refreshCounters() {
        clickCountValue.setText(Integer.toString(clicks.size()))
        int enabledCount = 0
        for (ClickAction click : clicks) {
            if (click.isEnabled()) {
                enabledCount++
            }
        }
        enabledCountValue.setText(Integer.toString(enabledCount))
    }

    private int getSelectedIndex() {
        if (clickList == null) {
            return -1
        }
        return clickList.getSelectionModel().getSelectedIndex()
    }

    private void startCoordinateCapture() {
        if (capturePending) {
            statusUpdater.accept('Capture deja en cours: fais un clic gauche')
            return
        }

        // Try global capture via JNativeHook. Fallback to overlay.
        capturePending = true
        statusUpdater.accept('Capture active: clique gauche dans n\'importe quelle fenetre pour enregistrer X/Y')

        boolean registeredForCapture = false
        try {
            try {
                Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName())
                logger.setLevel(Level.OFF)
            } catch (Exception ignored) {}

            if (!globalHookRegistered) {
                GlobalScreen.registerNativeHook()
                globalHookRegistered = true
                registeredForCapture = true
            }

            NativeMouseListener listener = new NativeMouseListener() {
                @Override
                void nativeMouseClicked(NativeMouseEvent event) {
                    try {
                        final int sx = event.getX()
                        final int sy = event.getY()
                        Platform.runLater {
                            xField.setText(Integer.toString(sx))
                            yField.setText(Integer.toString(sy))
                            stopCaptureOverlay('Coordonnees capturees: (' + sx + ', ' + sy + ')')
                        }
                    } finally {
                        try { GlobalScreen.removeNativeMouseListener(this) } catch (Exception ignored) {}
                        if (registeredForCapture && !globalHotkeysCheckBox.isSelected()) {
                            try { GlobalScreen.unregisterNativeHook(); globalHookRegistered = false } catch (Exception ignored) {}
                        }
                    }
                }

                @Override void nativeMousePressed(NativeMouseEvent event) {}
                @Override void nativeMouseReleased(NativeMouseEvent event) {}
            }

            GlobalScreen.addNativeMouseListener(listener)
        } catch (NativeHookException ignored) {
            // Fallback to overlay capture
            capturePending = true
            statusUpdater.accept('Impossible d\'initialiser la capture globale, utilisation de l\'overlay')

            Rectangle2D bounds = Screen.getPrimary().getBounds()
            Pane pane = new Pane()
            pane.setStyle('-fx-background-color: rgba(37,99,235,0.12);')

            captureOverlay = new Stage(StageStyle.TRANSPARENT)
            captureOverlay.setAlwaysOnTop(true)
            captureOverlay.setX(bounds.getMinX())
            captureOverlay.setY(bounds.getMinY())

            Scene overlayScene = new Scene(pane, bounds.getWidth(), bounds.getHeight(), Color.TRANSPARENT)
            overlayScene.setOnKeyPressed({ KeyEvent event ->
                if (event.getCode() == KeyCode.ESCAPE) {
                    stopCaptureOverlay('Capture annulee')
                }
            } as EventHandler<KeyEvent>)

            pane.setOnMouseClicked { event ->
                if (event.getButton() != MouseButton.PRIMARY || !capturePending) {
                    return
                }
                xField.setText(Integer.toString((int) event.getScreenX()))
                yField.setText(Integer.toString((int) event.getScreenY()))
                stopCaptureOverlay('Coordonnees capturees: (' + (int) event.getScreenX() + ', ' + (int) event.getScreenY() + ')')
            }

            captureOverlay.setScene(overlayScene)
            captureOverlay.show()
            Platform.runLater { pane.requestFocus() }
        }
    }

    private void stopCaptureOverlay(String status) {
        capturePending = false
        if (captureOverlay != null) {
            captureOverlay.close()
            captureOverlay = null
        }
        statusUpdater.accept(status)
    }

    private static void addRow(GridPane grid, int rowIndex, String labelText, Node editor) {
        Label label = new Label(labelText)
        label.setStyle('-fx-text-fill: #6b7280;')
        grid.add(label, 0, rowIndex)
        grid.add(editor, 1, rowIndex)
    }

    private static Sequence createDemoSequence() {
        Sequence demo = new Sequence('Sequence de gestion de clics')
        demo.setCycles(1)
        demo.setInitialDelayMs(0L)
        demo.setTotalDurationMs(0L)
        demo.addAction(new ClickAction(ClickType.LEFT, 250L, 420, 320))
        demo.addAction(new ClickAction(ClickType.RIGHT, 400L, 640, 350))
        demo.addAction(new ClickAction(ClickType.MIDDLE, 500L, 720, 420))
        return demo
    }
}

