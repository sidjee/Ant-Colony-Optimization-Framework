package org.cloudbus.cloudsim;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.*;
import java.lang.*;
import java.io.*;

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
	protected double Q;
	protected double alpha;
	protected double beta;
	protected double gamma;
	protected double rho;
	protected int m;
	protected Random r;


	public Map<Integer,Integer> implement(List<Cloudlet> taskList,List<Vm> vmList,int tmax) throws FileNotFoundException{
		int tasks = taskList.size();
		int vms = vmList.size();
		Map<Integer,Integer> allocatedtasks = new HashMap<>();
		Map<Integer, Map<Integer,Double> > execTimes;
		Map<Integer,Double> cc, pheromones;
				
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
					probab.put(i,probab.get(i)/total);
				}

				int []votes = new int[vms];
				for(int k=0;k<m;k++){
					double max = 0;

					int vmIndexChosen = vote(vms,probab);
					votes[vmIndexChosen]++;
				}

				int max_votes = 0;
				int opt_vm = 0;
				for(int i=0;i<vms;i++){
					if(max_votes<votes[i]){
						max_votes = votes[i];
						allocatedtasks.put(task,i);
						opt_vm = i;
					}
				}

				eet.put(opt_vm,eet.get(opt_vm)+execTimes.get(task).get(opt_vm));
				pheromones.put(opt_vm,pheromones.get(opt_vm)*(1-rho)+Q/execTimes.get(task).get(opt_vm));
			}

		}
		return allocatedtasks;
	}

	protected int vote(int vms, Map<Integer,Double> probab){
		int []freq = new int[vms];
		int sum = 0;
		
		for(int i=0;i<vms;i++){
			freq[i] = (int)(probab.get(i)*100000.0);
			sum += freq[i];
		}

		int n = 1 + r.nextInt(sum);
		if(n <= freq[0]){
			return 0;
		}
		
		for(int i=0;i<vms-1;i++){
			freq[i+1] += freq[i];
			if(n>freq[i] && n<= freq[i+1]){
				return i+1;
			}
		}
		return 0;
	}

	public LBACO(int m, double Q, double alpha, double beta, double gamma, double rho){
		this.m = m;
		this.Q = Q;
		this.alpha = alpha;
		this.beta = beta;
		this.gamma = gamma;
		this.rho = rho;
		r = new Random();
	}


	protected Map<Integer,Double> initializePheromone(Map<Integer,Double> cc){
		Map<Integer, Double> pheromones = new HashMap<>();
		
		for (int j=0; j<cc.size() ; j++) {
			pheromones.put(j,cc.get(j));
		}
		
		return pheromones;
	}


	protected double getExecutionTime(Vm VM, Cloudlet cloudlet){
		return (cloudlet.getCloudletLength()/(VM.getNumberOfPes()*VM.getMips()));
	}
}
