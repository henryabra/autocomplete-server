package edu.macalester.acs;

import junit.framework.TestCase;

import java.util.List;

/**
 * Author: Shilad Sen
 * Date: Dec 20, 2009
 */
public class TestSimpleFragmenter extends TestCase {
    private SimpleFragmenter frag = new SimpleFragmenter();

    public void testNormalize() {
        assertEquals(frag.normalize("asdf"), "asdf");
        assertEquals(frag.normalize("Asdf"), "asdf");
        assertEquals(frag.normalize("Asdf a"), "asdf a");
        assertEquals(frag.normalize("Asdf  a"), "asdf a");
        assertEquals(frag.normalize("Asdf  a'f"), "asdf af");
        assertEquals(frag.normalize("Asdf  a.!f"), "asdf a f");
        assertEquals(frag.normalize(" Asdf  a.!f!"), "asdf a f");
    }

    public void testFragmenter() {
        String test = "gender and sexuality in literature";
        List<String> fragments = frag.getFragments(new AutocompleteEntry<Integer, String>(0, test));
        assertEquals(fragments.size(), 5);
        assertTrue(fragments.contains("literature"));
        assertTrue(fragments.contains("in literature"));
        assertTrue(fragments.contains("sexuality in literature"));
        assertTrue(fragments.contains("and sexuality in literature"));
        assertTrue(fragments.contains("gender and sexuality in literature"));
    }
}
