package edu.macalester.acs;

import java.util.*;

/**
 * Supports autocomplete queries on some entities.
 * Autocomplete queries return entities whose name begins with a query.<p>
 * <p/>
 * Autocomplete queries may have more matches than the user desires.  For
 * examples, thousands of cities that start with 'S'.  Each entity
 * has a score associated with it that is used to decide which entities to
 * return when there are too many matches.<p>
 * <p/>
 * Every entity entry has a unique key (such as a database identifier),
 * a value (the entity), and a score.  These three pieces of information are
 * stored in an instanc of the {@link AutocompleteEntry} class.
 * The key should be a unique identifier for an entity such as a database id.
 * The value should contain data that your application needs to provide about
 * the entity in the external autocomplete interface, such as a name or
 * description. The keys must implement reasonable hashCode, compareTo,
 * and equals methods.<p>
 * <p/>
 * Entities may have multiple names, called "fragments."  For example,
 * if an entity for the 44th president of the United States wants to support
 * prefix matching against "Barack Hussain Obama," "Barack Obama" and "Obama"
 * it will have fragments for each of these pseudonyms.
 * Each fragment is stored in an instance of the {@link AutocompleteFragment}
 * class, which simply stores a (textual fragment, autocomplete entry) pair.<p>
 * <p/>
 * This class uses a fragmenter that provides string normalization and
 * fragmentation.  It normalizes both query strings and fragments (e.g. converts
 * <i>Shilad W. Sen</i> to <i>shilad w sen</i>.  And also fragments as described
 * earlier.  By default, a SimpleFragmenter is used, but you can specify your
 * own fragmenter.<p>
 * <p/>
 * This implementation optionally caches results for short prefixes.  The
 * maximum cached query length can be controlled by changing the
 * maxCacheQueryLength attribute.  Setting this to one or two (default is 2)
 * can dramatically speed up the number of queries at the cost of increased
 * memory.  The number of results stored for a particular prefix is specified
 * by the numCacheResults attribute.  This should ideally be equal to the
 * maxResults parameter passed to the autocomplete method.  Cache entries are
 * automatically invalidated when entries are created, removed, or when their
 * score is updated.
 * <p/>
 * <p/>
 * An example usage for the tree for entities of the example {@link City} class:
 * <p/>
 * <pre><blockquote>
 * AutocompleteTree<Integer, City> tree = new AutocompleteTree<Integer, City>();
 * tree.add(1, new City("Chicago", "Illinois"));
 * tree.add(2, new City("Moline", "Illinois"));
 * tree.add(3, new City("Minneapolis", "Minnesota"));
 * tree.add(4, new City("St. Paul", "Minnesota"));
 * tree.add(5, new City("Boston", "Massachussets"));
 * ...
 * <p/>
 * tree.increment(5);      // increments the score for Boston by 1.
 * tree.setScore(3, 9.0);  // sets the score for Minneapolis to 9.0;
 * <p/>
 * SortedSet<AutocompleteEntry<Integer, City>> results = tree.autocomplete("ch", 3);
 * for (AutocompleteEntry<Integer, City> entry : results) {
 * System.out.println("city " + entry.getValue() + " with score " + entry.getScore());
 * }
 * </blockquote></pre>
 * <p/>
 * This class should be thread-safe.
 * <p/>
 * TODO:
 * <ul>
 * <li>Enable the autocomplete() method to supply a predicate so that it
 * can only match a subset of the entities in the tree.  E.g. just show
 * me People starting with "fo."  Adjust the cache so that it is predicate
 * aware, perhaps by asking the caller to define a named "scope" associated
 * with the passed-in predicate.
 * <li>Support lucene (or something similar) as a persistent, disk-based
 * datastructure.
 * <li>Explore ways of improving locks so that threading scales better.
 * <li>Optimize cache invalidation logic.
 * </ul>
 *
 * @author Shilad Sen
 */
public class AutocompleteTree<K extends Comparable, V> extends BaseAutocompleter<K, V> implements Autocompleter<K, V> {

    /**
     * Maximum length for prefixes that should be cached
     */
    private static final int DEFAULT_MAX_CACHE_QUERY_LENGTH = 2;

    /**
     * Initial number of results to cache per prefix
     */
    private static final int DEFAULT_NUM_CACHE_RESULTS = 20;

