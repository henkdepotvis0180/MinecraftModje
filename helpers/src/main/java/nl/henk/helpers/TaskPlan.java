package nl.henk.helpers;

import java.util.regex.Pattern;

/** Closed command vocabulary: model text is never a Minecraft command. */
public record TaskPlan(Action action, int count) {
    public enum Action { CHOP, BUILD, STOP, STATUS }
    private static final Pattern CHOP = Pattern.compile("\\{\\s*\"action\"\\s*:\\s*\"chop\"\\s*,\\s*\"count\"\\s*:\\s*(\\d{1,3})\\s*}");
    private static final Pattern SIMPLE = Pattern.compile("\\{\\s*\"action\"\\s*:\\s*\"(build|stop|status)\"\\s*}");
    public TaskPlan {
        if (action == null || (action == Action.CHOP ? count < 1 || count > 64 : count != 0))
            throw new IllegalArgumentException("Ongeldige taak; houtaantal moet 1 t/m 64 zijn.");
    }
    public static TaskPlan parse(String json) {
        var chop = CHOP.matcher(json.trim());
        if (chop.matches()) return new TaskPlan(Action.CHOP, Integer.parseInt(chop.group(1)));
        var simple = SIMPLE.matcher(json.trim());
        if (simple.matches()) return new TaskPlan(Action.valueOf(simple.group(1).toUpperCase(java.util.Locale.ROOT)), 0);
        throw new IllegalArgumentException("Onbekend AI-antwoord. Gebruik chop, build, stop of status.");
    }
}
