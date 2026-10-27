package com.careerfit.service;

import java.util.ArrayList;
import java.util.List;

/** Compares program output with the expected output: line endings and trailing spaces/blank lines are ignored. */
public final class OutputComparator {

    private OutputComparator() {
    }

    public static boolean matches(String expected, String actual) {
        return normalize(expected).equals(normalize(actual));
    }

    static List<String> normalize(String text) {
        List<String> lines = new ArrayList<>();
        if (text == null) return lines;
        for (String line : text.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1)) {
            lines.add(line.stripTrailing());
        }
        while (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) {
            lines.remove(lines.size() - 1);
        }
        return lines;
    }
}
