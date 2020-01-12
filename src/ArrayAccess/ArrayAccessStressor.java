package ArrayAccess;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;

import jvstm.Transaction;
import contlib.Continuation;

public class ArrayAccessStressor{
	
	/**
	 * @return the txnPercentages
	 */
	public float getTxnPercentages() {
		return txnPercentages;
	}

	/**
	 * @param txnPercentages the txnPercentages to set
	 */
	public void setTxnPercentages(float txnPercentages) {
		this.txnPercentages = txnPercentages;
	}

	/**
	 * @return the seq
	 */
	public boolean isSeq() {
		return seq;
	}

	/**
	 * @param seq the seq to set
	 */
	public void setSeq(boolean seq) {
		this.seq = seq;
	}

	/**
	 * @return the bankAgencyNum
	 */
	public int getBankAgencyNum() {
		return bankAgencyNum;
	}

	/**
	 * @param bankAgencyNum the bankAgencyNum to set
	 */
	public void setBankAgencyNum(int bankAgencyNum) {
		this.bankAgencyNum = bankAgencyNum;
	}

	private long startTime = 0;
	private long endTime = 0;
	private boolean running = true;
	private boolean active = true;
	private long simulationDuration = 30L;
	//FIXME:remove volatile should also work, because only one thread is accessing threadsStartingPoint
	private volatile CountDownLatch threadsStartingPoint;
	private int threadsNum = 1;
	private int siblingNum = 0;
	private int bankAgencyNum = 1;
	private int max_num_of_cores;
	private int num_of_read_in_prefix_disjoint_read = 0;
	private int num_of_hot_spots_in_the_whole_array = 0;
	private int num_of_read_and_write_to_hot_spots;
	private String write_in_high_contenction = "false";
	private int cpu_work_amount_between_memory_read = 0;
	private float txnPercentages;
	
	public int getNum_of_rounds_for_transfer() {
		return num_of_rounds_for_transfer;
	}

	public void setNum_of_rounds_for_transfer(int num_of_rounds_for_transfer) {
		this.num_of_rounds_for_transfer = num_of_rounds_for_transfer;
	}

	private int num_of_rounds_for_transfer;
	
	private boolean seq;
	
	private Timer finishTimer = new Timer("Finish-Timer");
	
	private List<Stressor> stressors = new LinkedList<Stressor>();
	private int streaming;
	
	public void setThreadsNum(int num){
		this.threadsNum = num;
	}
	
	public void setSimuDuration(int num){
		this.simulationDuration = num;
	}
	
	public void setSiblingNum(int num){
		this.siblingNum = num;
	}
	
	public void setNumReadInPrefixDisjointRead(int num){
		this.num_of_read_in_prefix_disjoint_read = num;
	}
	
	public void setMaxNumofCores(int num){
		this.max_num_of_cores = num;
	}
	
	public void setNumofHotspots(int num){
		this.num_of_hot_spots_in_the_whole_array = num;
	}
	
	public void setNumofReadAndWriteToHotSpots(int num){
		this.num_of_read_and_write_to_hot_spots = num;
	}
	
	public void setWriteInHighContention(String contention){
//		if(contention.equalsIgnoreCase("true"))
		this.write_in_high_contenction = contention;
	}
	
	public void setCpuWorkBetweenMemoryRead(int amount){
		this.cpu_work_amount_between_memory_read = amount;
	}
	
	public Map<String, String> stress() throws InterruptedException{
		finishTimer.schedule(new TimerTask(){

			@Override
			public void run() {
				finishThreadsActivities();
			}

			
		}, simulationDuration*1000);
		
		executeTransactionsInAllThreads();
		
		finishTimer.cancel();
		
		return processResults(stressors);
	}
	
