package nl.henk.helpers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

final class Ollama {
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private Ollama() {}
    static CompletableFuture<TaskPlan> plan(String text) {
        if (text.isBlank() || text.length() > 500) throw new IllegalArgumentException("Gebruik 1 tot 500 tekens.");
        var body = new JsonObject();
        body.addProperty("model", HenkHelpers.MODEL.get());
        body.addProperty("stream", false);
        body.addProperty("format", "json");
        body.addProperty("system", "Translate the user's Dutch or English request into exactly one JSON task. "
                + "Allowed outputs: {\"action\":\"chop\",\"count\":N} with N from 1 through 64, "
                + "{\"action\":\"build\"} for an oak hut, {\"action\":\"stop\"}, or {\"action\":\"status\"}. "
                + "For unsupported or unclear requests return status. Never include explanations or additional keys.");
        body.addProperty("prompt", text);
        var options = new JsonObject();
        options.addProperty("temperature", 0);
        options.addProperty("num_predict", 80);
        body.add("options", options);
        // Fixed loopback address: player chat cannot select a remote endpoint.
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:11434/api/generate"))
                .timeout(Duration.ofSeconds(90)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            if (response.statusCode() != 200 || response.body().length() > 65536)
                throw new IllegalStateException("Ollama gaf geen bruikbaar antwoord.");
            var result = JsonParser.parseString(response.body()).getAsJsonObject();
            return TaskPlan.parse(result.get("response").getAsString());
        });
    }
}
