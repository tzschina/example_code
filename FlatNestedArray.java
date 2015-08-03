import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Stack;

/**
 * Write some code, that will flatten an array of arbitrarily nested arrays of integers into a flat array of integers. e.g. [[1,2,[3]],4] -> [1,2,3,4]
 * @author ZhenshengTan
 *
 */
public class FlatNestedArray {
	
	final public static int MAX_SUB_ARRAY_LENGTH = 3;
	final public static int MAX_INT_VALUE = 100;
	
	/**
	 * Basically use object array Object[] of object array Object[] to represent in Java for nested array
	 * Example if depth = 1, then generate [1,2,3] 
	 * Example if depth = 3, then generate [ [ [1,5] ] ,[ [2] ] ,[ [4,3,9] , [7,6,10] ] ]
	 * Change MAX_SUB_ARRAY_LENGTH to set the maximum length of the sub array
	 * Change MAX_INT_VALUE to set the maximum value
	 * @param Assert Depth >=1. 
	 * @return nested array
	 */
	public static Object[] generateRandomIntegerNestedArray(int depth) {
		Object[] arr = null;
		if(depth < 1) {
			return arr;
		}
		Random rand = new Random();
		int randLength = rand.nextInt(MAX_SUB_ARRAY_LENGTH) + 1;  
		arr = new Object[randLength];
		if(depth == 1) {
			for (int i = 0; i < randLength; i++) {
				arr[i] = rand.nextInt(MAX_INT_VALUE) + 1;
			}
		} else {

			for (int i = 0; i < randLength; i++) {
				arr[i] = generateRandomIntegerNestedArray(depth-1);
			}
		}
		return (Object[]) arr;
	}
	
	public static String generateStringFromNestedArray(Object[] arrs) {
		Stack<Object[]> st = new Stack<Object[]>();
		List<Integer> ints = new ArrayList<Integer>();
		if(arrs != null) {
			st.push(arrs);
			while(st.size() > 0) {
				Object[] check = st.pop();
				if(check instanceof Integer[]) {
					for ( Integer i : (Integer[])check) {
						ints.add(i);
					}
				} 
				else {
					for ( int i=check.length-1 ; i >= 0; i--) {
						if(check[i] instanceof Integer[]) st.push((Integer[])check[i]);
						else if(check[i] instanceof Integer) st.push(new Integer[]{(Integer)check[i]});
						else st.push((Object[]) check[i]);
					}
				}
			}
			return ints.toString();
		}
		else {
			return "";
		}

	}
	
	public static void main(String arfs[]) {
		//Check the nestedArray in the Java debug mode if you want to see its origin form.
		Object[] nestedArray = generateRandomIntegerNestedArray(5);
		//Now transform into a flatten string
		if(nestedArray != null) System.out.print(generateStringFromNestedArray(nestedArray));
	}
}
