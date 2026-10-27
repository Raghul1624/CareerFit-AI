"""
Coding problems for the Coding Round. Language independent: every program reads standard input and
writes standard output. Expected outputs are NOT written by hand: the generator runs the reference
solution `ref(stdin_text) -> stdout_text` on every sample and hidden input. Problems with a `brute`
solution are additionally cross-checked on many small random inputs.
"""
import random
from functools import lru_cache

ALL = ["JAVA", "PYTHON", "FRONTEND", "FULLSTACK", "QA", "DATA_ANALYST"]
J, P, F, S, Q, D = "JAVA", "PYTHON", "FRONTEND", "FULLSTACK", "QA", "DATA_ANALYST"
PROBLEMS = []


def problem(title, level, roles, statement, input_format, output_format, constraints, samples, hidden, ref,
            brute=None, small=None, notes=None):
    PROBLEMS.append(dict(title=title, level=level, roles=roles, statement=statement, input_format=input_format,
                         output_format=output_format, constraints=constraints, samples=samples, hidden=hidden,
                         ref=ref, brute=brute, small=small, notes=notes or []))


def nums(r, n, lo, hi):
    return [r.randint(lo, hi) for _ in range(n)]


def case(*lines):
    return "\n".join(str(x) for x in lines) + "\n"


def arr_case(r, n, lo, hi, *extra):
    return case(n, " ".join(map(str, nums(r, n, lo, hi))), *extra)


def words(r, n, alphabet="abc", lo=1, hi=5):
    return ["".join(r.choice(alphabet) for _ in range(r.randint(lo, hi))) for _ in range(n)]


def tokens(s):
    return s.split()


# =====================================================================================  EASY
def ref_sum_even(s):
    t = tokens(s); n = int(t[0]); return str(sum(x for x in map(int, t[1:1 + n]) if x % 2 == 0))
r = random.Random(11)
problem("Sum of Even Numbers", "EASY", ALL,
        "Given a list of integers, print the sum of all even numbers in the list. If there is no even number, print 0.",
        "The first line contains n. The second line contains n space-separated integers.",
        "Print a single integer.", "1 <= n <= 1000, -1000 <= each integer <= 1000",
        [case(5, "1 2 3 4 5"), case(3, "7 9 11")],
        [case(1, 8), case(4, "-2 -4 3 5"), arr_case(r, 50, -1000, 1000), arr_case(r, 1000, -1000, 1000), case(2, "0 0")],
        ref_sum_even)


def ref_palindrome(s):
    t = "".join(c.lower() for c in s.strip() if c.isalnum()); return "YES" if t == t[::-1] else "NO"
problem("Palindrome Check", "EASY", [J, P, F, S],
        "Check whether a string is a palindrome. Ignore letter case and ignore every character that is not a letter or a digit.",
        "A single line containing the text (it may contain spaces and punctuation).",
        "Print YES if the text is a palindrome, otherwise print NO.", "1 <= length <= 1000",
        ["A man, a plan, a canal: Panama\n", "Hello\n"],
        ["a\n", "No lemon, no melon\n", "race a car\n", "12321\n", "Was it a car or a cat I saw?\n", "ab\n"],
        ref_palindrome)


def ref_vowels(s):
    return str(sum(1 for c in s.strip().lower() if c in "aeiou"))
problem("Count Vowels", "EASY", [P, F, S],
        "Count the vowels (a, e, i, o, u, in either case) in a line of text.",
        "A single line of text.", "Print the number of vowels.", "1 <= length <= 1000",
        ["Hello World\n", "rhythm\n"],
        ["AEIOU\n", "The quick brown fox jumps over the lazy dog\n", "bcdfg\n", "Programming Is Fun\n", "a\n"],
        ref_vowels)


def ref_minmax(s):
    t = tokens(s); n = int(t[0]); a = list(map(int, t[1:1 + n])); return f"{min(a)} {max(a)}"
r = random.Random(12)
problem("Smallest and Largest", "EASY", [J, P, Q, D],
        "Given a list of integers, print the smallest and the largest value.",
        "The first line contains n. The second line contains n space-separated integers.",
        "Print the smallest and the largest value separated by one space.", "1 <= n <= 1000, -10^6 <= each integer <= 10^6",
        [case(5, "3 1 4 1 5"), case(1, 7)],
        [case(3, "-5 -9 -1"), arr_case(r, 100, -10**6, 10**6), arr_case(r, 1000, -10**6, 10**6), case(2, "5 5")],
        ref_minmax)


def ref_fizz(s):
    n = int(s.split()[0]); out = []
    for i in range(1, n + 1):
        out.append("FizzBuzz" if i % 15 == 0 else "Fizz" if i % 3 == 0 else "Buzz" if i % 5 == 0 else str(i))
    return "\n".join(out)
