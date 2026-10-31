package nl.henk.helpers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Local HTTP integration tests, not a test of a real Ollama model. */
public final class ChatTest {
    private static void check(boolean value) { if (!value) throw new AssertionError(); }
    private static void rejected(Runnable action) {
        try { action.run(); } catch (RuntimeException expected) { return; }
        throw new AssertionError("Invalid response accepted");
    }
    private static String envelope(String content) {
        var message = new JsonObject(); message.addProperty("role", "assistant"); message.addProperty("content", content);
        var root = new JsonObject(); root.add("message", message); root.addProperty("done", true); return root.toString();
    }
    public static void main(String[] args) throws Exception {
        var memory = new ChatMemory();
        for (int i = 0; i < 6; i++) memory.remember("vraag " + i, "antwoord " + i);
        check(memory.snapshot().size() == 8);
        check(memory.snapshot().getFirst().content().equals("vraag 2"));
        var saved = memory.snapshot(); memory.clear(); check(saved.size() == 8 && memory.snapshot().isEmpty());

        var reply = ChatReply.parse("{\"message\":\"Ik help je.\",\"task\":{\"count\":16,\"action\":\"chop\"}}");
        check(reply.task().count() == 16);
        check(ChatReply.parse("{\"task\":null,\"message\":\"Hoi Henk!\"}").task() == null);
        for (String input : List.of(
                "{\"message\":\"x\",\"task\":{\"action\":\"op\"}}",
                "{\"message\":\"x\",\"task\":{\"action\":\"chop\",\"count\":65}}",
                "{\"message\":\"x\",\"task\":{\"action\":\"chop\",\"count\":1.5}}",
                "{\"message\":\"x\",\"task\":{\"action\":\"chop\",\"count\":\"16\"}}",
                "{\"message\":\"x\",\"task\":{\"action\":\"stop\",\"command\":\"kill @e\"}}",
                "{\"message\":\"x\",\"task\":null,\"command\":\"op\"}",
                "{\"message\":\"\",\"task\":null}",
                "{\"message\":123,\"task\":null}",
                "{\"message\":\"" + "x".repeat(241) + "\",\"task\":null}")) rejected(() -> ChatReply.parse(input));

        var response = new AtomicReference<>(envelope("{\"message\":\"Hoi Henk!\",\"task\":null}"));
        var status = new AtomicInteger(200);
        var delay = new AtomicInteger(0);
        var request = new AtomicReference<JsonObject>();
        var count = new AtomicInteger(0);
        var executor = Executors.newCachedThreadPool();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(executor);
        server.createContext("/api/chat", exchange -> {
            try {
                check(exchange.getRequestMethod().equals("POST"));
                request.set(JsonParser.parseString(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject());
                count.incrementAndGet();
                try { Thread.sleep(delay.get()); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                byte[] bytes = response.get().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(status.get(), bytes.length);
                exchange.getResponseBody().write(bytes);
            } catch (java.io.IOException ignored) { /* Client timed out. */ }
            finally { exchange.close(); }
        });
        server.start();
        try {
            var uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/api/chat");
            var client = new OllamaClient(uri, Duration.ofSeconds(5));
            check(client.chat("test-model", "Hoi, ik ben Henk!", "Ik wacht.", saved).join().message().equals("Hoi Henk!"));
            check(request.get().get("model").getAsString().equals("test-model"));
            check(!request.get().get("stream").getAsBoolean());
            check(request.get().getAsJsonObject("format").get("type").getAsString().equals("object"));
            var messages = request.get().getAsJsonArray("messages");
            check(messages.size() == 10);
            check(messages.get(0).getAsJsonObject().get("content").getAsString().contains("Ik wacht."));
            check(messages.get(9).getAsJsonObject().get("content").getAsString().equals("Hoi, ik ben Henk!"));
            response.set(envelope("{\"message\":\"Ik ga hakken.\",\"task\":{\"count\":16,\"action\":\"chop\"}}"));
            check(client.chat("test-model", "Hak hout", "Ik wacht.", List.of()).join().task().count() == 16);
            response.set(envelope("{\"message\":\"x\",\"task\":{\"action\":\"kill\"}}"));
            rejected(() -> client.chat("test-model", "test", "", List.of()).join());
            status.set(404); rejected(() -> client.chat("missing-model", "test", "", List.of()).join());
            status.set(200); response.set("{\"done\":false}"); rejected(() -> client.chat("test-model", "test", "", List.of()).join());
            response.set("broken json"); rejected(() -> client.chat("test-model", "test", "", List.of()).join());
            response.set("x".repeat(65537)); rejected(() -> client.chat("test-model", "test", "", List.of()).join());
            int before = count.get();
            rejected(() -> client.chat("test-model", " ", "", List.of()));
            rejected(() -> client.chat("test-model", "x".repeat(501), "", List.of()));
            check(before == count.get());
            delay.set(500);
            var fast = new OllamaClient(uri, Duration.ofMillis(100));
            try {
                fast.chat("test-model", "test", "", List.of()).join();
                throw new AssertionError("Expected HTTP timeout");
            } catch (CompletionException expected) {
                check(expected.getCause() instanceof java.net.http.HttpTimeoutException);
            }
            System.out.println("Chat tests passed: conversation, history isolation/bounds, HTTP requests, tasks, invalid responses, errors and timeout.");
        } finally {
            server.stop(0); executor.shutdownNow();
        }
    }
}
