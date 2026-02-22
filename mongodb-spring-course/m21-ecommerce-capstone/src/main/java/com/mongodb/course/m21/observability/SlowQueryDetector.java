package com.mongodb.course.m21.observability;

import com.mongodb.event.CommandFailedEvent;
import com.mongodb.event.CommandListener;
import com.mongodb.event.CommandSucceededEvent;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public class SlowQueryDetector implements CommandListener {

    private static final Set<String> TRACKED_COMMANDS = Set.of(
            "find", "insert", "update", "delete", "aggregate",
            "count", "distinct", "findAndModify", "getMore", "createIndexes"
    );

    private volatile long thresholdMs;
    private final CopyOnWriteArrayList<SlowQueryEntry> capturedQueries = new CopyOnWriteArrayList<>();

    public SlowQueryDetector(long thresholdMs) {
        this.thresholdMs = thresholdMs;
    }

    @Override
    public void commandSucceeded(CommandSucceededEvent event) {
        captureIfSlow(event.getCommandName(),
                TimeUnit.NANOSECONDS.toMillis(event.getElapsedTime(TimeUnit.NANOSECONDS)),
                event.getDatabaseName());
    }

    @Override
    public void commandFailed(CommandFailedEvent event) {
        captureIfSlow(event.getCommandName(),
                TimeUnit.NANOSECONDS.toMillis(event.getElapsedTime(TimeUnit.NANOSECONDS)),
                event.getDatabaseName());
    }

    private void captureIfSlow(String commandName, long durationMs, String databaseName) {
        if (!TRACKED_COMMANDS.contains(commandName)) {
            return;
        }
        if (durationMs >= thresholdMs) {
            capturedQueries.add(new SlowQueryEntry(commandName, durationMs, databaseName, Instant.now()));
        }
    }

    public List<SlowQueryEntry> getCapturedQueries() {
        return Collections.unmodifiableList(List.copyOf(capturedQueries));
    }

    public int getCapturedCount() {
        return capturedQueries.size();
    }

    public void clear() {
        capturedQueries.clear();
    }

    public void setThresholdMs(long thresholdMs) {
        this.thresholdMs = thresholdMs;
    }

    public long getThresholdMs() {
        return thresholdMs;
    }
}