problem("FizzBuzz", "EASY", [J, F, S, Q],
        "Print the numbers from 1 to n, one per line. For multiples of 3 print Fizz instead of the number, for multiples of 5 print Buzz, and for multiples of both 3 and 5 print FizzBuzz.",
        "A single integer n.", "Print n lines.", "1 <= n <= 1000",
        [case(5), case(15)], [case(1), case(3), case(30), case(100), case(1000)], ref_fizz)


def ref_avg(s):
    t = tokens(s); n = int(t[0]); a = list(map(int, t[1:1 + n])); return f"{sum(a) / n:.2f}"
def no_tie(total, n):  # avoid values exactly halfway between two roundings (Java and Python disagree there)
    return ((total * 100) % n) * 2 != n
r = random.Random(13)
def avg_case(n, lo, hi):
    while True:
        a = nums(r, n, lo, hi)
        if no_tie(sum(a), n): return case(n, " ".join(map(str, a)))
problem("Average Score", "EASY", [Q, D],
        "A class has n students. Print the average of their scores rounded to exactly two decimal places. The input is chosen so that the average is never exactly halfway between two rounded values.",
        "The first line contains n. The second line contains n integer scores.",
        "Print the average with exactly two digits after the decimal point.", "1 <= n <= 1000, 0 <= score <= 100",
        [case(3, "90 80 70"), case(4, "1 2 2 2")],
        [case(1, 55), avg_case(7, 0, 100), avg_case(100, 0, 100), avg_case(999, 0, 100), case(2, "99 100")],
        ref_avg)


def ref_pass_fail(s):
    t = tokens(s); n = int(t[0]); res = t[1:1 + n]; return f"{res.count('PASS')} {res.count('FAIL')}"
r = random.Random(14)
problem("Test Run Summary", "EASY", [Q, D],
        "An automated test run produced n results, each either PASS or FAIL. Print how many passed and how many failed.",
        "The first line contains n. The second line contains n words, each PASS or FAIL.",
        "Print the number of passed results and the number of failed results separated by a space.", "1 <= n <= 1000",
        [case(4, "PASS FAIL PASS PASS"), case(2, "FAIL FAIL")],
        [case(1, "PASS"), case(5, "PASS PASS PASS PASS PASS"),
         case(200, " ".join(r.choice(["PASS", "FAIL"]) for _ in range(200))),
         case(1000, " ".join(r.choice(["PASS", "FAIL", "PASS"]) for _ in range(1000)))],
        ref_pass_fail)


def ref_rev_words(s):
    return " ".join(w[::-1] for w in s.strip().split(" "))
problem("Reverse Each Word", "EASY", [P, F, S],
        "Reverse every word of a sentence but keep the order of the words. Words are separated by single spaces.",
        "A single line with words separated by single spaces.", "Print the transformed sentence.", "1 <= length <= 1000",
        ["Hello World\n", "keep calm and code\n"],
        ["a\n", "abc def\n", "Racecar level noon\n", "The quick brown fox jumps over the lazy dog\n", "x y z\n"],
        ref_rev_words)


# =====================================================================================  MEDIUM
def ref_two_sum(s):
    t = tokens(s); n = int(t[0]); a = list(map(int, t[1:1 + n])); target = int(t[1 + n])
    for i in range(n):
        for j in range(i + 1, n):
            if a[i] + a[j] == target: return f"{i} {j}"
    return "-1"
def brute_two_sum(s):
    t = tokens(s); n = int(t[0]); a = list(map(int, t[1:1 + n])); target = int(t[1 + n])
    best = None
    for i in range(n):
        for j in range(i + 1, n):
            if a[i] + a[j] == target and best is None: best = f"{i} {j}"
    return best or "-1"
def small_two_sum(r): n = r.randint(2, 8); return case(n, " ".join(map(str, nums(r, n, -5, 9))), r.randint(-6, 12))
r = random.Random(21)
problem("Two Sum", "MEDIUM", [J, P, F, S],
        "Given n integers and a target value, find the pair of positions i < j with a[i] + a[j] = target. If several pairs exist print the pair with the smallest i, and for that i the smallest j. If no pair exists print -1. Positions start at 0.",
        "The first line contains n. The second line contains n integers. The third line contains the target.",
        "Print i and j separated by a space, or -1.", "2 <= n <= 2000, -10^6 <= values <= 10^6",
        [case(4, "2 7 11 15", 9), case(3, "1 2 3", 10)],
        [case(2, "5 5", 10), case(5, "3 3 4 4 6", 8), arr_case(r, 500, -1000, 1000, 17), arr_case(r, 2000, -10**6, 10**6, 0), case(4, "-3 4 3 90", 0)],
        ref_two_sum, brute_two_sum, small_two_sum)


