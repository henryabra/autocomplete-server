package edu.macalester.acs;

import java.util.List;

/**
 * Creates a collection of strings that will be autocompleted against.
 * Used by the AutocompleteTree to populate the fragments property of
 * the AutocompleteEntry.
 */
public interface Fragmenter<K extends Comparable, V> {
    public List<String> getFragments(AutocompleteEntry<K, V> entry);

    /**
     * Normalize a string (maybe by folding case and removing punctuation).
     *
     * @param dirty the string to be normalized.
     * @return
     */
    public String normalize(String dirty);
}
