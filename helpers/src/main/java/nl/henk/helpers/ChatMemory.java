package nl.henk.helpers;

import java.util.ArrayDeque;
import java.util.List;

/** Short, per-worker session history. Never stored on disk. Access on server thread. */
public final class ChatMemory {
    public record Message(String role, String content) {}
    private final ArrayDeque<Message> messages = new ArrayDeque<>();
    public List<Message> snapshot() { return List.copyOf(messages); }
    public void remember(String prompt, String answer) {
        if (prompt.length() > 500 || answer.length() > 240) throw new IllegalArgumentException("Gesprek te lang.");
        messages.addLast(new Message("user", prompt));
        messages.addLast(new Message("assistant", answer));
        while (messages.size() > 8) messages.removeFirst();
    }
    public void clear() { messages.clear(); }
}
