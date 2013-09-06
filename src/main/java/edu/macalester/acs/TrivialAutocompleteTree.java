package edu.macalester.acs;

import java.util.*;

/**
 * Supports autocomplete queries on some entities.
 * Autocomplete queries return entities whose name begins with a query.<p>
 * <p/>
 * Based on AutocompleteTree, but has no caching or locking.
 *
 * @author Shilad Sen
 */
public class TrivialAutocompleteTree<K extends Comparable, V> extends BaseAutocompleter<K, V> implements Autocompleter<K, V> {

    /**
     * Cleans and tokenizes entity names
     */
    private Fragmenter<K, V> fragmenter;

    /**
     * Compares Autocomplete entires by score.  Ties broken by key
     */
    private Comparator<AutocompleteEntry<K, V>> SCORE_COMPARATOR =
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

    /**
     * Mapping from keys to ac entries
     */
    private final Map<K, AutocompleteEntry<K, V>> map = new HashMap<K, AutocompleteEntry<K, V>>();

    /**
     * Alpha sorted autocomplete tree
     */
    private final TreeSet<AutocompleteFragment<K, V>> tree = new TreeSet<AutocompleteFragment<K, V>>();

    /**
     * Creates a new autocomplete tree with a SimpleFragmenter.
     */
    public TrivialAutocompleteTree() {
        fragmenter = new SimpleFragmenter<K, V>();
    }

    /**
     * Creates a new autocomplete tree with a custom fragmenter.
     *
     * @param fragmenter
     */
    public TrivialAutocompleteTree(Fragmenter<K, V> fragmenter) {
        this.fragmenter = fragmenter;
    }

    /* (non-Javadoc)
     * @see edu.macalester.acs.Autocompleter#clear()
     */
    public void clear() {
        map.clear();
        tree.clear();

    }

    /* (non-Javadoc)
     * @see edu.macalester.acs.Autocompleter#add(edu.macalester.acs.AutocompleteEntry)
     */
    public void add(AutocompleteEntry<K, V> entry) {
        if (map.containsKey(entry.getKey())) {
            //            throw new IllegalArgumentException("entry for " + entry.getKey() + " already exists");
            return;
        }
        map.put(entry.getKey(), entry);
        // Populate fragments
        entry.clearFragments();
        for (String fragment : fragmenter.getFragments(entry)) {
            entry.addFragment(new AutocompleteFragment<K, V>(entry, fragment));
        }
        entry.freezeFragments();
        // Add the data structures.
        tree.addAll(entry.getFragments());
    }

    /**
     * Returns a collection of all the autocomplete entries in the
     * tree.  This is an expensive operation memory-wise, since the
     * list is copied.  We need to do this to ensure that underlying
     * collection is not changed by the caller.
     *
     * @return
     */
    public Collection<AutocompleteEntry<K, V>> getEntries() {
        return new ArrayList<AutocompleteEntry<K, V>>(map.values());
    }

    /* (non-Javadoc)
     * @see edu.macalester.acs.Autocompleter#remove(K)
     */
    public void remove(K key) {
        AutocompleteEntry entry = map.get(key);
        if (entry == null) {
            //            throw new IllegalArgumentException("unknown key: " + key);
            return;
        }
        map.remove(entry.getKey());
        tree.removeAll(entry.getFragments());
    }

    /* (non-Javadoc)
     * @see edu.macalester.acs.Autocompleter#contains(K)
     */
    public boolean contains(K key) {
        return map.containsKey(key);
    }

    /* (non-Javadoc)
     * @see edu.macalester.acs.Autocompleter#get(K)
     */
    public AutocompleteEntry<K, V> get(K key) {
        return map.get(key);
    }

    /**
     * Sets the score of the entry associated with the key.
     * This function MUST be used to set the score of an entry once
     * it is in the tree.  Otherwise, the tree will get very confused.
     *
     * @param key
     * @param score
     */
    public void setScore(K key, double score) {
        AutocompleteEntry entry = map.get(key);
        if (entry == null) {
            throw new IllegalArgumentException("unknown key: " + key);
        }

        // Not sure why this has to loop over objects, but I couldn't get it
        // to work properly with generics.
        for (Object o : entry.getFragments()) {
            AutocompleteFragment<K, V> fragment = (AutocompleteFragment<K, V>) o;
            tree.remove(fragment);
        }
        entry.setScore(score);
        for (Object o : entry.getFragments()) {
            AutocompleteFragment<K, V> fragment = (AutocompleteFragment<K, V>) o;
            tree.add(fragment);
        }

    }

    /* (non-Javadoc)
     * @see edu.macalester.acs.Autocompleter#autocomplete(java.lang.String, java.lang.String, edu.macalester.acs.AutocompleteFilter, int)
     */
    public SortedSet<AutocompleteEntry<K, V>> autocomplete(String domain, String query, AutocompleteFilter<K, V> filter, int maxResults) {
        String start = fragmenter.normalize(query);
        TreeSet<AutocompleteEntry<K, V>> results = null;

        int n = maxResults;
        AutocompleteFragment<K, V> startWrapper = new AutocompleteFragment<K, V>(null, start);
        String end = start.substring(0, start.length() - 1);
        end += (char) (start.charAt(start.length() - 1) + 1);
        AutocompleteFragment<K, V> endWrapper = new AutocompleteFragment<K, V>(null, end);
        results = new TreeSet<AutocompleteEntry<K, V>>(SCORE_COMPARATOR);

        for (AutocompleteFragment<K, V> fragment : tree.subSet(startWrapper, endWrapper)) {
            if (filter != null && !filter.matches(fragment.getEntry())) {
                continue;
            }
            if (results.size() < n) {
                results.add(fragment.getEntry());
            }
            else if (SCORE_COMPARATOR.compare(fragment.getEntry(), results.last()) < 0) {
                results.remove(results.last());
                results.add(fragment.getEntry());
            }
        }

        return results;
    }

}
