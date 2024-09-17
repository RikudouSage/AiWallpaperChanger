package cz.chrastecky.aiwallpaperchanger.helper;

import androidx.annotation.NonNull;

public class DateInterval {
    private int years = 0;
    private int months = 0;
    private int days = 0;
    private int weeks = 0;
    private int hours = 0;
    private int minutes = 0;
    private int seconds = 0;

    public DateInterval(@NonNull final String interval) {
        this.parse(interval);
    }

    public int getYears() {
        return years;
    }

    public int getMonths() {
        return months;
    }

    public int getDays() {
        return days;
    }

    public int getWeeks() {
        return weeks;
    }

    public int getHours() {
        return hours;
    }

    public int getMinutes() {
        return minutes;
    }

    public int getSeconds() {
        return seconds;
    }

    private void parse(@NonNull final String interval) {
        char[] chars = interval.toCharArray();

        boolean timeMode = false;
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < chars.length; ++i) {
            if (i == 0 && chars[i] != 'P') {
                throw new RuntimeException("Invalid format, date interval must start with P");
            } else if (i == 0) {
                continue;
            }
            if (chars[i] == 'T') {
                timeMode = true;
                continue;
            }

            if (Character.isDigit(chars[i])) {
                buffer.append(chars[i]);
                continue;
            }

            int value = Integer.parseInt(buffer.toString());
            buffer = new StringBuilder();
            switch (chars[i]) {
                case 'Y':
                    this.years = value;
                    break;
                case 'M':
                    if (timeMode) {
                        this.minutes = value;
                    } else {
                        this.months = value;
                    }
                    break;
                case 'D':
                    this.days = value;
                    break;
                case 'W':
                    this.weeks = value;
                    break;
                case 'H':
                    this.hours = value;
                    break;
                case 'S':
                    this.seconds = value;
                    break;
                default:
                    throw new RuntimeException("Unknown character: " + chars[i]);
            }
        }
    }
}
