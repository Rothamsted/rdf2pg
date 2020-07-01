* Continue from MultiPGLoader
* its getSimpleLoader factory needs a factory, which needs to be initialised
  with a generated class. 
* To be decided: where is the class set? In the MPGL?! Probably yes, and this
  requires that MPGL becomes abstract.
* MPGL needs a method to load config items and orchestrate base exec over them.
