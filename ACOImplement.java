import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

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
	protected Map<int, Map<int,double> > pheromones;
	protected double Q;
	protected double alpha;
	protected double beta;
	protected double rho;
	protected int m;

	protected void implement(taskList,vmList,tmax){
		int tasks = taskList.size();
		int vms = vmList.size();
		List<int> newVmList = new ArrayList<>();
		newVmList.addAll(Range.range(vms));
		// Map<char,int> []edges = new HashMap<char,int>()[tasks];
		List<double> lengths = new ArrayList<>();
		Map<int,int> []tabu;
		Map<int, Map<int,double> > execTimes;
		execTimes = new Hashmap<>();

		for(int i=0;i<tasks;i++){
			Map<int,double> x = new Hashmap<>();
			for (int j=0; j<vms ; j++) {
				double t = getExecutionTime(vmList[j],taskList[i]);
				x.put(j,t);
			}
			execTimes.put(i,x);
		}
		
		Map<int, Map<int,double> > pheromones = initializePheromone(tasks,vms);

		for(int t=1;t<=tmax;t++){
			tabu = new HashMap<>()[vms];

			Collections.shuffle(newVmList);

			for(int k=0;k<m;k++){
				tabu[k].put(-1,newVmList[k]);
				double max = 0;

				for(int task=0;task<tasks;task++){
					int vmIndexChosen = chooseVM(taskList[task],vmList,tabu[k],execTimes,pheromones);
					tabu[k].put(task,vmIndexChosen);
					double time = execTimes.get(task).get(vmIndexChosen);
					max = (max<time)?time:max;
				}

				lengths[k]=max;
			}

			double min = lengths[0];
			int kmin = 0;

			for(int k=1;k<m;k++){
				min = (min>lengths[k])?lengths[k]:min;
				kmin = (min>lengths[k])?k:kmin;
			}

			updatePheromones(pheromones,lengths,tabu);
			globalUpdatePheromones(pheromones,min,tabu[kmin]);
		}
	}

	public ACOImplement(int m, int initialPheromone, int Q, int alpha, int beta, int rho){
		this.initialPheromone = initialPheromone;
		this.Q = Q;
		this.alpha = alpha;
		this.beta = beta;
		this.rho = rho;
	}

	protected Map<int, Map<int,double> > initializePheromone(int tasks, int vms){
		pheromones = new HashMap<>();
		for(int i=0;i<tasks;i++){
			Map<int,double> x = new Hashmap<>();
			for (int j=0; j<vms ; j++) {
				x.put(j,initialPheromone);
			}
			pheromones.put(i,x);
		}
		return pheromones;
	}

	protected updatePheromones(Map<int, Map<int,double> > pheromones, List<double> length, Map<int,int> []tabu){
		double updateValue = Q/length;
		Map<int, Map<int,double> > updatep;
		for(int k=0;k<tabu.size();k++)
			for(int i=0;i<tabu[k].size();i++){
				Map<int,double> v;
				v.put(tabu[k].get(i), updateValue);
				updatep.put(i,v);
			}
		for(int i=0;i<tasks;i++){
			Map<int,double> x = pheromones.get(i);
			for (int j=0; j<vms ; j++) {
				x.put(j,(1-rho)*x.get(j)+updatep.get(i).get(j));
			}
			pheromones.put(i,x);
		}
	}

	protected globalUpdatePheromones(Map<int, Map<int,double> > pheromones, double length, Map<int,int> tabu){
		double updateValue = Q/length;
		for(int i=0;i<tabu.size();i++){
			Map<int,double> v = pheromones.get(i);
			v.put(tabu.get(i),v.get(tabu.get(i))+updateValue);
			pheromones.put(i,v);
		}
	}

	protected double getExecutionTime(Vm VM, Cloudlet cloudlet){
		return (cloudlet.getCloudletLength()/(VM.getPes()*VM.getMips()) + cloudlet.getCloudletFileSize()/VM.getBW());
	}
}
