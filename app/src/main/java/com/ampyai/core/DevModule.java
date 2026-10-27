package com.ampyai.core;

/** Extension point for independent, on-device AmpyDev command engines. */
public interface DevModule {
    /** Return null when this module does not understand the request. */
    Result apply(String request, UiDocument document);

    final class Result {
        public final boolean changed;
        public final String reply;

        public Result(boolean changed, String reply) {
            this.changed = changed;
            this.reply = reply;
        }
    }
}