    /**
     * Name of the default cache
     */
    private static final String DEFAULT_DOMAIN = "DEFAULT_DOMAIN";

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
     * Cache for common queries.
     * The top-level map is domain names to their individual caches.
     */
    private final Map<String, Map<String, TreeSet<AutocompleteEntry<K, V>>>> cache =
            new HashMap<String, Map<String, TreeSet<AutocompleteEntry<K, V>>>>();

    /**
     * Maximum query length for entries in the cache.
     */
    private int maxCacheQueryLength = DEFAULT_MAX_CACHE_QUERY_LENGTH;

    /**
     * Number of results to cache for each query
     */
    private int numCacheResults = DEFAULT_NUM_CACHE_RESULTS;

    /**
     * Creates a new autocomplete tree with a SimpleFragmenter.
     */
    public AutocompleteTree() {
        fragmenter = new SimpleFragmenter<K, V>();
    }

    /**
     * Creates a new autocomplete tree with a custom fragmenter.
     *
     * @param fragmenter
     */
    public AutocompleteTree(Fragmenter<K, V> fragmenter) {
        this.fragmenter = fragmenter;
    }

    /* (non-Javadoc)
     * @see edu.macalester.acs.Autocompleter#clear()
	 */
    public void clear() {
        synchronized (map) {
            synchronized (tree) {
                synchronized (cache) {
                    map.clear();
                    tree.clear();
                    cache.clear();
                }
            }
        }
    }

    public int getMaxCacheQueryLength() {
        return maxCacheQueryLength;
    }

    /**
     * Set the maximumum length for queries that are cached.
     * Also clears the cache.
     *
     * @param maxCacheQueryLength
     */
    public void setMaxCacheQueryLength(int maxCacheQueryLength) {
        synchronized (cache) {
            this.maxCacheQueryLength = maxCacheQueryLength;
            cache.clear();
        }
    }

    /**
     * Returns the maximum number of results cached for any query.
     *
     * @return
     */
    public int getNumCacheResults() {
        return numCacheResults;
    }

    /**
     * Sets the maximum number of results that are cached for short queries.
     *
     * @param numCacheResults
     */
    public void setNumCacheResults(int numCacheResults) {
        synchronized (cache) {
            this.numCacheResults = numCacheResults;
            cache.clear();
        }
    }

    /* (non-Javadoc)
     * @see edu.macalester.acs.Autocompleter#add(edu.macalester.acs.AutocompleteEntry)
	 */
    public void add(AutocompleteEntry<K, V> entry) {
        synchronized (map) {
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
            synchronized (tree) {
                // Add the data structures.
                tree.addAll(entry.getFragments());
            }
            adjustCacheForIncreasedScore(entry);
        }
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
        synchronized (map) {
            return new ArrayList<AutocompleteEntry<K, V>>(map.values());
        }
    }

    /* (non-Javadoc)
     * @see edu.macalester.acs.Autocompleter#remove(K)
	 */
    public void remove(K key) {
        synchronized (map) {
            AutocompleteEntry entry = map.get(key);
            if (entry == null) {
                //            throw new IllegalArgumentException("unknown key: " + key);
                return;
            }
            map.remove(entry.getKey());
            synchronized (tree) {
                tree.removeAll(entry.getFragments());
            }
            adjustCacheForDecreasedScore(entry);
        }
    }

    /* (non-Javadoc)
	 * @see edu.macalester.acs.Autocompleter#contains(K)
	 */
    public boolean contains(K key) {
        synchronized (map) {
            return map.containsKey(key);
        }
    }

    /* (non-Javadoc)
	 * @see edu.macalester.acs.Autocompleter#get(K)
	 */
    public AutocompleteEntry<K, V> get(K key) {
        synchronized (map) {
            return map.get(key);
        }
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
        synchronized (map) {
            AutocompleteEntry entry = map.get(key);
            if (entry == null) {
                throw new IllegalArgumentException("unknown key: " + key);
            }

            synchronized (tree) {
                // Not sure why this has to loop over objects, but I couldn't get it
                // to work properly with generics.
                for (Object o : entry.getFragments()) {
                    AutocompleteFragment<K, V> fragment = (AutocompleteFragment<K, V>) o;
                    tree.remove(fragment);
                }
                double df = score - entry.getScore();
                if (df < 0) {
                    adjustCacheForDecreasedScore(entry);
                }
                else if (df > 0) {
                    adjustCacheForIncreasedScore(entry);
                }
                entry.setScore(score);
                for (Object o : entry.getFragments()) {
                    AutocompleteFragment<K, V> fragment = (AutocompleteFragment<K, V>) o;
                    tree.add(fragment);
                }
            }
        }

    }

