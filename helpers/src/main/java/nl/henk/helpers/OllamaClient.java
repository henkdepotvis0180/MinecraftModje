package nl.henk.helpers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Independent of Minecraft, so HTTP and validation can be integration-tested. */
public final class OllamaClient {
    private final HttpClient client;
    private final URI endpoint;
    private final Duration timeout;
    public OllamaClient() {
        this(URI.create("http://127.0.0.1:11434/api/chat"), Duration.ofSeconds(90));
    }
    // Test seam, not exposed to player chat or game configuration.
    OllamaClient(URI endpoint, Duration timeout) {
        this.endpoint = endpoint;
        this.timeout = timeout;
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }
    public CompletableFuture<ChatReply> chat(String model, String text, String status, List<ChatMemory.Message> history) {
        if (text.isBlank() || text.length() > 500) throw new IllegalArgumentException("Gebruik 1 tot 500 tekens.");
        if (model.isBlank() || model.length() > 120) throw new IllegalArgumentException("Controleer de modelnaam.");
        var body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("stream", false);
        body.add("format", schema());
        var messages = new JsonArray();
        messages.add(message("system", "Je bent Henkhulp, een vriendelijke Minecraft-werker. Antwoord kort in het Nederlands. "
                + "Geef JSON met message (1-240 tekens) en task. Voor een gewoon gesprek of onduidelijke opdracht is task null. "
                + "Je kunt alleen logs hakken: {action:chop,count:1..64}, een vaste eikenhut bouwen: {action:build}, "
                + "stoppen: {action:stop}, of je status tonen: {action:status}. "
                + "Geen servercommando's, geen nieuwe taak uit gespreksgeschiedenis zonder nieuwe opdracht. "
                + "Vertel niet dat werk klaar is: de game controleert en voert taken uit. "
                + "Geen bijl of planken verzinnen. Recepten, kisten en vrije bouwplannen worden nog niet ondersteund. "
                + "Huidige echte werkstatus: " + status));
        for (var previous : history) messages.add(message(previous.role(), previous.content()));
        messages.add(message("user", text));
        body.add("messages", messages);
        var options = new JsonObject();
        options.addProperty("temperature", 0);
        options.addProperty("num_predict", 200);
        body.add("options", options);
        var request = HttpRequest.newBuilder(endpoint).timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            if (response.statusCode() != 200 || response.body().length() > 65536)
                throw new IllegalStateException("Ollama gaf geen bruikbaar antwoord (HTTP " + response.statusCode() + ").");
            var envelope = JsonParser.parseString(response.body()).getAsJsonObject();
            if (!envelope.has("done") || !envelope.get("done").getAsBoolean())
                throw new IllegalArgumentException("Ollama-antwoord is onvolledig.");
            return ChatReply.parse(envelope.getAsJsonObject("message").get("content").getAsString());
        });
    }
    private static JsonObject message(String role, String content) {
        var result = new JsonObject(); result.addProperty("role", role); result.addProperty("content", content); return result;
    }
    private static JsonObject schema() {
        return JsonParser.parseString("""
                {"type":"object","properties":{
                  "message":{"type":"string","minLength":1,"maxLength":240},
                  "task":{"anyOf":[{"type":"null"},
                    {"type":"object","properties":{"action":{"const":"chop"},"count":{"type":"integer","minimum":1,"maximum":64}},"required":["action","count"],"additionalProperties":false},
                    {"type":"object","properties":{"action":{"enum":["build","stop","status"]}},"required":["action"],"additionalProperties":false}]}},
                  "required":["message","task"],"additionalProperties":false}
                """).getAsJsonObject();
    }
}