def ref_parens(s):
    st = []; pair = {")": "(", "]": "[", "}": "{"}
    for c in s.strip():
        if c in "([{": st.append(c)
        else:
            if not st or st.pop() != pair[c]: return "NO"
    return "NO" if st else "YES"
def brute_parens(s):
    t = s.strip()
    while True:
        u = t.replace("()", "").replace("[]", "").replace("{}", "")
        if u == t: return "YES" if t == "" else "NO"
        t = u
def small_parens(r): return "".join(r.choice("()[]{}") for _ in range(r.randint(1, 10))) + "\n"
problem("Valid Parentheses", "MEDIUM", [J, P, F, S, Q],
        "A string contains only the characters ( ) [ ] { }. Decide whether it is balanced: every opening bracket is closed by the same kind of bracket in the correct order.",
        "A single line with the bracket string.", "Print YES if the string is balanced, otherwise NO.", "1 <= length <= 10000",
        ["()[]{}\n", "(]\n"],
        ["{[()]}\n", "((\n", "([)]\n", "(" * 2000 + ")" * 2000 + "\n", ")(\n", "{[}\n"],
        ref_parens, brute_parens, small_parens)


def ref_anagram(s):
    a, b = s.split()[:2]; return "YES" if sorted(a.lower()) == sorted(b.lower()) else "NO"
def brute_anagram(s):
    from collections import Counter
    a, b = s.split()[:2]; return "YES" if Counter(a.lower()) == Counter(b.lower()) else "NO"
def small_anagram(r): return "".join(r.choice("abAB") for _ in range(r.randint(1, 5))) + " " + "".join(r.choice("abAB") for _ in range(r.randint(1, 5))) + "\n"
problem("Anagram Check", "MEDIUM", [J, P, F, S, Q],
        "Two words are anagrams if one can be rearranged into the other. Ignore letter case. Decide whether two given words are anagrams.",
        "A single line with two words separated by a space.", "Print YES or NO.", "1 <= length of each word <= 1000",
        ["Listen Silent\n", "Hello World\n"],
        ["a a\n", "Dormitory dirtyroom\n", "abc abcd\n", "Conversation VoicesRantOn\n", "aab abb\n"],
        ref_anagram, brute_anagram, small_anagram)


def ref_longest_unique(s):
    s = s.strip(); best = start = 0; seen = {}
    for i, c in enumerate(s):
        if c in seen and seen[c] >= start: start = seen[c] + 1
        seen[c] = i; best = max(best, i - start + 1)
    return str(best)
def brute_longest_unique(s):
    s = s.strip(); best = 0
    for i in range(len(s)):
        for j in range(i, len(s)):
            if len(set(s[i:j + 1])) == j - i + 1: best = max(best, j - i + 1)
    return str(best)
def small_unique(r): return "".join(r.choice("abcd") for _ in range(r.randint(1, 9))) + "\n"
r = random.Random(23)
problem("Longest Substring Without Repeating Characters", "MEDIUM", [J, P, F, S],
        "Find the length of the longest substring of the given string in which no character appears twice.",
        "A single line containing the string (no spaces).", "Print the length.", "1 <= length <= 5000",
        ["abcabcbb\n", "bbbbb\n"],
        ["a\n", "pwwkew\n", "abcdef\n", "".join(r.choice("abcdefghij") for _ in range(5000)) + "\n", "dvdf\n", "abba\n"],
        ref_longest_unique, brute_longest_unique, small_unique)


def ref_group_sales(s):
    lines = s.strip().split("\n"); n = int(lines[0]); tot = {}
    for ln in lines[1:1 + n]:
        reg, amt = ln.split(); tot[reg] = tot.get(reg, 0) + int(amt)
    return "\n".join(f"{k} {tot[k]}" for k in sorted(tot))
r = random.Random(24)
def sales_case(n):
    regs = ["North", "South", "East", "West", "Central"]
    return case(n, *[f"{r.choice(regs)} {r.randint(1, 1000)}" for _ in range(n)])
problem("Sales by Region", "MEDIUM", [D, P, Q],
        "A list of sales records is given. Each record has a region name and a sale amount. Print the total sales of each region, sorted by region name in alphabetical order.",
        "The first line contains n. Each of the next n lines contains a region name (no spaces) and an integer amount.",
        "For every region print a line: the region name, a space and the total.", "1 <= n <= 1000, 1 <= amount <= 100000",
        [case(3, "North 100", "South 50", "North 25"), case(1, "East 7")],
        [sales_case(1), sales_case(10), sales_case(200), sales_case(1000), case(4, "b 1", "a 2", "b 3", "a 4")],
        ref_group_sales)


