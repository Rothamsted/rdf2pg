# rdf2pg, Core Library

This is the core package, providing with the API and machinery to perform the mappings and conversions.  

The code in this package is target-independent, ie, it deals with loading RDF, mapping it to an abstract property graph
and providing the scaffholding needed to run a conversion process to any specific conversion target. Specific modules 
based on this (eg, rdf2neo, rdf2graphml) adds classes to manage specific conversions. 
