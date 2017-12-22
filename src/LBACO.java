package org.cloudbus.cloudsim;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.*;
import java.lang.*;
// import java.util.Calendar;
// import java.util.LinkedList;
// import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;


public class LBACO{
	// protected double initialPheromone;
	protected double Q;
	protected double alpha;
	protected double beta;
	protected double gamma;
	protected double rho;
	protected int m;

	// public Map<Integer,Integer> allocateTasks(List<Cloudlet> taskList,List<Vm> vmList,int tmax){
	// 	int n = vmList.size();
	// 	Map<Integer,Integer> allocatedtasks = new HashMap<>();
		
	// 	// for(int i=0;i<(int)taskList.size()/(n-1);i++){
	// 	// 	Map<Integer,Integer> at = implement(taskList.subList(i*(n-1),(i+1)*(n-1)),vmList,tmax);
	// 	// 	for(int j=0;j<at.size();j++){
	// 	// 		allocatedtasks.put(j+i*(n-1),at.get(j));
	// 	// 	}
	// 	// }
		
	// 	Map<Integer,Integer> at = implement(taskList,vmList,tmax);
		
	// 	// allocatedtasks.putAll(),
	// 		// vmList,tmax));
	// 	// for(int j=0;j<at.size();j++){
	// 	// 	allocatedtasks.put(j+(taskList.size()/(n-1))*(n-1),at.get(j));
	// 	// }
	// 	// return allocatedtasks;
	// 	return at;
	// }

	public Map<Integer,Integer> implement(List<Cloudlet> taskList,List<Vm> vmList,int tmax){
		int tasks = taskList.size();
		int vms = vmList.size();
		// List<Double> lengths = new ArrayList<>();
		List<Integer> allocatedtasks = new ArrayList<>();
		Map<Integer, Map<Integer,Double> > execTimes;
		Map<Integer,Double> cc, pheromones;
		
		// List<Integer> newVmList = IntStream.range(0,vms).boxed().collect(Collectors.toList());
		// Map<char,int> []edges = new HashMap<char,int>()[tasks];
		
		execTimes = new HashMap<>();
		cc = new HashMap<>();

		for(int i=0;i<tasks;i++){
			Map<Integer,Double> x = new HashMap<>();
			for (int j=0; j<vms ; j++) {
				double t = getExecutionTime(vmList.get(j),taskList.get(i));
				x.put(j,t);
			}
			execTimes.put(i,x);
		}
		
		for(int i=0;i<vms;i++){
			Vm vm = vmList.get(i);
			double Cc = vm.getNumberOfPes()*vm.getMips() + vm.getBw();
			cc.put(i,Cc);
		}

		pheromones = initializePheromone(cc);

		// int kmin=0;
		for(int t=1;t<=tmax;t++){
			Map<Integer,Double> eet = new HashMap<>();
			
			for(int i=0;i<vms;i++)
				eet.put(i,0.0);

			for(int task=0;task<tasks;task++){
				
				Map<Integer,Double> probab = new HashMap<>();
				Map<Integer,Double> eetTemp = new HashMap<>();
				Map<Integer,Double> lbfValues = new HashMap<>();
				for(int i=0;i<vms;i++)
					eetTemp.put(i,eet.get(i)+execTimes.get(task).get(i));

				double total = 0;
				for (int i=0; i<vms; i++) {
					total += eetTemp.get(i);
				}
				for(int i=0; i<vms; i++){
					lbfValues.put(i,total/eetTemp.get(i));
				}
				
				total = 0;
				for(int i=0; i<vms; i++){
					double p = Math.pow(pheromones.get(i),alpha)*
					Math.pow(cc.get(i),beta)*Math.pow(lbfValues.get(i),gamma);
					
					probab.put(i,p);
					total += p;
				}
				for(int i=0; i<vms; i++){
					p.put(i,probab.get(i)/total);
				}

				int []votes = new int[vms];
				for(int k=0;k<m;k++){
				// tabu.add(k,new HashMap<Integer,Integer>());
				// tabu.get(k).put(-1,newVmList.get(k));
					double max = 0;

					int vmIndexChosen = vote(probab);
					// tabu.get(k).put(task,vmIndexChosen);
					votes[vmIndexChosen]++;
					// double time = execTimes.get(task).get(vmIndexChosen);
					// max = (max<time)?time:max;
				}

				// lengths.add(k,max);
				int max_votes = 0;
				int opt_vm = 0;
				for(int i=0;i<vms;i++){
					if(max_votes<votes[i]){
						max_votes = votes[i];
						allocatedtasks.add(task,i);
						opt_vm = i;
					}
				}
				eet.put(opt_vm,eet.get(opt_vm)+execTimes.get(task).get(opt_vm));
				pheromones.put(opt_vm,pheromones.get(opt_vm)*(1-rho)+Q/execTimes.get(task).get(opt_vm));
			}

			// double min = lengths.get(0);
			// kmin = 0;

			// for(int k=1;k<m;k++){
			// 	min = (min>lengths.get(k))?lengths.get(k):min;
			// 	kmin = (min>lengths.get(k))?k:kmin;
			// }

			// updatePheromones(pheromones,lengths,tabu);
			// globalUpdatePheromones(pheromones,min,tabu.get(kmin));
		}
		return allocatedtasks;
	}

