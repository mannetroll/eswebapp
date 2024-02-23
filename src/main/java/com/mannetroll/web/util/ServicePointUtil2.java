package com.mannetroll.web.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.mannetroll.servicepoints.Coordinate;
import com.mannetroll.servicepoints.NotificationArea;
import com.mannetroll.servicepoints.ServicePoint;
import com.mannetroll.servicepoints.ServicePointInformationResponse;

public class ServicePointUtil2 {
	private final static Logger LOG = LogManager.getLogger(ServicePointUtil2.class);
	private static List<ServicePoint> servicePoints;
	private static final Map<String, Coordinate> postalCodeMap = new HashMap<>();
	private static final Map<String, Coordinate> servicePointCoordMap = new HashMap<>();
	private static final Map<String, ServicePoint> servicePointMap = new HashMap<>();
	private static final ObjectMapper mapper;
	private static Map<String, String> cmap = new HashMap<String, String>();
	private static long start = System.currentTimeMillis();
	private static final String PIPE = "|";

	static {
		mapper = new ObjectMapper();
		mapper.enable(DeserializationFeature.UNWRAP_ROOT_VALUE);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.setSerializationInclusion(Include.NON_NULL);
		mapper.registerModule(new JodaModule());
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
		try {
			ServicePointUtil2.init();
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}

	private static void init() {
		URL file = ServicePointUtil2.class.getResource("getServicePointInformation.json.gz");
		try (InputStream fileStream = file.openStream();
				GZIPInputStream gzipInputStream = new GZIPInputStream(fileStream);
				InputStreamReader inStreamReader = new InputStreamReader(gzipInputStream, StandardCharsets.UTF_8);
				BufferedReader bufferedReader = new BufferedReader(inStreamReader)) {
			ServicePointUtil2.getModel(bufferedReader);
			ServicePointUtil2.getNordicPostalCodeMap();
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
		String[] countries = Locale.getISOCountries();
		for (String country : countries) {
			cmap.put(country, country);
			Locale locale = new Locale("", country);
			cmap.put(locale.getISO3Country().toUpperCase(), country);
		}
		LOG.info("ZIP 17173: " + ServicePointUtil2.getCoord("SE", "17173"));
	}

	private static synchronized void getNordicPostalCodeMap() {
		int notificationAreas = 0;
		LOG.info("ServicePointMap: " + servicePoints.size());
		for (ServicePoint pointInformation : servicePoints) {
			final String serviePointId = pointInformation.getServicePointId().toString();
			String countryCode = pointInformation.getDeliveryAddress().getCountryCode();
			servicePointMap.put(serviePointId + PIPE + countryCode, pointInformation);
			NotificationArea notificationArea = pointInformation.getNotificationArea();
			notificationAreas += (notificationArea != null ? notificationArea.getPostalCodes().size() : 0);
			List<Coordinate> coordinates = pointInformation.getCoordinates();
			if (coordinates != null && coordinates.size() > 0) {
				Coordinate coordinate = coordinates.get(0);
				servicePointCoordMap.put(serviePointId, coordinate);
				if (notificationArea != null) {
					if (coordinate != null) {
						List<String> postalCodes = notificationArea.getPostalCodes();
						for (String code : postalCodes) {
							String key = countryCode + PIPE + code;
							postalCodeMap.put(key, coordinate);
						}
					}
				}
				String dCode = pointInformation.getDeliveryAddress().getPostalCode();
				postalCodeMap.put(countryCode + PIPE + dCode, coordinate);
				String vCode = pointInformation.getVisitingAddress().getPostalCode();
				postalCodeMap.put(countryCode + PIPE + vCode, coordinate);
			}
		}
		LOG.info("ServicePointCoordinateMap: " + servicePointCoordMap.size());
		LOG.info("NordicPostalCodeMap: " + postalCodeMap.size());
		LOG.info("NotificationAreas: " + notificationAreas);
	}

	private static void getModel(Reader reader) {
		try {
			String string = IOUtils.toString(reader);
			ServicePointInformationResponse spir = ServicePointUtil2.parseJson(string);
			servicePoints = spir.getServicePoints();
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}

	public static ServicePointInformationResponse parseJson(String json) {
		try {
			return mapper.readValue(json, ServicePointInformationResponse.class);
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			return null;
		}
	}

	public static ServicePoint getServicePoint(String spId, String countryCode) {
		return servicePointMap.get(spId + PIPE + countryCode);
	}

	public static Map<String, ServicePoint> getServicePoints() {
		return servicePointMap;
	}

	public static Coordinate getCoordSP(String spId) {
		return servicePointCoordMap.get(spId);
	}

	public static Coord getCoord(String compositePostalCode) {
		if (compositePostalCode != null && compositePostalCode.contains(PIPE)) {
			String[] split = compositePostalCode.split("\\|");
			return getCoord(split[0], split[1]);
		} else {
			return null;
		}
	}

	public static Coord getCoord(String country, String postcode) {
		String key = cmap.get(country) + PIPE + postcode;
		Coordinate tmp = postalCodeMap.get(key);
		if (tmp != null) {
			return new Coord(tmp.getNorthing(), tmp.getEasting());
		} else {
			return null;
		}
	}

	public static long getDistance(Coord geo1, Coord geo2) {
		Double distance = 0D;
		if (geo1 == null || geo2 == null) {
			return distance.longValue();
		}
		if (geo1.lat.doubleValue() > 0 && geo2.lat.doubleValue() > 0 && geo1.lon.doubleValue() > 0
				&& geo2.lon.doubleValue() > 0) {
			distance = distance(geo1.lat.doubleValue(), geo2.lat.doubleValue(), geo1.lon.doubleValue(),
					geo2.lon.doubleValue(), 0, 0);
		}
		return distance.longValue();
	}

	public static double distance(double lat1, double lat2, double lon1, double lon2, double el1, double el2) {
		final int R = 6371; // Radius of the earth
		Double latDistance = Math.toRadians(lat2 - lat1);
		Double lonDistance = Math.toRadians(lon2 - lon1);
		Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) + Math.cos(Math.toRadians(lat1))
				* Math.cos(Math.toRadians(lat2)) * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
		Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double distance = R * c * 1000; // convert to meters
		double height = el1 - el2;
		distance = Math.pow(distance, 2) + Math.pow(height, 2);
		return Math.sqrt(distance);
	}

	public static void main(String[] args) {
		System.out.println(ServicePointUtil2.getServicePoint("336538", "SE").getName());
		System.out.println(ServicePointUtil2.getCoord("SWE", "79385").lat);
		System.out.println(ServicePointUtil2.getCoord("SWE|79385").lon);
		System.out.println(ServicePointUtil2.getDistance(ServicePointUtil2.getCoord("SWE|79385"),
				ServicePointUtil2.getCoord("SWE|10500")) / 1000);
		System.out.println(ServicePointUtil2.getDistance(ServicePointUtil2.getCoord("SWE|11436"),
				ServicePointUtil2.getCoord("SWE|62379")) / 1000);
		System.out.println("elapsed: " + (System.currentTimeMillis() - start));
	}

}
