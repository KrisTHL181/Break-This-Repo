package com.mojang.authlib.minecraft;

public interface TelemetrySession {
    TelemetrySession DISABLED = new TelemetrySession() {
        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        public TelemetryEvent createNewEvent(final String type) {
            return TelemetryEvent.EMPTY;
        }
    };

    boolean isEnabled();

    TelemetryEvent createNewEvent(String type);
}
