package benchmark.vacation;

public abstract class Operation {

    public static boolean nestedParallelismOn;
    public static int numberParallelSiblings;
    public static boolean parallelizeUpdateTables;
    public static boolean readOnly;
    public static boolean unsafe;
    public Integer futureAborts=0;
    public Integer futuresEarlyAborts =0;

    public abstract void doOperation();

}
