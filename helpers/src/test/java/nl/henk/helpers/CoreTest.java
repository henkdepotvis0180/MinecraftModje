package nl.henk.helpers;

import java.util.HashSet;

public final class CoreTest {
    private static void check(boolean value) { if (!value) throw new AssertionError(); }
    private static void reject(String text) {
        try { TaskPlan.parse(text); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Accepted: " + text);
    }
    public static void main(String[] args) {
        check(TaskPlan.parse("{\"action\":\"chop\",\"count\":16}").count() == 16);
        check(TaskPlan.parse(" { \"action\": \"stop\" } ").action() == TaskPlan.Action.STOP);
        check(TaskPlan.parse("{\"action\":\"build\"}").action() == TaskPlan.Action.BUILD);
        check(TaskPlan.parse("{\"action\":\"status\"}").action() == TaskPlan.Action.STATUS);
        for (String input : new String[]{"{\"action\":\"chop\",\"count\":0}", "{\"action\":\"chop\",\"count\":65}",
                "{\"action\":\"chop\",\"count\":-1}", "{\"action\":\"op\"}",
                "{\"action\":\"stop\",\"command\":\"kill @e\"}", "```json\n{\"action\":\"stop\"}\n```",
                "{\"action\":\"build\"} trailing", "{\"action\":\"stop\",\"action\":\"build\"}"}) reject(input);
        var hut = Blueprint.hut();
        check(hut.size() == 96);
        check(new HashSet<>(hut).size() == 96);
        check(!hut.contains(new Blueprint.Cell(2, 1, 0)));
        check(!hut.contains(new Blueprint.Cell(2, 2, 0)));
        check(!hut.contains(new Blueprint.Cell(2, 2, 2)));
        check(hut.contains(new Blueprint.Cell(2, 4, 2)));
        System.out.println("Core tests passed: bounded AI tasks and 96-block hut blueprint.");
    }
}
