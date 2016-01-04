package gilp.rule;

import java.util.Iterator;
import java.util.List;

public interface Clause {
	
	public void addPredicate(Predicate p);
	
	public Iterator<Predicate> getIterator();
	 
	public int getBodyLength();
	
	public Clause clone();
}
