package bio2rdflucene;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.junit.Test;

import bio2rdflucene.shared.Bio2RDFLuceneIndexSearcher;
import bio2rdflucene.shared.Triple;

public class TestSearch {

	@Test
	public void test() {
		
		IndexReader reader;
		IndexSearcher dataSearcher;
		Bio2RDFLuceneIndexSearcher searcher;
		List<Triple> results = new ArrayList<Triple>();
		String[] fields = {Bio2RDFLuceneIndexSearcher.xns, Bio2RDFLuceneIndexSearcher.xsubject, Bio2RDFLuceneIndexSearcher.xobject };

		try {
			reader = IndexReader.open(FSDirectory.open(new File("/home/alison/sgd_index")), true);
			dataSearcher = new IndexSearcher(reader);
			searcher = new  Bio2RDFLuceneIndexSearcher();
			StandardAnalyzer sAnalyzer = new StandardAnalyzer(Version.LUCENE_35);
			
			searcher.getSearchResults(dataSearcher, sAnalyzer, results, "sgd", fields, "gal2p", 5);

			System.out.println("***** Results *****");
			Iterator<Triple> itr = results.iterator();
			while(itr.hasNext()){
				Triple result = itr.next();
				System.out.println(result.uri+ " "+result.name);
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
