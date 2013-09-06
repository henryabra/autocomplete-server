package edu.macalester.acs;

/**
 * @author Shilad Sen
 * @see TrivialAutocompleteTree
 */
public class AutocompleteFragment<K extends Comparable, V> implements Comparable<AutocompleteFragment<K, V>> {
    private AutocompleteEntry<K, V> entry;
    private String                  fragment;

    public AutocompleteFragment(AutocompleteEntry<K, V> entry, String fragment) {
        this.entry = entry;
        this.fragment = fragment;
    }

    public AutocompleteEntry<K, V> getEntry() {
        return entry;
    }

    public String getFragment() {
        return fragment;
    }

    public int compareTo(AutocompleteFragment<K, V> other) {
        int r = fragment.compareTo(other.fragment);
        if (r != 0) {
            return r;
        }
        if (entry != null && other.entry != null) {
            if (r == 0) {
                if (entry.getScore() > other.entry.getScore()) {
                    r = -1;
                }
                else if (entry.getScore() < other.entry.getScore()) {
                    r = +1;
                }
            }
            if (r == 0) {
                r = entry.getKey().compareTo(other.entry.getKey());
            }
        }
        else if (entry == null && other.entry != null) {
            r = -1;
        }
        else if (entry != null && other.entry == null) {
            r = +1;
        }
        else {
            assert (entry == null && other.entry == null);
            r = 0;
        }
//        System.out.println("result for " + fragment + " and " + other.fragment + " is " + r);
        return r;
    }

    public boolean equals(Object o) {
        if (o instanceof AutocompleteFragment) {
            AutocompleteFragment<K, V> other = (AutocompleteFragment<K, V>) o;
            return (this.compareTo(other) == 0);
        }
        else {
            return false;
        }
    }

    public String toString() {
        return "fragment: '" + fragment + "' for entry " + entry;
    }
}
