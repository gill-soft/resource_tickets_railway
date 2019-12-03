package com.gillsoft.client;

import java.io.IOException;
import java.util.Properties;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

public class Config {
	
	private static Properties properties;
	
	static {
		try {
			Resource resource = new ClassPathResource("resource.properties");
			properties = PropertiesLoaderUtils.loadProperties(resource);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String getUrl() {
		return properties.getProperty("url");
	}
	
	public static String getKey() {
		return properties.getProperty("key");
	}
	
	public static String getSaleName() {
		return properties.getProperty("sale.name");
	}
	
	public static String getSalePhone() {
		return properties.getProperty("sale.phone");
	}
	
	public static String getSaleEmailSuffix() {
		return properties.getProperty("sale.email.suffix");
	}
	
	public static String getShopApiKey() {
		return properties.getProperty("shop_api_key");
	}
	
	public static String getShopSecretKey() {
		return properties.getProperty("shop_secret_key");
	}
	
	public static int getRequestTimeout() {
		return Integer.valueOf(properties.getProperty("request.timeout"));
	}
	
	public static int getSearchRequestTimeout() {
		return Integer.valueOf(properties.getProperty("request.search.timeout"));
	}
	
	public static long getCacheTripTimeToLive() {
		return Long.valueOf(properties.getProperty("cache.trip.time.to.live"));
	}
	
	public static long getCacheTripSeatsTimeToLive() {
		return Long.valueOf(properties.getProperty("cache.trip.seats.time.to.live"));
	}
	
	public static long getCacheErrorTimeToLive() {
		return Long.valueOf(properties.getProperty("cache.error.time.to.live"));
	}
	
}
