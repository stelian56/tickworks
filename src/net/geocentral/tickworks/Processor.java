package net.geocentral.tickworks;

import java.util.ArrayList;
import java.util.List;

public class Processor {

    private List<Rule> rules;
    
    public Processor() {
        rules = new ArrayList<Rule>();
    }
    
    public void addRule(Rule rule) {
        rules.add(rule);
    }

    public void start() {
        for (Rule rule : rules) {
            rule.start();
        }
    }
}