	private Map<String, String> processResults(List<Stressor> stressors) {
		int failures = 0;
		int readOnlyTxn = 0;
		int writeTxn = 0;
		int readOnlyTxnFailure = 0;
		int writeTxnFailure = 0;
		float txn_running_duration_all_thead = 0;
		int txn_num_all_thread = 0;
		int val = 0;
		Map<String,String> results = new LinkedHashMap<String,String>();
		for(Stressor s:stressors){
			failures += s.failure;
			readOnlyTxn += s.readOnlyTxn;
			writeTxn += s.writeTxn;
			readOnlyTxnFailure += s.readOnlyTxnFailure;
			writeTxnFailure += s.writeTxnFailure;
			txn_running_duration_all_thead += s.total_txn_exe_duration_single_thread;
			txn_num_all_thread += s.txn_num;
			val += s.val;
		}
		long simuDuration = endTime-startTime;
		float throughput = (float)(writeTxn+readOnlyTxn)/simuDuration*1000;
		float abort_rate = (float)failures/(writeTxn+readOnlyTxn+failures);
		
		float average_running_time_all_thread  = (float)txn_running_duration_all_thead/txn_num_all_thread/1000000;
		results.put("Duration", str(simuDuration));
		results.put("read_only_txn", str(readOnlyTxn));
		results.put("write_txn", str(writeTxn));
		results.put("failures", str(failures));
		results.put("read_only_failures", str(readOnlyTxnFailure));
		results.put("write_txn_failure", str(writeTxnFailure));
		results.put("throughput", str(throughput));
		results.put("abort_rate", str(abort_rate));
		results.put("avarage_txn_running_time_in_millis", str(average_running_time_all_thread));
		results.put("transfer time in microSecod: ", str(TimerDebug.getAverage(TimerDebug.getTransferTime())/1000));
		results.put("get total time in microSecod: ", str(TimerDebug.getAverage(TimerDebug.getTotalTime())/1000));
		results.put("reading time in microSecod: ", str(TimerDebug.getAverage(TimerDebug.getReadingTime())/1000));
		results.put("writing time in microSecod: ", str(TimerDebug.getAverage(TimerDebug.getWritingTime())/1000));
		
		results.put("val", str(val));
		results.put("internal abort rate: ", str(TimerDebug.getFutureAbortRate()));
		results.put("commit duration in microSecod: ", str(TimerDebug.getAverage(TimerDebug.getCommitTime())/1000));
		return results;
	}

	private String str(Object o) {
		return String.valueOf(o);
	}

	//FIXME:may not need to be synchronized because only one threads will be accessing
	private synchronized void finishThreadsActivities() {
		if (!running) {
			return;
		}
		running = false;
		for (Stressor stressor : stressors) {
			stressor.finish();
		}
		notifyAll();
	}		

	public void executeTransactionsInAllThreads() throws InterruptedException{
		threadsStartingPoint = new CountDownLatch(1);
		for(int threadsIndex = 0 ; threadsIndex < threadsNum ; threadsIndex++){
			Stressor stressor = createSressor(threadsIndex);
			stressors.add(stressor);
			stressor.start();
		}
		threadsStartingPoint.countDown();
		startTime = System.currentTimeMillis();
		
		blockWhileRunning();
		for(Stressor stressor:stressors){
			stressor.join();
		}
		endTime = System.currentTimeMillis();
		
	}
	
	//FIXME:may not need synchronized because only one threads is calling this method at a time
	private synchronized void blockWhileRunning() throws InterruptedException {
		//FIXME:improve so that this thread is not busy checking
		while(this.running){
				wait();
		}
	}

	private Stressor createSressor(int threadsIndex) {
		return new Stressor(threadsIndex);
		
	}
	
	private class Stressor extends Thread{
		private int readOnlyTxn = 0;
		private int writeTxn = 0;
		private int failure = 0;
		private int readOnlyTxnFailure = 0;
		private int writeTxnFailure = 0;
		private float average_running_time = 0;
		private long total_txn_exe_duration_single_thread = 0;
		private int txn_num = 0;
		private int val = 0;
		
		private boolean runWithContinuationSupport = false;

		public Stressor(int threadsIndex) {
			super("Stressor Num:"+threadsIndex);
		}

		public final synchronized void finish() {
			active = true;
			running = false;
			notifyAll();
		}
		
		private synchronized boolean assertRunning() {
			return running;
		}