    /* (non-Javadoc)
	 * @see edu.macalester.acs.Autocompleter#autocomplete(java.lang.String, java.lang.String, edu.macalester.acs.AutocompleteFilter, int)
	 */
    public SortedSet<AutocompleteEntry<K, V>> autocomplete(String domain, String query, AutocompleteFilter<K, V> filter, int maxResults) {
        if (domain == null && filter != null) {
            throw new IllegalArgumentException("cannot use a filter without a domain");
        }
        domain = (domain == null) ? DEFAULT_DOMAIN : domain;
        String start = fragmenter.normalize(query);
        TreeSet<AutocompleteEntry<K, V>> results = null;

        // Check the cache
        if (start.length() <= maxCacheQueryLength) {
            synchronized (cache) {
                if (!cache.containsKey(domain)) {
                    cache.put(domain, new HashMap<String, TreeSet<AutocompleteEntry<K, V>>>());
                }
                results = cache.get(domain).get(start);
            }
        }

        if (results == null) {
            int n = maxResults;
            if (start.length() <= maxCacheQueryLength) {
                n = Math.max(n, numCacheResults);
            }
            AutocompleteFragment<K, V> startWrapper = new AutocompleteFragment(null, start);
            String end = start.substring(0, start.length() - 1);
            end += (char) (start.charAt(start.length() - 1) + 1);
            AutocompleteFragment<K, V> endWrapper = new AutocompleteFragment(null, end);
            results = new TreeSet<AutocompleteEntry<K, V>>(SCORE_COMPARATOR);

            synchronized (tree) {
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
            }
        }

        if (start.length() <= maxCacheQueryLength) {
            synchronized (cache) {
                cache.get(domain).put(start, results);
            }
        }

        // Truncate if necessary (because of cache fills)
        if (results.size() > maxResults) {
            TreeSet<AutocompleteEntry<K, V>> truncated = new TreeSet<AutocompleteEntry<K, V>>(SCORE_COMPARATOR);
            Iterator<AutocompleteEntry<K, V>> iterator = results.iterator();
            for (int i = 0; i < maxResults; i++) {
                truncated.add(iterator.next());
            }
            results = truncated;
        }

        return results;
    }

    /**
     * Adjusts the cache when the score of an entry is about to be decreased.
     * Note that this has to be called BEFORE the score is decreased, since
     * the score is incorporated in the entry's compareTo() function.
     *
     * @param entry
     */
    private void adjustCacheForDecreasedScore(AutocompleteEntry<K, V> entry) {
        synchronized (cache) {
            for (Object o : entry.getFragments()) {
                // hack for unknown reasons
                AutocompleteFragment<K, V> fragment = (AutocompleteFragment<K, V>) o;
                for (int n = 1; n <= maxCacheQueryLength; n++) {
                    if (fragment.getFragment().length() >= n) {
                        String prefix = fragment.getFragment().substring(0, n);
                        for (Map<String, TreeSet<AutocompleteEntry<K, V>>> domainCache : cache.values()) {
                            SortedSet<AutocompleteEntry<K, V>> results = domainCache.get(prefix);
                            if (results != null && results.contains(entry)) {
                                domainCache.remove(prefix);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Adjusts the cache when the score of an entry is about to be increased.
     * Note that this has to be called BEFORE the score is increased, since
     * the score is incorporated in the entry's compareTo() function.
     *
     * @param entry
     */
    private void adjustCacheForIncreasedScore(AutocompleteEntry<K, V> entry) {
        // Check to see if the new entry will make the cut in the cache.
        synchronized (cache) {
            for (AutocompleteFragment<K, V> fragment : entry.getFragments()) {
                for (int n = 1; n <= maxCacheQueryLength; n++) {
                    if (fragment.getFragment().length() >= n) {
                        String prefix = fragment.getFragment().substring(0, n);
                        for (Map<String, TreeSet<AutocompleteEntry<K, V>>> domainCache : cache.values()) {
                            SortedSet<AutocompleteEntry<K, V>> results = domainCache.get(prefix);
                            if (results != null) {
                                if (results.size() < numCacheResults ||
                                        results.last().getScore() <= entry.getScore()) {
                                    domainCache.remove(prefix);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
