package com.telemed.common.constant;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ScheduleSlotConstants {

    public static final LocalTime START_TIME = LocalTime.of(8, 0);
    public static final LocalTime END_TIME = LocalTime.of(18, 0);
    public static final int SLOT_INTERVAL_MINUTES = 15;
    public static final DateTimeFormatter SLOT_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private static final String[] ALL_SLOT_TIMES;

    static {
        List<String> slots = new ArrayList<>();
        LocalTime current = START_TIME;
        while (!current.isAfter(END_TIME)) {
            slots.add(current.format(SLOT_FORMATTER));
            current = current.plusMinutes(SLOT_INTERVAL_MINUTES);
        }
        ALL_SLOT_TIMES = slots.toArray(new String[0]);
    }

    public static String[] getAllSlotTimes() {
        return ALL_SLOT_TIMES.clone();
    }

    public static int getSlotIndex(String slotTime) {
        LocalTime time = LocalTime.parse(slotTime, SLOT_FORMATTER);
        int minutesFromStart = (time.getHour() - START_TIME.getHour()) * 60
                + (time.getMinute() - START_TIME.getMinute());
        return minutesFromStart / SLOT_INTERVAL_MINUTES;
    }

    public static String getSlotTimeByIndex(int index) {
        if (index < 0 || index >= ALL_SLOT_TIMES.length) {
            throw new IllegalArgumentException("Invalid slot index: " + index);
        }
        return ALL_SLOT_TIMES[index];
    }

    private ScheduleSlotConstants() {
    }
}
