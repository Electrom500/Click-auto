package fr.clickauto.app.ui.pages

import groovy.transform.CompileStatic
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import javafx.scene.text.FontWeight

import java.awt.Robot
import java.awt.event.InputEvent
import java.util.function.Consumer

@CompileStatic
final class TestPageView {
    private final Consumer<String> statusUpdater
    private Node rootNode

    TestPageView(Consumer<String> statusUpdater) {
        this.statusUpdater = statusUpdater
    }

    Node getView() {
        if (rootNode == null) {
            rootNode = createPage()
        }
        return rootNode
    }

    private Node createPage() {
        Label title = new Label('Page de test')
        title.setFont(Font.font('System', FontWeight.BOLD, 18))

        Label help = new Label('Teste un clic souris direct avec des coordonnees X/Y puis un popup.')
        help.setStyle('-fx-text-fill: #6b7280;')

        TextField xInput = new TextField('500')
        TextField yInput = new TextField('400')
        xInput.setPromptText('X')
        yInput.setPromptText('Y')

        Button clickButton = new Button('Creer un clic')
        clickButton.setOnAction({ ActionEvent ignored ->
            performTestClick(xInput, yInput)
        } as EventHandler<ActionEvent>)

        Button popupButton = new Button('Afficher popup')
        popupButton.setOnAction({ ActionEvent ignored ->
            Alert alert = new Alert(Alert.AlertType.INFORMATION)
            alert.setTitle('Test popup')
            alert.setHeaderText('Popup de test')
            alert.setContentText('Le bouton popup fonctionne correctement.')
            alert.showAndWait()
            statusUpdater.accept('Popup de test affichee')
        } as EventHandler<ActionEvent>)

        HBox inputs = new HBox(10, new Label('X:'), xInput, new Label('Y:'), yInput)
        inputs.setAlignment(Pos.CENTER_LEFT)

        HBox actionsBox = new HBox(10, clickButton, popupButton)
        actionsBox.setAlignment(Pos.CENTER_LEFT)

        VBox card = new VBox(12, title, help, inputs, actionsBox)
        card.setPadding(new Insets(16))
        card.setStyle('-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 8; -fx-background-radius: 8;')

        VBox wrapper = new VBox(card)
        wrapper.setPadding(new Insets(8, 0, 0, 0))
        return wrapper
    }

    private void performTestClick(TextField xInput, TextField yInput) {
        try {
            int x = Integer.parseInt(xInput.getText().trim())
            int y = Integer.parseInt(yInput.getText().trim())

            Robot robot = new Robot()
            robot.mouseMove(x, y)
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
            statusUpdater.accept("Clic effectue aux coordonnees (${x}, ${y})".toString())
        } catch (NumberFormatException ignored) {
            statusUpdater.accept('Coordonnees invalides: utilise des nombres entiers')
        } catch (Exception ex) {
            statusUpdater.accept('Impossible d\'effectuer le clic de test: ' + ex.getMessage())
        }
    }
}

