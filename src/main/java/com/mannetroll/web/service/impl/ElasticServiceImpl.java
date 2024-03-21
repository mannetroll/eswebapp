package com.mannetroll.web.service.impl;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.HistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.percentiles.PercentilesAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.stats.extended.ExtendedStatsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.mannetroll.metrics.LogKeys;
import com.mannetroll.servicepoints.ServicePoint;
import com.mannetroll.web.model.ArrivalCompletedResponse;
import com.mannetroll.web.model.ArrivalCompletedStatusResponse;
import com.mannetroll.web.model.EdiPressureStatusResponse;
import com.mannetroll.web.model.EdiToStartResponse;
import com.mannetroll.web.model.PopularTimesResponse;
import com.mannetroll.web.service.ElasticService;
import com.mannetroll.web.util.ServicePointUtil2;

import io.searchbox.client.JestClient;
import io.searchbox.core.Count;
import io.searchbox.core.CountResult;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.SearchResult.Hit;
import io.searchbox.core.search.aggregation.DateHistogramAggregation;
import io.searchbox.core.search.aggregation.DateHistogramAggregation.DateHistogram;
import io.searchbox.core.search.aggregation.ExtendedStatsAggregation;
import io.searchbox.core.search.aggregation.HistogramAggregation;
import io.searchbox.core.search.aggregation.HistogramAggregation.Histogram;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.PercentilesAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation.Entry;

/**
 * @author mannetroll
 */
@Service
public class ElasticServiceImpl implements ElasticService {
	private static final Logger LOG = LogManager.getLogger(ElasticServiceImpl.class);
	private static final String WEEKDAY = "Weekday";
	private static final String S_AGGS = "S_AGGS";
	private static final String HOUR = "Hour";
	private static final String MINUTE = "Minute";
	private static final String STATS = "Stats";
	private static final String LOCS = "locs";
	private static final String KPI = "kpi";
	private static final String IDREF = "idref";
	private static final String ITEMS = "items";
	private JestClient jestClient;
	private static DateTimeFormatter DAY = DateTimeFormat.forPattern("yyyy-MM-dd");
	private static DateTimeFormatter YEAR = DateTimeFormat.forPattern("yyyy.*");
	private static DateTimeFormatter MONTH = DateTimeFormat.forPattern("yyyy.MM*");
	private static final String TIMESTAMP = "@timestamp";
	private static final String SE = "SE";
	private static final int COUNT_LIMIT = 40; // minimum sample size
	private static final float STD_CUTOFF = 1.5f; // ignore high variance
	private static final float STD_THRESHOLD = 1.0f; // modify in between variance
	private static Map<String, String> countryMap = new HashMap<String, String>();
	//
	// queries
	//
	private static final TermsQueryBuilder SWE = QueryBuilders.termsQuery("consignee_address_countryCode", "SWE");
	private static final TermsQueryBuilder DELIVERED = QueryBuilders.termsQuery("status", "DELIVERED");

	static {
		DAY = DAY.withZone(DateTimeZone.forID("CET"));
		YEAR = YEAR.withZone(DateTimeZone.forID("CET"));
		MONTH = MONTH.withZone(DateTimeZone.forID("CET"));
		init2By3Country();
	}

	private static void init2By3Country() {
		String[] countries = Locale.getISOCountries();
		for (String country2 : countries) {
			Locale locale = new Locale("", country2);
			countryMap.put(country2.toUpperCase(), locale.getISO3Country().toUpperCase());
		}
	}

	public ElasticServiceImpl(JestClient jestClient) {
		this.jestClient = jestClient;
		// make a startup search
		DateTime enddate = new DateTime();
		DateTime startdate = enddate.minusDays(1);
		long gte = startdate.getMillis();
		long lte = enddate.getMillis();
		Long countItems = this.countItems(gte, lte);
		LOG.info("***** kpi: " + countItems);
	}

	@Override
	public Long countItems(long gte, long lte) {
		long start = System.currentTimeMillis();
		long totalHits = -1;
		try {
			RangeQueryBuilder range = QueryBuilders.rangeQuery(TIMESTAMP).gte(gte).lte(lte);
			String query = "{\"query\":" + range.toString() + "}";
			final Count.Builder searchBuilder = new Count.Builder();
			searchBuilder.addIndices(lastYears()).query(query);
			Count build = searchBuilder.build();
			CountResult result = jestClient.execute(build);
			if (result != null) {
				String errorMessage = result.getErrorMessage();
				if (errorMessage == null) {
					totalHits = result.getCount().longValue();
				} else {
					LOG.error("errorMessage: " + errorMessage);
				}
			} else {
				LOG.error("NULL from jestClient: " + jestClient);
			}
		} catch (IOException e) {
			LOG.error(lastYears() + ", " + e.getMessage());
		}
		long elapsed = System.currentTimeMillis() - start;
		LOG.info("elapsed: " + elapsed);
		return totalHits;
	}

	public static String toDate(Object object) {
		if (object == null) {
			return "";
		}
		if (object instanceof String) {
			return (String) object;
		} else if (object instanceof Double) {
			Double tmp = (Double) object;
			return toDate(tmp.longValue());
		} else if (object instanceof Long) {
			Long tmp = (Long) object;
			return toDate(tmp);
		}
		return "";
	}

