package com.mannetroll.web.controller;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.mannetroll.metrics.helper.Constants;
import com.mannetroll.metrics.util.LogKeys;
import com.mannetroll.web.model.ApiError;
import com.mannetroll.web.model.Fault;
import com.mannetroll.web.model.KpiResponse;
import com.mannetroll.web.service.impl.ISearchResult;

import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.SearchResult.Hit;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/*
 * curl -is http://localhost:8080/kpi/itemid/00370726201859187521
 */

@RestController
@Api(value = "Kpi")
public class KpiController {
    private static final Logger LOGGER = LogManager.getLogger(KpiController.class);
    private static final String KPI = "kpi";
    private static DateTimeFormatter month = DateTimeFormat.forPattern("yyyy.MM.*");
    private static DateTimeFormatter year = DateTimeFormat.forPattern("yyyy");
    private static final String KPIS_KPI_ID = "/kpi/itemid/{itemid}";
    private static Checksum checksum = Checksum.instance();

    static {
        month = month.withZone(DateTimeZone.forID("GMT"));
        year = year.withZone(DateTimeZone.forID("GMT"));
    }

    @Autowired
    private JestClient jestClient;

    public void setJestClient(JestClient jestClient) {
        this.jestClient = jestClient;
    }

    @SuppressWarnings("rawtypes")
    @ApiOperation(value = "kpi", notes = "Get KPI document")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = KpiResponse.class),
            @ApiResponse(code = 400, message = "Bad request") })
    @RequestMapping(value = "/kpi/itemid/{itemid}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<KpiResponse> kpi(@PathVariable String itemid) {
        ThreadContext.put(Constants.METRICS_NAME, KPIS_KPI_ID);
        ThreadContext.put(Constants.JAVA_METHOD, KPI);
        ThreadContext.put(Constants.JAVA_ITEMID, itemid);
        // remove DPD checksum if exists
        itemid = deleteChecksum(itemid);
        try {
            // itemid
            TermsQueryBuilder query = QueryBuilders.termsQuery("itemId", itemid);
            SearchSourceBuilder builder = new SearchSourceBuilder().query(query).size(1);
            final Search.Builder searchBuilder = new Search.Builder(builder.toString());
            searchBuilder.addIndices(lastHalfYear());
            final Instant start = Instant.now();
            final SearchResult execute = jestClient.execute(searchBuilder.build());
            if (execute != null) {
                SearchResult result = new ISearchResult(execute);
                String errorMessage = result.getErrorMessage();
                if (errorMessage == null) {
                    final List<Hit<HashMap, Void>> hits = result.getHits(HashMap.class);
                    if (hits.size() > 0) {
                        @SuppressWarnings("unchecked") final Map<String, Object> hitMap = hits.get(0).source;
                        //GPDR
                        hitMap.put("consignee_address_street1", null);
                        hitMap.put("consignee_address_street2", null);
                        hitMap.put("consignee_address_addressId", null);
                        final Map<String, Object> sorted = new TreeMap<>(hitMap);
                        KpiResponse response = new KpiResponse(sorted);
                        long elapsed = Duration.between(start, Instant.now()).toMillis();
                        Map<String, Object> lmap = new HashMap<String, Object>();
                        lmap.put(LogKeys.DESCRIPTION, "itemid: " + itemid + ", elapsed: " + elapsed);
                        lmap.put("itemid", itemid);
                        lmap.put("elapsed", elapsed);
                        LOGGER.info(lmap);
                        return new ResponseEntity<KpiResponse>(response, HttpStatus.OK);
                    } else {
                        return createError("No hits for: " + itemid, "001");
                    }
                } else {
                    LOGGER.error("errorMessage: " + errorMessage);
                    return createError("errorMessage: " + errorMessage, "002");
                }
            } else {
                return createError("No result for: " + itemid, "001");
            }
        } catch (Exception ex) {
            LOGGER.info(ex.getMessage(), ex);
            return createError("Exception: " + ex.getMessage(), "003");
        }
    }

    public static List<String> lastHalfYear() {
        List<String> indexNames = new ArrayList<String>();
        DateTime now = new DateTime();
        indexNames.add(KPI + "-" + month.print(now));
        indexNames.add(KPI + "-" + month.print(now.minusMonths(1)));
        indexNames.add(KPI + "-" + month.print(now.minusMonths(2)));
        indexNames.add(KPI + "-" + month.print(now.minusMonths(3)));
        indexNames.add(KPI + "-" + month.print(now.minusMonths(4)));
        indexNames.add(KPI + "-" + month.print(now.minusMonths(5)));
        indexNames.add(KPI + "-" + month.print(now.minusMonths(6)));
        return indexNames;
    }

    private static final int LENGTH_WITHOUT_CHECKSUM = 14;
    private static final int LENGTH_WITH_CHECKSUM = 15;
    public String deleteChecksum(String id) {
        if (id == null || id.length() != LENGTH_WITH_CHECKSUM) {
            return id;
        }
        try {
            char sign = id.charAt(LENGTH_WITHOUT_CHECKSUM);
            String externalId = id.substring(0, LENGTH_WITHOUT_CHECKSUM);
            char dpdChecksum = checksum.getDPDChecksum(externalId);
            if (sign == dpdChecksum) {
                return externalId;
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return id;
    }

    protected ResponseEntity<KpiResponse> createError(String explanationText, String faultCode) {
        ApiError apiError = new ApiError();
        apiError.addFault(new Fault(faultCode, explanationText));
        KpiResponse response = new KpiResponse(apiError);
        LOGGER.info("NOT_FOUND: " + explanationText + ", " + faultCode);
        return new ResponseEntity<KpiResponse>(response, HttpStatus.NOT_FOUND);
    }
}
