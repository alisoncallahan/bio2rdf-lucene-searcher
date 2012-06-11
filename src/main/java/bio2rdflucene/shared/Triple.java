package bio2rdflucene.shared;

/**
 * This object represents triples with URI, Name, Type and Definition
 * @author Alison Callahan
 * @author Glen Newton
 */
public class Triple {
	public String uri;
    public String name;
    public String type;
    public String definition;

    public Triple(String newUri, 
		   String newName,
		   String newType,
		   String newDefinition)
    {
    	uri = newUri;
    	name = newName;
    	type = newType;
    	definition = newDefinition;
    }
}
