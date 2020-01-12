
public class TimerDebug {
	private static final long SAMPLING_BATCH_RATE = 10;
	private static final float EXP_AVG_WEIGHT = 0.5f;
	public static long[] commitStartingTime = new long[56];
	public static float[] commitTime = new float[56];
	private static long[] commitIndex = new long[56];
	private static long[] commitFlag = new long[56]; 
	
	public static long[] futureLauched = new long[56];
	public static long[] abortCount = new long[56];
	public static long[] committedFuture = new long[56];
		

	public static float getAverage(float[] entries) {
		int count = 0;
		float sum = 0;
		for(float entry : entries) {
			if(entry!=0){
				count++;
				sum += entry;
			}
		}

		return sum / count;
	}
	
	public static long getSum(long[] times) {
		long sum = 0;
		for(long time : times) {
			if(time!=0){
				sum += time;
			}
		}

		return sum;
	}


	public static float getAverage(long[] times) {
		int count = 0;
		long sum = 0;
		for(long time : times) {
			if(time!=0){
				count++;
				sum += time;
			}
		}

		return sum/ count;
	}


	public static void startCommit(Integer threadID) {
		if((((commitIndex[threadID]++)%SAMPLING_BATCH_RATE)==(SAMPLING_BATCH_RATE-1))){
			if((commitFlag[threadID]%2)==0) {
				commitStartingTime[threadID] = System.nanoTime();
				commitFlag[threadID]++;
			}
		}
	}

	public static void endCommit(Integer threadID) {
		if((commitFlag[threadID]%2)!=0) {
			long sample = System.nanoTime() - commitStartingTime[threadID];
			commitTime[threadID] = commitTime[threadID] + (EXP_AVG_WEIGHT*(sample-commitTime[threadID]));
			commitFlag[threadID]++;
		}
	}

	public static float[] getCommitTime(){
		return commitTime;
	}	
	


	
	
	public static void startFuture(Integer threadID) {
		futureLauched[threadID]++;
	}
	
	public static void abortIncrement(Integer threadID) {
		abortCount[threadID]++;
	}
	
	public static void futureCommit(Integer threadID) {
		committedFuture[threadID]++;
	}
	
	public static float getFutureAbortRate(){
		long abortCountAccu = getSum(abortCount);
		long commitedFutureAccu = getSum(committedFuture);
		float abort_rate = (float) ((abortCountAccu*1.0)/(abortCountAccu+commitedFutureAccu));
		return abort_rate;
		
	}
}
