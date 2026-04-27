package fr.clickauto.app

import groovy.transform.CompileStatic
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.WindowConstants

@CompileStatic
final class MainApp {
    static void main(String[] args) {
        SwingUtilities.invokeLater(MainApp.&createAndShowWindow)
    }

    private static void createAndShowWindow() {
        JFrame frame = new JFrame('Click-auto')
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
        frame.setSize(1000, 650)
        frame.setLocationRelativeTo(null)
        frame.setVisible(true)
    }
}
