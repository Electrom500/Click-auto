package fr.clickauto.app.ui

import fr.clickauto.model.Action
import fr.clickauto.model.ClickAction
import fr.clickauto.model.ClickType
import fr.clickauto.model.KeyAction
import fr.clickauto.model.Sequence
import groovy.transform.CompileStatic
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.Separator
import javafx.scene.control.TextArea
import javafx.scene.control.ToolBar
import javafx.scene.layout.BorderPane
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.util.Callback

import java.awt.event.KeyEvent

@CompileStatic
final class MainView {
    private final Sequence sequence = createDemoSequence()
    private final ObservableList<Action> actions = FXCollections.observableArrayList(sequence.getActions())
    private final Label statusLabel = new Label('Prêt')
    private final Label selectedActionLabel = new Label('Aucune action sélectionnée')
    private final Label sequenceNameValue = new Label(sequence.getName())
    private final Label cyclesValue = new Label(Integer.toString(sequence.getCycles()))
    private final Label initialDelayValue = new Label(sequence.getInitialDelayMs().toString() + ' ms')
    private final Label totalDurationValue = new Label(sequence.getTotalDurationMs().toString() + ' ms')
    private final Label actionCountValue = new Label(Integer.toString(sequence.getActionCount()))
    private final TextArea detailsArea = new TextArea()

    Scene createScene() {
        ListView<Action> actionList = createActionList()

        BorderPane root = new BorderPane()
        root.setPadding(new Insets(12))
        root.setTop(createTopBar(actionList))
        root.setCenter(createCenterPane(actionList))
        root.setRight(createDetailsPane())
        root.setBottom(createStatusBar())

        BorderPane.setMargin(root.getCenter(), new Insets(0, 12, 0, 0))
        BorderPane.setMargin(root.getRight(), new Insets(0, 0, 0, 12))

        return new Scene(root, 1280, 760)
    }

    private Node createTopBar(ListView<Action> actionList) {
        Button startButton = new Button('Démarrer')
        Button pauseButton = new Button('Pause')
        Button stopButton = new Button('Arrêter')
        Button addClickButton = new Button('Ajouter clic')
        Button addKeyButton = new Button('Ajouter touche')

        startButton.setOnAction {
            statusLabel.setText('Séquence prête à être lancée')
        }
        pauseButton.setOnAction {
            statusLabel.setText('Pause demandée')
        }
        stopButton.setOnAction {
            statusLabel.setText('Arrêt demandé')
        }
        addClickButton.setOnAction {
            addAction(new ClickAction(ClickType.LEFT, 250L, 420, 320))
            actionList.refresh()
            selectLastAction(actionList)
            statusLabel.setText('Action clic ajoutée')
        }
        addKeyButton.setOnAction {
            addAction(new KeyAction(KeyEvent.VK_A, 120L, 500L))
            actionList.refresh()
            selectLastAction(actionList)
            statusLabel.setText('Action clavier ajoutée')
        }

        Region spacer = new Region()
        HBox.setHgrow(spacer, Priority.ALWAYS)

        Label title = new Label('Click-auto')
        title.setFont(Font.font('System', FontWeight.BOLD, 18))

        ToolBar toolBar = new ToolBar(startButton, pauseButton, stopButton, addClickButton, addKeyButton, spacer, title)
        toolBar.setPadding(new Insets(0, 0, 12, 0))
        return toolBar
    }

    private Node createCenterPane(ListView<Action> actionList) {
        Label title = new Label('Séquence en cours')
        title.setFont(Font.font('System', FontWeight.BOLD, 16))

        Label subtitle = new Label('Base de la timeline des actions à exécuter')
        subtitle.setStyle('-fx-text-fill: #6b7280;')

        VBox header = new VBox(4, title, subtitle)

        StackPane listWrapper = new StackPane(actionList)
        listWrapper.setPadding(new Insets(12))
        listWrapper.setStyle('-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 8; -fx-background-radius: 8;')

        VBox center = new VBox(12, header, listWrapper)
        VBox.setVgrow(listWrapper, Priority.ALWAYS)
        return center
    }