		public void run(){
			/*if(!runWithContinuationSupport){
				this.runWithContinuationSupport = true;
				Continuation.runWithContinuationSupport(this);
				return;
			}*/
			
			try {
				threadsStartingPoint.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			boolean successful = true;
			long txn_start_timing = System.nanoTime();
			
			while(assertRunning()){
				boolean readOnly;

				ArrayAccessTransaction  arrayAcessTransaction = createTransaction();
				readOnly = arrayAcessTransaction.isReadOnly();
				if(successful == true){
					txn_start_timing = System.nanoTime();
					txn_num += 1;
				}
				successful = true;
				
				Transaction tx = Transaction.begin(readOnly);
				try {
//					long start = System.nanoTime();
					val = arrayAcessTransaction.executeTransaction(siblingNum, streaming);
//					System.out.println("executeion time: "+ (System.nanoTime()-start));
				}
				catch(Throwable e){
					successful = false;
					tx.abort();
					failure++;
					if(arrayAcessTransaction instanceof ReadOnlyArrayAccess){
						readOnlyTxnFailure++;
					}
					else if (
							arrayAcessTransaction instanceof BaseTxnWithMultipleFuture
				|| arrayAcessTransaction instanceof ArrayAccessTxnMultipleFuture)
						writeTxnFailure++;
				}

				if(successful){
					try{
						tx.commit();
						if(arrayAcessTransaction instanceof ReadOnlyArrayAccess){
							readOnlyTxn++;
						}
						else if (
								arrayAcessTransaction instanceof ArrayAccessTxnMultipleFuture)
//							System.out.println("************commit a txn**********");
							writeTxn++;
					}
					catch(RuntimeException exception ){
						exception.printStackTrace();
						successful = false;
						tx.abort();
						failure++;
						if(arrayAcessTransaction instanceof ReadOnlyArrayAccess){
							readOnlyTxnFailure++;
						}
						else if (
								arrayAcessTransaction instanceof ArrayAccessTxnMultipleFuture){
							writeTxnFailure++;
						}
					}
				}
				if(successful){
				long single_txn_duration = System.nanoTime()-txn_start_timing;
				total_txn_exe_duration_single_thread += single_txn_duration;
				}
			}
			
			if(!successful){
				txn_num -= 1;
			}
				
				
		}

		private ArrayAccessTransaction createTransaction() {
//			if(Math.random() < RO_Txn_percentage)
//				return new ReadOnlyArrayAccess(num_of_read_in_RO_Txn,cpu_work_amount_between_memory_read);
//			else 
//				return new ReadWriteArrayAccess(num_of_read_in_RW_Txn,num_of_write_in_RW_Txn,write_in_high_contenction,cpu_work_amount_between_memory_read);
//			}
		
//		return new BaseTxnWithMultipleFuture(num_of_read_in_RW_Txn,num_of_write_in_RW_Txn,write_in_high_contenction,cpu_work_amount_between_memory_read);
			//int max_num_of_core, int prefix_disjoint_read, int hot_spot_in_the_array,int num_of_hot_spots_read_and_write, String high_contention, int spin
//			return new TxnPattern6Seq(max_num_of_cores, 
//					num_of_read_in_prefix_disjoint_read, 
//					num_of_hot_spots_in_the_whole_array, 
//					num_of_read_and_write_to_hot_spots, 
//					write_in_high_contenction, 
//					cpu_work_amount_between_memory_read);
			
			
//			return new TxnPattern6ByFutures(max_num_of_cores, 
//					num_of_read_in_prefix_disjoint_read, 
//					num_of_hot_spots_in_the_whole_array, 
//					num_of_read_and_write_to_hot_spots, 
//					write_in_high_contenction, 
//					cpu_work_amount_between_memory_read);
//		}
			
			// to decide which bank agency to access, i.e. which part of the array to manipulate
			int bank_agency_index = ThreadLocalRandom.current().nextInt(0, bankAgencyNum);
			if(seq)
				return new TxnPatternBankBenchmark(max_num_of_cores, 
						num_of_hot_spots_in_the_whole_array, 
						write_in_high_contenction, 
						cpu_work_amount_between_memory_read,bank_agency_index,bankAgencyNum,txnPercentages, 
						num_of_rounds_for_transfer, seq);	

			else

				return new TxnPatternBankBenchmark(max_num_of_cores, 
						num_of_hot_spots_in_the_whole_array, 
						write_in_high_contenction, 
						cpu_work_amount_between_memory_read,bank_agency_index,bankAgencyNum,txnPercentages, 
						num_of_rounds_for_transfer, seq);	

		}
			
	}

	public void setStreaming(int streaming) {
		this.streaming = streaming;
		
	}
}
