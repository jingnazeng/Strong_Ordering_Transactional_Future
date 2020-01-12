package ArrayAccess;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import contlib.Continuation;
import jvstm.ParallelTask;
import jvstm.Transaction;
import jvstm.VBox;

public class TxnPattern1 extends ArrayAccessTxnMultipleFuture {
	VBox<Integer>[] array;
	private int value;
	long sleeptime = 1000;
	
	public TxnPattern1(){
	}

	public int executeTransaction(int sibling, int streamingEnabled) throws Throwable {

		array = ArrayAccess.array;


		//F1

		NestedWorker worker = new NestedWorker(false,new AsyncOperation1());
		Future<Integer> f1 = Transaction.current().manageNestedParallelTxs(worker);

		Transaction.setNextCheckpoint(Continuation.capture());
		Transaction tx =Transaction.begin();

		//T2 empty 
		

		//Tf2
		NestedWorker worker_f2 = new NestedWorker(false,new AsyncOperation2());
		Future<Integer> f2 = Transaction.current().manageNestedParallelTxs(worker_f2);

		Transaction.setNextCheckpoint(Continuation.capture());
		Transaction tx1 =Transaction.begin();

		//Tf3
		NestedWorker worker_f3 = new NestedWorker(false,new AsyncOperation3());
		Future<Integer> f3 = Transaction.current().manageNestedParallelTxs(worker_f3);

		Transaction.setNextCheckpoint(Continuation.capture());
		Transaction tx2 =Transaction.begin();
		
		//Tf4
		NestedWorker worker_f4 = new NestedWorker(false,new AsyncOperation4());
		Future<Integer> f4 = Transaction.current().manageNestedParallelTxs(worker_f4);

		Transaction.setNextCheckpoint(Continuation.capture());
		Transaction tx3 =Transaction.begin();

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
	    		//Tf1 logic
				Thread.sleep(sleeptime);
				int val = (int) (Math.random()*10);
				array[0].put(val); 
//				System.out.println("Tf1 write x to be :" + val);
				return value;
	    	}
	    }
	  
	  
	  private class AsyncOperation2 implements Callable<Integer>{
	    	
	    	public AsyncOperation2() {
	    	}
	    	
	    	@Override
	    	public Integer call() throws InterruptedException {
				//Tf2 logic
				
				int val = array[0].get(); 
//				System.out.println("Tf2 read x to be :" + val);
				return value;

			}
	    }
	  
	  
	  private class AsyncOperation3 implements Callable<Integer>{
	    	
	    	public AsyncOperation3() {
	    	}
	    	
	    	@Override
	    	public Integer call() throws InterruptedException {

				//Tf3 logic
				Thread.sleep(sleeptime);
				int val = (int) (Math.random()*10);
				array[1].put(val); 
//				System.out.println("Tf3 write y to be :" + val);
				return value;

			
	    	}
	    }
	  
	  
	  private class AsyncOperation4 implements Callable<Integer>{
	    	
	    	public AsyncOperation4() {
	    	}
	    	
	    	@Override
	    	public Integer call() throws InterruptedException {
				//Tf4 logic
				
				int val = array[1].get(); 
//				System.out.println("Tf4 read y to be :" + val);
				return value;

			}
	    }
}
