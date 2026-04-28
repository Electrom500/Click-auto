package fr.clickauto.app

import groovy.transform.CompileStatic
import javafx.application.Application

@CompileStatic
final class MainLauncher {
    private MainLauncher() {
    }

    static void main(String[] args) {
        Application.launch(MainApp, args)
    }
}

