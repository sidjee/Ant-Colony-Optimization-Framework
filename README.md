# Ant-Colony-Optimization-Framework

## About ACO 
Ant Colony Optimization is a member of the ant colony algorithms family, in swarm intelligence methods, 
and it constitutes some metaheuristic optimizations. The original algorithm was aiming to search 
for an optimal path in a graph, based on the behavior of ants seeking a path between their colony 
and a source of food.

## Used in Cloud Task Scheduling
This framework aims at optimizing the tasks submitted to a number of Virtual Machines.
The simulator to be used is Cloudsim by Melbourne Clouds Lab.

## How to use:
1. Clone the Repository
2. Compress src/LBACO.java into jar file and add it in cloudsim/jars directory <cloudsim is the installation directory of Cloudsim>
3. Adjust the DatacenterBroker.java file in cloudsim sources. See sample/DatacenterBroker.java
```
  // For Load Balancing ACO
  LBACO lbaco1 = new LBACO(<no of ants>,<Q>,<alpha>,<beta>,<gamma>,<rho>);
  Map<Integer,Integer> allocated = lbaco1.implement(<cloudletList>,<vm_list>,<No of iterations, Let->100>);
  
  // For usual ACO
  ACOImplement aco1 = new ACOImplement(<no of ants>,<initialPheromonevalue>,<Q>,<alpha>,<beta>,<rho>);
  Map<Integer,Integer> allocatedTasks = aco1.allocateTasks(<CloudletList>,<VMList>,<Max Iterations to be performed>);
```
4. The hashmap `allocatedTasks` will represent the cloudlets mapped to VMs.
