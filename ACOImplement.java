public class ACOImplement{
	protected double initialPheromone;
	protected Map<int, Map<int,double> > pheromones;
	protected double Q;
	protected double alpha;
	protected double beta;
	protected double rho;


	protected void implement(taskList,vmList,tmax){
		int tasks = taskList.size();
		int vms = vmList.size();
		// Map<char,int> []edges = new HashMap<char,int>()[tasks];
		Map<int,int> []tabu = new HashMap<>()[vms];

		initializePheromone(tasks,vms);

		for(int t=1;t<=tmax;t++){
			for(int k=0;k<vms;k++){
				tabu[k].put(-1,vmList[k].getID());
				for(int task=0;task<tasks;task++){
					int vmIDchosen = chooseVM(taskList[task].getID,vmList,tabu[k]);
					tabu[k].put(task,vmIDchosen);
				}
			}
		}
	}

	public ACOImplement(int initialPheromone,int Q,int alpha, int beta, int rho){
		this.initialPheromone = initialPheromone;
		this.Q = Q;
		this.alpha = alpha;
		this.beta = beta;
		this.rho = rho;
	}

	protected void initializePheromone(int tasks, int vms){
		pheromones = new HashMap<>();
		for(int i=0;i<tasks;i++){
			Map<int,double> x = new Hashmap<>();
			for (int j=0; j<vms ; j++) {
				x.put(j,initialPheromone);
			}
			pheromones.put(i,x);
		}
	}
	protected double getExecutionTime(Vm VM, Cloudlet cloudlet){
		return (cloudlet.getCloudletLength()/(VM.getPes()*VM.getMips()) + cloudlet.getCloudletFileSize()/VM.getBW());
	}
}