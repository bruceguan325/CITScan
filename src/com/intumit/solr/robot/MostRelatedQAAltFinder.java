package com.intumit.solr.robot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.IndexSchema;

import com.intumit.solr.SearchManager;

public class MostRelatedQAAltFinder {

	private final static String SEARCH_FIELD = "QUESTION_ALT_mt";

	private IndexSchema getSchema(String coreName) {
		SolrCore core = SearchManager.getLocalCores().getCore(coreName);
		return core.getLatestSchema();
	}

	private Analyzer getIndexAnalyzer(String coreName) {
		return getSchema(coreName).getIndexAnalyzer();
	}

	private Analyzer getQueryAnalyzer(String coreName) {
		return getSchema(coreName).getQueryAnalyzer();
	}

	private Similarity getSimilarity(String coreName) {
		return getSchema(coreName).getSimilarity();
	}

	private Path createTempDirectory() throws IOException {
		return Files.createTempDirectory(getClass().getName());
	}

	private Directory buildIndexDirectory(Path path) throws IOException {
		return FSDirectory.open(path.toFile());
	}

	private void index(String coreName, Directory indexDirectory, String[] qaAlts) throws IOException {
		IndexWriterConfig iwc = new IndexWriterConfig(Version.LATEST, getIndexAnalyzer(coreName));
		iwc.setOpenMode(OpenMode.CREATE);

		IndexWriter writer = new IndexWriter(indexDirectory, iwc);
		for (String qaAlt : qaAlts) {
			Document doc = new Document();
			doc.add(new TextField(SEARCH_FIELD, qaAlt, Store.YES));
			writer.addDocument(doc);
		}
		writer.close();
	}

	private TopDocs search(String coreName, Directory indexDirectory, String question, int n) throws IOException {
		IndexReader reader = DirectoryReader.open(indexDirectory);
		IndexSearcher searcher = new IndexSearcher(reader);
		searcher.setSimilarity(getSimilarity(coreName));

		QueryParser parser = new QueryParser(SEARCH_FIELD, getQueryAnalyzer(coreName));
		Query q;
		try {
			q = parser.parse(question);
		} catch (ParseException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e);
		}
		return searcher.search(q, n);
	}

	private List<RelatedQAAltSearchResult> toRelatedQaAltSearchResults(TopDocs topDocs, String[] qaAlts) {
		List<RelatedQAAltSearchResult> result = new ArrayList<>();
		for (ScoreDoc doc : topDocs.scoreDocs) {
			RelatedQAAltSearchResult altResult = new RelatedQAAltSearchResult();
			altResult.setText(qaAlts[doc.doc]);
			altResult.setScore(doc.score);
			result.add(altResult);
		}
		return result;
	}

	public List<RelatedQAAltSearchResult> search(String coreName, String question, String[] qaAlts, int n) {
		Path tempDirectoryPath = null;
		try {
			tempDirectoryPath = createTempDirectory();
			Directory indexDirectory = buildIndexDirectory(tempDirectoryPath);
			index(coreName, indexDirectory, qaAlts);
			TopDocs topDocs = search(coreName, indexDirectory, question, n);
			return toRelatedQaAltSearchResults(topDocs, qaAlts);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (tempDirectoryPath != null) {
				tempDirectoryPath.toFile().delete();
			}
		}
		return null;
	}

	public List<RelatedQAAltSearchResult> search(String coreName, String question, String[] qaAlts) {
		return search(coreName, question, qaAlts, Integer.MAX_VALUE);
	}
}
