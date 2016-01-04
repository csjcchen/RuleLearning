package gilp.utility;

public class StringUtils {

	/*remove the < and > in the input strings
	 * CJC, Nov. 18, 2015
	 * */
	public static String removePointBrackets(String str){
		str = str.replace("<", "");
		str = str.replace(">", "");
		return str;
	}
}
