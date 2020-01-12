
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import contlib.Continuation;
import jvstm.ParallelTask;
import jvstm.Transaction;
import jvstm.VBox;

public class TxnPattern2 extends ArrayAccessTxnMultipleFuture {
	VBox<Integer>[] array;
	private int value;
	long sleeptime = 1000;
	
	public TxnPattern2(){
	}

	public int executeTransaction(int sibling) throws Throwable {

		array = ArrayAccess.array;


		//F1

		NestedWorker worker = new NestedWorker(false,new AsyncOperation1());
		Future<Integer> f1 = Transaction.current().manageNestedParallelTxs(worker);

		Transaction.setNextCheckpoint(Continuation.capture());
		Transaction tx =Transaction.begin();

		//T2 
		Integer val = array[0].get();
//		System.out.println("T2 read X and the value is: "+ val);

		//Tf2
		NestedWorker worker_f2 = new NestedWorker(false,new AsyncOperation2());
		Future<Integer> f2 = Transaction.current().manageNestedParallelTxs(worker_f2);

		Transaction.setNextCheckpoint(Continuation.capture());
		Transaction tx1 =Transaction.begin();

		//T3
		Integer val2 = array[1].get();
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

				Thread.sleep(sleeptime);
				int val = (int) (Math.random()*10);
				array[0].put(val);
//				System.out.println("F1 write to x :" + val);
				return value;
			
	    		}
	    }
	  
	  
	  private class AsyncOperation2 implements Callable<Integer>{
	    	
	    	public AsyncOperation2() {
	    	}
	    	
	    	@Override
	    	public Integer call() throws InterruptedException {
				Thread.sleep(sleeptime);
				int val = (int) (Math.random()*10);
				array[1].put(val);
//				System.out.println("F2 write to y :" + val);
				return value;
			}
	    }
}
