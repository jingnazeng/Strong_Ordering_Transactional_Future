
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

public class TxnPatternBankBenchmarkSeq extends ArrayAccessTxnMultipleFuture {
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

	public TxnPatternBankBenchmarkSeq(int max_num_of_core, int hot_spot_in_the_array, String high_contention, int spin, int bank_agency_index, int bank_agency_number,float txnPercentages){
		this.max_num_of_core = max_num_of_core;
		this.cpu_work_amount_between_memory_read = spin;
		this.hot_spot = hot_spot_in_the_array;
		this.high_contention = high_contention;
		this.bank_agency_index = bank_agency_index;
		this.bank_agency_number = bank_agency_number;
		this.txnPercentages = txnPercentages;
	}

	public int executeTransaction(int sibling) throws Throwable {

		array = ArrayAccess.array;
		array_length = array.length;
		array_length_each_bank_agency = array_length/bank_agency_number;

		int value_returned = 0;

		for(int i = 1; i <= max_num_of_core ; i++){


			//different types of operation
			if(ThreadLocalRandom.current().nextDouble(0, 1) < txnPercentages){
				//				System.out.println("get total amount ");
				value_returned = getTotalAmountInTheBank();
			}else{
				//				System.out.println("transfer ");
				value_returned = transferBetweenTwoAccounts();
			}
			if(value_returned < 0){
				throw new Exception();
			}
		}

		return value_returned;
	}






	private int transferBetweenTwoAccounts() {

		//Transfer a certain amount of money from one account to another account
		int amount = (int) (ThreadLocalRandom.current().nextDouble(0, 1)*1000);
		//the deposit account and withdrawal account
		int index_deposit;
		int index_withdrawal;
		do{
			index_deposit = array_length_each_bank_agency*bank_agency_index;
			index_withdrawal = array_length_each_bank_agency*bank_agency_index;

			if(high_contention.equalsIgnoreCase("false")){
				index_deposit += (int)(ThreadLocalRandom.current().nextDouble(0, 1)*(array_length_each_bank_agency));
				index_withdrawal += (int)(ThreadLocalRandom.current().nextDouble(0, 1)*(array_length_each_bank_agency));
			}
			else if(high_contention.equals("true")){
				//				System.out.println("in high contention");
				index_deposit += (int)(ThreadLocalRandom.current().nextDouble(0, 1)*hot_spot);
				index_withdrawal += (int)(ThreadLocalRandom.current().nextDouble(0, 1)*hot_spot);
			}
			else if(high_contention.equalsIgnoreCase("middle")){
				index_deposit += (int)(ThreadLocalRandom.current().nextDouble(0, 1)*(hot_spot*10));
				index_withdrawal += (int)(ThreadLocalRandom.current().nextDouble(0, 1)*(hot_spot*10));
			}
		}while(index_deposit == index_withdrawal);

		//insert some spin to simulate operations delay to database
		double sqrt_amount = 0;
		//before accessing memory, assign some CPU computation work
		for(int j = 0; j < cpu_work_amount_between_memory_read; j++){
			sqrt_amount += Math.sqrt(j+index_deposit);
		}

		array[index_deposit].put(array[index_deposit].get() + amount) ;
		array[index_withdrawal].put(array[index_withdrawal].get() - amount) ;

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

	@Override
	public boolean isReadOnly() {
		return false;
	}



}
