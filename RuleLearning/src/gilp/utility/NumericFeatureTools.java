package gilp.utility;

import java.util.Arrays;

public class NumericFeatureTools {
	
	
	// to test whether the @value is inside the @range
	// @value should be parsed into double
	// format of @range: (a, b), (a,b], [a, b), [a,b]
	public static boolean inside(String value, String range) {
		double dv = Double.parseDouble(value);

		String left = range.trim().substring(0, 1);
		String right = range.trim().substring(range.length() - 1, range.length());

		double[] bounds = getBounds(range);

		double d_l = bounds[0];
		double d_h = bounds[1];
		if (d_l > d_h)
			return false;
		if (left.equals("(") && dv <= d_l)
			return false;
		if (left.equals("[") && dv < d_l)
			return false;
		if (right.equals(")") && dv >= d_h)
			return false;
		if (right.equals("]") && dv > d_h)
			return false;
		return true;
	}

	// @r1 and @r2 can be either ranges or constants
	// @return 1 if r1 is a true superset of r2;
	// -1 if r1 is a true subset of r2;
	// 0 otherwise
	public static int compareRange(String r1, String r2) {
		if (isRange(r1) && isRange(r2)) {
			double[] bounds1 = getBounds(r1);
			double[] bounds2 = getBounds(r1);
			String left1 = r1.trim().substring(0, 1);
			String right1 = r1.trim().substring(r1.length() - 1, r1.length());

			String left2 = r2.trim().substring(0, 1);
			String right2 = r2.trim().substring(r2.length() - 1, r2.length());

			int compareLeft = 0;
			int comparesRight = 0;
			if (bounds1[0] < bounds2[0])
				compareLeft = 1;
			else if (bounds1[0] > bounds2[0])
				compareLeft = -1;
			else {
				if (left1.equals("[") && left2.equals("("))
					compareLeft = 1;
				else if (left1.equals("(") && left2.equals("["))
					compareLeft = -1;
				else
					compareLeft = 0;
			}

			if (bounds1[1] > bounds2[1])
				comparesRight = 1;
			else if (bounds1[1] < bounds2[1])
				comparesRight = -1;
			else {
				if (right1.equals("]") && right2.equals(")"))
					comparesRight = 1;
				else if (right1.equals(")") && right2.equals("]"))
					comparesRight = -1;
				else
					comparesRight = 0;
			}

			if (compareLeft == comparesRight)
				return compareLeft;
			else if (compareLeft == 0)
				return comparesRight;
			else if (comparesRight == 0)
				return compareLeft;
			else
				return 0;
		}

		if (!isRange(r1) && isRange(r2)) {
			if (inside(r1, r2))
				return -1;
			else
				return 0;
		}

		if (isRange(r1) && !isRange(r2)) {
			if (inside(r2, r1))
				return 1;
			else
				return 0;
		}

		if (!isRange(r1) && !isRange(r2)) {
			return 0;
		}

		return 0;
	}

	public static boolean isRange(String str) {
		if (str.trim().startsWith("(") || str.trim().startsWith("["))
			return true;
		else
			return false;

	}

	public static double[] getBounds(String range) {

		double[] bounds = new double[2];
		String left = range.trim().substring(0, 1);
		String right = range.trim().substring(range.length() - 1, range.length());

		String low = range.substring(range.indexOf(left) + 1, range.indexOf(","));
		String high = range.substring(range.indexOf(",") + 1, range.indexOf(right));

		double d_l = Double.parseDouble(low);
		double d_h = Double.parseDouble(high);
		bounds[0] = d_l;
		bounds[1] = d_h;
		return bounds;
	}
	
	//e.g. [1, 3, 5] --> [-infinity, 2, 4, infinity]
	public static double[] calcBoundPoints(double[] keys){
		
		double[] bound_points = new double[keys.length+1];
		System.arraycopy(keys, 0, bound_points, 1, keys.length);
		bound_points[0] = Double.MIN_VALUE;
		Arrays.sort(bound_points);
		for(int i=1;i<bound_points.length-1;i++){
			bound_points[i] = (bound_points[i] + bound_points[i+1])/2.0;
		}
		bound_points[bound_points.length-1] = Double.MAX_VALUE; 
		
		return bound_points;
		
	}
		

}
