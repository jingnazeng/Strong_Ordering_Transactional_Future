package ArrayAccess;

public interface ArrayAccessTransaction {
	
	public int executeTransaction(int sibling, int streaming) throws Throwable;
	
	public boolean isReadOnly();

}
