package org.example.triharf.services;

import java.util.*;

public class EventManager {
    public enum EventType {
        DOUBLE_POINTS,      // 2x score this round
        HALF_TIME,          // Timer reduced by 50%
        EXTRA_TIME,         // +30 seconds
        LETTER_SWAP,        // Change to different letter
        CATEGORY_LOCK,      // Remove random category
        BONUS_CATEGORY      // Add extra category
    }

    private Random random = new Random();

    // Trigger random event with probability
    public Event triggerRandomEvent(double probability) {
        if (random.nextDouble() < probability) {
            EventType type = EventType.values()[random.nextInt(EventType.values().length)];
            return new Event(type, System.currentTimeMillis());
        }
        return null;
    }

    /**
     * Apply event effects to game state
     */
    public void applyEvent(Event event, GameEngine engine, ResultsManager resultsManager) {
        switch (event.getType()) {
            case DOUBLE_POINTS -> event.getData().put("scoreMultiplier", 2.0);

            case HALF_TIME -> {
                int currentTime = engine.getRemainingTime();
                engine.setRemainingTime(currentTime / 2);
            }

            case EXTRA_TIME -> {
                int currentTime = engine.getRemainingTime();
                engine.setRemainingTime(currentTime + 30);
            }

            case LETTER_SWAP -> {
                Character newLetter = engine.generateRandomLetter();
                event.getData().put("newLetter", newLetter);
            }

            case CATEGORY_LOCK -> {
                // Store locked category index
                event.getData().put("lockedCategoryIndex", random.nextInt(8));
            }

            case BONUS_CATEGORY -> {
                event.getData().put("bonusCategory", true);
            }
        }
    }

    /**
     * Get event description for display
     */
    public String getEventDescription(Event event) {
        return switch(event.getType()) {
            case DOUBLE_POINTS -> "🎉 DOUBLE POINTS! All answers worth 2x!";
            case HALF_TIME -> "⚡ TIME CUT! Timer reduced by 50%!";
            case EXTRA_TIME -> "⏰ BONUS TIME! +30 seconds added!";
            case LETTER_SWAP -> "🔄 LETTER CHANGE! New letter: " + event.getData().get("newLetter");
            case CATEGORY_LOCK -> "🔒 CATEGORY LOCKED! One category removed!";
            case BONUS_CATEGORY -> "✨ BONUS CATEGORY! Extra category added!";
        };
    }

    // Inner class for event data
    public static class Event {
        private EventType type;
        private long timestamp;
        private Map<String, Object> data;

        // Constructor, getters, setters
        public Event(EventType type, long timestamp) {
            this.type = type;
            this.timestamp = timestamp;
            this.data = new HashMap<>();
        }

        public EventType getType() {
            return type;
        }
        public void setType(EventType type) {
            this.type = type;
        }

        public long getTimestamp() {
            return timestamp;
        }
        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public Map<String, Object> getData() {
            return data;
        }
        public void setData(Map<String, Object> data) {
            this.data = data;
        }
    }
}