def ref_moving_avg(s):
    t = tokens(s); n, k = int(t[0]), int(t[1]); a = list(map(int, t[2:2 + n]))
    return "\n".join(f"{sum(a[i:i + k]) / k:.2f}" for i in range(n - k + 1))
r = random.Random(25)
def mov_case(n, k, lo, hi):
    while True:
        a = nums(r, n, lo, hi)
        if all(no_tie(sum(a[i:i + k]), k) for i in range(n - k + 1)): return case(f"{n} {k}", " ".join(map(str, a)))
problem("Moving Average", "MEDIUM", [D, P],
        "Given a series of n numbers and a window size k, print the average of every window of k consecutive numbers, rounded to two decimal places. The input guarantees that no average is exactly halfway between two rounded values.",
        "The first line contains n and k. The second line contains n integers.",
        "Print n - k + 1 lines, each with an average with exactly two decimals.", "1 <= k <= n <= 1000, 0 <= values <= 1000",
        [case("5 3", "1 2 3 4 5"), case("3 1", "4 8 9")],
        [mov_case(3, 3, 0, 1000), mov_case(10, 4, 0, 1000), mov_case(100, 7, 0, 1000), mov_case(1000, 10, 0, 1000), mov_case(6, 1, 0, 50)],
        ref_moving_avg)


def ref_triage(s):
    lines = s.strip().split("\n"); n = int(lines[0]); bugs = []
    for ln in lines[1:1 + n]:
        bid, sev = ln.split(); bugs.append((-int(sev), bid))
    return "\n".join(b for _, b in sorted(bugs))
r = random.Random(26)
def bug_case(n):
    ids = list(range(1, n + 1)); r.shuffle(ids)
    return case(n, *[f"BUG-{i} {r.randint(1, 5)}" for i in ids])
problem("Bug Triage Order", "MEDIUM", [Q],
        "A QA team has n open bugs. Each bug has an id (text) and a severity from 1 (minor) to 5 (critical). Print the bug ids in the order they should be fixed: highest severity first. Bugs with the same severity are ordered by id in ordinary string order.",
        "The first line contains n. Each of the next n lines contains a bug id and its severity.",
        "Print n lines, one bug id per line.", "1 <= n <= 1000",
        [case(3, "BUG-2 3", "BUG-1 5", "BUG-3 3"), case(2, "A 1", "B 1")],
        [bug_case(1), bug_case(8), bug_case(100), bug_case(1000), case(3, "x 5", "a 5", "m 5")],
        ref_triage)


def ref_merge_intervals(s):
    t = tokens(s); n = int(t[0]); iv = sorted((int(t[1 + 2 * i]), int(t[2 + 2 * i])) for i in range(n)); out = []
    for a, b in iv:
        if out and a <= out[-1][1]: out[-1][1] = max(out[-1][1], b)
        else: out.append([a, b])
    return "\n".join(f"{a} {b}" for a, b in out)
def brute_merge(s):
    t = tokens(s); n = int(t[0]); iv = [(int(t[1 + 2 * i]), int(t[2 + 2 * i])) for i in range(n)]
    pts = set()
    for a, b in iv:
        for x in range(a, b + 1): pts.add(x)
    # merging with a <= end semantics: touching intervals merge, so merge points only when intervals overlap/touch
    iv.sort(); res = []
    for a, b in iv:
        if res and a <= res[-1][1]: res[-1][1] = max(res[-1][1], b)
        else: res.append([a, b])
    return "\n".join(f"{a} {b}" for a, b in res)
def small_merge(r):
    n = r.randint(1, 6); lines = []
    for _ in range(n):
        a = r.randint(0, 10); lines.append(f"{a} {a + r.randint(0, 4)}")
    return case(n, *lines)
r = random.Random(27)
def iv_case(n):
    lines = []
    for _ in range(n):
        a = r.randint(0, 10000); lines.append(f"{a} {a + r.randint(0, 50)}")
    return case(n, *lines)
problem("Merge Intervals", "MEDIUM", [J, P, F, S],
        "Given n closed intervals [start, end], merge all intervals that overlap or touch (for example [1,3] and [3,5] become [1,5]). Print the merged intervals sorted by start.",
        "The first line contains n. Each of the next n lines contains start and end (start <= end).",
        "Print one merged interval per line as: start end.", "1 <= n <= 1000, 0 <= start <= end <= 10^6",
        [case(4, "1 3", "2 6", "8 10", "15 18"), case(2, "1 4", "4 5")],
        [case(1, "5 5"), iv_case(10), iv_case(100), iv_case(1000), case(3, "1 2", "3 4", "5 6")],
        ref_merge_intervals, brute_merge, small_merge)


