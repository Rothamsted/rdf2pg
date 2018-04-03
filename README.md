# The RDF->Neo4j Converter

This is a Java-based project providing configurable components to convert RDF data into [Cypher](TODO) commands that can populate a [Neo4J](TODO) graph database.

You can configure the way RDF is mapped into Neo4J entities (nodes, node properties, relations and relation properties) by means of SPARQL queries. More details in the next sections.

The core of the project is the [rdf2neo](rdf2neo) library, while [rdf2neo-cli](rdf2neo-cli) module is a command line tool to manage Neo4J imports.


# Introduction

## Cypher and Neo4j

Our converter works entirely via Cypher instructions, i.e., we don't use graph-level APIs to access Neo4j. While we haven't extended our code to support [other graph databases](https://www.opencypher.org/projects) that support [OpenCypher](https://www.opencypher.org/), we would expect this to be easy to do. In the follow we mention mappings from RDF to Cypher, meaning the Cypher running on Neo4j. 


## Mapping RDF to Cypher/Neo4j entities: general concepts

The RDF data model and the Cypher data model are rather similar, but there are significant differences:

  * The native entities of Cypher are nodes, relations, node labels, relation types, and properties attached to nodes or relation 
  * In RDF essentially everything is a triple/statement, the equivalents of the the above entities are all modelled after triples:
  		* nodes are URI-provided resources that appear as subject or object of triples
  		* statements based on [rdf:type](TODO) are the closest thing to the definition of node labels (in Cypher labels are strings, in RDF they are other resources/URIs)
  		* A triple (or statement) joining two resources/URIs is the closest thing to a Cypher relation. In that case, another resource/URI is used for the triple predicate, which similar to stating the relation type. Again, the latter is a string, while a predicate is essentially a URI.
  		* An RDF triple having a literal as object (datatype properties in OWL) is roughly equivalent to a node property in Cypher. Again, Cypher property names are strings, triple datatype properties are URIs. There are other significant differences. For instance, string literals in RDF can have a language tag attached, no equivalent exists in Cypher (can be modelled as a 2-sized array). As another example, a property in Cypher can have an array as value, but the array contents must be homogeneous (ie, all values must have the same raw type), while in RDF an array is nothing but a [special set of statements](TODO). Moreover, in RDF you can (obviously) have multiple statements associated to a subject and all based on the same datatype predicate (e.g., ex:bob schema:name 'Robert', 'Bob'). This can only be emulated in Cypher, by merging multiple values into an array.      
		  * Cypher relations can have property/value pairs attached. In RDF you can only emulate this with constructs like [reified statements](TODO), named graphs or singleton properties (TODO). As you can see.
  		
So, two similar graph models and a number of differences. How to map one to the other?
 
On a first look, one may think that there is a sort of 'natural mapping' between RDF and Cypher. Rougly: anything having an rdf:type will generate a Cypher node with that type used as label (maybe not a whole URI like `http://www.example.com/ontology#Person`, just the last part of it, `Person`), any triple will generate a Cypher relation, maybe any reified statement based on the RDF syntax will be converted into a property-attached relation.

Indeed, [other projects](TODO) before our rdf2neo have adopted this approach. However, hard-wiring the mapping to a particular view of things can be too inflexible, no matter how natural that view is. For instance, in RDF we might have an ontology providing targets for `rdf:type` where all the classes have an `rdfs:label` associated (e.g., `ex:bob rdf:type ex:Person. ex:Person a owl:Class; rdfs:label 'Human Being'.` => `(bob:'Human Being')`. ), and this might be the thing we want to use as the Cypher node label. As another example, we might be using our own way to define reified relations on the RDF side (e.g., `ex:annotation1 a ex:Annotation; ex:source ex:doc1; ex:target ex:topic1; ex:score '0.9'^xsd:real`) and we may want to turn that schema of ours into Cypher relations (e.g., `(doc1:Document)-[:ANNOTATION{ score: 0.9}]->(topic1:Topic)`), while in the 'natural' mapping those sets of statements would be blindly mapped to Cypher nodes and binary property-less relations, (`(annotation1:Annotation{ score:0.9 })-[:SOURCE]->(doc1:Document), (annotation1)-[:TARGET]->(topic1:Topic)`).


## SPARQL-based mapping

That's why we have decided another way to map RDF to Cypher: a set of SPARQL queries that returns a list of Cypher entities (nodes and their labels, node details like properties, relations, etc) from the initial RDF data.

This makes rdf2neo very flexible, allowing you to support use cases like the ones mentioned above. 

In follow show how to define such queries.

In addition to SPARQL, we use a couple of components to define further configuration details, which cannot be managed via SPARQL-based mapping, or it's too difficult to do so. For example, we have a default URI to identifier converter, which converts URIs to short identifier strings, suitable to be used as node labels or relation types (e.g., `http://www.example.com/ontology#Person` => `Person`).  

TODO: we plan to ship our tools with SPARQL mappings for 'natural RDF mapping'.


## Spring-based configuration

SPARQL queries, the target Neo4j database and components like the URI-to-identifier converters are all configurable components in neo4j. You can work out a particular configuration for a given RDF data set, where you put together all these components. A configuration is defined as a [Spring configuration file](TODO), which provides with a powerful language to assemble components together (it plug in the underlying Java entities, but you don't need to know Java to understand these files).

*Note to developers: because we're using Spring, if you're going to use our [core library](rdf2neo) programmatically, you can additionally/optionally other Spring configuration means, such as [Java annotations](TODO)*      


## rdf2neo4 architecture

rdf2neo is more precisely a "TDB-to-neo" converter. That is, it takes RDF data from a [Jena](TODO) [TDB triple store](). Both the [programmatic interface](TODO) and the [command line tool](TOOL) starts from the path to the input TDB to use to populate a target Neo4j database.

We cannot load data directly from RDF files because we need to view all the dataset that you want to convert. For instance, before we can issue a Cypher command to create a node, we need to be able to fetch all of its details about its labels and properties. If these details were spread across different RDF files, we would need to first load all the files and then query them with SPARQL (the way shown below). Which is precisely what we do by using a TDB. The alternative would be an in-memory triple store (i.e., what Jena calls a memory [Model](TODO), but this might not be good if you have large datasets.

We plan to make it possible to query an HTTP-based SPARQL endpoint in future.


# Mapping details

In this section we are going to show abstracts from the [DBPedia example](rdf2neo/src/test/resources), which maps some example RDF downloaded from [DBPedia](TODO) into Neo4j.

rdf2neo allows you to define multiple config sets (named `ConfigItem` in the Spring configurations). Each has a list of SPARQL mapping queries and possibly other configuration elements about a logical subset of your RDF data. For instance in the DBPedia example we have a `ConfigItem` for mapping data about places and another to map data about people. In simple project you might have just one config set, we allows for many because this helps keeps data subsets separated.

## Node mappings

RDF data can be mapped to Cypher nodes by means of the following query types. 

### Node URIs

This is a SPARQL query that lists all the URIs about RDF resources that represent a node. An example:

*Note*: For sake of precision, the configuration files uses the word 'IRI'. If you don't consider the [technical differences], it can be considered synonym of URI.  

```sql
# The node list query must always project a ?iri variable
# (further returned variables are safely ignored, performance is usually better if you don't mention them at all  
SELECT DISTINCT ?iri
WHERE
{
  # This picks up nodes of interests based on their rdf:type, which should be pretty common.
  # Any instance of Person and Employee is considered
  { ?iri a schema:Person }
  UNION { ?iri a schema:Employee }
  
  # Another option is to consider anyone in the domain or range of a property, i.e., you know that anyone involved in a foaf:knows relation
  # must be a person.
  UNION { ?someone foaf:knows|^foaf:knows ?iri }
}
```

**It's very important that the query above returns distinct results.**


### Node labels

This query takes is invoked for each of the URIs found by the node URIs and is parameterised over a single node URIs. It should return all the labels that you want to assign to that node on the Cypher side. For instance,

```sql
# The node list query must always project a ?label variable and must use the ?iri variable in the WHERE clause. ?iri will be bound to one of 
# IRIs found in the node IRI query. The label query will be invoked once per node IRI, its purpose is to list all the Cypher labels that have to be 
# assigned to the node.
#
# A label can be either a IRI or a literal, or a string. If it's a URI, it will be translated into a Cypher identifier by means of the configured
# IRI-to-ID converter. At the moment we're using the default DefaultIri2IdConverter (see the Java sources), which takes the last part of an IRI.
# 
SELECT DISTINCT ?label
WHERE 
{  
  # As said above, ?iri is a constant during the actual execution of this query.
  # When DefaultIri2IdConverter is used, schema:Person will become the label 'Person'.
  { ?iri a ?label }
  
  # We always want this label
  UNION { BIND ( schema:Person AS ?label }
}
```

### Node properties

This works with the same mechanism (one query per node URI, the `?iri` variable bound to a specific URI) and lists all the pairs of property name + value that you want to assign to the node: 

```sql
# You need to return these two variables. ?iri is bound to a constant, as above.
#
# - ?name is typically a IRI and is converted into a shorter ID by means of a configured IRI->ID converter. (no conversion if it's a literal)
# - ?value is a literal and, for the moment, is converted to string, using its lexical value. We'll offer
# more customisation soon (e.g., mapping XSD types to Cypher/Java types).
#
SELECT DISTINCT ?name ?value
{
  ?iri ?name ?value.
  FILTER ( isNumeric (?value) || LANG ( ?value ) = 'en' ). # Let's consider only these values

  # We're interested in these properties only
  # Again, these are passed to DefaultIri2IdConverter by default, and so things like rdfs:label, dbo:areaTotal become 'label', 'areaTotal'   
  VALUES ( ?name ) {
    ( rdfs:label )
    ( rdfs:comment )
    ( foaf:givenName )
    ( foaf:familyName )
  }
}
```

So, this RDF exists:
 
```java
@prefix ex: <http://www.example.com/resources/>

ex:john a schema:Person, schema:Employee;
  foaf:givenName "John";
  foaf:familyName "Smith".
```

The queries above will give the following Cypher node:

```sql
  { iri:"http://www.example.com/resources/john", givenName: 'John', familyName: 'Smith' }: [ `Person`, `Employee`, `Resource` ]
```

As you can see there are values that are created implicitly: 

  - every node has always an iri property. We need this to correctly process the RDF-defined relations (see below) and we think it can be useful to track the URI of provenance for a node. This property is always indexed and has distinct values.
  
  - every node has a always a default label. The default is `Resource`, but it can be changed by configuring a 
  String bean `defaultNodeLabel` as id. Again, we need this in order to find nodes by their iri (the Cypher construct: `MATCH ( n: { id: $const }:Resource )` is very fast, not so if whe have to match the label with `WHERE $myLabel IN LABELS (n)`).
    
- Spring

## Relation mappings
- lists and labels
- relations with properties
- Identifiers
- Spring

## Other configuration elements
### Neo4j connection
### Cypher Indexes
### default label
### default ID converter
### See the beans!

