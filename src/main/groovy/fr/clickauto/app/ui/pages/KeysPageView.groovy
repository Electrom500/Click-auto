package fr.clickauto.app.ui.pages

import groovy.transform.CompileStatic
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import javafx.scene.text.FontWeight

@CompileStatic
final class KeysPageView {
    private Node rootNode

    Node getView() {
        if (rootNode == null) {
            Label title = new Label('Touches clavier')
            title.setFont(Font.font('System', FontWeight.BOLD, 18))

            Label body = new Label('Zone en cours de preparation pour les sequences clavier.')
            body.setWrapText(true)

            VBox box = new VBox(10, title, body)
            box.setPadding(new Insets(16))
            box.setStyle('-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 8; -fx-background-radius: 8;')
            rootNode = box
        }
        return rootNode
    }
}

