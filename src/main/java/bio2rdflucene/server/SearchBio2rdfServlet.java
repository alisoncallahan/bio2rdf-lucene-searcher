package bio2rdflucene.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.Version;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import bio2rdflucene.shared.NameSpaceCount;
import bio2rdflucene.shared.OpenIndexesSingleton;
import bio2rdflucene.shared.Bio2RDFLuceneIndexSearcher;
import bio2rdflucene.shared.Triple;

/**
 * This servlet searches Bio2RDF as indexed by Lucene
 * 
 * @author Alison Callahan
 * @author Glen Newton
 */
public class SearchBio2rdfServlet extends HttpServlet {
	String DATA = "data";
	String NS = "ns";
	// CGI Parameters
	static String AsTextParameter = "asText";
	static String NameSpaceParameter = "ns";
	static String QueryParameter = "query";
	static String FormatParameter = "format";
	static String NumHitsWantedParameter = "num";

	static IndexReader datasetReader = null;
	static IndexReader registryReader = null;
	static IndexSearcher searcher = null;
	static IndexSearcher registrySearcher = null;
	static Analyzer analyzer = null;

	static ArrayList<String> localNSList = new ArrayList<String>();

	static String datasetIndex = "./indexes/bio2rdfindex";
	static String DatasetIndexInitParameter = "datasetIndex";

	static String registryIndex = "./indexes/registryindex";
	static String RegistryIndexInitParameter = "registryIndex";

	static String NSListInitParameter = "nsList";
	static String nsList = null;

	final static String[] fields = { Bio2RDFLuceneIndexSearcher.xns, Bio2RDFLuceneIndexSearcher.xsubject,
			Bio2RDFLuceneIndexSearcher.xobject };

	static int DefaultHitsWanted = 5;
	static int MaxNumHitsWanted = 500;
	Bio2RDFLuceneIndexSearcher srch = new Bio2RDFLuceneIndexSearcher();

	public SearchBio2rdfServlet() {

	}

	public enum Format {
		JSON, XML
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		Format format = Format.XML;
		String mimeType = "text/xml";

		if (req.getParameter(FormatParameter) != null
				&& req.getParameter(FormatParameter).toLowerCase()
						.equals("json")) {
			format = Format.JSON;
			mimeType = "application/json";
		}
		boolean AsText = false;
		String nameSpace = null;
		String query = null;
		if (req.getParameter(AsTextParameter) != null)
			AsText = true;

		String nsP = req.getParameter(NameSpaceParameter);
		if (nsP != null)
			nameSpace = nsP;
		String qP = req.getParameter(QueryParameter);
		if (qP != null)
			query = qP;

		if (!AsText)
			res.setContentType(mimeType);
		PrintWriter out = res.getWriter();
		/**
		 * if(query == null) { out.println(
		 * "<results><bio2rdfHits><result><uri></uri><name>No results found.</name><definition></definition></result></bio2rdfHits><nsHits><nsHit><ns>All namespaces [ns]</ns><count>0</count></nsHit></nsHits></results>"
		 * ); out.close(); return; }
		 **/

		int hitsWanted = DefaultHitsWanted;
		String hitsWantedP = req.getParameter(NumHitsWantedParameter);
		if (hitsWantedP != null)
			hitsWanted = Integer.parseInt(hitsWantedP);
		if (hitsWanted > MaxNumHitsWanted)
			hitsWanted = MaxNumHitsWanted;

		try {
			List<Triple> tResults = new ArrayList<Triple>(hitsWanted);
			int numHits = srch.getSearchResults(searcher, analyzer, tResults,
					nameSpace, fields, query, hitsWanted);

			if (numHits == 0) {
				out.println("<results><bio2rdfHits><result><uri></uri><label>No results found.</label><type>N/A</type><definition>N/A</definition></result></bio2rdfHits><nsHits><nsHit><ns>All namespaces [ns]</ns><count>0</count></nsHit></nsHits></results>");
				out.close();
				return;
			}

			if (format == Format.XML)
				out.println("<results>");

			List<String> nslist = null;
			if (nameSpace != null) {
				nslist = Arrays.asList(nameSpace);
			} else if (nameSpace == null) {
				nslist = localNSList;
			}

			List<NameSpaceCount> nsCounts = new ArrayList<NameSpaceCount>();
			srch.getNSCounts(registrySearcher, searcher, analyzer, nsCounts,
					nslist, fields, query, hitsWanted);

			makeOutput(out, format, tResults, nsCounts);
			// makeNSOutput(out, format, nsCounts);

			if (format == Format.XML)
				out.println("</results>");
		} catch (Throwable t) {
			out.println("<internalError>" + t.toString() + "</internalError>");
		} finally {
			out.close();
		}
	}

	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		String datasetIndexInit = getServletConfig().getInitParameter(
				DatasetIndexInitParameter);
		if (datasetIndexInit != null)
			datasetIndex = datasetIndexInit;

