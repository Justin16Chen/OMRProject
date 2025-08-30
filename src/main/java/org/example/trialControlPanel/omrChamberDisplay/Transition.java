package org.example.trialControlPanel.omrChamberDisplay;

public record Transition(DisplayState from, DisplayState to) {
    @Override
    public boolean equals(Object other) {
        if (other.getClass() != Transition.class)
            return false;
        Transition otherTransition = (Transition) other;
        return otherTransition.from == from && otherTransition.to == to;
    }
}
