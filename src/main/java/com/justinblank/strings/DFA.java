package com.justinblank.strings;

import com.justinblank.strings.RegexAST.Node;
import com.justinblank.strings.Search.SearchMethod;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

class DFA {

    private boolean accepting;
    private final int stateNumber;
    private DFA root;
    // Only populated on the root
    private List<DFA> states;
    private List<Pair<CharRange, DFA>> transitions = new ArrayList<>();

    static DFA root(boolean accepting) {
        return new DFA(true, accepting, 0);
    }

    private DFA(boolean root, boolean accepting, int stateNumber) {
        if (root) {
            this.root = this;
            this.states = new ArrayList<>();
            this.states.add(this);
        }
        this.accepting = accepting;
        this.stateNumber = stateNumber;
    }

    protected DFA(DFA root, boolean accepting, int stateNumber) {
        if (root == null) {
            throw new IllegalArgumentException("Cannot create DFA with null root");
        }
        this.root = root;
        this.accepting = accepting;
        this.stateNumber = stateNumber;
        this.root.states.add(this);
    }

    public static DFA createDFA(String regex) {
        Node node = RegexParser.parse(regex);
        try {
            NFA nfa = new NFA(RegexInstrBuilder.createNFA(node));
            return NFAToDFACompiler.compile(nfa);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to create dfa from string '" + regex + "'", e);
        }
    }

    protected void addTransition(CharRange charRange, DFA dfa) {
        assert !charRange.isEmpty() : "cannot add an epsilon transition to a DFA";
        if (transitions.stream().anyMatch(t -> t.getLeft().equals(charRange))) {
            return;
        }
        transitions.add(Pair.of(charRange, dfa));
        // we trust that our character ranges don't overlap
        transitions.sort(Comparator.comparingInt(p -> p.getLeft().getStart()));
    }

    protected List<Pair<CharRange, DFA>> getTransitions() {
        return transitions;
    }

    protected boolean isAccepting() {
        return accepting;
    }

    protected DFA transition(char c) {
        for (Pair<CharRange, DFA> transition : transitions) {
            if (transition.getLeft().inRange(c)) {
                return transition.getRight();
            }
        }
        return null;
    }