    private Node createDetailsPane() {
        Label title = new Label('Propriétés')
        title.setFont(Font.font('System', FontWeight.BOLD, 16))

        GridPane grid = new GridPane()
        grid.setHgap(10)
        grid.setVgap(10)
        grid.setPadding(new Insets(12))
        grid.setStyle('-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 8; -fx-background-radius: 8;')
        grid.getColumnConstraints().addAll(new ColumnConstraints(140), new ColumnConstraints(220))

        addPropertyRow(grid, 0, 'Nom', sequenceNameValue)
        addPropertyRow(grid, 1, 'Cycles', cyclesValue)
        addPropertyRow(grid, 2, 'Délai initial', initialDelayValue)
        addPropertyRow(grid, 3, 'Durée totale', totalDurationValue)
        addPropertyRow(grid, 4, 'Actions', actionCountValue)

        Separator separator = new Separator()
        Label selectedTitle = new Label('Action sélectionnée')
        selectedTitle.setFont(Font.font('System', FontWeight.BOLD, 13))

        detailsArea.setEditable(false)
        detailsArea.setWrapText(true)
        detailsArea.setPrefRowCount(8)
        detailsArea.setText('Sélectionne une action dans la liste pour afficher ses détails.')

        VBox detailsBox = new VBox(10, title, grid, separator, selectedTitle, selectedActionLabel, detailsArea)
        detailsBox.setPrefWidth(360)
        return detailsBox
    }

    private Node createStatusBar() {
        HBox statusBar = new HBox(10)
        statusBar.setPadding(new Insets(12, 0, 0, 0))
        statusBar.setAlignment(Pos.CENTER_LEFT)
        statusBar.setStyle('-fx-border-color: transparent transparent transparent transparent;')
        Label ready = new Label('État :')
        ready.setStyle('-fx-font-weight: bold;')
        statusBar.getChildren().addAll(ready, statusLabel)
        return statusBar
    }

    private ListView<Action> createActionList() {
        ListView<Action> actionList = new ListView<>(actions)
        actionList.setCellFactory(actionCellFactory())
        actionList.getSelectionModel().selectedItemProperty().addListener(this.&onSelectedActionChanged)
        actionList.setPrefHeight(540)
        actionList.setPlaceholder(new Label('Aucune action dans la séquence.'))

        if (!actions.isEmpty()) {
            actionList.getSelectionModel().select(0)
        }
        return actionList
    }

    private Callback<ListView<Action>, ListCell<Action>> actionCellFactory() {
        return { ListView<Action> ignored ->
            new ListCell<Action>() {
                @Override
                protected void updateItem(Action item, boolean empty) {
                    super.updateItem(item, empty)
                    if (empty || item == null) {
                        setText(null)
                    } else {
                        setText("${item.getActionType()} - ${item.getDescription()}  | délai ${item.getDelayMs()} ms")
                    }
                }
            }
        }
    }

    private void onSelectedActionChanged(ObservableValue<? extends Action> observable, Action oldValue, Action newValue) {
        selectAction(newValue)
    }

    private void selectAction(Action action) {
        if (action == null) {
            selectedActionLabel.setText('Aucune action sélectionnée')
            detailsArea.setText('Sélectionne une action dans la liste pour afficher ses détails.')
            return
        }

        selectedActionLabel.setText(action.getDescription())
        detailsArea.setText(
                "Type: ${action.getActionType()}\n" +
                "Description: ${action.getDescription()}\n" +
                "Délai avant action: ${action.getDelayMs()} ms\n" +
                "Classe: ${action.getClass().getSimpleName()}"
        )
    }

    private void addAction(Action action) {
        sequence.addAction(action)
        actions.add(action)
        refreshSequenceSummary()
    }

    private void selectLastAction(ListView<Action> actionList) {
        if (!actions.isEmpty()) {
            actionList.getSelectionModel().select(actions.size() - 1)
        }
    }

    private void refreshSequenceSummary() {
        cyclesValue.setText(Integer.toString(sequence.getCycles()))
        initialDelayValue.setText(sequence.getInitialDelayMs().toString() + ' ms')
        totalDurationValue.setText(sequence.getTotalDurationMs().toString() + ' ms')
        actionCountValue.setText(Integer.toString(sequence.getActionCount()))
    }

    private void addPropertyRow(GridPane grid, int rowIndex, String labelText, Label valueLabel) {
        Label label = new Label(labelText)
        label.setStyle('-fx-text-fill: #6b7280;')
        grid.add(label, 0, rowIndex)
        grid.add(valueLabel, 1, rowIndex)
    }

    private Sequence createDemoSequence() {
        Sequence demo = new Sequence('Séquence de démonstration')
        demo.setCycles(3)
        demo.setInitialDelayMs(500L)
        demo.setTotalDurationMs(0L)
        demo.addAction(new ClickAction(ClickType.LEFT, 250L, 420, 320))
        demo.addAction(new KeyAction(KeyEvent.VK_A, 120L, 500L))
        demo.addAction(new ClickAction(ClickType.RIGHT, 400L, 640, 350))
        return demo
    }
}

