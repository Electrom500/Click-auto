package fr.clickauto.persistence

import fr.clickauto.model.Action
import fr.clickauto.model.ClickAction
import fr.clickauto.model.ClickType
import fr.clickauto.model.KeyAction
import fr.clickauto.model.Sequence
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic

import java.nio.charset.StandardCharsets

/**
 * Persist/restore a Sequence as JSON.
 * Keeps the format simple so it can be used as a profile file.
 */
@CompileStatic
final class SequenceProfileStore {
    static void save(File file, Sequence sequence) {
        if (file == null) {
            throw new IllegalArgumentException('file must not be null')
        }
        if (sequence == null) {
            throw new IllegalArgumentException('sequence must not be null')
        }

        Map<String, Object> payload = [
                name            : sequence.getName(),
                cycles          : sequence.getCycles(),
                totalDurationMs : sequence.getTotalDurationMs(),
                initialDelayMs  : sequence.getInitialDelayMs(),
                actions         : sequence.getActions().collect { Action action -> actionToMap(action) }
        ]

        file.withWriter(StandardCharsets.UTF_8.name()) { writer ->
            writer.write(JsonOutput.prettyPrint(JsonOutput.toJson(payload)))
        }
    }

    static Sequence load(File file) {
        if (file == null) {
            throw new IllegalArgumentException('file must not be null')
        }
        if (!file.exists()) {
            throw new IllegalArgumentException("File not found: ${file.absolutePath}")
        }

        def parsed = new JsonSlurper().parse(file)
        Map map = parsed instanceof Map ? (Map) parsed : [:]

        Sequence sequence = new Sequence(asString(map.get('name'), 'Sequence chargee'))
        sequence.setCycles(asInt(map.get('cycles'), 1))
        sequence.setTotalDurationMs(asLong(map.get('totalDurationMs'), 0L))
        sequence.setInitialDelayMs(asLong(map.get('initialDelayMs'), 0L))

        Object actionsValue = map.get('actions')
        if (actionsValue instanceof List) {
            for (Object entry : (List) actionsValue) {
                if (entry instanceof Map) {
                    Action action = mapToAction((Map) entry)
                    if (action != null) {
                        sequence.addAction(action)
                    }
                }
            }
        }

        return sequence
    }

    private static Map<String, Object> actionToMap(Action action) {
        Map<String, Object> data = [
                actionType: action.getActionType(),
                delayMs   : action.getDelayMs()
        ] as Map<String, Object>

        if (action instanceof ClickAction) {
            ClickAction click = (ClickAction) action
            data.put('clickType', click.getClickType().name())
            data.put('x', click.getX())
            data.put('y', click.getY())
            data.put('enabled', click.isEnabled())
        } else if (action instanceof KeyAction) {
            KeyAction key = (KeyAction) action
            data.put('keyCode', key.getKeyCode())
            data.put('durationMs', key.getDurationMs())
            data.put('enabled', key.isEnabled())
        }

        return data
    }

    private static Action mapToAction(Map data) {
        String actionType = asString(data.get('actionType'), '')
        boolean enabled = data.containsKey('enabled') ? asBoolean(data.get('enabled'), true) : true

        if (actionType == 'CLICK') {
            ClickType clickType = ClickType.valueOf(asString(data.get('clickType'), 'LEFT'))
            long delayMs = asLong(data.get('delayMs'), 0L)
            int x = asInt(data.get('x'), 0)
            int y = asInt(data.get('y'), 0)
            ClickAction action = new ClickAction(clickType, delayMs, x, y)
            action.setEnabled(enabled)
            return action
        }

        if (actionType == 'KEY') {
            int keyCode = asInt(data.get('keyCode'), 0)
            long durationMs = asLong(data.get('durationMs'), 0L)
            long delayMs = asLong(data.get('delayMs'), 0L)
            KeyAction action = new KeyAction(keyCode, durationMs, delayMs)
            action.setEnabled(enabled)
            return action
        }

        return null
    }

    private static String asString(Object value, String defaultValue) {
        return value == null ? defaultValue : value.toString()
    }

    private static int asInt(Object value, int defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).intValue()
        }
        if (value == null) {
            return defaultValue
        }
        try {
            return Integer.parseInt(value.toString())
        } catch (Exception ignored) {
            return defaultValue
        }
    }

    private static long asLong(Object value, long defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).longValue()
        }
        if (value == null) {
            return defaultValue
        }
        try {
            return Long.parseLong(value.toString())
        } catch (Exception ignored) {
            return defaultValue
        }
    }

    private static boolean asBoolean(Object value, boolean defaultValue) {
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue()
        }
        if (value == null) {
            return defaultValue
        }
        return Boolean.parseBoolean(value.toString())
    }
}

