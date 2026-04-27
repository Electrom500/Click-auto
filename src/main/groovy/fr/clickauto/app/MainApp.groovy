package fr.clickauto.app

import fr.clickauto.app.ui.MainView
import groovy.transform.CompileStatic
import javafx.application.Application
import javafx.stage.Stage

@CompileStatic
final class MainApp extends Application {
    static void main(String[] args) {
        launch(args)
    }

    @Override
    void start(Stage primaryStage) {
        MainView mainView = new MainView()
        primaryStage.setTitle('Click-auto')
        primaryStage.setScene(mainView.createScene())
        primaryStage.setMinWidth(1200)
        primaryStage.setMinHeight(700)
        primaryStage.show()
    }
}
