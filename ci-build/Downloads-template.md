[WARNING]: # (Downloads.md is auto-generated from Downloads_template.md. DO NOT CHANGE the former!)

# rdf2pg Tools Donwloads

## rdf2neo

* [Latest Stable Release](%rdf2neoRel%)
* [Latest Dev Release](%rdf2neoSnap%)

## rdf2graphml

* [Latest Stable Release](%rdf2graphmlRel%)
* [Latest Dev Release](%rdf2graphmlSnap%)

## Including rdf2pg tools in your project

We use Maven and we have our own repository for Maven artifacts. So, to use our tools 
programmatically you can point to them in your POM this way:

```xml
<pom>
...
	<!-- For rdf2neo -->
	<dependency>
		<groupId>uk.ac.rothamsted.kg</groupId>
		<artifactId>rdf2neo</artifactId>
		<version>SEE POM AT https://github.com/Rothamsted/rdf2pg/blob/master/rdf2neo/pom.xml</version>
	</dependency>

	<!-- For rdf2graphml -->
	<dependency>
		<groupId>uk.ac.rothamsted.kg</groupId>
		<artifactId>rdf2graphml</artifactId>
		<version>SEE POM AT https://github.com/Rothamsted/rdf2pg/blob/master/rdf2graphml/pom.xml</version>
	</dependency>
...
	<repositories>
	  <repository>
	  	<!-- Where the artifacts above are -->
			<id>knetminer-repo</id>
			<name>Knetminer Unified Repository</name>
			<url>https://knetminer.com/artifactory/repository/maven-public</url>
			<snapshots><enabled>true</enabled></snapshots>
			<releases><enabled>true</enabled></releases>
			  </repository>
	</repositories>	
```

The entry point to start a conversion (like the command line tools do) is 
[`MultiConfigPGMaker#getSpringInstance()`](TODO). See [here][10] for details on the framework
architecture.

[10]: https://github.com/Rothamsted/rdf2pg/wiki/rdf2pg-Architecture