    public boolean matches(String s) {
        int length = s.length();
        DFA current = this;
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            current = current.transition(c);
            if (current == null) {
                return false;
            }
        }
        return current.accepting;
    }

    public MatchResult search(String s) {
        // In order to match, we need to know the earliest index we could start from to reach a given state
        int[] stateStarts = initSearchStateArray();
        int[] newStateStarts = null;
        // the starting index of a successful match. May be set multiple times, but can only ever decrease.
        int matchStart = Integer.MAX_VALUE;
        // the final index of a successful match. May be set multiple times, but should only ever increase.
        int matchEnd = Integer.MIN_VALUE;
        if (accepting) {
            matchStart = 0;
            matchEnd = 0;
        }
        for (int i = 0; i < s.length(); i++) {
            if (i == 0) {
                newStateStarts = initSearchStateArray();
            }
            else {
                clearSearchStateArray(newStateStarts);
            }
            char c = s.charAt(i);
            int earliestCurrentStart = Integer.MAX_VALUE;
            for (int j = 0; j < stateStarts.length; j++) {
                int stateStart = stateStarts[j];
                // We consider states that are live, but ignore those which started later than the best match we've seen
                boolean shouldConsider = stateStart > -1 && stateStart <= matchStart;
                // We also always consider the initial state when we haven't yet seen a match
                shouldConsider |= j == 0 && matchStart == Integer.MAX_VALUE;
                if (shouldConsider) {
                    DFA dfa = states.get(j);
                    DFA found = dfa.transition(c);
                    if (found != null) {
                        int foundStateNumber = found.stateNumber;
                        if (newStateStarts[foundStateNumber] == -1 ||
                                newStateStarts[foundStateNumber] > stateStarts[foundStateNumber]) {
                            if (i == 0) {
                                stateStart = 0;
                            }
                            else if (dfa.stateNumber == 0) {
                                stateStart = i;
                            }
                            newStateStarts[foundStateNumber] = stateStart;
                            earliestCurrentStart = Math.min(earliestCurrentStart, stateStart);
                            if (found.accepting) {
                                int newMatchStart = newStateStarts[foundStateNumber];
                                if (newMatchStart <= matchStart) {
                                    matchStart = newMatchStart;
                                    matchEnd = i + 1;
                                }
                            }
                        }
                    }
                }
            }
            if (earliestCurrentStart > matchStart) {
                return new MatchResult(true, matchStart, matchEnd);
            }
            // Swap arrays, to avoid repeatedly allocating
            int[] tmp = stateStarts;
            stateStarts = newStateStarts;
            newStateStarts = tmp;
        }
        if (matchStart == Integer.MAX_VALUE) {
            return MatchResult.FAILURE;
        }
        else {
            return new MatchResult(true, matchStart, matchEnd);
        }
    }

    private int[] initSearchStateArray() {
        int[] stateStarts = new int[states.size()];
        clearSearchStateArray(stateStarts);
        return stateStarts;
    }

    private void clearSearchStateArray(int[] stateStarts) {
        Arrays.fill(stateStarts, -1);
    }

    public int statesCount() {
        return states.size();
    }

    public Set<DFA> allStates() {
        return new HashSet<>(states);
    }

    protected Set<DFA> acceptingStates() {
        return allStates().stream().filter(DFA::isAccepting).collect(Collectors.toSet());
    }

    protected boolean hasSelfTransition() {
        for (Pair<CharRange, DFA> transition : transitions) {
            if (transition.getRight() == this) {
                return true;
            }
        }
        return false;
    }

    Optional<Pair<Integer, CharRange>> calculateOffset() {
        var seen = new HashSet<>();
        var count = new AtomicInteger();
        var next = this;
        Optional<Pair<Integer, CharRange>> best = Optional.empty();
        while (true) {
            if (seen.contains(next) || next.hasSelfTransition() || next.transitions.size() == 0) {
                return best;
            }
            if (next.transitions.size() != 1) {
                for (int i = 0; i < next.transitions.size() - 1; i++) {
                    if (next.transitions.get(i).getRight() != next.transitions.get(i + 1).getRight()) {
                        return best;
                    }
                }
            }
            seen.add(next);

            var transition = next.transitions.get(0);
            next = transition.getRight();
            if (count.get() > 0) {
                best = Optional.of(Pair.of(count.get(), transition.getLeft()));
            }
            count.incrementAndGet();
        }
    }

    protected int charCount() {
        int chars = 0;
        for (Pair<CharRange, DFA> transition : transitions) {
            chars += 1 + (int) transition.getLeft().getEnd() - (int) transition.getLeft().getStart();
        }
        return chars;
    }

    public int getStateNumber() {
        return stateNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DFA dfa = (DFA) o;
        return root == dfa.root && stateNumber == dfa.stateNumber;
    }

    @Override
    public int hashCode() {
        return stateNumber;
    }

    /**
     * Check invariants:
     * <ul>
     *     <li>The states reachable from the root should be contained in the states member variable</li>
     *     <li>All states should have distinct stateNumbers</li>
     *     <li>All states should refer to the same root</li>
     * </ul>
     *
     * Only applicable to the root node.
     *
     * @throws IllegalStateException if the invariants are broken
     */
    protected boolean checkRep() {
        assert allStatesReachable() : "Some state was unreachable";
        assert allStates().containsAll(states);
        assert states.containsAll(allStates());
        assert states.stream().allMatch(dfa -> dfa.root == this);
        assert states.stream().map(DFA::getStateNumber).count() == states.size();
        assert states.contains(this) : "root not included in states";
        assert states.stream().anyMatch(DFA::isAccepting) : "no accepting state found";
        assert transitions.stream().map(Pair::getRight).allMatch(dfa -> states.contains(dfa));
        return true;
    }

    private boolean allStatesReachable() {
        for (DFA dfa : states) {
            for (Pair<CharRange, DFA> transition : dfa.transitions) {
                DFA target = transition.getRight();
                if (target.stateNumber > states.size() || states.get(target.stateNumber) != target) {
                    return false;
                }
            }
        }
        return true;
    }

    protected boolean isRoot() {
        return this == root;
    }
}
