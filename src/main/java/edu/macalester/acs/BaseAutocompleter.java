package edu.macalester.acs;

import java.util.Comparator;
import java.util.SortedSet;

/**
 * @author Shilad Sen
 */
public abstract class BaseAutocompleter<K extends Comparable, V> implements Autocompleter<K, V> {

    /**
     * Name of the default cache
     */
    private static final String DEFAULT_DOMAIN = "DEFAULT_DOMAIN";

    /**
     * Compares Autocomplete entires by score.  Ties broken by key
     */
    public Comparator<AutocompleteEntry<K, V>> SCORE_COMPARATOR =
            new Comparator<AutocompleteEntry<K, V>>() {
                public int compare(AutocompleteEntry<K, V> e1, AutocompleteEntry<K, V> e2) {
                    double r = e1.getScore() - e2.getScore();
                    if (r > 0) {
                        return -1;
                    }
                    else if (r < 0) {
                        return +1;
                    }
                    else {
                        return e1.getKey().compareTo(e2.getKey());
                    }
                }
            };

    /* (non-Javadoc)
     * @see edu.macalester.acs.Autocompleter#add(K, V)
	 */
    public void add(K key, V value) {
        add(new AutocompleteEntry<K, V>(key, value));
    }

    /* (non-Javadoc)
     * @see edu.macalester.acs.Autocompleter#add(K, V, double)
	 */
    public void add(K key, V value, double score) {
        add(new AutocompleteEntry<K, V>(key, value, score));
    }

    /* (non-Javadoc)
     * @see edu.macalester.acs.Autocompleter#increment(K)
	 */
    public void increment(K key) {
        setScore(key, get(key).getScore() + 1);
    }

    /* (non-Javadoc)
	 * @see edu.macalester.acs.Autocompleter#decrement(K)
	 */
    public void decrement(K key) {
        setScore(key, get(key).getScore() - 1);
    }

    /* (non-Javadoc)
	 * @see edu.macalester.acs.Autocompleter#autocomplete(java.lang.String, int)
	 */
    public SortedSet<AutocompleteEntry<K, V>> autocomplete(String query, int maxResults) {
        return autocomplete(DEFAULT_DOMAIN, query, null, maxResults);
    }
}
