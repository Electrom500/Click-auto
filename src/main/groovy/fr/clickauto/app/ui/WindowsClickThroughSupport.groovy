package fr.clickauto.app.ui

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
import groovy.transform.CompileStatic
import javafx.stage.Stage

import java.lang.reflect.Method

/**
 * Best-effort Windows click-through support for a JavaFX Stage.
 * Uses JNA + reflection on JavaFX internals to get the native window handle.
 */
@CompileStatic
final class WindowsClickThroughSupport {
    private static final WinDef.HWND HWND_TOPMOST = new WinDef.HWND(Pointer.createConstant(-1L))

    private WindowsClickThroughSupport() { }

    static void apply(Stage stage, boolean clickThrough) {
        if (stage == null || !isWindows()) {
            return
        }

        // Always keep the JavaFX stage aligned with the requested mode as a fallback.
        if (stage.getScene() != null && stage.getScene().getRoot() != null) {
            stage.getScene().getRoot().setMouseTransparent(clickThrough)
        }

        try {
            long hwndValue = resolveWindowHandle(stage)
            if (hwndValue == 0L) {
                return
            }

            WinDef.HWND hwnd = new WinDef.HWND(Pointer.createConstant(hwndValue))
            int exStyle = User32.INSTANCE.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE)
            if (clickThrough) {
                exStyle = exStyle | WinUser.WS_EX_LAYERED | WinUser.WS_EX_TRANSPARENT
            } else {
                exStyle = exStyle & ~WinUser.WS_EX_TRANSPARENT
            }

            User32.INSTANCE.SetWindowLong(hwnd, WinUser.GWL_EXSTYLE, exStyle)
            User32.INSTANCE.SetWindowPos(
                    hwnd,
                    HWND_TOPMOST,
                    0, 0, 0, 0,
                    WinUser.SWP_NOMOVE | WinUser.SWP_NOSIZE | WinUser.SWP_NOACTIVATE | WinUser.SWP_FRAMECHANGED
            )
        } catch (Exception ignored) {
            // Fallback already applied with stage.setMouseTransparent(clickThrough)
        }
    }

    private static boolean isWindows() {
        String os = System.getProperty('os.name', '').toLowerCase()
        return os.contains('win')
    }

    private static long resolveWindowHandle(Stage stage) {
        try {
            Class<?> stageHelper = Class.forName('com.sun.javafx.stage.StageHelper')
            Method getPeer = stageHelper.getDeclaredMethod('getPeer', Stage.class)
            getPeer.setAccessible(true)
            Object peer = getPeer.invoke(null, stage)
            if (peer == null) {
                return 0L
            }

            Object platformWindow = invokeNoArg(peer, ['getPlatformWindow', 'getWindow', 'getPeer'])
            if (platformWindow == null) {
                return 0L
            }

            Object handle = invokeNoArg(platformWindow, ['getRawHandle', 'getNativeWindow', 'getNativeWindowHandle', 'getHandle'])
            if (handle instanceof Number) {
                return ((Number) handle).longValue()
            }
            if (handle instanceof Pointer) {
                return Pointer.nativeValue((Pointer) handle)
            }
        } catch (Exception ignored) {
            // fallback will be used
        }
        return 0L
    }

    private static Object invokeNoArg(Object target, List<String> methodNames) {
        if (target == null) {
            return null
        }
        for (String methodName : methodNames) {
            try {
                Method method = target.getClass().getMethod(methodName)
                method.setAccessible(true)
                return method.invoke(target)
            } catch (Exception ignored) {
                // try next
            }
        }
        return null
    }
}

