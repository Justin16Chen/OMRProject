package org.example;

import java.util.ArrayList;
import java.util.HashMap;

public class StatsTest {
    public static void main(String[] args) {
        HashMap<String, Integer> outcomes = new HashMap<>();
        String[] outcome1 = { "BA ", "BE " };
        String[] outcome2 = { "AB ", "AD " };
        String[] outcome3 = { "DE ", "DA " };
        String[] outcome4 = { "EB ", "ED " };

        for (int i=0; i<500; i++) {
            String result = outcome1[choose()] + outcome2[choose()] + outcome3[choose()] + outcome4[choose()];
            outcomes.put(result, outcomes.getOrDefault(result, 0) + 1);
        }
        System.out.println("distinct outcomes: " + outcomes.keySet().size());
        for (String key : outcomes.keySet())
            System.out.println(key + " | " + outcomes.get(key));
    }
    private static int choose() {
        return (int) (Math.random() * 2);
    }
}
