package gilp.learning;

import java.util.Comparator;

public class RulePHatDSCComparator implements Comparator<RulePackage> {
	@Override
	public int compare(RulePackage o1, RulePackage o2) {
		if(o1.getPHat()>o2.getPHat())
			return -1;
		else if(o1.getPHat()<o2.getPHat())
			return 1;
		else
			return 0;
	}
}
