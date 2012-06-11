package bio2rdflucene.shared;

import java.util.Comparator;

/**
 * This comparator compares integers
 * @author Alison Callahan
 * @author Glen Newton
 */
public class ValueComparator implements Comparator<Integer>{
	
	public ValueComparator() {}
	
	public int compare(Integer v1, Integer v2){
		if(v1 > v2){
			return -1;
		} else if (v1 < v2){
			return 1;
		} else {
			return 0;
		}//else
	}//compare
}//ValueComparator
