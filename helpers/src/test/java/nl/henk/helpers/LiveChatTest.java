package nl.henk.helpers;

import java.util.List;

/** Optional smoke test against a real local Ollama service and installed model. */
public final class LiveChatTest {
    public static void main(String[] args) {
        String model = args.length == 0 ? "qwen2.5:3b" : args[0];
        var client = new OllamaClient();
        var greeting = client.chat(model, "Hoi, wat kun je?", "Ik wacht op werk.", List.of()).join();
        if (greeting.task() != null) throw new AssertionError("Greeting unexpectedly started a task: " + greeting);
        System.out.println("Conversation: " + greeting.message());
        var memory = new ChatMemory();
        memory.remember("Hoi, wat kun je?", greeting.message());
        var chopping = client.chat(model, "Hak 16 boomstammen voor me.", "Ik wacht op werk.", memory.snapshot()).join();
        if (!new TaskPlan(TaskPlan.Action.CHOP, 16).equals(chopping.task()))
            throw new AssertionError("Expected chop 16, got " + chopping);
        System.out.println("Task: " + chopping.task() + " / " + chopping.message());
        var stop = client.chat(model, "Stop met werken.", "CHOP: 3 gedaan.", List.of()).join();
        if (!new TaskPlan(TaskPlan.Action.STOP, 0).equals(stop.task()))
            throw new AssertionError("Expected stop, got " + stop);
        System.out.println("Live Ollama tests passed: conversation, chop 16 and stop.");
    }
}
