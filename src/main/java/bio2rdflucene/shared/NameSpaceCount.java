package bio2rdflucene.shared;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class represents the number of hits retrieved by a Lucene search, for a given namespace.
 * 
 * @author Alison Callahan
 * @author Glen Newton
 * 
 */
public class NameSpaceCount {
	
	 public String namespace;
	    public String prefix;
	    public int count;

	    public NameSpaceCount(String newNameSpace, 
				  int newCount)
	    {
		namespace=newNameSpace;
		count = newCount;
		
		Pattern p = Pattern.compile("\\[(\\S+)\\]");
		Matcher m = p.matcher(namespace);
		while(m.find()){
		    prefix = m.group(1);
		}//if
		
	    }
}
