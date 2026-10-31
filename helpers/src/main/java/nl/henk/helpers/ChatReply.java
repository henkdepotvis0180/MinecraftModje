package nl.henk.helpers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Set;

/** Validated model output. A null task means conversation only. */
public record ChatReply(String message, TaskPlan task) {
    public static ChatReply parse(String text) {
        if (text == null || text.length() > 4096) throw new IllegalArgumentException("AI-antwoord te groot.");
        var root = JsonParser.parseString(text).getAsJsonObject();
        if (!root.keySet().equals(Set.of("message", "task"))) throw new IllegalArgumentException("Onbekende antwoordvelden.");
        var message = root.get("message");
        if (!message.isJsonPrimitive() || !message.getAsJsonPrimitive().isString())
            throw new IllegalArgumentException("Antwoordtekst ontbreekt.");
        String clean = message.getAsString().replaceAll("\\p{Cntrl}", " ").strip();
        if (clean.isBlank() || clean.length() > 240) throw new IllegalArgumentException("Antwoord moet 1 tot 240 tekens zijn.");
        TaskPlan plan = null;
        if (!root.get("task").isJsonNull()) {
            JsonObject input = root.getAsJsonObject("task");
            var action = input.get("action");
            if (action == null || !action.isJsonPrimitive() || !action.getAsJsonPrimitive().isString())
                throw new IllegalArgumentException("Taak ontbreekt.");
            var canonical = new JsonObject();
            canonical.add("action", action);
            boolean chop = action.getAsString().equals("chop");
            if (!input.keySet().equals(chop ? Set.of("action", "count") : Set.of("action")))
                throw new IllegalArgumentException("Onbekende taakvelden.");
            if (chop) canonical.add("count", input.get("count"));
            // Canonical key order avoids rejecting valid model output with count first.
            plan = TaskPlan.parse(canonical.toString());
        }
        return new ChatReply(clean, plan);
    }
}
