package ArrayAccess;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import contlib.Continuation;
import jvstm.ParallelTask;
import jvstm.Transaction;
import jvstm.VBox;

//by reading and writing a "hot spot" in the same array.

public class TxnPattern4 extends ArrayAccessTxnMultipleFuture {
	VBox<Integer>[] array;
	private int value;
	private int num_of_write = 1;
	private int num_of_read = 1;
	private int array_length = 0;
	private int cpu_work_amount_between_memory_read = 0;
	private int number_of_hot_spots_read_and_write=10;
	private String high_contention = "false";
	
	public TxnPattern4(int read, int write, String high_contention, int spin){
		this.num_of_read = read;
		this.num_of_write = write;
		this.cpu_work_amount_between_memory_read = spin;
		this.high_contention = high_contention;
	}

	public int executeTransaction(int sibling, int streamingEnabled) throws Throwable {

		array = ArrayAccess.array;
		array_length = array.length;

		//F1

		NestedWorker worker = new NestedWorker(false,new AsyncOperation1());
		Future<Integer> f1 = Transaction.current().manageNestedParallelTxs(worker);

		Transaction.setNextCheckpoint(Continuation.capture());
		Transaction tx =Transaction.begin();

		//T2 
//		Integer val = array[0].get();
//		System.out.println("T2 read X and the value is: "+ val);
		readHotSpots(number_of_hot_spots_read_and_write);

		//Tf2
		NestedWorker worker_f2 = new NestedWorker(false,new AsyncOperation2());
		Future<Integer> f2 = Transaction.current().manageNestedParallelTxs(worker_f2);

		Transaction.setNextCheckpoint(Continuation.capture());
		Transaction tx1 =Transaction.begin();

		//T3
		readHotSpots(number_of_hot_spots_read_and_write);
//		Integer val2 = array[1].get();
//		System.out.println("T3 read y and the value is: "+ val2);

		value += f1.get();
		value += f2.get();

		return value;
	}



	
	

	@Override
	public boolean isReadOnly() {
		return false;
	}
	
	private class NestedWorker extends ParallelTask<Integer> {

		private final boolean readOnly;

		public NestedWorker(boolean readOnly, Callable c) {
			super(c);
			this.readOnly = readOnly;
		}

		@Override
		protected boolean isReadOnly() {
			return readOnly;
		}

		@Override
		public Integer execute() throws Throwable {
			return -1;
		}
	}
	
	
	  private class AsyncOperation1 implements Callable<Integer>{
	    	
	    	public AsyncOperation1() {
	    	}
	    	
	    	@Override
	    	public Integer call() throws InterruptedException {
	    		readSequentially();
	    		writeHotSpots(number_of_hot_spots_read_and_write);
//				int val = (int) (Math.random()*10);
//				array[0].put(val);
//				System.out.println("F1 write to x :" + val);
				return value;
			
	    		}
	    }
	  
	  
	  private class AsyncOperation2 implements Callable<Integer>{
	    	
	    	public AsyncOperation2() {
	    	}
	    	
	    	@Override
	    	public Integer call() throws InterruptedException {
	    		readSequentially();
	    		writeHotSpots(number_of_hot_spots_read_and_write);
//				int val = (int) (Math.random()*10);
//				array[1].put(val);
//				System.out.println("F2 write to y :" + val);
				return value;
			}
	    }
	  
	  private int readSequentially() {
			return read(0,num_of_read);
		}
		
		private int read(int min, int max) {
			for(int i = min;i < max;i ++){
					double sqrt_amount = 0;
					//before accessing memory, assign some CPU computation work
					for(int j = 0; j < cpu_work_amount_between_memory_read; j++){
						sqrt_amount += Math.sqrt(j+i);
					}
					int index = ((int)(sqrt_amount+Math.random()*array_length)) % array_length;
					if(index < 3)
						index+=2;
					
					value = array[index].get();
				}
			return value;
		}
		
		private void writeHotSpots(int number_of_hot_spots_read_and_write) {

			int index =0;
			for(int i=0; i<number_of_hot_spots_read_and_write;i++){
				if(high_contention.equalsIgnoreCase("false")){
					index = (int)(Math.random()*(array_length));
				}
				else if(high_contention.equals("true")){
//					System.out.println("in high contention");
					index = (int)(Math.random()*num_of_write);
				}
				else if(high_contention.equalsIgnoreCase("middle")){
					index = (int)(Math.random()*(num_of_write*10));
				}
				
//				array[index].put((int)(Math.random()*1000)+value);
				int j = 0;
				try{
				j = array[index].get();
				}catch(Exception e){
//	 				System.out.println("READ conflict..");
					throw e;
				}
				try{
				if(j>500){
					array[index].put(j-(int)(Math.random()*1000));
				}
				else{
					array[index].put(j+(int)(Math.random()*1000));
				}
				}catch(Exception e){
//					System.out.println("write conflict..");
					throw e;
				}
			}
		
		}
		
		private void readHotSpots(int number_of_hot_spots_read_and_write) {
			int index = 0;
			for(int i = 0;i < number_of_hot_spots_read_and_write;i ++){
				if(high_contention.equalsIgnoreCase("false")){
					index = (int)(Math.random()*(array_length));
				}
				else if(high_contention.equals("true")){
					//System.out.println("in high contention");
					index = (int)(Math.random()*num_of_write);
				}
				else if(high_contention.equalsIgnoreCase("middle")){
					index = (int)(Math.random()*(num_of_write*10));
				}
				try{
					value += array[index].get();
				}catch(Exception e){
					//System.out.println("READ conflict..");
					throw e;
				}
			}
		}
}
