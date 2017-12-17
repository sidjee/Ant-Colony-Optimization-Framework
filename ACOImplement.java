
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


public class ACOImplement{
	protected double initialPheromone;
	protected Map<Integer, Map<Integer,Double> > pheromones;
	protected double Q;
	protected double alpha;
	protected double beta;
	protected double rho;
	protected int m;

	public Map<Integer,Integer> implement(List<Cloudlet> taskList,List<Vm> vmList,int tmax){
		int tasks = taskList.size();
		int vms = vmList.size();
		List<Integer> newVmList = IntStream.range(0,vms).boxed().collect(Collectors.toList());
		// Map<char,int> []edges = new HashMap<char,int>()[tasks];
		List<Double> lengths = new ArrayList<>();
		List<Map<Integer,Integer>> tabu = new ArrayList<>();
		Map<Integer, Map<Integer,Double> > execTimes;
		execTimes = new HashMap<>();

		for(int i=0;i<tasks;i++){
			Map<Integer,Double> x = new HashMap<>();
			for (int j=0; j<vms ; j++) {
				double t = getExecutionTime(vmList.get(j),taskList.get(i));
				x.put(j,t);
			}
			execTimes.put(i,x);
		}
		
		Map<Integer, Map<Integer,Double> > pheromones = initializePheromone(tasks,vms);

		for(int t=1;t<=tmax;t++){
			tabu = new ArrayList<>();

			Collections.shuffle(newVmList);

			for(int k=0;k<m;k++){
				tabu.add(k,new HashMap<Integer,Integer>());
				tabu.get(k).put(-1,newVmList.get(k));
				double max = 0;

				for(int task=0;task<tasks;task++){
					int vmIndexChosen = chooseVM(taskList.get(task),vmList,tabu.get(k),execTimes,pheromones);
					tabu.get(k).put(task,vmIndexChosen);
					double time = execTimes.get(task).get(vmIndexChosen);
					max = (max<time)?time:max;
				}

				lengths.get(k)=max;
			}

			double min = lengths.get(0);
			int kmin = 0;

			for(int k=1;k<m;k++){
				min = (min>lengths.get(k))?lengths.get(k):min;
				kmin = (min>lengths.get(k))?k:kmin;
			}

			updatePheromones(pheromones,lengths,tabu);
			globalUpdatePheromones(pheromones,min,tabu.get(kmin));
		}
	}

	public ACOImplement(int m, int initialPheromone, int Q, int alpha, int beta, int rho){
		this.initialPheromone = initialPheromone;
		this.Q = Q;
		this.alpha = alpha;
		this.beta = beta;
		this.rho = rho;
	}

	protected Map<Integer, Map<Integer,Double> > initializePheromone(int tasks, int vms){
		pheromones = new HashMap<>();
		for(int i=0;i<tasks;i++){
			Map<Integer,Double> x = new HashMap<>();
			for (int j=0; j<vms ; j++) {
				x.put(j,initialPheromone);
			}
			pheromones.put(i,x);
		}
		return pheromones;
	}

	protected void updatePheromones(Map<Integer, Map<Integer,Double> > pheromones, List<Double> length, List<Map<Integer,Integer>> tabu){
		double updateValue = Q/length;
		Map<Integer, Map<Integer,Double> > updatep;
		for(int k=0;k<tabu.size();k++)
			for(int i=0;i<tabu.get(k).size();i++){
				Map<Integer,Double> v;
				v.put(tabu.get(k).get(i), updateValue);
				updatep.put(i,v);
			}
		for(int i=0;i<tasks;i++){
			Map<Integer,Double> x = pheromones.get(i);
			for (int j=0; j<vms ; j++) {
				x.put(j,(1-rho)*x.get(j)+updatep.get(i).get(j));
			}
			pheromones.put(i,x);
		}
	}

	protected void globalUpdatePheromones(Map<Integer, Map<Integer,Double> > pheromones, double length, Map<Integer,Integer> tabu){
		double updateValue = Q/length;
		for(int i=0;i<tabu.size();i++){
			Map<Integer,Double> v = pheromones.get(i);
			v.put(tabu.get(i),v.get(tabu.get(i))+updateValue);
			pheromones.put(i,v);
		}
	}

	protected double getExecutionTime(Vm VM, Cloudlet cloudlet){
		return (cloudlet.getCloudletLength()/(VM.getPes()*VM.getMips()) + cloudlet.getCloudletFileSize()/VM.getBW());
	}
}
