package edu.macalester.acs;

public interface AutocompleteFilter<K extends Comparable, V> {

    /**
     * Returns true iff the entry should be returned in the results.
     *
     * @param entry
     * @return
     */
    public boolean matches(AutocompleteEntry<K, V> entry);
}
