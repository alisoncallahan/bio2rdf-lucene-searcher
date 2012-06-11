package bio2rdflucene.shared;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.FilteredQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.util.Version;

/**
 * This class searches Bio2RDF as indexed by Lucene for use by the
 * SearchBio2rdfServlet
 * 
 * @author Alison Callahan
 * @author Glen Newton
 */

public class Bio2RDFLuceneIndexSearcher {
	public static String xpredicate = "predicate";
	public static String xsubject = "subject";
	public static String xobject = "object";
	public static String xns = "ns";

	static String obo_definition = "http://bio2rdf.org/obo:def";
	static String namespace_prefix = "http://bio2rdf.org/serv:namespace_prefix";
	static String rdfs_label = "http://www.w3.org/2000/01/rdf-schema#label";
	static String rdfs_subclassof = "http://www.w3.org/2000/01/rdf-schema#subClassOf";
	static String rdf_type = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
	static String dc_title = "http://purl.org/dc/terms/title";

	// List<String> nsList = null;

	public Bio2RDFLuceneIndexSearcher() {

	}

	public int getSearchResults(IndexSearcher searcher, Analyzer analyzer,
			List<Triple> triples, String ns, String[] fields,
			String queryString, Integer numResults) throws Exception {
		
		QueryParser parser = new QueryParser(Version.LUCENE_35, xobject,
				analyzer);

		Query query = makeQueryString(parser, ns, fields, queryString);
		System.out.println("getSearchResults Query=" + query.toString());

		TopScoreDocCollector collector = TopScoreDocCollector.create(
				numResults, true);

		searcher.search(query, collector);

		Integer hitCount = collector.getTotalHits();

		ScoreDoc[] topHits = collector.topDocs().scoreDocs;

		List<String> topResults = new ArrayList<String>();
		List<Float> topScores = new ArrayList<Float>();

		for (int i = 0; i < topHits.length; i++) {

			Document doc = searcher.doc(topHits[i].doc);
			String subject = doc.get(xsubject);

			if (topResults.contains(subject) == false) {
				{
					topResults.add(subject);
					topScores.add(new Float(topHits[i].score));
				}
			}// if
		}// for

		if (topResults.size() == 0) {
			return 0;
		}

		if (hitCount <= 5) {
			hitCount = topResults.size();
		}

		if (hitCount > 5 && topResults.size() < 5) {

			TopScoreDocCollector newCollector = TopScoreDocCollector.create(50,
					true);
			searcher.search(query, newCollector);
			ScoreDoc[] newHits = newCollector.topDocs().scoreDocs;

			for (int m = 0; m < newHits.length; m++) {
				Document newDoc = searcher.doc(newHits[m].doc);
				String newSubject = newDoc.get(xsubject);
				if (topResults.contains(newSubject) == false
						&& topResults.size() < 5) {
					topResults.add(newSubject);
				}// if
			}// for
		}// if

		for (int j = 0; j < topResults.size(); j++) {
			/*
			 * QueryParser defparser = new QueryParser(Version.LUCENE_35,
			 * xpredicate, analyzer);
			 * 
			 * Query predquery =
			 * defparser.parse(QueryParser.escape(obo_definition)); Query
			 * namequery = defparser.parse(QueryParser.escape(rdfs_label));
			 * Query typequery = defparser.parse(QueryParser.escape(rdf_type));
			 */

			// using term query to search on predicate because regular
			// QueryParser
			// interprets URL symbols as special search symbols
			TermQuery labelquery = new TermQuery(new Term(xpredicate,
					rdfs_label));
			TermQuery typequery = new TermQuery(new Term(xpredicate, rdf_type));
			TermQuery defquery = new TermQuery(new Term(xpredicate,
					obo_definition));

			Filter labelfilter = new QueryWrapperFilter(labelquery);
			Filter typefilter = new QueryWrapperFilter(typequery);
			Filter deffilter = new QueryWrapperFilter(defquery);

			PrefixQuery subjquery = new PrefixQuery(new Term(xsubject,
					topResults.get(j)));

			Query labelfilteredquery = new FilteredQuery(subjquery, labelfilter);
			Query typefilteredquery = new FilteredQuery(subjquery, typefilter);
			Query deffilteredquery = new FilteredQuery(subjquery, deffilter);

			TopScoreDocCollector labelcollector = TopScoreDocCollector.create(
					1, true);
			TopScoreDocCollector typecollector = TopScoreDocCollector.create(1,
					true);
			TopScoreDocCollector defcollector = TopScoreDocCollector.create(1,
					true);

			searcher.search(deffilteredquery, defcollector);

			String definition = new String();
			Integer defHitsCount = defcollector.getTotalHits();
			if (defHitsCount != 0) {
				ScoreDoc[] defhits = defcollector.topDocs().scoreDocs;
				Document defdoc = searcher.doc(defhits[0].doc);
				definition = defdoc.get(xobject);
			} else {
				definition = "No definition found.";
			}

			Document namedoc = null;
			String subject = topResults.get(j);
			String name = "No name found.";

			searcher.search(labelfilteredquery, labelcollector);

			ScoreDoc[] namehits = labelcollector.topDocs().scoreDocs;
			if (namehits.length != 0) {

				namedoc = searcher.doc(namehits[0].doc);
				subject = namedoc.get(xsubject);
				name = namedoc.get(xobject);

			}

			searcher.search(typefilteredquery, typecollector);

			String type = new String();
			Integer typeHitsCount = typecollector.getTotalHits();
			if (typeHitsCount != 0) {
				ScoreDoc[] typehits = typecollector.topDocs().scoreDocs;
				Document typedoc = searcher.doc(typehits[0].doc);
				type = typedoc.get(xobject);
				TermQuery typesubjquery = new TermQuery(
						new Term(xsubject, type));
				Query labelfilteredquery2 = new FilteredQuery(typesubjquery,
						labelfilter);

				TopScoreDocCollector typenamecollector = TopScoreDocCollector
						.create(1, true);
				searcher.search(labelfilteredquery2, typenamecollector);
				Integer typenameHitsCount = typenamecollector.getTotalHits();
				if (typenameHitsCount != 0) {
					ScoreDoc[] typenamehits = typenamecollector.topDocs().scoreDocs;
					Document typenamedoc = searcher.doc(typenamehits[0].doc);
					type = typenamedoc.get(xobject);
				}
			} else {
				TermQuery scquery = new TermQuery(new Term(xpredicate,
						rdfs_subclassof));

				org.apache.lucene.search.Filter scfilter = new QueryWrapperFilter(
						scquery);
				Query scfilteredquery = new FilteredQuery(subjquery, scfilter);
				TopScoreDocCollector sccollector = TopScoreDocCollector.create(
						1, true);
				searcher.search(scfilteredquery, sccollector);
				Integer scHitsCount = sccollector.getTotalHits();
				if (scHitsCount != 0) {
					ScoreDoc[] schits = sccollector.topDocs().scoreDocs;
					Document scdoc = searcher.doc(schits[0].doc);
					type = scdoc.get(xobject);
					PrefixQuery typesubjquery = new PrefixQuery(new Term(
							xsubject, type));
					Query namefilteredquery2 = new FilteredQuery(typesubjquery,
							labelfilter);
					TopScoreDocCollector typenamecollector = TopScoreDocCollector
							.create(1, true);
					searcher.search(namefilteredquery2, typenamecollector);
					int typenameHitsCount = typenamecollector.getTotalHits();
					if (typenameHitsCount != 0) {
						ScoreDoc[] typenamehits = typenamecollector.topDocs().scoreDocs;
						Document typenamedoc = searcher
								.doc(typenamehits[0].doc);
						type = typenamedoc.get(xobject);
					}// if
				} else {
					type = "No type found.";
				}// else
			}// else

			name = removeChar(name, '\"');
			type = removeChar(type, '\"');
			definition = removeChar(definition, '\"');

			triples.add(new Triple(subject, name.trim(), type.trim(),
					definition.trim()));
		}// for

		return topResults.size();

	}// getSearchResults