		String registryIndexInit = getServletConfig().getInitParameter(
				RegistryIndexInitParameter);
		if (registryIndexInit != null)
			registryIndex = registryIndexInit;

		String nsListInit = getServletConfig().getInitParameter(
				NSListInitParameter);
		if (nsListInit != null)
			nsList = nsListInit;
		try {
			localInit();
		} catch (IOException t) {
			t.printStackTrace();
			throw new ServletException("Failed in localInit()");
		}
	}

	public void localInit() throws IOException {
		// datasetReader = IndexReader.open(FSDirectory.open(new
		// File(datasetIndex)), true); //searching, so read-only=true
		System.out.println("SearchBio2rdfServlet: web.xml: "
				+ DatasetIndexInitParameter + "=[" + datasetIndex + "]");

		System.out.println("SearchBio2rdfServlet: web.xml: "
				+ RegistryIndexInitParameter + "=[" + registryIndex + "]");

		String[] dsi = parseByCommas(datasetIndex);
		for (int i = 0; i < dsi.length; i++)
			System.out.println("  SearchBio2rdfServlet: datasetIndex " + i
					+ ":" + dsi[i]);

		// datasetReader = OpenIndexesSingleton.openIndex(DATA, datasetIndex);
		datasetReader = OpenIndexesSingleton.openIndex(DATA, dsi);
		IndexReader[] dreaders = datasetReader.getSequentialSubReaders();
		for (int i = 0; i < dreaders.length; i++)
			System.out
					.println("   SearchBio2rdfServlet: datasetIndex Lucene readers:"
							+ i + ": " + dreaders[i].directory());

		String[] ri = parseByCommas(registryIndex);
		for (int i = 0; i < ri.length; i++)
			System.out.println("  SearchBio2rdfServlet: registryIndex " + i
					+ ":" + ri[i]);

		//
		// ri[0] = registryIndex;
		// registryReader = IndexReader.open(FSDirectory.open(new
		// File(registryIndex)), true); //searching, so read-only=true
		registryReader = OpenIndexesSingleton.openIndex(NS, ri);
		IndexReader[] readers = registryReader.getSequentialSubReaders();
		for (int i = 0; i < readers.length; i++)
			System.out
					.println("   SearchBio2rdfServlet: registryIndex Lucene readers:"
							+ i + ": " + readers[i].directory());

		// IndexReader.open(FSDirectory.open(new File(registryIndex)), true);
		// //searching, so read-only=true

		BufferedReader nsbr = new BufferedReader(new FileReader(
				new File(nsList)));

		String line;

		while ((line = nsbr.readLine()) != null) {
			String[] nsl = parseByCommas(line);
			for (int z = 0; z < nsl.length; z++) {
				localNSList.add(nsl[z]);
			}
		}

		searcher = new IndexSearcher(datasetReader);
		registrySearcher = new IndexSearcher(registryReader);
		
		analyzer = new StandardAnalyzer(Version.LUCENE_35);

	}

	public void done() throws IOException {
		datasetReader.close();
		registryReader.close();
	}

	public static void main(String[] args) throws Exception {

		SearchBio2rdfServlet sb2s = new SearchBio2rdfServlet();
		sb2s.localInit();

		try {
			sb2s.done();
		} catch (Exception e) {
			// System.err.println(e);
		}// catch
	}// main

	// /////////////////
	void makeOutput(PrintWriter out, Format format, List<Triple> tResults,
			List<NameSpaceCount> nsCounts) {

		if (format == Format.JSON) {
			out.println(makeJSONOutput(tResults, nsCounts));
		} else
			makeXMLOutput(out, tResults, nsCounts);

	}

	void makeXMLOutput(PrintWriter out, List<Triple> tResults,
			List<NameSpaceCount> nsCounts) {
		out.println(makeXMLContentOutput(tResults));
		out.println(makeXMLNSOutput(nsCounts));
	}

	String makeXMLContentOutput(List<Triple> tResults) {
		StringBuilder sb = new StringBuilder();
		sb.append("\t<bio2rdfHits>");

		for (Triple t : tResults) {
			sb.append("\n\t\t<result>");
			sb.append("\n\t\t\t<uri>" + t.uri + "</uri>");
			sb.append("\n\t\t\t<label>" + t.name + "</label>");
			sb.append("\n\t\t\t<type>" + t.type + "</type>");
			sb.append("\n\t\t\t<definition>" + t.definition + "</definition>");
			sb.append("\n\t\t</result>");
		}
		sb.append("\n\t</bio2rdfHits>");
		return sb.toString();
	}

	String makeXMLNSOutput(List<NameSpaceCount> nsCounts) {
		StringBuilder sb = new StringBuilder();
		sb.append("\t<nsHits>");

		for (NameSpaceCount ns : nsCounts) {
			sb.append("\n\t\t<nsHit>");
			sb.append("\n\t\t\t<ns> ");
			sb.append(ns.namespace);
			sb.append(" </ns>");
			sb.append("\n\t\t\t<prefix>");
			sb.append(ns.prefix);
			sb.append("</prefix>");
			sb.append("\n\t\t\t<count> ");
			sb.append(ns.count);
			sb.append(" </count>");
			sb.append("\n\t\t</nsHit>");

		}
		sb.append("\n\t</nsHits>");
		return sb.toString();
	}

	JSONObject makeJSONOutput(List<Triple> tResults,
			List<NameSpaceCount> nsCounts) {
		JSONArray content = makeJSONContentOutput(tResults);
		JSONObject ns = makeJSONNSOutput(nsCounts);

		JSONObject results = new JSONObject();
		results.put("bio2rdfHits", content);
		results.put("nsHits", ns);

		JSONObject allResults = new JSONObject();
		allResults.put("results", results);

		return allResults;
	}

	JSONArray makeJSONContentOutput(List<Triple> tResults) {
		// JSONObject bio2rdfHits = new JSONObject();
		JSONArray results = new JSONArray();
		for (Triple t : tResults) {
			Map<String, String> result = new HashMap<String, String>();
			result.put("uri", t.uri);
			result.put("name", t.name);
			result.put("type", t.type);
			result.put("definition", t.definition);
			JSONObject z = new JSONObject();
			z.put("result", result);
			results.add(z);
		}
		// bio2rdfHits.put("results",results);

		// JSONObject root = new JSONObject();
		// root.put("bio2rdfHits", bio2rdfHits);

		// return bio2rdfHits;
		return results;
	}

	JSONObject makeJSONNSOutput(List<NameSpaceCount> nsCounts) {
		JSONArray nsHits = new JSONArray();

		for (NameSpaceCount ns : nsCounts) {
			Map<String, String> result = new HashMap<String, String>();
			result.put("ns", ns.namespace);
			result.put("count", Integer.toString(ns.count));
			JSONObject z = new JSONObject();
			z.put("nsHit", result);
			nsHits.add(z);
		}

		JSONObject root = new JSONObject();
		root.put("nsHits", nsHits);
		return root;
		// return nsHits;
	}

	void makeNSOutput(PrintWriter out, Format format,
			List<NameSpaceCount> nsCounts) {
		if (format == Format.JSON)
			makeJSONNSOutput(out, nsCounts);
		else
			makeXMLNSOutput(out, nsCounts);
	}

	void makeJSONNSOutput(PrintWriter out, List<NameSpaceCount> nsCounts) {

	}

	void makeXMLNSOutput(PrintWriter out, List<NameSpaceCount> nsCounts) {
		out.println("\t<nsHits>");

		for (NameSpaceCount nsCount : nsCounts) {
			out.println("\t\t<nsHit>");
			out.print("\t\t\t<ns>");
			out.print(nsCount.namespace);
			out.println("</ns>");
			out.print("\t\t\t<prefix>");
			out.print(nsCount.prefix);
			out.println("</prefix>");
			out.print("\t\t\t<count>");
			out.print(nsCount.count);
			out.println("</count>");
			out.println("\t\t</nsHit>");
		}

		out.println("\t</nsHits>");
	}

	String[] parseByCommas(String ind) {
		ind = ind.trim();
		String[] s;
		// Single index
		if (!ind.contains(",")) {
			s = new String[1];
			s[0] = ind;
		} else {
			s = ind.split(",");
			for (int i = 0; i < s.length; i++)
				s[i] = s[i].trim();
		}

		return s;
	}

}