	protected int vote(int vms, Map<Integer,Double> probab){
		int []freq = new int[vms];
		int sum = 0;
		
		for(int i=0;i<vms;i++){
			freq[i] = probab*100000;
			sum += probab[i];
		}

		Random r = new Random();
		int n = 1 + r.nextInt(total);
		for(int i=0;i<vms-1;i++){
			if(n>=freq[i] && n<= freq[i+1])
				return i;
		}
		return 0;
	}

	public LBACO(int m, double Q, double alpha, double beta, double gamma, double rho){
		this.m = m;
		// this.initialPheromone = initialPheromone;
		this.Q = Q;
		this.alpha = alpha;
		this.beta = beta;
		this.gamma = gamma;
		this.rho = rho;
	}

	// protected int 
	// chooseVM(Map<Integer,Double> execTimes, Map<Integer,Double> pheromones, Map<Integer,Integer> tabu){
		
	// 	Map<Integer,Double> probab = new HashMap<>();
	// 	double denominator = 0;
		
	// 	for(int i=0;i<pheromones.size();i++){
	// 		if(!tabu.containsValue(i)){
	// 			double exec = execTimes.get(i), pher = pheromones.get(i);
	// 			double p = Math.pow(1/exec,beta)*Math.pow(pher,alpha);
	// 			probab.put(i,p);
	// 			denominator+=p;
	// 		}
	// 		else
	// 			probab.put(i,0.0);
	// 	}
		
	// 	double max = 0;
	// 	int maxvm = -1;
		
	// 	for(int i=0;i<pheromones.size();i++){
	// 		double p = probab.get(i)/denominator;
	// 		if(max<p){
	// 			max = p;
	// 			maxvm = i;
	// 		}
	// 	}
	// 	return maxvm;
	// }

	protected Map<Integer,Double> initializePheromone(Map<Integer,Double> cc){
		Map<Integer, Double> pheromones = new HashMap<>();
		
		for (int j=0; j<vms ; j++) {
			pheromones.put(i,cc.get(j));
		}
		
		return pheromones;
	}

	// protected void updatePheromones(Map<Integer, Map<Integer,Double> > pheromones, List<Double> length, 
	// 	List<Map<Integer,Integer>> tabu){
	// 	Map<Integer, Map<Integer,Double> > updatep = new HashMap<>();

	// 	for(int i=0;i<pheromones.size();i++){
	// 		Map<Integer,Double> v = new HashMap<>();
	// 		for(int j=0;j<pheromones.get(i).size();j++){
	// 			v.put(j,0.0);
	// 		}
	// 		updatep.put(i,v);
	// 	}

	// 	for(int k=0;k<tabu.size();k++){
	// 		double updateValue = Q/length.get(k);
	// 		Map<Integer,Integer> tour = new HashMap<>();
	// 		tour.putAll(tabu.get(k));
	// 		tour.remove(-1);
	// 		// for(int i=0;i<tabu.get(k).size()-1;i++){
	// 		// 	Map<Integer,Double> v = new HashMap<>();
	// 		// 	v.put(tabu.get(k).get(i), updateValue);
	// 		// 	updatep.put(i,v);
	// 		// }
	// 		for(int i=0;i<pheromones.size();i++){
	// 			Map<Integer,Double> v = new HashMap<>();
	// 			for(int j=0;j<pheromones.get(i).size();j++){
	// 				if(tour.containsValue(j)){
	// 					v.put(j,updatep.get(i).get(j)+updateValue);
	// 				}
	// 				else
	// 					v.put(j,updatep.get(i).get(j));
	// 			}
	// 			updatep.put(i,v);
	// 		}
	// 	}
	// 	for(int i=0;i<pheromones.size();i++){
	// 		Map<Integer,Double> x = pheromones.get(i);
		
	// 		for (int j=0; j<pheromones.get(i).size() ; j++) {
	// 			x.put(j,(1-rho)*x.get(j)+updatep.get(i).get(j));
	// 		}
	// 		pheromones.put(i,x);
	// 	}
	// }

	// protected void globalUpdatePheromones(Map<Integer, Map<Integer,Double> > pheromones, double length, Map<Integer,Integer> tabu){
	// 	double updateValue = Q/length;
	// 	for(int i=0;i<tabu.size()-1;i++){
	// 		Map<Integer,Double> v = pheromones.get(i);
	// 		v.put(tabu.get(i),v.get(tabu.get(i))+updateValue);
	// 		pheromones.put(i,v);
	// 	}
	// }

	protected double getExecutionTime(Vm VM, Cloudlet cloudlet){
		return (cloudlet.getCloudletLength()/(VM.getNumberOfPes()*VM.getMips()));
			// + cloudlet.getCloudletFileSize()/VM.getBw());
	}
}