	public int getNSCounts(IndexSearcher registrySearcher,
			IndexSearcher searcher, Analyzer analyzer,
			List<NameSpaceCount> nsCounts, List<String> ns, String[] fields,
			String queryString, int numResults) throws Exception {

		TreeMap<Integer, String> nsHitCounts = new TreeMap<Integer, String>(
				new ValueComparator());

		for (int n = 0; n < ns.size(); n++) {

			String namespace = ns.get(n);

			QueryParser nsParser = new QueryParser(Version.LUCENE_35, xsubject,
					analyzer);

			Query nsQuery = makeQueryString(nsParser, namespace, fields,
					queryString);

			TopScoreDocCollector allCollector = TopScoreDocCollector.create(
					new Integer(numResults), true);

			searcher.search(nsQuery, allCollector);

			Integer totalHitCount = allCollector.getTotalHits();

			ScoreDoc[] nstopHits = allCollector.topDocs().scoreDocs;

			List<String> nstopResults = new ArrayList<String>();
			for (int i = 0; i < nstopHits.length; i++) {

				Document nsdoc = searcher.doc(nstopHits[i].doc);
				String nssubject = nsdoc.get(xsubject);

				if (nstopResults.contains(nssubject) == false) {
					nstopResults.add(nssubject);
				}// if
			}// for

			if (totalHitCount <= 5) {
				totalHitCount = nstopResults.size();
			}

			QueryParser predicateParser = new QueryParser(Version.LUCENE_35,
					xpredicate, analyzer);

			/******************* FIRST GET NS ID WHERE NS MATCHES namespace *********************/

			Query prefixQuery = predicateParser.parse(QueryParser
					.escape(namespace_prefix));

			org.apache.lucene.search.Filter prefixFilter = new QueryWrapperFilter(
					prefixQuery);

			Query objectQuery = new TermQuery(new Term(xobject, namespace));

			Query prefixFilteredquery = new FilteredQuery(objectQuery,
					prefixFilter);

			TopScoreDocCollector prefixCollector = TopScoreDocCollector.create(
					1, true);

			registrySearcher.search(prefixFilteredquery, prefixCollector);

			ScoreDoc[] prefixHits = prefixCollector.topDocs().scoreDocs;
			if (prefixHits.length > 0) {
				Document prefixDoc = registrySearcher.doc(prefixHits[0].doc);

				String nameSpaceId = prefixDoc.get(xsubject);

				/******* NOW GET DC:TITLE OF SUBJECT *********/

				TermQuery titleQuery = new TermQuery(new Term(xpredicate,
						dc_title));

				// Query titleQuery =
				// predicateParser.parse(QueryParser.escape(dc_title));

				org.apache.lucene.search.Filter titleFilter = new QueryWrapperFilter(
						titleQuery);

				Query subjectQuery = new TermQuery(new Term(xsubject,
						nameSpaceId));

				Query titleFilteredquery = new FilteredQuery(subjectQuery,
						titleFilter);

				TopScoreDocCollector titleCollector = TopScoreDocCollector
						.create(1, true);

				registrySearcher.search(titleFilteredquery, titleCollector);

				ScoreDoc[] titleHits = titleCollector.topDocs().scoreDocs;

				Document titleDoc = registrySearcher.doc(titleHits[0].doc);

				String nameSpaceObject = titleDoc.get(xobject);

				String nameSpaceDescriptor = nameSpaceObject.substring(0,
						nameSpaceObject.length() - 2).trim();

				if (totalHitCount != 0) {
					nsHitCounts.put(totalHitCount, nameSpaceDescriptor + " ["
							+ namespace + "]");
				}// if
			} // if
		}// for

		if (nsHitCounts.size() == 0) {
			/*
			 * return "<nsHits>" + "\n\t<nsHit>" +
			 * "\n\t\t<ns>All namespaces [ns]</ns>" + "\n\t\t<count>0</count>" +
			 * "\n\t</nsHit>" + "\n</nsHits>";
			 */
			return 0;
		}

		if (nsHitCounts.size() > 5) {
			for (int m = 0; m < 5; m++) {
				Map.Entry<Integer, String> entry = nsHitCounts.pollFirstEntry();
				int count = (Integer) entry.getKey();
				String nsDesc = (String) entry.getValue();
				nsCounts.add(new NameSpaceCount(nsDesc, count));
			}// for
		} else if (0 < nsHitCounts.size() && nsHitCounts.size() < 6) {
			for (Map.Entry<Integer, String> entry : nsHitCounts.entrySet()) {
				int count = (Integer) entry.getKey();
				String nsDesc = (String) entry.getValue();
				nsCounts.add(new NameSpaceCount(nsDesc, count));
			}// for
		} else if (nsHitCounts.size() == 0) {
			/*
			 * nsCountXML = nsCountXML + "\n\t<nsHit>" +
			 * "\n\t\t<ns>No results found. </ns>" + "\n\t\t<count>0</count>" +
			 * "\n\t</nsHit>";
			 */
			return 0;
		}// elseif

		return nsHitCounts.size();

	}// getMultipleNSCounts

	Query makeQueryString(QueryParser parser, String ns, String[] fields,
			String queryString) throws ParseException {
		StringBuffer q = new StringBuffer();

		if (ns != null)
			q.append(xns + ":" + ns + " AND ");
		String quotes = "";
		if (!queryString.contains("\""))
			quotes = "\"";
		if (queryString.contains("*")) {
			q.append("(" + xsubject + ":" + queryString + " OR " + xobject
					+ ":" + queryString + ")");
		} else {
			q.append("(" + xsubject + ":" + quotes + queryString + quotes
					+ " OR " + xobject + ":" + quotes + queryString + quotes
					+ ")");
		}// else
		System.out.println("Query string: " + q);
		System.out.println(parser.parse(q.toString()));
		//return parser.parse(q.toString());
		return parser.parse("ns:sgd +(subject:gal2p object:gal2p)");
	}//

	public static String removeChar(String s, char c) {
		String r = "";
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) != c)
				r += s.charAt(i);
		}
		return r;
	}// removeChar
}
