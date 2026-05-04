package fr.clickauto.app.ui

import fr.clickauto.app.ui.pages.ClicksPageView
import fr.clickauto.app.ui.pages.KeysPageView
import fr.clickauto.app.ui.pages.RecordReplayPageView
import fr.clickauto.app.ui.pages.TestPageView
import groovy.transform.CompileStatic
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.text.Font
import javafx.scene.text.FontWeight

import java.util.function.Consumer

@CompileStatic
final class MainView {
    private final Label statusLabel = new Label('Pret')
    private final BorderPane root = new BorderPane()

    private final Button clicksButton = new Button('clicks')
    private final Button keysButton = new Button('touches clavier')
    private final Button recordReplayButton = new Button('record and replay')
    private final Button testButton = new Button('test')

    private final Consumer<String> statusUpdater = { String message ->
        statusLabel.setText(message)
    } as Consumer<String>

    private final ClicksPageView clicksPage = new ClicksPageView(statusUpdater)
    private final KeysPageView keysPage = new KeysPageView()
    private final RecordReplayPageView recordReplayPage = new RecordReplayPageView()
    private final TestPageView testPage = new TestPageView(statusUpdater)

    Scene createScene() {
        root.setPadding(new Insets(12))
        root.setTop(createHeader())
        root.setBottom(createStatusBar())
        switchSection('clicks')
        return new Scene(root, 1280, 760)
    }

    private Node createHeader() {
        Label title = new Label('Click-auto')
        title.setFont(Font.font('System', FontWeight.BOLD, 18))

        configureHeaderButton(clicksButton, 'clicks')
        configureHeaderButton(keysButton, 'touches')
        configureHeaderButton(recordReplayButton, 'record')
        configureHeaderButton(testButton, 'test')

        HBox nav = new HBox(8, clicksButton, keysButton, recordReplayButton, testButton)
        nav.setAlignment(Pos.CENTER_LEFT)

        Region spacer = new Region()
        HBox.setHgrow(spacer, Priority.ALWAYS)

        HBox header = new HBox(10, nav, spacer, title)
        header.setAlignment(Pos.CENTER_LEFT)
        header.setPadding(new Insets(0, 0, 12, 0))
        return header
    }

    private void configureHeaderButton(Button button, String section) {
        button.setOnAction({ ActionEvent ignored ->
            switchSection(section)
        } as EventHandler<ActionEvent>)
        button.setStyle('-fx-background-color: #e5e7eb; -fx-font-weight: bold;')
    }

    private void switchSection(String section) {
        if (section == 'clicks') {
            root.setCenter(clicksPage.getView())
            setHeaderSelection(clicksButton)
            statusLabel.setText('Onglet clicks ouvert')
            return
        }
        if (section == 'touches') {
            root.setCenter(keysPage.getView())
            setHeaderSelection(keysButton)
            statusLabel.setText('Onglet touches clavier ouvert')
            return
        }
        if (section == 'record') {
            root.setCenter(recordReplayPage.getView())
            setHeaderSelection(recordReplayButton)
            statusLabel.setText('Onglet record and replay ouvert')
            return
        }

        root.setCenter(testPage.getView())
        setHeaderSelection(testButton)
        statusLabel.setText('Onglet test ouvert')
    }

    private void setHeaderSelection(Button activeButton) {
        Button[] navigationButtons = [clicksButton, keysButton, recordReplayButton, testButton] as Button[]
        for (Button button : navigationButtons) {
            if (button == activeButton) {
                button.setStyle('-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold;')
            } else {
                button.setStyle('-fx-background-color: #e5e7eb; -fx-font-weight: bold;')
            }
        }
    }

    private Node createStatusBar() {
        HBox statusBar = new HBox(10)
        statusBar.setPadding(new Insets(12, 0, 0, 0))
        statusBar.setAlignment(Pos.CENTER_LEFT)

        Label label = new Label('Etat :')
        label.setStyle('-fx-font-weight: bold;')
        statusBar.getChildren().addAll(label, statusLabel)
        return statusBar
    }
}