	public static String toDate(long millis) {
		ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.of("GMT"));
		return zdt.format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	}

	public static String[] getHeadersNoKey() {
		String[] headers = new String[4];
		headers[0] = "@timestamp";
		headers[1] = "_id";
		headers[2] = "mobileReference";
		headers[3] = "emailReference";
		return headers;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public List<String[]> nokey(DateTime fromDay) {
		List<String[]> data = new ArrayList<String[]>();
		data.add(getHeadersNoKey());
		//
		QueryBuilder m1 = QueryBuilders.matchPhraseQuery("service_code", "19");
		QueryBuilder m2 = QueryBuilders.matchPhraseQuery("numberOfSubs", 0);
		QueryBuilder m3 = QueryBuilders.matchPhraseQuery("numberOfSubsEmail", 0);
		QueryBuilder m4 = QueryBuilders.matchPhraseQuery("numberOfSubsMobile", 0);
		QueryBuilder m5 = QueryBuilders.matchPhraseQuery("status", "DELIVERED");
		QueryBuilder m6 = QueryBuilders.matchPhraseQuery("numberOfItems", 1);
		QueryBuilder m7 = QueryBuilders.matchPhraseQuery("service_sourceSystem", "ELLA");
		QueryBuilder e1 = QueryBuilders.existsQuery("mobileReference");
		QueryBuilder e2 = QueryBuilders.existsQuery("emailReference");
		//
		DateTime toDay = fromDay.plusHours(24);
		BoolQueryBuilder query = QueryBuilders.boolQuery().must(m1).must(m2).must(m3).must(m4).must(m5).must(m6)
				.must(m7).must(e1).must(e2);
		QueryBuilder rangeQuery = QueryBuilders.rangeQuery(TIMESTAMP).gte(fromDay.getMillis()).lte(toDay.getMillis());
		query.must(rangeQuery);
		//
		SearchSourceBuilder builder = new SearchSourceBuilder().query(query).size(10001);
		final String queryString = builder.toString();
		LOG.debug("queryString: \r\n" + queryString + "\r\n");
		final Search.Builder searchBuilder = new Search.Builder(queryString);
		searchBuilder.addIndices(lastIdrefMonth());
		final Instant start = Instant.now();
		//
		try {
			final SearchResult execute = jestClient.execute(searchBuilder.build());
			if (execute != null) {
				SearchResult response = new ISearchResult(execute);
				String errorMessage = response.getErrorMessage();
				if (errorMessage == null) {
					final List<Hit<HashMap, Void>> hits = response.getHits(HashMap.class);
					for (Hit<HashMap, Void> hit : hits) {
						@SuppressWarnings("unchecked")
						final Map<String, Object> hitMap = hit.source;
						String[] row = new String[4];
						row[0] = (String) hitMap.get("@timestamp");
						row[1] = (String) hitMap.get("es_metadata_id");
						row[2] = (String) ((List) hitMap.get("mobileReference")).get(0);
						row[3] = (String) ((List) hitMap.get("emailReference")).get(0);
						data.add(row);
					}
					long elapsed = Duration.between(start, Instant.now()).toMillis();
					Map<String, Object> lmap = new HashMap<String, Object>();
					lmap.put(LogKeys.MESSAGE,
							"total: " + response.getTotal() + ", rows: " + data.size() + ", elapsed: " + elapsed);
					lmap.put("kpis_total", response.getTotal());
					lmap.put("kpis_elapsed", elapsed);
					lmap.put("kpis_rows", data.size());
					LOG.info(lmap);
				} else {
					LOG.error("errorMessage: " + errorMessage);
				}
			}
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
		return data;
	}

	public static List<String> lastYears() {
		List<String> indexNames = new ArrayList<String>();
		DateTime now = new DateTime();
		indexNames.add(KPI + "-" + YEAR.print(now));
		indexNames.add(KPI + "-" + YEAR.print(now.minusYears(1)));
		return indexNames;
	}

	private Collection<? extends String> lastIdrefMonth() {
		List<String> indexNames = new ArrayList<String>();
		DateTime now = new DateTime();
		indexNames.add(IDREF + "-" + MONTH.print(now));
		indexNames.add(IDREF + "-" + MONTH.print(now.minusMonths(1)));
		return indexNames;
	}

	/*
	 * MARKET
	 */

	private Collection<? extends String> lastQuarter() {
		List<String> indexNames = new ArrayList<String>();
		DateTime now = new DateTime();
		indexNames.add(KPI + "-" + MONTH.print(now));
		indexNames.add(KPI + "-" + MONTH.print(now.minusMonths(1)));
		indexNames.add(KPI + "-" + MONTH.print(now.minusMonths(2)));
		indexNames.add(KPI + "-" + MONTH.print(now.minusMonths(3)));
		return indexNames;
	}

	private Collection<? extends String> lastMonth() {
		List<String> indexNames = new ArrayList<String>();
		DateTime now = new DateTime();
		indexNames.add(KPI + "-" + MONTH.print(now));
		indexNames.add(KPI + "-" + MONTH.print(now.minusMonths(1)));
		return indexNames;
	}

	private Collection<? extends String> lastMonthItems() {
		List<String> indexNames = new ArrayList<String>();
		DateTime now = new DateTime();
		indexNames.add(ITEMS + "-" + MONTH.print(now));
		indexNames.add(ITEMS + "-" + MONTH.print(now.minusMonths(1)));
		return indexNames;
	}

	public static String[] getHeaders() {
		String[] headers = new String[5];
		headers[0] = "consignor_name";
		headers[1] = "date";
		headers[2] = "service_code";
		headers[3] = "consignee_address_postCode";
		headers[4] = "count";
		return headers;
	}

	@Override
	public List<String[]> volume(DateTime fromDay, DateTime toDay, String service, String consignor) {
		List<String[]> data = new ArrayList<String[]>();
		data.add(getHeaders());
		//
		QueryBuilder SC = QueryBuilders.termsQuery("service_code", service);
		QueryBuilder CN = QueryBuilders.matchQuery("consignor_name", consignor);
		BoolQueryBuilder query = QueryBuilders.boolQuery().must(SWE).must(DELIVERED).must(SC).must(CN);
		QueryBuilder rangeQuery = QueryBuilders.rangeQuery(TIMESTAMP).gte(fromDay.getMillis()).lte(toDay.getMillis());
		TermsAggregationBuilder s_aggs = AggregationBuilders.terms(S_AGGS).field("consignee_address_postCode")
				.size(700000);
		DateHistogramAggregationBuilder d_aggs = AggregationBuilders.dateHistogram(WEEKDAY).field(TIMESTAMP)
				.dateHistogramInterval(DateHistogramInterval.DAY).timeZone(DateTimeZone.forID("GMT")).subAggregation(s_aggs);
		query.must(rangeQuery);
		//
		SearchSourceBuilder builder = new SearchSourceBuilder().aggregation(d_aggs).query(query).size(0);
		final String queryString = builder.toString();
		LOG.debug("queryString: \r\n" + queryString + "\r\n");
		final Search.Builder searchBuilder = new Search.Builder(queryString);
		searchBuilder.addIndices(lastYears());
		final Instant start = Instant.now();
		try {
			final SearchResult execute = jestClient.execute(searchBuilder.build());
			if (execute != null) {
				SearchResult response = new ISearchResult(execute);
				String errorMessage = response.getErrorMessage();
				if (errorMessage == null) {
					MetricAggregation aggregations = response.getAggregations();
					if (aggregations != null) {
						DateHistogramAggregation dateAggregation = aggregations.getDateHistogramAggregation(WEEKDAY);
						List<DateHistogram> dlist = dateAggregation.getBuckets();
						for (DateHistogram hist : dlist) {
							LOG.debug("date: " + ElasticServiceImpl.DAY.print(hist.getTime()) + ", count "
									+ hist.getCount());
							TermsAggregation aggregation = hist.getAggregation(S_AGGS, TermsAggregation.class);
							List<Entry> slist = aggregation.getBuckets();
							for (Entry entry : slist) {
								// LOG.info(" - " + entry.getKey() + ", " + entry.getCount());
								String[] row = new String[5];
								row[0] = consignor;
								row[1] = ElasticServiceImpl.DAY.print(hist.getTime());
								row[2] = service;
								row[3] = entry.getKey();
								row[4] = String.valueOf(entry.getCount());
								data.add(row);
							}
						}
					}
					long elapsed = Duration.between(start, Instant.now()).toMillis();
					Map<String, Object> lmap = new HashMap<String, Object>();
					int days = Days.daysBetween(fromDay, toDay).getDays();
					lmap.put(LogKeys.MESSAGE,
							"consignor: " + consignor + ", service: " + service + ", days: " + days + ", total: "
									+ response.getTotal() + ", buckets: " + data.size() + ", elapsed: " + elapsed);
					lmap.put("market_total", response.getTotal());
					lmap.put("market_elapsed", elapsed);
					lmap.put("market_buckets", data.size());
					lmap.put("market_consignor", consignor);
					lmap.put("market_days", days);
					LOG.info(lmap);
				} else {
					LOG.error("errorMessage: " + errorMessage);
				}
			}
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
		return data;
	}

	@Override
	public EdiToStartResponse edi2Start(String consignor) {
		EdiToStartResponse result = new EdiToStartResponse();
		QueryBuilder CN = QueryBuilders.matchPhraseQuery("consignor_name.keyword", consignor);
		BoolQueryBuilder query = QueryBuilders.boolQuery().must(CN);
		PercentilesAggregationBuilder s_aggs = AggregationBuilders.percentiles(S_AGGS).field("edi_to_start_minutes")
				.percentiles(50, 90);
		TermsAggregationBuilder d_aggs = AggregationBuilders.terms(WEEKDAY).field("ediEventTime_dayOfWeek")
				.subAggregation(s_aggs);
		//
		SearchSourceBuilder builder = new SearchSourceBuilder().aggregation(d_aggs).query(query).size(0);
		final String queryString = builder.toString();
		LOG.info("editostart: \r\n" + queryString + "\r\n");
		final Search.Builder searchBuilder = new Search.Builder(queryString);
		searchBuilder.addIndices(lastQuarter());
		final Instant start = Instant.now();
		try {
			final SearchResult execute = jestClient.execute(searchBuilder.build());
			if (execute != null) {
				SearchResult response = new ISearchResult(execute);
				String errorMessage = response.getErrorMessage();
				if (errorMessage == null) {
					MetricAggregation aggregations = response.getAggregations();
					if (aggregations != null) {
						TermsAggregation ta = aggregations.getTermsAggregation(WEEKDAY);
						List<Entry> elist = ta.getBuckets();
						for (Entry entry : elist) {
							String day = entry.getKey();
							Map<String, Object> map = result.getWeekDay().get(day);
							if (map == null) {
								map = new TreeMap<String, Object>();
								result.getWeekDay().put(day, map);
							}
							map.put("count", entry.getCount());
							PercentilesAggregation percentilesAggregation = entry.getPercentilesAggregation(S_AGGS);
							Map<String, Double> percentiles = percentilesAggregation.getPercentiles();
							for (String key : percentiles.keySet()) {
								Double value = (1.0 * percentiles.get(key)) / 1440;
								map.put(key.substring(0, 2), Math.round(value * 10) / 10.0);
							}
						}
					}
				}
			}
			long elapsed = Duration.between(start, Instant.now()).toMillis();
			Map<String, Object> lmap = new HashMap<String, Object>();
			lmap.put(LogKeys.MESSAGE, "consignor: " + consignor + ", elapsed: " + elapsed);
			lmap.put("consignor", consignor);
			lmap.put("elapsed", elapsed);
			LOG.info(lmap);
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
		return result;
	}

	@Override
	@Cacheable(value = ElasticService.ARRIVALCOMPLETED)
	public ArrivalCompletedResponse arrivalCompleted(String spid, Integer percentile) {
		return arrivalCompleted(spid, percentile, null, null);
	}

	@Override
	public ArrivalCompletedResponse arrivalCompleted(String spid, Integer percentile, DateTime date, Integer weeks) {
		ArrivalCompletedResponse result = new ArrivalCompletedResponse();
		result.setServicePointId(spid);
		result.setPercentile(percentile);
		ServicePoint servicePoint = ServicePointUtil2.getServicePoint(spid, SE);
		if (servicePoint != null) {
			result.setName(servicePoint.getName());
		}
		QueryBuilder range;
		if (date == null) {
			result.setWeeks(8);
			range = QueryBuilders.rangeQuery(TIMESTAMP).gte("now-57d").lte("now");
		} else {
			result.setWeeks(weeks);
			DateTime minusWeeks = date.minusWeeks(weeks).minusDays(1);
			range = QueryBuilders.rangeQuery(TIMESTAMP).gte(minusWeeks.getMillis()).lte(date.getMillis());
		}
		QueryBuilder sp = QueryBuilders.termsQuery("arrivalCompleted_locationType", "SERVICE_POINT");
		QueryBuilder id = QueryBuilders.termsQuery("arrivalCompleted_locationId", spid);
		QueryBuilder se = QueryBuilders.termsQuery("arrivalCompleted_countryCode.keyword", "SWE");
		BoolQueryBuilder query = QueryBuilders.boolQuery().filter(range).must(sp).must(id).must(se);
		//
		PercentilesAggregationBuilder minute = AggregationBuilders.percentiles(MINUTE)
				.field("arrivalCompleted_minuteOfDay").percentiles(percentile);
		ExtendedStatsAggregationBuilder stats = AggregationBuilders.extendedStats(STATS)
				.field("arrivalCompleted_hourOfDayFloat");
		TermsAggregationBuilder d_aggs = AggregationBuilders.terms(WEEKDAY).field("arrivalCompleted_dayOfWeek")
				.order(BucketOrder.key(true)).size(7).subAggregation(minute).subAggregation(stats);
		//
		SearchSourceBuilder builder = new SearchSourceBuilder().aggregation(d_aggs).query(query).size(0);
		final String queryString = builder.toString();
		LOG.debug("arrivalcompleted: \r\n" + queryString + "\r\n");
		final Search.Builder searchBuilder = new Search.Builder(queryString);
		searchBuilder.addIndices(lastYears());
		final Instant start = Instant.now();
		try {
			final SearchResult execute = jestClient.execute(searchBuilder.build());
			if (execute != null) {
				SearchResult response = new ISearchResult(execute);
				String errorMessage = response.getErrorMessage();
				if (errorMessage == null) {
					MetricAggregation aggregations = response.getAggregations();
					if (aggregations != null) {
						TermsAggregation ta = aggregations.getTermsAggregation(WEEKDAY);
						List<Entry> elist = ta.getBuckets();
						for (Entry entry : elist) {
							String day = entry.getKey();
							List<Map<String, Object>> arrivalCompleted = result.getArrivalCompleted();
							PercentilesAggregation percentilesAggregation = entry.getPercentilesAggregation(MINUTE);
							ExtendedStatsAggregation extendedStatsAggregation = entry
									.getExtendedStatsAggregation(STATS);
							Map<String, Double> percentiles = percentilesAggregation.getPercentiles();
							for (String key : percentiles.keySet()) {
								Long count = entry.getCount();
								float standardDeviation = extendedStatsAggregation.getStdDeviation().floatValue();
								if (count > COUNT_LIMIT && standardDeviation < STD_CUTOFF) {
									Map<String, Object> map = new LinkedHashMap<String, Object>();
									arrivalCompleted.add(map);
									map.put("count", entry.getCount());
									map.put("dayOfWeek", Integer.valueOf(day));
									map.put("day", getWeekday(Integer.valueOf(day)));
									long hourOfDay = Math.round(percentiles.get(key) / 60);
									if (standardDeviation > STD_THRESHOLD) {
										// add one hour if STD is high
										hourOfDay++;
									}
									map.put("hourOfDay", hourOfDay);
									map.put("hour", toClock(hourOfDay));
									// stats
									map.put("avg", extendedStatsAggregation.getAvg().floatValue());
									map.put("std", standardDeviation);
								}
							}
						}
					}
				}
			}
			long elapsed = Duration.between(start, Instant.now()).toMillis();
			Map<String, Object> lmap = new HashMap<String, Object>();
			lmap.put(LogKeys.MESSAGE, "arrivalcompleted: " + spid + ", elapsed: " + elapsed);
			lmap.put("spid", spid);
			lmap.put("elapsed", elapsed);
			LOG.info(lmap);
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
			throw new RuntimeException("Error occurred: " + e.getMessage());
		}
		return result;
	}

	@Override
	public ArrivalCompletedResponse arrivalCompletedHour(String spid) {
		ArrivalCompletedResponse result = new ArrivalCompletedResponse();
		result.setServicePointId(spid);
		ServicePoint servicePoint = ServicePointUtil2.getServicePoint(spid, SE);
		if (servicePoint != null) {
			result.setName(servicePoint.getName());
		}
		QueryBuilder range = QueryBuilders.rangeQuery(TIMESTAMP).gte("now-57d").lte("now");
		QueryBuilder sp = QueryBuilders.termsQuery("arrivalCompleted_locationType", "SERVICE_POINT");
		QueryBuilder id = QueryBuilders.termsQuery("arrivalCompleted_locationId", spid);
		QueryBuilder se = QueryBuilders.termsQuery("arrivalCompleted_countryCode.keyword", "SWE");
		BoolQueryBuilder query = QueryBuilders.boolQuery().filter(range).must(sp).must(id).must(se);
		//
		PercentilesAggregationBuilder hour = AggregationBuilders.percentiles(HOUR).field("arrivalCompleted_hourOfDay")
				.percentiles(95);
		TermsAggregationBuilder d_aggs = AggregationBuilders.terms(WEEKDAY).field("arrivalCompleted_dayOfWeek")
				.order(BucketOrder.key(true)).size(7).subAggregation(hour);
		//
		SearchSourceBuilder builder = new SearchSourceBuilder().aggregation(d_aggs).query(query).size(0);
		final String queryString = builder.toString();
		LOG.debug("arrivalcompleted: \r\n" + queryString + "\r\n");
		final Search.Builder searchBuilder = new Search.Builder(queryString);
		searchBuilder.addIndices(lastQuarter());
		final Instant start = Instant.now();
		try {
			final SearchResult execute = jestClient.execute(searchBuilder.build());
			if (execute != null) {
				SearchResult response = new ISearchResult(execute);
				String errorMessage = response.getErrorMessage();
				if (errorMessage == null) {
					MetricAggregation aggregations = response.getAggregations();
					if (aggregations != null) {
						TermsAggregation ta = aggregations.getTermsAggregation(WEEKDAY);
						List<Entry> elist = ta.getBuckets();
						for (Entry entry : elist) {
							String day = entry.getKey();
							List<Map<String, Object>> arrivalCompleted = result.getArrivalCompleted();
							PercentilesAggregation percentilesAggregation = entry.getPercentilesAggregation(HOUR);
							Map<String, Double> percentiles = percentilesAggregation.getPercentiles();
							for (String key : percentiles.keySet()) {
								Long count = entry.getCount();
								if (count > COUNT_LIMIT) {
									Map<String, Object> map = new LinkedHashMap<String, Object>();
									arrivalCompleted.add(map);
									map.put("dayOfWeek", Integer.valueOf(day));
									map.put("day", getWeekday(Integer.valueOf(day)));
									map.put("hourOfDay", percentiles.get(key).intValue());
									map.put("hour", toClock(percentiles.get(key).intValue()));
									map.put("count", entry.getCount());
								}
							}
						}
					}
				}
			}
			long elapsed = Duration.between(start, Instant.now()).toMillis();
			Map<String, Object> lmap = new HashMap<String, Object>();
			lmap.put(LogKeys.MESSAGE, "arrivalcompleted: " + spid + ", elapsed: " + elapsed);
			lmap.put("spid", spid);
			lmap.put("elapsed", elapsed);
			LOG.info(lmap);
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
		return result;
	}

	@Override
	public ArrivalCompletedResponse arrivalCompleted(String spid, Integer dayofweek, Integer percentile) {
		ArrivalCompletedResponse result = new ArrivalCompletedResponse();
		result.setServicePointId(spid);
		ServicePoint servicePoint = ServicePointUtil2.getServicePoint(spid, SE);
		if (servicePoint != null) {
			result.setName(servicePoint.getName());
		}
		QueryBuilder range = QueryBuilders.rangeQuery(TIMESTAMP).gte("now-57d").lte("now");
		QueryBuilder sp = QueryBuilders.termsQuery("arrivalCompleted_locationType", "SERVICE_POINT");
		QueryBuilder id = QueryBuilders.termsQuery("arrivalCompleted_locationId", spid);
		QueryBuilder se = QueryBuilders.termsQuery("arrivalCompleted_countryCode.keyword", "SWE");
		QueryBuilder dow = QueryBuilders.termsQuery("arrivalCompleted_dayOfWeek", String.valueOf(dayofweek));
		BoolQueryBuilder query = QueryBuilders.boolQuery().filter(range).must(sp).must(id).must(se).must(dow);
		//
		PercentilesAggregationBuilder hour = AggregationBuilders.percentiles(MINUTE)
				.field("arrivalCompleted_minuteOfDay").percentiles(percentile);
		//
		SearchSourceBuilder builder = new SearchSourceBuilder().aggregation(hour).query(query).size(0);
		final String queryString = builder.toString();
		LOG.debug("arrivalcompleted: \r\n" + queryString + "\r\n");
		final Search.Builder searchBuilder = new Search.Builder(queryString);
		searchBuilder.addIndices(lastQuarter());
		final Instant start = Instant.now();
		try {
			final SearchResult execute = jestClient.execute(searchBuilder.build());
			if (execute != null) {
				SearchResult response = new ISearchResult(execute);
				String errorMessage = response.getErrorMessage();
				if (errorMessage == null) {
					MetricAggregation aggregations = response.getAggregations();
					Long count = response.getTotal();
					if (aggregations != null && count > COUNT_LIMIT) {
						List<Map<String, Object>> arrivalCompleted = result.getArrivalCompleted();
						PercentilesAggregation percentilesAggregation = aggregations.getPercentilesAggregation(MINUTE);
						Map<String, Double> percentiles = percentilesAggregation.getPercentiles();
						for (String key : percentiles.keySet()) {
							Map<String, Object> map = new LinkedHashMap<String, Object>();
							arrivalCompleted.add(map);
							map.put("dayOfWeek", dayofweek);
							map.put("day", getWeekday(dayofweek));
							final long hourOfDay = Math.round(percentiles.get(key) / 60);
							map.put("hourOfDay", hourOfDay);
							map.put("hour", toClock(hourOfDay));
						}
					}
				}
			}
			long elapsed = Duration.between(start, Instant.now()).toMillis();
			Map<String, Object> lmap = new HashMap<String, Object>();
			lmap.put(LogKeys.MESSAGE, "arrivalcompleted: " + spid + ", elapsed: " + elapsed);
			lmap.put("spid", spid);
			lmap.put("elapsed", elapsed);
			LOG.info(lmap);
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
		return result;
	}

	@Override
	public ArrivalCompletedResponse arrivalCompletedHour(String spid, Integer dayofweek) {
		ArrivalCompletedResponse result = new ArrivalCompletedResponse();
		result.setServicePointId(spid);
		ServicePoint servicePoint = ServicePointUtil2.getServicePoint(spid, SE);
		if (servicePoint != null) {
			result.setName(servicePoint.getName());
		}
		QueryBuilder range = QueryBuilders.rangeQuery(TIMESTAMP).gte("now-57d").lte("now");
		QueryBuilder sp = QueryBuilders.termsQuery("arrivalCompleted_locationType", "SERVICE_POINT");
		QueryBuilder id = QueryBuilders.termsQuery("arrivalCompleted_locationId", spid);
		QueryBuilder se = QueryBuilders.termsQuery("arrivalCompleted_countryCode.keyword", "SWE");
		QueryBuilder dow = QueryBuilders.termsQuery("arrivalCompleted_dayOfWeek", String.valueOf(dayofweek));
		BoolQueryBuilder query = QueryBuilders.boolQuery().filter(range).must(sp).must(id).must(se).must(dow);
		//
		PercentilesAggregationBuilder hour = AggregationBuilders.percentiles(HOUR).field("arrivalCompleted_hourOfDay")
				.percentiles(95);
		//
		SearchSourceBuilder builder = new SearchSourceBuilder().aggregation(hour).query(query).size(0);
		final String queryString = builder.toString();
		LOG.debug("arrivalcompleted: \r\n" + queryString + "\r\n");
		final Search.Builder searchBuilder = new Search.Builder(queryString);
		searchBuilder.addIndices(lastQuarter());
		final Instant start = Instant.now();
		try {
			final SearchResult execute = jestClient.execute(searchBuilder.build());
			if (execute != null) {
				SearchResult response = new ISearchResult(execute);
				String errorMessage = response.getErrorMessage();
				if (errorMessage == null) {
					MetricAggregation aggregations = response.getAggregations();
					Long count = response.getTotal();
					LOG.info("count: " + count);
					if (aggregations != null && count > COUNT_LIMIT) {
						List<Map<String, Object>> arrivalCompleted = result.getArrivalCompleted();
						PercentilesAggregation percentilesAggregation = aggregations.getPercentilesAggregation(HOUR);
						Map<String, Double> percentiles = percentilesAggregation.getPercentiles();
						for (String key : percentiles.keySet()) {
							Map<String, Object> map = new LinkedHashMap<String, Object>();
							arrivalCompleted.add(map);
							map.put("dayOfWeek", dayofweek);
							map.put("day", getWeekday(dayofweek));
							map.put("hourOfDay", percentiles.get(key).intValue());
							map.put("hour", toClock(percentiles.get(key).intValue()));
						}
					}
				}
			}
			long elapsed = Duration.between(start, Instant.now()).toMillis();
			Map<String, Object> lmap = new HashMap<String, Object>();
			lmap.put(LogKeys.MESSAGE, "arrivalcompleted: " + spid + ", elapsed: " + elapsed);
			lmap.put("spid", spid);
			lmap.put("elapsed", elapsed);
			LOG.info(lmap);
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
		return result;
	}

	private String toClock(Integer time) {
		return time + ":00";
	}

	private String toClock(Long time) {
		return time + ":00";
	}

	String[] weekdays = { "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY" };

	private String getWeekday(int day) {
		return weekdays[day - 1];
	}

	@Override
	@Cacheable(value = ElasticService.POPULARTIMES)
	public PopularTimesResponse popularTimes(String spid, String countryCode) {
		return popularTimes(spid, countryCode, null, null);
	}

	@Override
	public PopularTimesResponse popularTimes(String spid, String countryCode, DateTime fromDay, DateTime toDay) {
		PopularTimesResponse result = new PopularTimesResponse();
		result.setServicePointId(spid);
		result.setCountryCode(countryCode);

		ServicePoint servicePoint = ServicePointUtil2.getServicePoint(spid, countryCode);
		if (servicePoint != null) {
			result.setName(servicePoint.getName());
		}
		QueryBuilder range;
		if (fromDay == null && toDay == null) {
			range = QueryBuilders.rangeQuery(TIMESTAMP).gte("now-57d").lte("now");
		} else {
			result.setDateFrom(fromDay);
			result.setDateTo(toDay);
			range = QueryBuilders.rangeQuery(TIMESTAMP).gte(fromDay).lte(toDay);
		}
		QueryBuilder sp = QueryBuilders.termsQuery("distributed_locationKey", "SERVICE_POINT|" + spid);
		QueryBuilder cc = QueryBuilders.termsQuery("distributed_countryCode.keyword", "SWE");
		// use delivered for all except Sweden
		if (!SE.equalsIgnoreCase(countryCode)) {
			sp = QueryBuilders.termsQuery("delivered_locationKey.keyword", "SERVICE_POINT|" + spid);
			cc = QueryBuilders.termsQuery("delivered_countryCode.keyword", countryMap.get(countryCode.toUpperCase()));
		}
		BoolQueryBuilder query = QueryBuilders.boolQuery().must(sp).must(cc).filter(range);
		//
		HistogramAggregationBuilder histogramAggs = AggregationBuilders.histogram(HOUR).field("deliveryDate_hourOfDay")
				.interval(1);
		TermsAggregationBuilder weekdayAggs = AggregationBuilders.terms(WEEKDAY).field("deliveryDate_dayOfWeek")
				.order(BucketOrder.key(true)).size(7).subAggregation(histogramAggs);
		//
		SearchSourceBuilder builder = new SearchSourceBuilder().aggregation(weekdayAggs).query(query).size(0);
		final String queryString = builder.toString();
		LOG.info("populartimes: \r\n" + queryString + "\r\n");
		final Search.Builder searchBuilder = new Search.Builder(queryString);
		DateTime minusDays = new DateTime().minusDays(85);
		LOG.info("minusDays: " + minusDays);
		LOG.info("fromDay: " + fromDay);
		if (fromDay != null && fromDay.isBefore(minusDays)) {
			LOG.info("lastYears: " + lastYears());
			searchBuilder.addIndices(lastYears());
		} else {
			LOG.info("lastQuarter: " + lastQuarter());
			searchBuilder.addIndices(lastQuarter());
		}
		final Instant start = Instant.now();
		try {
			final SearchResult execute = jestClient.execute(searchBuilder.build());
			if (execute != null) {
				SearchResult response = new ISearchResult(execute);
				String errorMessage = response.getErrorMessage();
				if (errorMessage == null) {
					Long total = response.getTotal();
					result.setWeekAverage(total / 8); // 8 week average
					MetricAggregation aggregations = response.getAggregations();
					if (aggregations != null) {
						TermsAggregation ta = aggregations.getTermsAggregation(WEEKDAY);
						List<Entry> weeklist = ta.getBuckets();
						for (Entry entry : weeklist) {
							Long dayCount = entry.getCount();
							long dayAverage = dayCount / 8;
							double floorDay = Math.floor(100D * dayCount / (total + 1));
							Long percentDay = Double.valueOf(floorDay).longValue();
							if (dayAverage > 7 && percentDay > 0) {
								String weekday = entry.getKey();
								List<Map<String, Object>> popularTimes = result.getPopularTimes();
								Map<String, Object> weekMap = new LinkedHashMap<String, Object>();
								popularTimes.add(weekMap);
								String weekdayString = getWeekday(Integer.valueOf(weekday));
								weekMap.put("day", weekdayString);
								weekMap.put("percent", percentDay);
								weekMap.put("count", dayAverage);
								List<Map<String, Object>> visits = new ArrayList<>();
								weekMap.put("visits", visits);
								HistogramAggregation ha = entry.getHistogramAggregation(HOUR);
								List<Histogram> hourlist = ha.getBuckets();
								Long dayPercent = 0L;
								for (Histogram histogram : hourlist) {
									Long hour = histogram.getKey();
									Long hourCount = histogram.getCount();
									double floorHour = Math.floor(100D * hourCount / (dayCount + 1));
									Long percentHour = Double.valueOf(floorHour).longValue();
									dayPercent += percentHour;
									long hourAverage = hourCount / 8;
									if (hourAverage > 0 && percentHour > 0) {
										Map<String, Object> dayMap = new LinkedHashMap<String, Object>();
										visits.add(dayMap);
										dayMap.put("hour", toClock(hour));
										dayMap.put("percent", percentHour);
										dayMap.put("count", hourAverage);
									}
								}
								LOG.debug("weekdayString: " + weekdayString);
							}
						}
					}
				}
			}
			long elapsed = Duration.between(start, Instant.now()).toMillis();
			Map<String, Object> lmap = new HashMap<String, Object>();
			lmap.put(LogKeys.MESSAGE, "populartimes: " + spid + ", elapsed: " + elapsed);
			lmap.put("spid", spid);
			lmap.put("elapsed", elapsed);
			LOG.info(lmap);
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
		return result;
	}

	//
	// ecemstatistics
	//

	@Override
	public ArrivalCompletedStatusResponse arrivalCompletedStatus() {
		ArrivalCompletedStatusResponse result = new ArrivalCompletedStatusResponse();
		QueryBuilder range = QueryBuilders.rangeQuery(TIMESTAMP).gte("now-14d").lte("now");
		QueryBuilder sp = QueryBuilders.termsQuery("arrivalCompleted_locationType", "SERVICE_POINT");
		QueryBuilder se = QueryBuilders.termsQuery("arrivalCompleted_countryCode.keyword", "SWE");
		QueryBuilder swe = QueryBuilders.termsQuery("consignee_address_countryCode", "SWE");
		QueryBuilder status = QueryBuilders.termsQuery("status", "AVAILABLE_FOR_DELIVERY");
		BoolQueryBuilder query = QueryBuilders.boolQuery().filter(range).must(sp).must(se).must(swe).must(status);
		//
		TermsAggregationBuilder d_aggs = AggregationBuilders.terms(LOCS).field("arrivalCompleted_locationId")
				.order(BucketOrder.key(true)).size(5000);
		//
		SearchSourceBuilder builder = new SearchSourceBuilder().aggregation(d_aggs).query(query).size(0);
		final String queryString = builder.toString();
		LOG.debug("arrivalCompletedStatus: \r\n" + queryString + "\r\n");
		final Search.Builder searchBuilder = new Search.Builder(queryString);
		searchBuilder.addIndices(lastMonth());
		final Instant start = Instant.now();
		try {
			final SearchResult execute = jestClient.execute(searchBuilder.build());
			if (execute != null) {
				SearchResult response = new ISearchResult(execute);
				String errorMessage = response.getErrorMessage();
				if (errorMessage == null) {
					result.setNumberOfItems(response.getTotal());
					MetricAggregation aggregations = response.getAggregations();
					if (aggregations != null) {
						List<Map<String, Object>> list = result.getArrivalCompletedStatus();
						TermsAggregation ta = aggregations.getTermsAggregation(LOCS);
						List<Entry> elist = ta.getBuckets();
						for (Entry entry : elist) {
							String locationId = entry.getKey();
							Long doc_count = entry.getCount();
							Map<String, Object> map = new LinkedHashMap<String, Object>();
							list.add(map);
							map.put("locationId", locationId);
							map.put("count", doc_count);
						}
						result.setNumberOfServicePoints(list.size());
					}
				}
			}
			long elapsed = Duration.between(start, Instant.now()).toMillis();
			Map<String, Object> lmap = new HashMap<String, Object>();
			lmap.put(LogKeys.MESSAGE, "arrivalCompletedStatus: elapsed: " + elapsed);
			lmap.put("elapsed", elapsed);
			LOG.info(lmap);
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}

		return result;
	}

	@Override
	public EdiPressureStatusResponse ediPressureStatus() {
		EdiPressureStatusResponse result = new EdiPressureStatusResponse();
		QueryBuilder range = QueryBuilders.rangeQuery(TIMESTAMP).gte("now-14d").lte("now");
		QueryBuilder swe = QueryBuilders.termsQuery("consignee_address_countryCode", "SWE");
		QueryBuilder status = QueryBuilders.termsQuery("status", "INFORMED");
		BoolQueryBuilder query = QueryBuilders.boolQuery().filter(range).must(swe).must(status);
		//
		TermsAggregationBuilder d_aggs = AggregationBuilders.terms(LOCS).field("consignor_name.keyword").size(500);
		//
		SearchSourceBuilder builder = new SearchSourceBuilder().aggregation(d_aggs).query(query).size(0);
		final String queryString = builder.toString();
		LOG.debug("ediPressureStatus: \r\n" + queryString + "\r\n");
		final Search.Builder searchBuilder = new Search.Builder(queryString);
		searchBuilder.addIndices(lastMonthItems());
		final Instant start = Instant.now();
		try {
			final SearchResult execute = jestClient.execute(searchBuilder.build());
			if (execute != null) {
				SearchResult response = new ISearchResult(execute);
				String errorMessage = response.getErrorMessage();
				if (errorMessage == null) {
					result.setNumberOfItems(response.getTotal());
					MetricAggregation aggregations = response.getAggregations();
					if (aggregations != null) {
						List<Map<String, Object>> list = result.getEdiPressureStatus();
						TermsAggregation ta = aggregations.getTermsAggregation(LOCS);
						List<Entry> elist = ta.getBuckets();
						for (Entry entry : elist) {
							String consignor_name = entry.getKey();
							Long doc_count = entry.getCount();
							Map<String, Object> map = new LinkedHashMap<String, Object>();
							list.add(map);
							map.put("consignor_name", consignor_name);
							map.put("count", doc_count);
						}
						result.setNumberOfConsignorNames(list.size());
					}
				}
			}
			long elapsed = Duration.between(start, Instant.now()).toMillis();
			Map<String, Object> lmap = new HashMap<String, Object>();
			lmap.put(LogKeys.MESSAGE, "ediPressureStatus: elapsed: " + elapsed);
			lmap.put("elapsed", elapsed);
			LOG.info(lmap);
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}

		return result;
	}

}
