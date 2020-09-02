package com.intumit.solr.robot.qadialog;

import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import com.intumit.solr.util.WiSeUtils;

public class SearchableHelper {

	public boolean applySearchCriteria(RuleCheckResult result, SolrQuery query, String searchType, String queryType, String targetField, boolean partial) {
		if ("filterQuery".equals(searchType)) {
			if ("boolean".equals(queryType)) {
				query.addFilterQuery(targetField
						+ ":"
						+ (partial ? result.getValue() : WiSeUtils
								.dblQuote(result.getValue())));

				return true;
			}
		}
		return false;
	}

	public boolean applySearchCriteria(String value, SolrQuery query, String searchType, String queryType, String targetField, boolean partial) {
		if ("filterQuery".equals(searchType)) {
			if ("boolean".equals(queryType)) {
				query.addFilterQuery(targetField + ":" + (partial ? value : WiSeUtils.dblQuote(value)));

				return true;
			}
		}
		return false;
	}

	public boolean applySearchCriteria(String value, SolrDocumentList docs, String searchType, String queryType, final String targetField, boolean partial) {
		if ("filterQuery".equals(searchType)) {
			if ("boolean".equals(queryType)) {
				Iterator<SolrDocument> docItr = docs.iterator();
				boolean dirty = false;

				while (docItr.hasNext()) {
					SolrDocument doc = docItr.next();

					String targetFieldValue = (String) doc.getFieldValue(targetField);
					if ((targetFieldValue == null || value == null) && (targetFieldValue != value)) {
						docItr.remove();
						dirty = true;
					}
					else if (targetFieldValue == value) {
						continue;
					}
					else if (partial) {
						if (StringUtils.indexOfIgnoreCase(value,
								targetFieldValue) == -1
								&& !Pattern.matches(".*?" + value
										+ ".*?", targetFieldValue)) {
							docItr.remove();
							dirty = true;
						}
					} else {
						if (!StringUtils.equalsIgnoreCase(targetFieldValue, value)
								&& !Pattern.matches(value, targetFieldValue)) {
							docItr.remove();
							dirty = true;
						}
					}
				}
				return dirty;
			} else if ("prefixWildcard".equals(queryType)) {
				Iterator<SolrDocument> docItr = docs.iterator();
				boolean dirty = false;

				while (docItr.hasNext()) {
					SolrDocument doc = docItr.next();
					String targetFieldValue = (String) doc.getFieldValue(targetField);

					if ((targetFieldValue == null || value == null) && (targetFieldValue != value)) {
						docItr.remove();
						dirty = true;
					}
					else if (targetFieldValue == value) {
						continue;
					}
					else if (!StringUtils.endsWithIgnoreCase(targetFieldValue, value) 
							&& !Pattern.matches(".*?" + value , targetFieldValue)) {
						docItr.remove();
						dirty = true;
					}
				}
				return dirty;
			}
		}
		else if ("sort".equals(searchType)) {
			TreeSet<SolrDocument> sorting = new TreeSet<>(new Comparator() {
				@Override
				public int compare(Object o1, Object o2) {
					SolrDocument d1 = (SolrDocument)o1;
					SolrDocument d2 = (SolrDocument)o2;
					
					Object date1 = (Object)d1.getFirstValue(targetField);
					Object date2 = (Object)d2.getFirstValue(targetField);
					if (date1 == null) return 1;
					if (date2 == null) return -1;
					
					if (date1 instanceof Long) {
						return ((long)date1 < (long)date2) ? 1 : -1;
					}
					
					if (date1 instanceof Date) {
						return ((Date)date1).before((Date)date2) ? 1 : -1;
					}
					
					return 0;
				}
			});

			//sorting.addAll(docs);
			//docs.clear();
			//docs.addAll(sorting);
			return true;
		}
		return false;
	}

	public boolean applySearchCriteria(RuleCheckResult result,
			SolrDocumentList docs, String searchType, String queryType,
			final String targetField, boolean partial) {
		
		return applySearchCriteria(result.getValue(), docs, searchType, queryType, targetField, partial);
	}

}
