package edu.macalester.acs;

/**
 * Author: Shilad Sen
 * Example entity class for autocomplete tree.
 */
public class City {
    private Integer id;
    private String  name;
    private String  state;

    // Could have other fields, but they aren't important for testing.

    public City(Integer id, String name, String state) {
        this.id = id;
        this.name = name;
        this.state = state;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getState() {
        return state;
    }

    public String toString() {
        return name;
    }
}
