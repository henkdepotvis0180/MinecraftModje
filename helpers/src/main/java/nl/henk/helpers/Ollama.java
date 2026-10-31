package nl.henk.helpers;

import java.util.List;
import java.util.concurrent.CompletableFuture;

final class Ollama {
    private static final OllamaClient CLIENT = new OllamaClient();
    private Ollama() {}
    static CompletableFuture<ChatReply> chat(String text, String status, List<ChatMemory.Message> history) {
        return CLIENT.chat(HenkHelpers.MODEL.get(), text, status, history);
    }
}
