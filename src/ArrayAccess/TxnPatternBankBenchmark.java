package ArrayAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;


import contlib.Continuation;
import jvstm.ParallelTask;
import jvstm.Transaction;
import jvstm.VBox;

/** for futures: Long prefix: first read a disjoint set of keys, then write to a hot spot 
 ***	for continuations : read a "hot spot" 
 **/

public class TxnPatternBankBenchmark extends ArrayAccessTxnMultipleFuture {
	VBox<Integer>[] array;
	private int value;
	private int hot_spot = 100;
	private int num_of_prefix_sequential_read = 1;
	private int array_length = 0;
	private int cpu_work_amount_between_memory_read = 0;
	private int number_of_hot_spots_read_and_write=50;
	private String high_contention = "false";
	private int max_num_of_core;
	private int bank_agency_index;
	private int bank_agency_number;
	private int array_length_each_bank_agency; //basic array length that each bank agent can access
	private float txnPercentages;
	private int rounds_for_transfer;
	private boolean whether_to_run_it_seq;

	public TxnPatternBankBenchmark(int max_num_of_core, int hot_spot_in_the_array, 
			String high_contention, int spin, int bank_agency_index, 
			int bank_agency_number, float txnPercentages, int num_of_rounds, boolean seq){
		this.max_num_of_core = max_num_of_core;
		this.cpu_work_amount_between_memory_read = spin;
		this.hot_spot = hot_spot_in_the_array;
		this.high_contention = high_contention;
		this.bank_agency_index = bank_agency_index;
		this.bank_agency_number = bank_agency_number;
		this.txnPercentages = txnPercentages;
		this.rounds_for_transfer = num_of_rounds;
		this.whether_to_run_it_seq=seq;
	}

	public int executeTransaction(int sibling, int streaming) throws Throwable {

		array = ArrayAccess.array;
		array_length = array.length;
		array_length_each_bank_agency = array_length/bank_agency_number;

		int num_of_futures = sibling;
		List<Future<Integer>> future_array = 
				new ArrayList<Future<Integer>>(num_of_futures);
		
		if(streaming == 2) {
			AsyncOperation runner = new AsyncOperation();
			for(int i = 0; i< max_num_of_core; i++) {
				value += runner.call();
			}
		} else if(streaming == 1) {

			int spawnedFutures=0;
			int nextOp = 0;
			
			while(nextOp < max_num_of_core) {
				while(spawnedFutures < num_of_futures) {
					nextOp++;
					spawnedFutures++;
					NestedWorker worker = new NestedWorker(false,new AsyncOperation());
					future_array.add(Transaction.current().manageNestedParallelTxs(worker));
	
					Transaction.setNextCheckpoint(Continuation.capture());
					Transaction tx =Transaction.begin();
				}
				value += future_array.get(0).get();
				future_array.remove(0);
				spawnedFutures--;
			}
			
			for(int m = 0; m < future_array.size(); m++){
				value += future_array.get(m).get();
			}
		} else {
			for (int i = 1; i <= max_num_of_core / num_of_futures; i++) {
				for (int j = 1; j <= num_of_futures; j++) {
					// run_in_a_future(future_logic(())
					NestedWorker worker = new NestedWorker(false,
							new AsyncOperation());
					future_array.add(Transaction.current()
							.manageNestedParallelTxs(worker));

					Transaction.setNextCheckpoint(Continuation.capture());
					Transaction tx = Transaction.begin();

				}

				for (int m = 0; m < future_array.size(); m++) {
					value += future_array.get(m).get();
				}
				future_array.removeAll(future_array);
			}
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


	private class AsyncOperation implements Callable<Integer>{

		public AsyncOperation() {
		}

		@Override
		public Integer call() throws Exception {
			int value_returned = 0;
			//different types of operation
			if(Math.random()< txnPercentages){
				//System.out.println("get total amount ");
				TimerDebug.startGetTotalTime((int)Thread.currentThread().getId()%56);
				value_returned = getTotalAmountInTheBank();
				TimerDebug.endGetTotalTime((int)Thread.currentThread().getId()%56);
			}else{
				//System.out.println("transfer ");
				TimerDebug.startTransferTime((int)Thread.currentThread().getId()%56);
				value_returned = transferBetweenTwoAccounts();
				TimerDebug.endTransferTime((int)Thread.currentThread().getId()%56);
				
			}
			if(value_returned < 0){
				throw new Exception();
			}
			return value_returned;
		}

		private int transferBetweenTwoAccounts() {
			
			double sqrt_amount = 0;
			for(int round=1; round<=rounds_for_transfer; round++) {
				int amount = (int) (Math.random() * 1000);
				int index_deposit;
				int index_withdrawal;
				do {
					index_deposit = array_length_each_bank_agency * bank_agency_index;
					index_withdrawal = array_length_each_bank_agency * bank_agency_index;

					if (high_contention.equalsIgnoreCase("false")) {
						index_deposit += (int) (Math.random() * (array_length_each_bank_agency));
						index_withdrawal += (int) (Math.random() * (array_length_each_bank_agency));
					} else if (high_contention.equals("true")) {
						//				System.out.println("in high contention");
						index_deposit += (int) (Math.random() * hot_spot);
						index_withdrawal += (int) (Math.random() * hot_spot);
					} else if (high_contention.equalsIgnoreCase("middle")) {
						index_deposit += (int) (Math.random() * (hot_spot * 10));
						index_withdrawal += (int) (Math.random() * (hot_spot * 10));
					}
				} while (index_deposit == index_withdrawal);

				//insert some spin to simulate operations delay to database

				//before accessing memory, assign some CPU computation work
				for (int j = 0; j < cpu_work_amount_between_memory_read; j++) {
					sqrt_amount += Math.sqrt(j + index_deposit);
				}

				array[index_deposit].put(array[index_deposit].get() + amount);
				array[index_withdrawal].put(array[index_withdrawal].get() - amount);
			}
			return (int) sqrt_amount;

		}

		private int getTotalAmountInTheBank() {
			double sqrt_amount=0;
			int value_local = 0;

			//Get total amount in the bank, traverse the array and get the total amount
			for(int i = array_length_each_bank_agency*bank_agency_index;i < array_length_each_bank_agency*(bank_agency_index+1);i++){
				//before accessing memory, assign some CPU computation work
				for(int j = 0; j < cpu_work_amount_between_memory_read; j++){
					sqrt_amount += Math.sqrt(j+i);
				}
				value_local += array[i].get();
			}
			return (int) (sqrt_amount+value_local);

		}
	}

}
