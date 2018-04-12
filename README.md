# The RDF-to-Neo4j Converter

This is a Java-based project providing configurable components to convert RDF data into [Cypher](https://neo4j.com/developer/cypher-query-language/) commands that can populate a [Neo4j](https://neo4j.com) graph database.

You can configure the way RDF is mapped into Neo4J entities (nodes, node properties, relations and relation properties) by means of SPARQL queries. More details in the next sections.

The core of the project is the [rdf2neo](rdf2neo) library, while [rdf2neo-cli](rdf2neo-cli) module is a command line tool to manage Neo4J imports.


# Table of Contents

* [The RDF\-to\-Neo4j Converter](#the-rdf-to-neo4j-converter)
* [Table of Contents](#table-of-contents)
* [Introduction](#introduction)
  * [Cypher and Neo4j](#cypher-and-neo4j)
  * [Mapping RDF to Cypher/Neo4j entities: general concepts](#mapping-rdf-to-cypherneo4j-entities-general-concepts)
  * [SPARQL\-based mapping](#sparql-based-mapping)
  * [Spring\-based configuration](#spring-based-configuration)
  * [rdf2neo4 architecture](#rdf2neo4-architecture)
* [Mapping details](#mapping-details)
  * [Node mappings](#node-mappings)
  * [Relation mappings](#relation-mappings)
  * [Spring Configuration](#spring-configuration)
  * [Order of operations](#order-of-operations)
  * [Miscellanea](#miscellanea)


# Introduction

## Cypher and Neo4j

Our converter works entirely via Cypher instructions, i.e., we don't use graph-level APIs to access Neo4j. While we haven't extended our code to support [other graph databases](https://www.opencypher.org/projects) that support [OpenCypher](https://www.opencypher.org/), we would expect this to be easy to do. In the following, we mention mappings from RDF to Cypher, meaning the Cypher running on Neo4j.


## Mapping RDF to Cypher/Neo4j entities: general concepts

The RDF data model and the Cypher data model are rather similar, but there are significant differences:

  * The native entities of Cypher are nodes, relations, node labels, relation types, and properties attached to nodes or relation. 
  * Essentially, in RDF everything is a triple/statement, the equivalents of the above entities are all modelled after triples, even when the granularity on Cypher side is lower (e.g., node/relation properties).
  * Nodes are URI-provided resources that appear as subject or object of triples
  * Statements based on [rdf:type](https://www.w3.org/TR/rdf11-primer/#section-semantics) are the closest thing to the definition of node labels (in Cypher labels are strings, in RDF they are other resources/URIs)
  * A triple (or statement) joining two resources/URIs is the closest thing to a Cypher relation. In that case, another resource/URI is used for the triple predicate, this is similar to stating the relation type. Again, the latter is a string, while a predicate is a URI.
  * An RDF triple having a literal as object ([datatype properties](https://www.w3.org/TR/owl2-primer/#Datatypes) in [OWL](https://www.w3.org/TR/2012/REC-owl2-primer-20121211/)) is roughly equivalent to a node property in Cypher. Again, Cypher property names are strings, triple datatype properties are URIs. There are other significant differences, for instance, string literals in RDF can have a language tag attached, no equivalent exists in Cypher (it can be modelled as a 2-sized array). As another example, a property in Cypher can have an array as value, but the array contents must be homogeneous (i.e., all values must have the same raw type), while in RDF an array is nothing but a [special set of statements](https://www.w3.org/TR/rdf-schema/#ch_othervocab). Moreover, in RDF you can (obviously) have multiple statements associtated to a subject that define multiple values for a property  (e.g., ex:bob schema:name 'Robert', 'Bob'). This can only be emulated in Cypher, typically by merging multiple values into an array (having set semantics).
  * Cypher relations can have property/value pairs attached. In RDF you can only emulate this with constructs like [reified statements](https://www.w3.org/TR/rdf-schema/#ch_reificationvocab), [named graphs](https://www.w3.org/2011/prov/wiki/Using_named_graphs_to_model_Accounts) or [singleton properties](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC4350149/).
  		
So, two similar graph models and a number of differences. How to map one to the other?
 
On a first look, one may think that there is a sort of 'natural mapping' between RDF and Cypher. Rougly: anything having an `rdf:type` will generate a Cypher node with that type used as label (maybe not a whole URI like `http://www.example.com/ontology#Person`, just the last part of it, `Person`), any triple will generate a Cypher relation, maybe any reified statement based on the RDF syntax will be converted into a property-attached relation.

Indeed, [other projects](https://jbarrasa.com/2016/06/07/importing-rdf-data-into-neo4j) before our rdf2neo have adopted this approach. However, hard-wiring the mapping to a particular view of things can be too inflexible, no matter how natural that view is. For instance, in RDF we might have an ontology providing targets for `rdf:type` where all the classes have an `rdfs:label` associated (e.g., `ex:bob rdf:type ex:Person. ex:Person a owl:Class; rdfs:label 'Human Being'.`) and this might be the thing we want to use as the Cypher node label (`(bob:'Human Being')`). As another example, we might be using our own way to define reified relations on the RDF side (e.g., `ex:annotation1 a ex:Annotation; ex:source ex:doc1; ex:target ex:topic1; ex:score '0.9'^xsd:real`) and we may want to turn that schema of ours into Cypher relations (e.g., `(doc1:Document)-[:ANNOTATION{ score: 0.9}]->(topic1:Topic)`), while in the 'natural' mapping those sets of statements would be blindly mapped to Cypher nodes and binary property-less relations, (`(annotation1:Annotation{ score:0.9 })-[:SOURCE]->(doc1:Document), (annotation1)-[:TARGET]->(topic1:Topic)`).


## SPARQL-based mapping

In order to provide the flexibility necessary in the use case above, we have decided another way to map RDF to Cypher: a set of SPARQL queries that return a list of Cypher entities (nodes and their labels, node details like properties, relations, etc) from the initial RDF data.

In the following, we show how to define such queries.

In addition to SPARQL, we use a couple of components to define further configuration details, which cannot be managed via SPARQL-based mapping, or are too difficult to do so. For example, we have a [default URI-to-identifier converter](rdf2neo/src/main/java/uk/ac/rothamsted/rdf/neo4j/idconvert/DefaultIri2IdConverter.java), which converts URIs to short identifier strings, suitable to be used as node labels or relation types (e.g., `http://www.example.com/ontology#Person` => `Person`).  

TODO: we plan to ship our tools with SPARQL mappings for 'natural RDF mapping'.


## Spring-based configuration

SPARQL queries, the target Neo4j database and components like the URI-to-identifier converters are all configurable components in neo4j. You can work out a particular configuration for a given RDF data set, where you put together all these components. A configuration is defined as a [Spring configuration file](https://docs.spring.io/spring/docs/current/spring-framework-reference/core.html), which provides with a powerful language to assemble components together (it plugs in the underlying Java entities, but you don't need to know Java to understand these files).

*Note to developers: because we're using Spring, if you're going to use our [core library](rdf2neo) programmatically, you can additionally/optionally use other Spring configuration means, such as [Java annotations](https://docs.spring.io/spring/docs/current/spring-framework-reference/core.html#beans-java)*      


##  rdf2neo4 architecture

rdf2neo is more precisely a "TDB-to-Neo4j" converter. That is, it takes RDF data from a [Jena](https://jena.apache.org/) [TDB triple store](https://jena.apache.org/documentation/tdb/index.html). Both the [programmatic interface](rdf2neo) and the [command line tool](rdf2neo-cli) start from the path to the input TDB to use to populate a target Neo4j database.

We cannot load data directly from RDF files because we need to view all the data set that you want to convert. For instance, before we can issue a Cypher command to create a node, we need to fetch all of its details about its labels and properties. If these details were spread across different RDF files, we would need to first load all the files and then query them with SPARQL (the way shown below). Which is precisely what we do by using a TDB. The alternative would be an in-memory triple store (i.e., what Jena calls a memory [Model](https://jena.apache.org/tutorials/rdf_api.html), but this might not be good if you have large data sets.

A wrapper that abstracts away from these details, can be found [a script in the command line package](rdf2neo-cli/src/main/assembly/resources/rdf2neo.sh).   

We plan to make it possible to query an HTTP-based SPARQL endpoint in future.


# Mapping details

In this section we are going to show abstracts from the [DBPedia example](rdf2neo/src/test/resources), which maps some example RDF downloaded from [DBPedia](http://wiki.dbpedia.org/about) into Neo4j.

rdf2neo allows you to define multiple configuration sets (named `[ConfigItem](rdf2neo/src/main/java/uk/ac/rothamsted/rdf/neo4j/load/MultiConfigCyLoader.java#L54)` in the Spring configurations). Each has a list of SPARQL mapping queries and possibly other configuration elements about a logical subset of your RDF data. For instance, in the DBPedia example we have a `ConfigItem` for mapping data about places and another to map data about people. While in simple projects you might have just one configuration set, we allow for many because this helps with keeping data subsets separated.


##  Node mappings

RDF data can be mapped to Cypher nodes by means of the following query types. 


### Node URIs

*Note: For sake of precision, the configuration files uses the word 'IRI'. If you don't consider the [technical differences](https://devblast.com/b/url-uri-iri-urn), it can be considered synonym of URI.*  

This is a SPARQL query that lists all the URIs about RDF resources that represent a node. An example:

```sql
#  The node list query must always project a ?iri variable
# (further returned variables are safely ignored, performance is usually better if you don't mention them at all  
SELECT DISTINCT ?iri
WHERE
{
  # This picks up nodes of interests based on their rdf:type, which should be pretty common.
  # Any instance of Person and Employee is considered
  { ?iri a schema:Person }
  UNION { ?iri a schema:Employee }
  
  # Another option is to consider anyone in the domain or range of a property, i.e., you know 
  # that anyone involved in a foaf:knows relation must be a person.
  UNION { ?someone foaf:knows|^foaf:knows ?iri }
}
```

Typically this query will be listing instances of target classes, although you might also catch resources of interest by targeting subjects or objects of given relations.


**Note: it is very important that the query above returns distinct results.**


### Node labels

This query is invoked for each of the URIs found by the node URIs and is parameterised over a single node URIs. It should return all the labels that you want to assign to that node on the Cypher side. For instance,

```sql
#  The node list query must always project a ?label variable and must use the ?iri variable in the WHERE clause. 
# ?iri will be bound to one of IRIs found in the node IRI query. The label query will be invoked once per node IRI,
# its purpose is to list all the Cypher labels that have to be assigned to the node.
#
# A label can be either a IRI or a literal, or a string. If it's a URI, it will be translated into a Cypher 
# identifier by means of the configured IRI-to-ID converter. At the moment we're using the default DefaultIri2IdConverter
# (see the Java sources), which takes the last part of an IRI.
# 
SELECT DISTINCT ?label
WHERE 
{  
  # As said above, ?iri is a constant during the actual execution of this query.
  # When DefaultIri2IdConverter is used, schema:Person will become the label 'Person'.
  { ?iri a ?label }
  
  # We always want this label
  UNION { BIND ( schema:Person AS ?label ) }
}
```


###  Node properties

This works with the same mechanism (one query per node URI, the `?iri` variable bound to a specific URI) and lists all the pairs of property name + value that you want to assign to the node: 

```sql
#  You need to return these two variables. ?iri is bound to a constant, as above.
#
#  - ?name is typically a IRI and is converted into a shorter ID by means of a configured IRI->ID converter
# (no conversion if it's a literal).
#  - ?value is a literal and, for the moment, is converted to simple value types (e.g., string, number), using
# its lexical value. We'll offer more customisation soon (e.g., mapping XSD types to Cypher/Java types).
#
SELECT DISTINCT ?name ?value
{
  ?iri ?name ?value.
  FILTER ( isNumeric (?value) || LANG ( ?value ) = 'en' ). #  Let's consider only these values

  # We're interested in these properties only
  #  Again, these are passed to DefaultIri2IdConverter by default, and so things like 
  # rdfs:label, dbo:areaTotal become 'label', 'areaTotal'   
  VALUES ( ?name ) {
    ( rdfs:label )
    ( rdfs:comment )
    ( foaf:givenName )
    ( foaf:familyName )
  }
}
```

So, if this RDF exists in the input:
 
```java
...
@prefix ex: <http://www.example.com/resources/>

ex:john a schema:Person, schema:Employee;
  foaf:givenName "John";
  foaf:familyName "Smith".
```

The queries above will give the following Cypher node:

```sql
  { iri:"http://www.example.com/resources/john", givenName: 'John', familyName: 'Smith' }: [ `Person`, `Employee`, `Resource` ]
```

As you can see, there are values that are created implicitly: 

  - every node has always an `iri` property. We need this to correctly process the RDF-defined relations (see below) and we think it can be useful to track the provenance URI for a node. This property is always indexed and has distinct values.
  
  - every node has a always a default label. The predefined value fo this is `Resource`, but it can be changed by configuring a String bean `defaultNodeLabel` as ID. Again, we need this in order to find nodes by their IRI (the Cypher construct: `MATCH ( n: { id: $const }:Resource )` is very fast, not so when you try to match the label with `WHERE $myLabel IN LABELS (n)`).
  

**Notes**

  - If values are literals, you should expect reasonable conversions (e.g., RDF numbers => Cypher numbers). TODO: we plan to add a configuration option to define custom literal converters.  
    

##  Relation mappings

Cypher relations between nodes are mapped from RDF in a similar way.

###  List of relations and their types

Similarly to nodes, rdf2neo needs first a list of relations to be created. These must refer to their linking nodes
by means of the node URIs (mapped earlier via the `iri` property). This is an example for the DBPedia people resources:

```sql
#  You must always return a relation IRI, a relation type (IRI or string), the IRIs of the relation source and target.
SELECT DISTINCT ?iri ?type ?fromIri ?toIri
{
  #  Plain relations, non-reified
  ?fromIri ?type ?toIri.

  # We're interested in these predicates only
  VALUES ( ?type ) {
    ( dct:subject )
	 ( dbo:team )
	 ( dbo:birthPlace )
  }
	
	FILTER ( isIRI ( ?toIri ) ).	#  Just in case of problems
	
	#  Fictitious IRI for plain relations. We always need a relation iri on the Cypher end, 
	#  so typically will do this for straight triples 
	BIND ( 
		IRI ( CONCAT ( 
		  STR ( ex: ),
	  	  MD5 ( CONCAT ( STR ( ?type ), STR ( ?fromIri ), STR ( ?toIri ) ) )
	  	))
	  AS ?iri
	)
}
```

As you can see, we need certain properties always reported after the `SELECT` keyword. Among these, we always need the relation URI, which has to be computed for straight (non reified) triples too.

Similarly to nodes, relation URIs (i.e., `?iri`) are needed by rdf2neo in order to check for their properties with the relation property query. Moreover, it is a good way to keep track of multiple statements about the same subject/predicate/property.


### Relation properties

As said above, this is similar to the nodes case. If there are relations with attached properties on the RDF side, these will be defined by means of some RDF graph structure, which puts together multiple triples per relation.

For example, if such relations are reified via the `rdf:` vocabulary: 

```sql
SELECT DISTINCT ?iri ?type ?fromIri ?toIri
WHERE {
  ?iri a rdf:Statement;
    rdf:subject ?fromIri;
    rdf:predicate ?type;
    rdf:object ?toIri.
} 
```

Once rdf2neo receives reified relations, it uses a query like this to select their properties: 
 
```sql
# You must always return these and bind ?iri below
SELECT DISTINCT ?name ?value
WHERE {
  ?iri ?name ?value.
  
  FILTER ( isNumeric (?value) || LANG ( ?value ) = 'en' ). # again, safeguarding code 

  # Again, we're interested in this datatype properties only
  VALUES ( ?name ) {
    ( rdfs:label )
    ( rdfs:comment )
    ( dbo:areaTotal )
    ( dbo:populationTotal )
  }
}
```

As above, `?name` is the property name that will be used for Cypher. If it is a URI, it will be converted by an URI-identifier converter. `?value` is converted to Cypher following the same rules described above.


## Spring Configuration

As mentioned earlier, all of the SPARQL mapping queries above can be configured to be used with a given dataset by means of a [Spring XML beans configuration file](https://docs.spring.io/spring/docs/current/spring-framework-reference/core.html). Here it is an abstract: 

```xml
<beans...>
	...
	
	<!-- 
	  Each ConfigItem is a mapping query set.
	  Every config item is supposed to be associated to a data subset, e.g., one for people, another for places, etc.   	  
	-->
	
	<!-- Queries to map places -->
	<bean class = "uk.ac.rothamsted.rdf.neo4j.load.MultiConfigCyLoader.ConfigItem">		
		<!--
		  This is the name of this config item/subset of data. It is used only for communication purposes,
		  such as log messages.
		-->
		<property name = "name" value = "places" />
		
		<!-- 
		  The query to list nodes. It must be the nodeIrisSparql of this ConfigItem 
		  (i.e., will be passed to ConfigItem.setNodeIrisSparql ( String ) ). 
		-->
		<property name = "nodeIrisSparql">
			<!-- 
			  Use this syntax to read a SPARQ query from a file, it invokes IOUtils.readFile( path ).
			  You need to specify "index = '0'" below, to fix some ambiguity that confuses Spring.  
			-->
			<bean class = "uk.ac.ebi.utils.io.IOUtils" factory-method = "readFile">
			  <!-- pwd is defined above -->
				<constructor-arg value = "#{ pwd + 'mapping/dbpedia_node_iris.sparql' }" index = "0" />
			</bean>
		</property>
		
		<!-- The query mapping node labels -->
		<property name="labelsSparql">
			<bean class = "uk.ac.ebi.utils.io.IOUtils" factory-method = "readFile">
				<constructor-arg value = "#{ pwd + 'mapping/dbpedia_node_labels.sparql' }" index = "0" />
			</bean>
		</property>

		<!-- The query mapping node properties -->
		<property name="nodePropsSparql">
			<bean class = "uk.ac.ebi.utils.io.IOUtils" factory-method = "readFile">
				<constructor-arg value = "#{ pwd + 'mapping/dbpedia_node_props.sparql' }" index = "0" />
			</bean>
		</property>

      <!-- Now the relations and their types -->
		<property name="relationTypesSparql">
			<bean class = "uk.ac.ebi.utils.io.IOUtils" factory-method = "readFile">
				<constructor-arg value = "#{ pwd + 'mapping/dbpedia_rel_types.sparql' }" index = "0" />
			</bean>
		</property>

		<!-- And the relation properties -->
		<property name="relationPropsSparql">
			<bean class = "uk.ac.ebi.utils.io.IOUtils" factory-method = "readFile">
				<constructor-arg value = "#{ pwd + 'mapping/dbpedia_rel_props.sparql' }" index = "0" />
			</bean>
		</property>

		<!-- Query defining which node properties should be Cypher-indexed (see below) -->
		<property name="indexesSparql">
			<bean class = "uk.ac.ebi.utils.io.IOUtils" factory-method = "readResource">
				<constructor-arg value = "dbpedia_node_indexes.sparql" />
			</bean>
		</property>
	</bean>
	...
</beans>
```


## Order of operations

You might need to be aware of the order in which rdf2neo runs its operations. When you invoke either the [command line](rdf2neo-cli) or its [programmatic equivalent](rdf2neo/src/main/java/uk/ac/rothamsted/rdf/neo4j/load/MultiConfigCyLoader.java) a procedure is run that can be summarised as:

  1. Node loop. For each `ConfigItem`:
    1. Run the node list query. Split the resulting URIs into subsets of a given size and for each subset run this as a thread:
      1. Run the node labels query
      1. Run the node properties query
      1. Prepare a Cypher statement that creates the node and queue it
      1. Commit all Cypher `CREATE` statements against the configured Neo4j
  1. Relation loop. For each `ConfigItem`:
    1. Run the relation list/type query. Split the results as above and run threads that:
      1. Run the relation properties query. Prepare a Cypher statement that creates a new relation and refers to existing nodes via their `iri` property
      1. Commit all relation-creation the statements at the end
      
So, even if nodes are mapped across multiple configurations, they are all created in Cypher before any relation is considered. This allows us to issue relation creation statements that don't need to check if a relation already exists (it doesn't), or if a node already exists during the first stage (it doesn't) or during the relation creation (it does).

Moreover, chunks of nodes and properties are mapped and submitted to Cypher in parallel, to speed up things. This is influenced by the `Long` property named `destinationMaxSize` (which is passed to instances of `[CyLoadingProcessor](rdf2neo/src/main/java/uk/ac/rothamsted/rdf/neo4j/load/support/CyLoadingProcessor.java)`, a suitable default is defined for it).


##  Miscellanea

The Spring configuration is a complex system, which depends on the way [Spring itself works](https://docs.spring.io/spring/docs/current/spring-framework-reference/core.html) and the [Java code available for running rdf2neo](rdf2neo/src/main/java/uk/ac/rothamsted/rdf/neo4j/load).  

In addition to the details mentioned in this section, see the examples in the [core package](rdf2neo/rdf2neo/src/test/resources) and in the [command line package](rdf2neo-cli/src/main/assembly/resources) for details.


### Neo4j connection

Every configuration is supposed to be used with a given Neo4j target instance, which should be configured in Spring. See the examples for details. Note also that you might achieve more modularity by using mechanisms like [Spring imports](https://docs.spring.io/spring/docs/current/spring-framework-reference/core.html#beans-factory-xml-import) or combination of Spring XML configuration and Java property files (see [here](https://docs.spring.io/spring/docs/current/spring-framework-reference/core.html#beans-java-combining)).


### Neo4j pre-conditions

We expect an empty database when rdf2neo is run, or at least a database where the nodes and relations you are going to import from RDF are not already there (e.g., no node with same URIs, or no node with the same labels).

You can do cleanup or pre/post processing operations against Neo4j by invoking the [Neo4j Cypher Shell](https://neo4j.com/docs/operations-manual/current/tools/cypher-shell/) in scripts of yours (which will invoke rdf2neo too). 


###  Cypher Indexes

By default, the only Cypher node property that rdf2neo [indexes](https://neo4j.com/docs/developer-manual/current/cypher/schema/index) is `iri`. You should decide which other properties should be indexed in your application, in order to optimise performance. Cypher indexes can be configured inside the `ConfigItem`, the way shown above. 
[Here](rdf2neo/src/test/resources/dbpedia_node_indexes.sparql) you can find an example of the SPARQL query to be used to define the indexes.   

*Limitations: we don't support the configuration of multi-property indexes. If you need that, you can send Cypher commands to Neo4j using the [Neo4j Cypher Shell](https://neo4j.com/docs/operations-manual/current/tools/cypher-shell/)*

### default label

As mentioned earlier, every Cypher node created by rdf2neo has at least a default label, to which further labels can be added. The default can be changed by means of the property `defaultNodeLabel`: 

```xml
<beans...>
  ...
	<bean id = "defaultNodeLabel" class = "java.lang.String">
		<constructor-arg value = "ItemNode" />		
	</bean>
	...
</beans>
```

### ID Converters

As explained above, node labels, relation types and node/relation property names can be converted from URIs or literals. We have a simple [default converter](rdf2neo/src/main/java/uk/ac/rothamsted/rdf/neo4j/idconvert/DefaultIri2IdConverter.java) and we are working on more options. You can even define your own Java-based converter and configure it via Spring, by using the beans named `nodeLabelIdConverter`, `relationIdConverter`, `propertyIdConverter`. For instance:

```xml
<beans...>
   <!-- 
     Must be a class implementing java.util.function.Function<String, String>, which takes a IRI and returns an identifier.
     If your class needs config values, either in the constructor or via bean setters, you can use the usual Spring syntax to pass them.  
   --> 
	<bean id = "nodeLabelIdConverter" class = "my.package.MyCustomIdConverter" />
</beans>
```

