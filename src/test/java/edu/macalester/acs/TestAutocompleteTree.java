package edu.macalester.acs;

import junit.framework.TestCase;

import java.util.SortedSet;

/**
 * @author Shilad Sen
 *         Date: Dec 20, 2009
 */
public class TestAutocompleteTree extends TestCase {
    private static final City CHICAGO     = new City(1, "Chicago", "Illinois");
    private static final City MINNEAPOLIS = new City(2, "Minneapolis", "Minnesota");
    private static final City BOSTON      = new City(3, "Boston", "Massachusetts");
    private static final City CINCINATTI  = new City(4, "Cincinatti", "Ohio");
    private static final City CLEVELAND   = new City(5, "Cleveland", "Ohio");
    private static final City CHARLESTON  = new City(6, "Charleston", "South Carolina");
    private static final City ST_PAUL     = new City(7, "St. Paul", "Minnesota");

    /**
     * The tree used in the tests
     */
    private AutocompleteTree<Integer, City> tree = makeTree();

    public void testBasicOperations() {

        // Test the very basic operations
        assertTrue(tree.contains(1));
        assertTrue(tree.contains(2));
        assertTrue(tree.contains(3));
        assertTrue(tree.contains(4));
        assertFalse(tree.contains(32423423));

        // make sure that add and remove work
        tree.remove(1);
        assertFalse(tree.contains(1));
        tree.add(makeEntry(CHICAGO));
        assertTrue(tree.contains(1));

        // make sure that increment and decrement work.
        assertEquals(tree.get(2).getScoreAsInt(), 0);
        tree.increment(2);
        tree.increment(2);
        assertEquals(tree.get(2).getScoreAsInt(), 2);
        tree.decrement(2);
        assertEquals(tree.get(2).getScoreAsInt(), 1);
    }

    public void testBasicAutocomplete() {
        SortedSet<AutocompleteEntry<Integer, City>> results;

        results = tree.autocomplete("z", 2);
        assertEquals(results.size(), 0);
        results = tree.autocomplete("C", 2);
        assertEquals(results.size(), 2);
        results = tree.autocomplete("CH", 2);
        assertEquals(results.size(), 2);
        assertTrue(results.contains(makeEntry(CHICAGO)));
        assertTrue(results.contains(makeEntry(CHARLESTON)));
        results = tree.autocomplete("CHI", 2);
        assertEquals(results.size(), 1);
    }

    public void testScoreUpdates() {
        SortedSet<AutocompleteEntry<Integer, City>> results;
        results = tree.autocomplete("C", 2);

        // Initially, things should be sorted by key (i.e. ID).
        assertEquals(results.first().getValue(), CHICAGO);
        assertEquals(results.last().getValue(), CINCINATTI);

        // After incrementing the count for cleveland, it should be first.
        tree.increment(CLEVELAND.getId());
        results = tree.autocomplete("C", 2);
        assertEquals(results.first().getValue(), CLEVELAND);
        assertEquals(results.last().getValue(), CHICAGO);
    }

    public void testScoreUpdates2() {
        tree.setMaxCacheQueryLength(2);
        SortedSet<AutocompleteEntry<Integer, City>> results;
        results = tree.autocomplete("C", 2);

        // Initially, things should be sorted by key (i.e. ID).
        assertEquals(results.first().getValue(), CHICAGO);
        assertEquals(results.last().getValue(), CINCINATTI);

        // After incrementing the count for cleveland, it should be first.
        tree.increment(CLEVELAND.getId());
        results = tree.autocomplete("C", 2);
        assertEquals(results.first().getValue(), CLEVELAND);
        assertEquals(results.last().getValue(), CHICAGO);
    }

    /**
     * Test for bug #2.
     */
    public void testBug2() {

        AutocompleteTree<Integer, City> tree = new AutocompleteTree<Integer, City>();

        // Associate cities with their ids
        tree.add(1, new City(1, "Paris", "France"));
        tree.add(2, new City(2, "Paris", "Texas"));
        tree.add(3, new City(3, "Paris Plage", "France"));

        tree.setScore(1, 1);      // increments the score for paris by 1.
        tree.setScore(2, 2);
        tree.setScore(3, 3);

        SortedSet<AutocompleteEntry<Integer, City>> results = tree.autocomplete("pari", 3);
        assertEquals(results.size(), 3);
        SortedSet<AutocompleteEntry<Integer, City>> results2 = tree.autocomplete("paris", 3);
        assertEquals(results2.size(), 3);
    }

    public void testDomain() {
        tree.setMaxCacheQueryLength(2);
        AutocompleteFilter<Integer, City> evenFilter = new AutocompleteFilter<Integer, City>() {
            public boolean matches(AutocompleteEntry<Integer, City> entry) {
                return entry.getValue().getName().length() % 2 == 0;
            }
        };
        AutocompleteFilter<Integer, City> oddFilter = new AutocompleteFilter<Integer, City>() {
            public boolean matches(AutocompleteEntry<Integer, City> entry) {
                return entry.getValue().getName().length() % 2 == 1;
            }
        };
        SortedSet<AutocompleteEntry<Integer, City>> results;

        // NUM letters in:
        // Chicago : 7
        // Cincinatti : 10
        // Cleveland : 9
        // Charleston : 10
        results = tree.autocomplete("odd", "C", oddFilter, 10);
        assertEquals(results.size(), 2);
        assertEquals(results.first().getValue(), CHICAGO);
        assertEquals(results.last().getValue(), CLEVELAND);
        results = tree.autocomplete("even", "C", evenFilter, 10);
        assertEquals(results.first().getValue(), CINCINATTI);
        assertEquals(results.last().getValue(), CHARLESTON);

        // repeat to test cache.
        results = tree.autocomplete("odd", "C", oddFilter, 10);
        assertEquals(results.size(), 2);
        assertEquals(results.first().getValue(), CHICAGO);
        assertEquals(results.last().getValue(), CLEVELAND);
        results = tree.autocomplete("even", "C", evenFilter, 10);
        assertEquals(results.first().getValue(), CINCINATTI);
        assertEquals(results.last().getValue(), CHARLESTON);

        // try getting everything
        results = tree.autocomplete("C", 10);
        assertEquals(results.size(), 4);
        assertEquals(results.first().getValue(), CHICAGO);
        assertEquals(results.last().getValue(), CHARLESTON);
    }

    public AutocompleteTree<Integer, City> makeTree() {
        AutocompleteTree<Integer, City> act = new AutocompleteTree<Integer, City>();
        act.setMaxCacheQueryLength(0);  // Don't cache

        act.add(makeEntry(CHICAGO));
        act.add(makeEntry(MINNEAPOLIS));
        act.add(makeEntry(BOSTON));
        act.add(makeEntry(CINCINATTI));
        act.add(makeEntry(CLEVELAND));
        act.add(makeEntry(CHARLESTON));
        act.add(makeEntry(ST_PAUL));

        return act;
    }

    public AutocompleteEntry<Integer, City> makeEntry(City city) {
        return new AutocompleteEntry<Integer, City>(city.getId(), city);
    }
}
