Copyright Shilad Sen, 2009-2010

A Java library supporting server-side AJAX autocomplete.
This project can be integrated in two ways:

   1. A standalone http server with a REST / JSON interface that can provide
      AJAX autocomplete functionality for any language.
   2. A jar providing Autocomplete datastructures that can be easily integrated
      into an existing Java projects. 

(Functionality #1 is still under construction).

This project is licensed under the Apache License 2.0.

You can build and run this project using mvn.

As a quickstart, you can do the following:

        AutocompleteTree<Integer, City> tree = new AutocompleteTree<Integer, City>();
        tree.add(1, new City("Chicago", "Illinois"));
        tree.add(2, new City("Moline", "Illinois"));
        tree.add(3, new City("Minneapolis", "Minnesota"));
        tree.add(4, new City("St. Paul", "Minnesota"));
        tree.add(5, new City("Boston", "Massachussets"));
        ...

        tree.increment(5);      // increments the score for Boston by 1.
        tree.setScore(3, 9.0);  // sets the score for Minneapolis to 9.0;
 
        SortedSet<AutocompleteEntry<Integer, City>> results = tree.autocomplete("ch", 3);
        for (AutocompleteEntry<Integer, City> entry : results) {
            System.out.println("city " + entry.getValue() + " with score " + entry.getScore());
        }

For more information, visit the Google Code project page at
http://code.google.com/p/autocomplete-server/
