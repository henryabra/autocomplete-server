package edu.macalester.acs;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragments strings by splitting on whitespace, and normalizing them.
 * User: shilad
 * Date: Dec 20, 2009
 */
public class SimpleFragmenter<K extends Comparable, V> implements Fragmenter<K, V> {

    public List<String> getFragments(AutocompleteEntry<K, V> entry) {
        // Build up list of fragments
        List<String> words = new ArrayList<String>();
        for (String token : normalize(entry.getValue().toString()).split("\\s+")) {
            words.add(token);
        }

        // Make each fragment extend to the end of the phrase
        List<String> fragments = new ArrayList<String>();
        String suffix = "";
        for (int i = words.size() - 1; i >= 0; i--) {
            if (!suffix.isEmpty()) {
                suffix = " " + suffix;
            }
            suffix = words.get(i) + suffix;
            fragments.add(suffix);
        }
        return fragments;
    }

    public String normalize(String dirty) {
        return dirty.toLowerCase()
                    .replaceAll("'", "")
                    .replaceAll("[^0-9a-zA-Z]", " ")
                    .replaceAll(" \\s+", " ")
                    .trim();
    }
}
