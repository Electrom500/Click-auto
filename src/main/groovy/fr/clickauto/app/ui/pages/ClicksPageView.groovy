package fr.clickauto.app.ui.pages

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
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.ChoiceBox
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.Separator
import javafx.scene.control.TextField
import javafx.scene.control.ToolBar
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseButton
import javafx.scene.layout.BorderPane
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.util.Callback
import javafx.stage.Screen
import javafx.stage.Stage
import javafx.stage.StageStyle

import java.util.function.Consumer

@CompileStatic
final class ClicksPageView {
    private final Consumer<String> statusUpdater
    private final Sequence sequence = createDemoSequence()
    private final ObservableList<ClickAction> clicks = FXCollections.observableArrayList()

    private final Label sequenceNameValue = new Label(sequence.getName())
    private final Label clickCountValue = new Label('0')
    private final Label enabledCountValue = new Label('0')
    private final Label selectedActionLabel = new Label('Aucun clic sélectionné')
    private final ChoiceBox<ClickType> typeChoiceBox = new ChoiceBox<>(FXCollections.observableArrayList(ClickType.values()))
    private final TextField xField = new TextField()
    private final TextField yField = new TextField()
    private final TextField delayField = new TextField()
    private final CheckBox enabledCheckBox = new CheckBox('Clic actif')
    private final Button captureCoordinatesButton = new Button('Enregistrer coordonnées')

    private Node rootNode
    private ListView<ClickAction> clickList
    private boolean capturePending = false
    private Stage captureOverlay

    ClicksPageView(Consumer<String> statusUpdater) {
        this.statusUpdater = statusUpdater
        for (Object action : sequence.getActions()) {
            clicks.add((ClickAction) action)
        }
        typeChoiceBox.getSelectionModel().select(ClickType.LEFT)
    }

    Node getView() {
        if (rootNode == null) {
            rootNode = createPage()
        }
        return rootNode
    }

    private Node createPage() {
        clickList = createClickList()

        BorderPane page = new BorderPane()
        page.setTop(createToolbar())
        page.setCenter(createCenterPane())
        page.setRight(createEditorPane())

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

        addButton.setOnAction({ ActionEvent ignored -> addClickFromEditor() } as EventHandler<ActionEvent>)
        duplicateButton.setOnAction({ ActionEvent ignored -> duplicateSelectedClick() } as EventHandler<ActionEvent>)
        deleteButton.setOnAction({ ActionEvent ignored -> deleteSelectedClick() } as EventHandler<ActionEvent>)
        toggleButton.setOnAction({ ActionEvent ignored -> toggleSelectedClick() } as EventHandler<ActionEvent>)
        upButton.setOnAction({ ActionEvent ignored -> moveSelectedClick(-1) } as EventHandler<ActionEvent>)
        downButton.setOnAction({ ActionEvent ignored -> moveSelectedClick(1) } as EventHandler<ActionEvent>)

        return new ToolBar(addButton, duplicateButton, deleteButton, toggleButton, upButton, downButton)
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

        selectedActionLabel.setWrapText(true)

        VBox editor = new VBox(10, title, grid, applyButton, new Separator(), selectedActionLabel)
        editor.setPrefWidth(380)
        return editor
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
            new ListCell<ClickAction>() {
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
        } catch (Exception ex) {
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

        capturePending = true
        statusUpdater.accept('Capture active: clique gauche (overlay) pour enregistrer X/Y')

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

