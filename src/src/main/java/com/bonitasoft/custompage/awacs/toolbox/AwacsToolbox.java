package com.bonitasoft.custompage.awacs.toolbox;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class AwacsToolbox {

		/**
		 * decode a String
		 * 
		 * @param value
		 * @param defaultValue
		 * @return
		 */
		
		public static String jsonToString(Object value, String defaultValue, boolean nullIfEmpty) {
				if (value == null)
						return defaultValue;
				try {
				    String valueSt= value.toString();
				    if (nullIfEmpty && valueSt.trim().length()==0)
				        return null;
				    return valueSt;
				} catch (Exception e) {
				}
				
				return defaultValue;
		}

		public static Boolean jsonToBoolean(Object value, Boolean defaultValue) {
				try {
						if (value == null || value.toString().length()==0)
								return defaultValue;
						return Boolean.valueOf(value.toString());
				} catch (Exception e) {
						return defaultValue;
				}
		}

		@SuppressWarnings("unchecked")
		public static List<String> jsonToListString(Object value) {
		
				if (value == null || !(value instanceof List))
						return new ArrayList<String>();
				ArrayList<String> result = new ArrayList<String>();
				List<Object> listValue = (List<Object>) value;
				for (Object oneValue : listValue) {
						result.add(oneValue == null ? null : oneValue.toString());
				}
				return result;
		}

		@SuppressWarnings("unchecked")
		public  static List<Long> jsonToListLong(Object value) {
		
				if (value == null || !(value instanceof List))
						return new ArrayList<Long>();
				ArrayList<Long> result = new ArrayList<Long>();
				List<Object> listValue = (List<Object>) value;
				for (Object oneValue : listValue) {
						try {
								if (oneValue != null)
										result.add(oneValue == null ? null : Long.valueOf(oneValue.toString()));
						} catch (Exception e) {
						}
				}
				return result;
		}

		/**
		 * decode a date
		 * 
		 * @param value
		 * @return
		 */
		public  static Date jsonToDate(Object value) {
				Logger logger = Logger.getLogger(AwacsToolbox.class.getName());
				logger.info("ToDate[" + value + "] class:" + (value == null ? "null" : value.getClass().getName()));
				if (value instanceof Date)
						return (Date) value;
				if (value instanceof String) {
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
						Date myDate;
						try {
								myDate = sdf.parse(value.toString());
								logger.info("After Parsing ToDate[" + myDate + "]");
		
						} catch (ParseException e) {
								return null;
						}
						return myDate;
				}
				return null;
		}

		/**
		 * decode an integer
		 * 
		 * @param value
		 * @param defaultValue
		 * @return
		 */
		public  static Long jsonToLong(Object value, Long defaultValue) {
				if (value == null || value.toString().length()==0)
						return defaultValue;
				if (value instanceof Integer)
						return ((Integer) value).longValue();
				if (value instanceof Long)
						return ((Long) value);
				try {
						if (value != null)
								return Long.valueOf(value.toString());
				} catch (Exception e) {
				}
				return defaultValue;
		}

		public final static String formatDateJson = "yyyy-MM-dd'T'HH:mm:ss.SSSX";

}
