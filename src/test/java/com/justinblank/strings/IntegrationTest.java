package com.justinblank.strings;

import com.justinblank.strings.RegexAST.Node;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class IntegrationTest {

    @Test
    public void testConcatenatedRangesWithRepetition() {
        Node node = RegexParser.parse("[A-Za-z][A-Za-z0-9]*");
        DFA dfa = NFAToDFACompiler.compile(ThompsonNFABuilder.createNFA(node));
        assertTrue(dfa.matches("ABC0123"));
    }

    @Test
    public void testConcatenatedRangesWithRepetitionSearch() {
        Node node = RegexParser.parse("[A-Za-z][A-Za-z0-9]*");
        DFA dfa = NFAToDFACompiler.compile(ThompsonNFABuilder.createNFA(node));
        MatchResult result = dfa.search("ABC0123");
        assertEquals(0, result.start);
        assertEquals(7, result.end);
    }

    @Test
    public void testRanges() {
        Node node = RegexParser.parse("[A-Za-z]");
        DFA dfa = NFAToDFACompiler.compile(ThompsonNFABuilder.createNFA(node));
        assertTrue(dfa.matches("C"));
        assertTrue(dfa.matches("c"));
        assertFalse(dfa.matches("5"));
    }

    @Test
    public void testRangesSearch() {
        Node node = RegexParser.parse("[A-Za-z]");
        DFA dfa = NFAToDFACompiler.compile(ThompsonNFABuilder.createNFA(node));
        assertEquals(0, dfa.search("c").start);
        assertEquals(1, dfa.search("c").end);
        assertFalse(dfa.search("5").matched);
    }

    @Test
    public void testNFARepetition() {
        Node node = RegexParser.parse("C*");
        NFA nfa = ThompsonNFABuilder.createNFA(node);
        assertTrue(nfa.matches(""));
        assertTrue(nfa.matches("C"));
        assertTrue(nfa.matches("CCCCCC"));
    }

    @Test
    public void testDFARepetition() {
        Node node = RegexParser.parse("C*");
        DFA dfa = NFAToDFACompiler.compile(ThompsonNFABuilder.createNFA(node));
        assertTrue(dfa.matches(""));
        assertTrue(dfa.matches("C"));
        assertTrue(dfa.matches("CCCCCC"));
    }

    @Test
    public void testDFARepetitionSearch() {
        Node node = RegexParser.parse("C*");
        DFA dfa = NFAToDFACompiler.compile(ThompsonNFABuilder.createNFA(node));
        assertTrue(dfa.search("").matched);
        assertTrue(dfa.search("C").matched);
        assertTrue(dfa.search("CCCCCC").matched);
        assertTrue(dfa.search("DD").matched);
        assertTrue(dfa.search("DDC").matched);
        assertTrue(dfa.search("DDCCCCDD").matched);
    }

    @Test
    public void testDFACountedRepetitionSearch() {
        Node node = RegexParser.parse("C+");
        DFA dfa = NFAToDFACompiler.compile(ThompsonNFABuilder.createNFA(node));
        assertTrue(dfa.search("C").matched);
        assertTrue(dfa.search("CCCCCC").matched);
        assertTrue(dfa.search("DDC").matched);
        assertTrue(dfa.search("DDCCCCDD").matched);
        assertFalse(dfa.search("DD").matched);
    }

    @Test
    public void testDFAAlternation() {
        Node node = RegexParser.parse("A|B");
        DFA dfa = NFAToDFACompiler.compile(ThompsonNFABuilder.createNFA(node));
        assertTrue(dfa.matches("A"));
        assertTrue(dfa.matches("B"));
        assertFalse(dfa.matches("AB"));
    }

    @Test
    public void testDFAAlternationSearch() {
        Node node = RegexParser.parse("A|B");
        DFA dfa = NFAToDFACompiler.compile(ThompsonNFABuilder.createNFA(node));
        assertTrue(dfa.search("A").matched);
        assertTrue(dfa.search("B").matched);
        MatchResult matchResult = dfa.search("AB");
        assertEquals(0, matchResult.start);
        assertEquals(1, matchResult.end);

        matchResult = dfa.search("CDAB");
        assertEquals(2, matchResult.start);
        assertEquals(3, matchResult.end);
    }

    @Test
    public void testGroupedDFAAlternation() {
        Node node = RegexParser.parse("(AB)|(BA)");
        DFA dfa = NFAToDFACompiler.compile(ThompsonNFABuilder.createNFA(node));
        assertTrue(dfa.matches("AB"));
        assertTrue(dfa.matches("BA"));
        assertFalse(dfa.matches("ABBA"));
    }

    @Test
    public void testGroupedDFAAlternationSearch() {
        Node node = RegexParser.parse("(AB)|(BA)");
        DFA dfa = NFAToDFACompiler.compile(ThompsonNFABuilder.createNFA(node));
        assertTrue(dfa.search("AB").matched);
        assertTrue(dfa.search("BA").matched);

        MatchResult result = dfa.search("ABBA");
        assertEquals(0, result.start);
        assertEquals(2, result.end);
    }

    @Test
    public void testManyStateRegex() {
        String regexString = "(123)|(234)|(345)|(456)|(567)|(678)|(789)|(0987)|(9876)|(8765)|(7654)|(6543)|(5432)|(4321)|(3210){1,24}";
        Node node = RegexParser.parse(regexString);
        NFA nfa = ThompsonNFABuilder.createNFA(node);
        assertTrue(nfa.matches("567"));
        assertTrue(nfa.matches("32103210"));
        assertFalse(nfa.matches(""));
        DFA dfa = NFAToDFACompiler.compile(nfa);
        assertTrue(dfa.matches("567"));
        assertFalse(dfa.matches(""));
        assertTrue(dfa.matches("32103210"));
    }

    @Test
    public void testManyStateDFASearch() {
        String regexString = "(123)|(234)|(345)|(456)|(567)|(678)|(789)|(0987)|(9876)|(8765)|(7654)|(6543)|(5432)|(4321)|(3210){1,24}";
        Node node = RegexParser.parse(regexString);
        NFA nfa = ThompsonNFABuilder.createNFA(node);
        DFA dfa = NFAToDFACompiler.compile(nfa);
        assertTrue(dfa.search("567").matched);
        assertFalse(dfa.search("").matched);
        assertTrue(dfa.search("32103210").matched);
    }

    @Test
    public void testCountedRepetitionOneChar() {
        String regexString = "1{1,1}";
        Node node = RegexParser.parse(regexString);
        NFA nfa = ThompsonNFABuilder.createNFA(node);
        assertTrue(nfa.matches("1"));
        assertFalse(nfa.matches(""));
        assertFalse(nfa.matches("11"));
    }

    @Test
    public void testZeroBoundCountedRepetition() {
        String regexString = "1{0,2}";
        Node node = RegexParser.parse(regexString);
        NFA nfa = ThompsonNFABuilder.createNFA(node);
        assertTrue(nfa.matches(""));
        assertTrue(nfa.matches("1"));
        assertTrue(nfa.matches("11"));
        assertFalse(nfa.matches("111"));
    }

    @Test
    public void testCountedRepetition() {
        String regexString = "(1|2){2,3}abc";
        Node node = RegexParser.parse(regexString);
        NFA nfa = ThompsonNFABuilder.createNFA(node);
        assertTrue(nfa.matches("12abc"));
        assertTrue(nfa.matches("121abc"));
        assertFalse(nfa.matches(""));
        assertFalse(nfa.matches("1abc"));
        assertFalse(nfa.matches("1221abc"));
        assertFalse(nfa.matches("12def"));
        DFA dfa = NFAToDFACompiler.compile(nfa);
        assertTrue(dfa.matches("12abc"));
        assertTrue(dfa.matches("121abc"));
        assertFalse(dfa.matches(""));
        assertFalse(dfa.matches("1abc"));
        assertFalse(dfa.matches("1211abc"));
        assertFalse(dfa.matches("12def"));
    }

    @Test
    public void test_ab_PLUS() {
        String regexString = "(ab)+";
        Node node = RegexParser.parse(regexString);
        NFA nfa = ThompsonNFABuilder.createNFA(node);
        assertTrue(nfa.matches("ab"));
        assertTrue(nfa.matches("ababab"));
        assertFalse(nfa.matches(""));
        DFA dfa = NFAToDFACompiler.compile(nfa);
        assertTrue(dfa.matches("ab"));
        assertTrue(dfa.matches("abab"));
        assertFalse(dfa.matches(""));
    }

    @Test
    public void test_a_PLUS() {
        String regexString = "a+";
        Node node = RegexParser.parse(regexString);
        NFA nfa = ThompsonNFABuilder.createNFA(node);
        assertTrue(nfa.matches("a"));
        assertTrue(nfa.matches("aaaaaaa"));
        assertFalse(nfa.matches(""));
        DFA dfa = NFAToDFACompiler.compile(nfa);
        assertTrue(dfa.matches("a"));
        assertTrue(dfa.matches("aaaaaaa"));
        assertFalse(dfa.matches(""));
    }

    @Test
    public void test_aORb_PLUS() {
        String regexString = "[a-c]+";
        Node node = RegexParser.parse(regexString);
        NFA nfa = ThompsonNFABuilder.createNFA(node);
        assertTrue(nfa.matches("a"));
        assertTrue(nfa.matches("c"));
        assertTrue(nfa.matches("abacbabababbbabababa"));
        assertFalse(nfa.matches(""));
    }

    @Test
    public void testConcatenatedMultiRegex() {
        String regexString = "[a-zA-Z@#]";
        Node node = RegexParser.parse(regexString);
        NFA nfa = ThompsonNFABuilder.createNFA(node);
        assertTrue(nfa.matches("a"));
        assertTrue(nfa.matches("@"));
        assertTrue(nfa.matches("#"));
        assertFalse(nfa.matches(""));
        assertFalse(nfa.matches("ab"));
        assertFalse(nfa.matches("a#"));
    }
}
