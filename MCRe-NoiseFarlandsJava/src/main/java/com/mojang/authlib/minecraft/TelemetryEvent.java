package com.mojang.authlib.minecraft;

public interface TelemetryEvent extends TelemetryPropertyContainer {
    TelemetryEvent EMPTY = new TelemetryEvent() {
        @Override
        public void addProperty(final String id, final String value) {
        }

        @Override
        public void addProperty(final String id, final int value) {
        }

        @Override
        public void addProperty(final String id, final long value) {
        }

        @Override
        public void addProperty(final String id, final boolean value) {
        }

        @Override
        public void addNullProperty(final String id) {
        }

        @Override
        public void send() {
        }
    };

    void send();
}
