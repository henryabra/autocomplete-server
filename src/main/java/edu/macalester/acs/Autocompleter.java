package edu.macalester.acs;

import java.util.SortedSet;

public interface Autocompleter<K extends Comparable, V> {

    /**
     * Removes all information in the tree.
     */
    public void clear();

    /**
     * Add a new autocomplete entry to the map.
     *
     * @param entry
     */
    public void add(AutocompleteEntry<K, V> entry);

    /**
     * Adds a new autocomplete entry to the tree.
     *
     * @param key
     * @param value
     */
    public void add(K key, V value);

    /**
     * Adds a new autocomplete entry to the tree.
     *
     * @param key
     * @param value
     * @param score
     */
    public void add(K key, V value, double score);

    /**
     * Removes an autocomplete entry from the map.
     *
     * @param key
     */
    public void remove(K key);

    /**
     * Check to see if the map contains an entry associated with the
     * provided key.
     *
     * @param key
     * @return
     */
    public boolean contains(K key);

    /**
     * Return the entry associated with the provided key.
     *
     * @param key
     * @return
     */
    public AutocompleteEntry<K, V> get(K key);

    /**
     * Sets the score of the entry associated with the key.
     * This function MUST be used to set the score of an entry once
     * it is in the tree.  Otherwise, the tree will get very confused.
     *
     * @param key
     * @param score
     */
    public void setScore(K key, double score);

    /**
     * Increments the score of the provided key by 1.
     *
     * @param key
     */
    public void increment(K key);

    /**
     * Decrements the score of the provided key by 1.
     *
     * @param key
     */
    public void decrement(K key);

    /**
     * Executes an autocomplete search against the stored entries.
     * Before comparing the query to fragments, each is normalized using
     * the fragmenter (or SimpleFragmenter if none was specified)
     * If there are more than maxResults that begin with the query, the
     * highest-score results are returned.
     *
     * @param query
     * @param maxResults Maximum number of results that are returned.
     * @return
     */
    public SortedSet<AutocompleteEntry<K, V>> autocomplete(
            String query, int maxResults);

    /**
     * Executes an autocomplete search against the stored entries.
     * Before comparing the query to fragments, each is normalized using
     * the fragmenter (or SimpleFragmenter if none was specified)
     * If there are more than maxResults that begin with the query, the
     * highest-score results are returned.
     *
     * @param query
     * @param maxResults Maximum number of results that are returned.
     * @return
     */
    public SortedSet<AutocompleteEntry<K, V>> autocomplete(
            String domain, String query, AutocompleteFilter<K, V> filter,
            int maxResults);

}