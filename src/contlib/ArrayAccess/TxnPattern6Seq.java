
import java.util.concurrent.Callable;

import jvstm.ParallelTask;
import jvstm.VBox;

/** for futures: Long prefix: first read a disjoint set of keys, then write to a hot spot 
***	for continuations : read a "hot spot" 
**/

public class TxnPattern6Seq extends ArrayAccessTxnMultipleFuture {
	VBox<Integer>[] array;
	private int value;
	private int hot_spot_in_the_array = 1;
	private int num_of_prefix_disjoint_read = 1;
	private int array_length = 0;
	private int cpu_work_amount_between_memory_read = 0;
	private int number_of_hot_spots_read_and_write=0;
	private String high_contention = "false";
	private int max_num_cores = 0;

	public TxnPattern6Seq(int max_num_of_core, int prefix_disjoint_read, int hot_spot_in_the_array,int num_of_hot_spots_read_and_write, String high_contention, int spin){
		this.num_of_prefix_disjoint_read = prefix_disjoint_read;
		this.hot_spot_in_the_array = hot_spot_in_the_array;
		this.cpu_work_amount_between_memory_read = spin;
		this.high_contention = high_contention;
		this.max_num_cores = max_num_of_core;
		this.number_of_hot_spots_read_and_write = num_of_hot_spots_read_and_write;
	}

	public int executeTransaction(int sibling) throws Throwable {

		array = ArrayAccess.array;
		array_length = array.length;
		
		for(int i = 1 ; i <= max_num_cores ; i++){
			//future logic
			readSequentially();
			writeHotSpots(number_of_hot_spots_read_and_write);
			// continuation logic
			readHotSpots(number_of_hot_spots_read_and_write);
		}
	
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


	private int readSequentially() {
		return read(0,num_of_prefix_disjoint_read);
	}

	private int read(int min, int max) {
		for(int i = min;i < max;i ++){
			double sqrt_amount = 0;
			//before accessing memory, assign some CPU computation work
			for(int j = 0; j < cpu_work_amount_between_memory_read; j++){
				sqrt_amount += Math.sqrt(j+i);
			}
			int index = ((int)(sqrt_amount+Math.random()*array_length)) 
					% (array_length-number_of_hot_spots_read_and_write);
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
				index = (int)(Math.random()*hot_spot_in_the_array);
			}
			else if(high_contention.equalsIgnoreCase("middle")){
				index = (int)(Math.random()*(hot_spot_in_the_array*10));
			}

			index = array_length - 1 - index; // write to a disjoint hotspt set w.r.t. long prefix read 
			try{
				array[index].put((int)(Math.random()*1000));
			}catch(Exception e){
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
				index = (int)(Math.random()*hot_spot_in_the_array);
			}
			else if(high_contention.equalsIgnoreCase("middle")){
				index = (int)(Math.random()*(hot_spot_in_the_array*10));
			}
			index = array_length - 1 - index; // read to a disjoint hotspt set w.r.t. long prefix read
			try{
				value += array[index].get();
			}catch(Exception e){
				throw e;
			}
		}
	}
}
