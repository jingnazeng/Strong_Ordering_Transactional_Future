
public interface ArrayAccessTransaction {

	public int executeTransaction(int sibling) throws Throwable;
	
	public boolean isReadOnly();

}
