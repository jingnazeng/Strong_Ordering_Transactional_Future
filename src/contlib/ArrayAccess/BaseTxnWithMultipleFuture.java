
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import jvstm.ParallelTask;
import jvstm.Transaction;
import jvstm.VBox;

public class BaseTxnWithMultipleFuture implements ArrayAccessTransaction {
	private int num_of_read = 1;
	private int num_of_write = 1;
	private int array_length = 0;
	private int sibling = 0;
	private String high_contention = "false";
	private int cpu_work_amount_between_memory_read = 0;
	VBox<Integer>[] array;
	private int value;
	
	public BaseTxnWithMultipleFuture(int read, int write, String high_contention, int amount){
		this.num_of_read = read;
		this.num_of_write = write;
		this.high_contention = high_contention;
		this.cpu_work_amount_between_memory_read = amount;
	}

	public int executeTransaction(int sibling) throws Throwable {
		
		this.sibling = sibling;
		array = ArrayAccess.array;
//		array_length = array.size();
		array_length = array.length;
		sibling = sibling-1;

		List<Future<Integer>> results = new ArrayList<Future<Integer>>();
		
		
		for(int i = 1 ; i < (sibling+1); i++){

			List<ParallelTask<Integer>> tasks = new ArrayList<ParallelTask<Integer>>();
			tasks.add(new NestedWorker((new AsynchronousOperation(num_of_read, num_of_write))));
//			Transaction.current().manageNestedParallelTxs(tasks);
		}
		AsynchronousOperation continuation =  new AsynchronousOperation(num_of_read,num_of_write);

		try {
			value = continuation.call();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		for(Future<Integer> e: results){
			while(!e.isDone()){}
			value = e.get();
		}
	
		return value;
	}

	private int read(int min, int max) {
		for(int i = min;i < max;i ++){
			//read N items at random in the array, on whose basis (sum/avg/std_dev of the values read) we change M items at random in the array

			int index = (int)(Math.random()*(array_length));

			//before accessing memory, assign some CPU computation work
			if(cpu_work_amount_between_memory_read!=0){
				double sqrt_amount = 0;
				for(int j = 0; j < cpu_work_amount_between_memory_read; j++){
					sqrt_amount += Math.sqrt(j+i);
				}
				index = (int)sqrt_amount % array_length;

				//System.out.println("index: "+index);
			}
			//				array.get(index).get();
			value += array[index].get();
		}
		return value;
	}


	private void writeSequentially(int num_of_write, int value) {

		int index =0;
		for(int i=0; i<num_of_write;i++){
			if(high_contention.equalsIgnoreCase("false")){
				index = (int)(Math.random()*(array_length));
			}
			else if(high_contention.equals("true")){
//				System.out.println("in high contention");
				index = (int)(Math.random()*num_of_write);
			}
			else if(high_contention.equalsIgnoreCase("middle")){
				index = (int)(Math.random()*(num_of_write*10));
			}
			
			array[index].put((int)(Math.random()*1000)+value);
//			System.out.println("write to index: "+ index);
//			int j = array[index].get();
//			if(j>500){
//				array[index].put(j-(int)(Math.random()*1000));
//			}
//			else{
//				array[index].put(j+(int)(Math.random()*1000));
//			}
		}
	}
	
	private class AsynchronousOperation implements Callable<Integer>{
		private int number_of_read;
		private int num_of_write;
		public AsynchronousOperation(int read, int write){
			this.number_of_read = read;
			this.num_of_write = write;
		}
		@Override
		public Integer call() throws Exception {
			//Tf logic
			value += read(0,number_of_read);
			writeSequentially(num_of_write,value);

			return value;

		}
	}

	@Override
	public boolean isReadOnly() {
		return false;
	}

	 private class NestedWorker extends ParallelTask<Integer> {
		 Callable c;
		NestedWorker(Callable c){
			this.c = c;
		}
		@Override
		public Integer execute() throws Throwable {
			
			return (Integer) c.call();
		}
		 
	 }
}
