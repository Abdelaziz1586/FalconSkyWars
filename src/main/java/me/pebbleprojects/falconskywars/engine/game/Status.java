package me.pebbleprojects.falconskywars.engine.game;

public enum Status {

    FAILED("Failed"),
    WAITING("Waiting"),
    STARTING("Starting"),
    WARMUP("Warmup"),
    ENDED("Ended"),
    PLAYING("Playing");


    public final String label;

    Status(final String label) {
        this.label = label;
    }
}
