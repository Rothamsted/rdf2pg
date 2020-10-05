* Review all
* Split generic/Neo4j/graphML into Maven modules
* Rerun tests for Neo4j
* Write tests for GraphML
* Review the CLI


* (DONE) its getSimpleLoader factory needs a factory, which needs to be initialised
  with a generated class. 
* (DONE) To be decided: where is the class set? In the MPGL?! Probably yes, and this
  requires that MPGL becomes abstract.
* (DONE) MPGL needs a method to load config items and orchestrate base exec over them.
  * Started
  * We need to move the old hardwired code about GML export to its own MultiConfigPGLoader
  * The output file used by that code is taken from the configuration. Obviously that's wrong, we
  need to pass the file as parameter and also to have the OutputStream option.
* (DONE) `GraphMLNodeExportHandler` changes
	* `gatheredNodeProperties` should not be static. It's not coherent with the rest, the architecture 
	is such that handlers and simple loaders have the lifespan of a loading session's lifespan, and the rest of Spring 
	components have the lifespan of a Spring context lifespan. Static stuff are bad because, for instance, one cannot 
	run two GraphML converters of different RDF datasets in the same JVM. For example, this is something that could happen
	in a web application that would serve conversion requests from web users. A library like this must be as much generic
	* So, my idea is to move these gathered properties to GraphMLDataManager (see below), which will have Spring context as 
	scope/lifespan (ie, will be singleton). 
	* Moreover, since both the node and the edge handlers have these in common, we should factorise them on their common 
	abstract parent
	* The first part of the handler (in `accept()`) prepares abstract PG entities, according to a common organisation
	(eg, per-label grouping, node properties collected from RDF).
	  * Is this needed?! It seems that the only thing GraphML requires is writing nodes, this complicated label-grouping
	  was necessary in Neo4j due to Cypher syntax limitations 
	  * If needed, this code DOES NOT depend on the target PG, in fact it's
	just the same code we have on the Neo4j side => WE NEED TO factorise it on a generic abstract handler.
	* The same happens for the relation handler.
	* Multiple labels are collected, joined in a string and used as node labelV attribute. Is it correct in GraphML?
* (DONE, moved to data manager)`defaultNodeLabel` in `GraphMLConfiguration`:
  * is it needed? It doesn't seem the case 
  * If yes, Neo4j needs too, maybe it's worth to factorise. Moreover, let's move this
to a new component, `GraphMLDataManager`, which will have a role similar to Neo4DataManager, ie, managing the data writing
on the target. This class will deal with details like this default label and gathered node/rel properties mentioned above
(it's a target PG detail, so it makes sense to have this here). This will also be a Spring component with singleton scope
(not loading session, since it has re-used stuff).

 