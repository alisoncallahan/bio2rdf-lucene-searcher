package bio2rdflucene.shared;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.store.FSDirectory;

/**
 * This class opens a Lucene index or list of indexes and locks them while open
 * 
 * @author Alison Callahan
 * @author Glen Newton
 *
 */
public class OpenIndexesSingleton {
	static Map<String, IndexReader>readerMap = new HashMap<String, IndexReader>(2);

    private OpenIndexesSingleton()
    {

    }

    static java.util.concurrent.locks.Lock lock = new ReentrantLock();

    public static IndexReader openIndex(String key,
					String indexDir)
	throws IOException
    {
	lock.lock();
	try
	    {
		if(!readerMap.containsKey(key))
		    openIndexInternal(key, indexDir);
	    }
	finally
	    {
		lock.unlock();
	    }
	return readerMap.get(key);
    }


    public static IndexReader openIndex(String key,
				 String indexDir[])
	throws IOException
    {
	lock.lock();
	try
	    {
		if(!readerMap.containsKey(key))
		    {
			IndexReader[] readers = new IndexReader[indexDir.length];
			for(int i=0; i<indexDir.length; i++)
			    {
				System.out.println("\tOpenIndexesSingleton: lucene opening: " 
						   + indexDir[i]);
				
				readers[i] = IndexReader.open(FSDirectory.open(new File(indexDir[i])), true); 
			    }
			MultiReader mr = new MultiReader(readers);
			readerMap.put(key, mr);
		    }
	    } 
	finally
	    {
		lock.unlock();
	    }
	return readerMap.get(key);
    }



    static void openIndexInternal(String key, String indexDir)
	throws IOException
    {
	IndexReader tir = IndexReader.open(FSDirectory.open(new File(indexDir)), true); 
	readerMap.put(key, tir);
    }

}