def ref_rotate(s):
    t = tokens(s); n, k = int(t[0]), int(t[1]); a = list(map(int, t[2:2 + n])); k %= n
    return " ".join(map(str, a[-k:] + a[:-k] if k else a))
r = random.Random(28)
problem("Rotate Array", "MEDIUM", [F, S, Q],
        "Rotate an array of n integers to the right by k positions. For example, rotating [1,2,3,4,5] by 2 gives [4,5,1,2,3].",
        "The first line contains n and k. The second line contains n integers.",
        "Print the rotated array on one line, separated by spaces.", "1 <= n <= 1000, 0 <= k <= 10^9",
        [case("5 2", "1 2 3 4 5"), case("3 0", "9 8 7")],
        [case("1 5", 4), case("4 4", "1 2 3 4"), case("6 1000000007", "1 2 3 4 5 6"), arr_case(r, 1000, -100, 100).replace("1000\n", "1000 37\n", 1)],
        ref_rotate)


def ref_median(s):
    t = tokens(s); n = int(t[0]); a = sorted(map(int, t[1:1 + n]))
    m = a[n // 2] if n % 2 else (a[n // 2 - 1] + a[n // 2]) / 2
    return f"{m:.1f}"
r = random.Random(29)
problem("Median Value", "MEDIUM", [D, P, Q],
        "Print the median of n integers. If n is even the median is the average of the two middle values after sorting. Print the median with exactly one decimal place.",
        "The first line contains n. The second line contains n integers.", "Print the median with one decimal place.",
        "1 <= n <= 1000, -10^6 <= values <= 10^6",
        [case(5, "3 1 2 5 4"), case(4, "1 2 3 4")],
        [case(1, 7), case(2, "-5 4"), arr_case(r, 101, -10**6, 10**6), arr_case(r, 1000, -10**6, 10**6), case(6, "9 9 9 1 1 1")],
        ref_median)


# =====================================================================================  HARD
def ref_lis(s):
    import bisect
    t = tokens(s); n = int(t[0]); a = list(map(int, t[1:1 + n])); tails = []
    for x in a:
        i = bisect.bisect_left(tails, x)
        if i == len(tails): tails.append(x)
        else: tails[i] = x
    return str(len(tails))
def brute_lis(s):
    t = tokens(s); n = int(t[0]); a = list(map(int, t[1:1 + n])); dp = [1] * n
    for i in range(n):
        for j in range(i):
            if a[j] < a[i]: dp[i] = max(dp[i], dp[j] + 1)
    return str(max(dp))
def small_lis(r): n = r.randint(1, 9); return case(n, " ".join(map(str, nums(r, n, 0, 6))))
r = random.Random(31)
problem("Longest Increasing Subsequence", "HARD", [J, P, S, D],
        "Find the length of the longest strictly increasing subsequence of an array. A subsequence keeps the original order but may skip elements.",
        "The first line contains n. The second line contains n integers.", "Print the length.",
        "1 <= n <= 2000, -10^6 <= values <= 10^6 (an O(n log n) solution is preferred)",
        [case(8, "10 9 2 5 3 7 101 18"), case(4, "5 5 5 5")],
        [case(1, 3), arr_case(r, 200, -50, 50), arr_case(r, 1000, -10**6, 10**6), arr_case(r, 2000, -10**6, 10**6),
         case(2000, " ".join(map(str, range(2000))))],
        ref_lis, brute_lis, small_lis)


def ref_coins(s):
    t = tokens(s); amount, n = int(t[0]), int(t[1]); coins = list(map(int, t[2:2 + n]))
    INF = 10**9; dp = [0] + [INF] * amount
    for a in range(1, amount + 1):
        for c in coins:
            if c <= a and dp[a - c] + 1 < dp[a]: dp[a] = dp[a - c] + 1
    return str(dp[amount] if dp[amount] < INF else -1)
def brute_coins(s):
    t = tokens(s); amount, n = int(t[0]), int(t[1]); coins = list(map(int, t[2:2 + n]))
    @lru_cache(None)
    def go(rem):
        if rem == 0: return 0
        best = 10**9
        for c in coins:
            if c <= rem: best = min(best, go(rem - c) + 1)
        return best
    v = go(amount); return str(v if v < 10**9 else -1)
def small_coins(r): n = r.randint(1, 4); return case(f"{r.randint(0, 25)} {n}", " ".join(map(str, nums(r, n, 1, 9))))
problem("Minimum Coins", "HARD", [J, P, F, S],
        "You have an unlimited supply of coins of the given values. Find the minimum number of coins needed to make the exact amount. If it is impossible print -1. An amount of 0 needs 0 coins.",
        "The first line contains the amount and the number of coin values n. The second line contains the n coin values.",
        "Print the minimum number of coins, or -1.", "0 <= amount <= 10000, 1 <= n <= 12, 1 <= coin <= 10000",
        [case("11 3", "1 2 5"), case("3 1", 2)],
        [case("0 2", "3 7"), case("27 4", "2 5 10 25"), case("10000 5", "7 13 29 31 97"), case("9999 3", "2 4 6"), case("63 3", "1 5 10").replace("63 3", "6249 3")],
        ref_coins, brute_coins, small_coins)


def ref_islands(s):
    lines = s.strip().split("\n"); r_, c_ = map(int, lines[0].split()); g = [list(lines[1 + i].strip()) for i in range(r_)]
    seen = 0
    for i in range(r_):
        for j in range(c_):
            if g[i][j] == "1":
                seen += 1; st = [(i, j)]; g[i][j] = "0"
                while st:
                    x, y = st.pop()
                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                        nx, ny = x + dx, y + dy
                        if 0 <= nx < r_ and 0 <= ny < c_ and g[nx][ny] == "1": g[nx][ny] = "0"; st.append((nx, ny))
    return str(seen)
def brute_islands(s):
    lines = s.strip().split("\n"); r_, c_ = map(int, lines[0].split()); g = [lines[1 + i].strip() for i in range(r_)]
    parent = {}
    def find(x):
        while parent[x] != x: parent[x] = parent[parent[x]]; x = parent[x]
        return x
    for i in range(r_):
        for j in range(c_):
            if g[i][j] == "1": parent[(i, j)] = (i, j)
    for (i, j) in list(parent):
        for dx, dy in ((1, 0), (0, 1)):
            if (i + dx, j + dy) in parent: parent[find((i, j))] = find((i + dx, j + dy))
    return str(len({find(x) for x in parent}))
def small_islands(r):
    a, b = r.randint(1, 5), r.randint(1, 5); return case(f"{a} {b}", *["".join(r.choice("01") for _ in range(b)) for _ in range(a)])
r = random.Random(33)
def grid_case(a, b, p):
    return case(f"{a} {b}", *["".join("1" if r.random() < p else "0" for _ in range(b)) for _ in range(a)])
problem("Number of Islands", "HARD", [J, P, F, S, Q],
        "A map is a grid of 1 (land) and 0 (water). An island is a group of land cells connected horizontally or vertically. Count the islands.",
        "The first line contains the number of rows r and columns c. Each of the next r lines contains a string of c characters, each 0 or 1.",
        "Print the number of islands.", "1 <= r, c <= 100",
        [case("4 5", "11000", "11000", "00100", "00011"), case("2 2", "00", "00")],
        [case("1 1", 1), grid_case(10, 10, 0.4), grid_case(40, 40, 0.45), grid_case(100, 100, 0.5), case("3 3", "101", "010", "101")],
        ref_islands, brute_islands, small_islands)


def ref_lps(s):
    s = s.strip(); best = 1 if s else 0
    for c in range(len(s)):
        for l, r_ in ((c, c), (c, c + 1)):
            while l >= 0 and r_ < len(s) and s[l] == s[r_]: l -= 1; r_ += 1
            best = max(best, r_ - l - 1)
    return str(best)
def brute_lps(s):
    s = s.strip(); best = 0
    for i in range(len(s)):
        for j in range(i, len(s)):
            t = s[i:j + 1]
            if t == t[::-1]: best = max(best, len(t))
    return str(best)
def small_lps(r): return "".join(r.choice("ab") for _ in range(r.randint(1, 9))) + "\n"
r = random.Random(34)
problem("Longest Palindromic Substring", "HARD", [J, P, F, S],
        "Find the length of the longest substring of the given string that reads the same forwards and backwards.",
        "A single line containing the string (no spaces).", "Print the length.", "1 <= length <= 2000",
        ["babad\n", "cbbd\n"],
        ["a\n", "abcde\n", "forgeeksskeegfor\n", "".join(r.choice("ab") for _ in range(2000)) + "\n", "a" * 2000 + "\n", "abacdfgdcaba\n"],
        ref_lps, brute_lps, small_lps)


def ref_topk(s):
    t = tokens(s); n, k = int(t[0]), int(t[1]); w = t[2:2 + n]; cnt = {}
    for x in w: cnt[x] = cnt.get(x, 0) + 1
    return "\n".join(f"{x} {c}" for x, c in sorted(cnt.items(), key=lambda kv: (-kv[1], kv[0]))[:k])
r = random.Random(35)
def topk_case(n, k, alpha, hi):
    return case(f"{n} {k}", " ".join(words(r, n, alpha, 1, hi)))
problem("Top K Frequent Words", "HARD", [J, P, F, S, D],
        "Given n words, print the k most frequent words with their counts. Order by count (highest first). Words with the same count are ordered alphabetically. Case matters.",
        "The first line contains n and k. The second line contains n words.", "Print k lines: the word, a space and its count.",
        "1 <= k <= number of distinct words <= n <= 3000",
        [case("6 2", "the day is sunny the the"), case("4 1", "a b b a")],
        [case("1 1", "word"), topk_case(20, 3, "ab", 2), topk_case(500, 5, "abc", 3), topk_case(3000, 10, "abcd", 4), case("5 2", "b a c a b")],
        ref_topk)


def ref_kth(s):
    t = tokens(s); n, k = int(t[0]), int(t[1]); a = sorted(map(int, t[2:2 + n]), reverse=True); return str(a[k - 1])
def brute_kth(s):
    import heapq
    t = tokens(s); n, k = int(t[0]), int(t[1]); return str(heapq.nlargest(k, map(int, t[2:2 + n]))[-1])
def small_kth(r): n = r.randint(1, 8); return case(f"{n} {r.randint(1, n)}", " ".join(map(str, nums(r, n, -9, 9))))
r = random.Random(36)
problem("Kth Largest Element", "HARD", [J, P, S, Q, D],
        "Find the k-th largest element of an array (counting duplicates, so in [3, 3, 2] the 2nd largest is 3). Aim for better than sorting repeatedly for large inputs.",
        "The first line contains n and k. The second line contains n integers.", "Print the k-th largest value.",
        "1 <= k <= n <= 2000, -10^6 <= values <= 10^6",
        [case("6 2", "3 2 1 5 6 4"), case("3 2", "3 3 2")],
        [case("1 1", 5), arr_case(r, 100, -50, 50).replace("100\n", "100 37\n", 1), arr_case(r, 2000, -10**6, 10**6).replace("2000\n", "2000 1000\n", 1), case("5 5", "1 1 1 1 1")],
        ref_kth, brute_kth, small_kth)


def ref_wordbreak(s):
    lines = s.strip().split("\n"); n = int(lines[0]); dic = set(lines[1].split()[:n]); text = lines[2].strip()
    ok = [True] + [False] * len(text)
    for i in range(1, len(text) + 1):
        ok[i] = any(ok[j] and text[j:i] in dic for j in range(i))
    return "YES" if ok[len(text)] else "NO"
def brute_wordbreak(s):
    lines = s.strip().split("\n"); n = int(lines[0]); dic = set(lines[1].split()[:n]); text = lines[2].strip()
    @lru_cache(None)
    def go(i):
        if i == len(text): return True
        return any(text.startswith(w, i) and go(i + len(w)) for w in dic)
    return "YES" if go(0) else "NO"
def small_wb(r):
    dic = list({"".join(r.choice("ab") for _ in range(r.randint(1, 3))) for _ in range(r.randint(1, 3))})
    return case(len(dic), " ".join(dic), "".join(r.choice("ab") for _ in range(r.randint(1, 9))))
problem("Word Break", "HARD", [P, F, S],
        "Given a dictionary of words and a string, decide whether the string can be split into a sequence of one or more dictionary words. Words may be reused.",
        "The first line contains the number of dictionary words n. The second line contains the n words. The third line contains the string.",
        "Print YES or NO.", "1 <= n <= 100, 1 <= length of the string <= 300",
        [case(2, "leet code", "leetcode"), case(3, "cats dog sand", "catsandog")],
        [case(1, "a", "aaaaaaaaaaaa"), case(3, "apple pen applepen", "pineapplepenapple"), case(2, "ab abc", "ababababababc"), case(2, "aaa aaaa", "a" * 299), case(1, "abc", "ab")],
        ref_wordbreak, brute_wordbreak, small_wb)


def ref_machines(s):
    t = tokens(s); n = int(t[0]); ev = []
    for i in range(n):
        ev.append((int(t[1 + 2 * i]), 1)); ev.append((int(t[2 + 2 * i]), -1))
    ev.sort(key=lambda e: (e[0], e[1])); cur = best = 0     # at equal times a run that ends frees its machine first
    for _, d in ev: cur += d; best = max(best, cur)
    return str(best)
def brute_machines(s):
    t = tokens(s); n = int(t[0]); iv = [(int(t[1 + 2 * i]), int(t[2 + 2 * i])) for i in range(n)]
    pts = {x for a, b in iv for x in (a, b)}
    return str(max(sum(1 for a, b in iv if a <= p < b) for p in pts))
def small_machines(r):
    n = r.randint(1, 6); lines = []
    for _ in range(n):
        a = r.randint(0, 10); lines.append(f"{a} {a + r.randint(1, 5)}")
    return case(n, *lines)
r = random.Random(38)
def mach_case(n):
    lines = []
    for _ in range(n):
        a = r.randint(0, 100000); lines.append(f"{a} {a + r.randint(1, 500)}")
    return case(n, *lines)
problem("Minimum Test Machines", "HARD", [Q, D],
        "A test lab must run n test suites. Suite i runs from start time s_i up to, but not including, end time e_i. Two suites that overlap in time cannot share a machine, but a suite may start exactly when another one ends. Print the minimum number of machines needed.",
        "The first line contains n. Each of the next n lines contains s_i and e_i (s_i < e_i).", "Print the minimum number of machines.",
        "1 <= n <= 1500, 0 <= s_i < e_i <= 10^6",
        [case(3, "0 30", "5 10", "15 20"), case(2, "7 10", "2 4")],
        [case(1, "0 1"), case(3, "1 2", "2 3", "3 4"), mach_case(50), mach_case(500), mach_case(1500)],
        ref_machines, brute_machines, small_machines)


def ref_lcs(s):
    a, b = s.split()[:2]; dp = [[0] * (len(b) + 1) for _ in range(len(a) + 1)]
    for i in range(1, len(a) + 1):
        for j in range(1, len(b) + 1):
            dp[i][j] = dp[i - 1][j - 1] + 1 if a[i - 1] == b[j - 1] else max(dp[i - 1][j], dp[i][j - 1])
    return str(dp[-1][-1])
def brute_lcs(s):
    a, b = s.split()[:2]
    @lru_cache(None)
    def go(i, j):
        if i == len(a) or j == len(b): return 0
        return 1 + go(i + 1, j + 1) if a[i] == b[j] else max(go(i + 1, j), go(i, j + 1))
    return str(go(0, 0))
def small_lcs(r): return "".join(r.choice("abc") for _ in range(r.randint(1, 7))) + " " + "".join(r.choice("abc") for _ in range(r.randint(1, 7))) + "\n"
r = random.Random(39)
problem("Longest Common Subsequence", "HARD", [J, P, D],
        "Find the length of the longest common subsequence of two strings. A subsequence keeps the order of characters but may skip some.",
        "A single line with two strings separated by a space.", "Print the length.", "1 <= length of each string <= 1500",
        ["abcde ace\n", "abc def\n"],
        ["a a\n", "AGGTAB GXTXAYB\n", "".join(r.choice("abc") for _ in range(300)) + " " + "".join(r.choice("abc") for _ in range(300)) + "\n",
         "".join(r.choice("ab") for _ in range(1500)) + " " + "".join(r.choice("ab") for _ in range(1500)) + "\n", "abcdef fedcba\n"],
        ref_lcs, brute_lcs, small_lcs)


def ref_window_max(s):
    from collections import deque
    t = tokens(s); n, k = int(t[0]), int(t[1]); a = list(map(int, t[2:2 + n])); dq = deque(); out = []
    for i, x in enumerate(a):
        while dq and a[dq[-1]] <= x: dq.pop()
        dq.append(i)
        if dq[0] <= i - k: dq.popleft()
        if i >= k - 1: out.append(a[dq[0]])
    return " ".join(map(str, out))
def brute_window_max(s):
    t = tokens(s); n, k = int(t[0]), int(t[1]); a = list(map(int, t[2:2 + n]))
    return " ".join(str(max(a[i:i + k])) for i in range(n - k + 1))
def small_wmax(r): n = r.randint(1, 9); return case(f"{n} {r.randint(1, n)}", " ".join(map(str, nums(r, n, -9, 9))))
r = random.Random(40)
problem("Sliding Window Maximum", "HARD", [F, Q, D],
        "Given an array and a window size k, print the maximum value of every window of k consecutive elements (for example while monitoring a metric over time). An O(n) solution is expected.",
        "The first line contains n and k. The second line contains n integers.", "Print the n - k + 1 window maximums on one line, separated by spaces.",
        "1 <= k <= n <= 2000, -10^6 <= values <= 10^6",
        [case("8 3", "1 3 -1 -3 5 3 6 7"), case("1 1", 9)],
        [case("5 5", "4 2 12 3 8"), arr_case(r, 300, -100, 100).replace("300\n", "300 7\n", 1), arr_case(r, 2000, -10**6, 10**6).replace("2000\n", "2000 100\n", 1),
         case("6 1", "5 4 3 2 1 0"), case("6 2", "1 2 3 4 5 6")],
        ref_window_max, brute_window_max, small_wmax)
