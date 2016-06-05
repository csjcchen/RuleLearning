package gilp.utility;

import java.util.Comparator;

public class NumericalKVPairComparator implements Comparator<KVPair> {

	@Override
	public int compare(KVPair arg0, KVPair arg1) {
		double d0 = (new Double("" + arg0.get_value())).doubleValue();
		double d1 = (new Double("" + arg1.get_value())).doubleValue();
		
		if (d0<d1)
			return -1;
		else if (d0 == d1)
			return 0;
		else
			return 1;
	}

}
