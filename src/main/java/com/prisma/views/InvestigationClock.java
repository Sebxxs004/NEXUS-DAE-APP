package com.prisma.views;

import java.time.Duration;

public final class InvestigationClock {
    private static final Duration INVESTIGATION_DURATION = Duration.ofHours(3);
    private static final long STARTED_AT_MILLIS = System.currentTimeMillis();
    private static long deductedMillis = 0L;

    private InvestigationClock() {
    }

    public static Duration getDuration() {
        return INVESTIGATION_DURATION;
    }

    public static Duration getElapsed() {
        long elapsedMillis = Math.max(0L, System.currentTimeMillis() - STARTED_AT_MILLIS);
        return Duration.ofMillis(elapsedMillis);
    }

    public static Duration getRemaining() {
        Duration remaining = INVESTIGATION_DURATION.minus(getElapsed()).minus(Duration.ofMillis(getDeductedMillis()));
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    public static synchronized void deduct(Duration duration) {
        if (duration == null || duration.isNegative() || duration.isZero()) {
            return;
        }
        deductedMillis = Math.max(0L, deductedMillis + duration.toMillis());
    }

    public static synchronized long getDeductedMillis() {
        return deductedMillis;
    }

    public static String formatRemaining() {
        return formatDuration(getRemaining());
    }

    public static boolean isCritical() {
        return getRemaining().compareTo(Duration.ofMinutes(2)) <= 0;
    }

    public static boolean isWarning() {
        Duration remaining = getRemaining();
        return remaining.compareTo(Duration.ofMinutes(5)) <= 0 && remaining.compareTo(Duration.ofMinutes(2)) > 0;
    }

    private static String formatDuration(Duration duration) {
        long totalSeconds = Math.max(0L, duration.getSeconds());
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}