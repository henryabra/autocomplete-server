autocomplete-server
===================

Copyright Shilad Sen, 2009-2010

A Java library supporting server-side AJAX autocomplete. This is a mirror repository of the project in http://code.google.com/p/autocomplete-server/

Overview
========

This project can be integrated in two ways:

Standalone server (Removed - see original project @ google code): A standalone autocomplete HTTP server providing JSON / REST AJAX autocomplete functionality for any language.
Java library : A jar providing Autocomplete datastructures that can be easily integrated into an existing Java projects.
The library is build around an autocomplete tree that supports prefix queries on entities. Information about entities are stored in entries containing the entitiy and a score for the entity. The score is used to rank and truncate results when there are many entities that match a prefix query.

Queries can match against multiple psuedonyms for the same entity("Obama", "Barack Obama", "Barack Hussain Obama").

Usage
=====

Library Usage
The library provides an AutocompleteTree containing AutocompleteEntries.

        // Create the autocomplete datastructure
        AutocompleteTree<Integer, City> tree = new AutocompleteTree<Integer, City>();

        // Associate cities with their ids
        tree.add(1, new City(1, "Chicago", "Illinois"));
        tree.add(2, new City(2, "Moline", "Illinois"));
        tree.add(3, new City(3, "Minneapolis", "Minnesota"));
        tree.add(4, new City(4, "St. Paul", "Minnesota"));
        tree.add(5, new City(5, "Boston", "Massachussets"));
        ...

        tree.increment(5);      // increments the score for Boston by 1.
        tree.setScore(3, 9.0);  // sets the score for Minneapolis to 9.0;

        // Returns the top three cities that start with "ch" ordered by score.
        SortedSet<AutocompleteEntry<Integer, City>> results = tree.autocomplete("ch", 3);
        for (AutocompleteEntry<Integer, City> entry : results) {
            System.out.println("city " + entry.getValue() + " with score " + entry.getScore());
        }
API details are described on the AutocompleteTree javadoc

Server Usage
The HTTP server is a standalone service that provides a RESTful JSON interface to a persistent AutocompleteTree. The tree is persisted through a transaction file that contains information about all entries in the tree.

The tree supports autocomplete features on extensible autocomplete entities represented as (key, value) hashtables. The hashtable keys must be strings. The values can be any types supported by json. Three keys are required (id, name, score).

To start up the server, download the zip file, and run the following command from inside it:

java -cp lib/jlhttp.jar:lib/json_simple-1.1.jar:autocomplete-server-0.4.jar \
      edu.macalester.acs.server.AutocompleteServer \
      tx.log \
      8888
The first argument after the class name is the name of transaction log used to persist the data about autocomplete entities. The second argument is the port on which the server should listen.

Clients for the service can be easily written in any language using Json. For example, a python client can be written as:

import httplib

# update (or create) an entity
conn = httplib.HTTPConnection("localhost", 10101)
conn.request("POST", "/update", '{ "id" : "34a", "name" : "Bob", "score" : 300.2, "foo" : "bar"}')
print conn.getresponse().read()
>>> okay

# execute an ajax query
conn.request("GET", "/autocomplete?query=b&max=2")
print conn.getresponse().read()
>>> [{"id":"34a","name":"Bob","score":300.2,"foo":"bar"}, {"id":"20395","name":"Myrtle Beach","state":"South Carolina","score":99.9812292931998}]
More details about the API are provided in the AutocompleteServer javadoc.

Performance
Library Performance
The AutocompleteBenchmarker estimates that the library can perform 50,000 autocomplete queries per second against a dictionary of size 25,000 on a Macbook Pro.

The lookup performance of the algorithm is O(log(n) + m) where n is the size of the dictionary, and m is the number of matching terms. The data structure incorporates a cache for short-length queries that are typically associated with a large number of matching terms.

The memory performance scales linearly with the number of dictionary entries, with somewhere on the order of 100 bytes overhead per dictionary entry.

Server Performance
The HTTP server imposes roughly a half millisecond performance penalty on top of all autocomplete instructions.
