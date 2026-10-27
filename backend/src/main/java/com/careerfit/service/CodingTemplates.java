package com.careerfit.service;

import com.careerfit.entity.CodeLanguage;

import java.util.EnumMap;
import java.util.Map;

/** Starter code shown in the editor. Every problem reads standard input and writes standard output. */
public final class CodingTemplates {

    private CodingTemplates() {
    }

    public static Map<CodeLanguage, String> starters() {
        Map<CodeLanguage, String> map = new EnumMap<>(CodeLanguage.class);
        map.put(CodeLanguage.JAVA, """
            import java.util.*;

            public class Main {
                public static void main(String[] args) {
                    Scanner sc = new Scanner(System.in);
                    // Read the input with sc.nextInt(), sc.next() or sc.nextLine()
                    // Write the answer with System.out.println(...)
                }
            }
            """);
        map.put(CodeLanguage.PYTHON, """
            import sys

            def main():
                data = sys.stdin.read().split()
                # Read the input from data (or use input() line by line)
                # Write the answer with print(...)

            main()
            """);
        map.put(CodeLanguage.JAVASCRIPT, """
            const lines = require('fs').readFileSync(0, 'utf8').split('\\n');
            // Read the input from lines (lines[0] is the first line)
            // Write the answer with console.log(...)
            """);
        return map;
    }
}
