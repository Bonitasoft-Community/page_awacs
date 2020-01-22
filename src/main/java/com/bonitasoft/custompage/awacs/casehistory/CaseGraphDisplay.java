package com.bonitasoft.custompage.awacs.casehistory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;



public class CaseGraphDisplay {

		
	
		/**
		 * 
		 * @param listLabels
		 * @param listValues
		 * @return
		 */
		public static class ActivityTimeLine
		{
				public String activityName;
				public Date dateBegin;
				public Date dateEnd;
				
				public static ActivityTimeLine getActivityTimeLine(String activityName, Date dateBegin, Date dateEnd)
				{
						ActivityTimeLine oneSample = new ActivityTimeLine();
						oneSample.activityName = activityName;
						oneSample.dateBegin=dateBegin;
						oneSample.dateEnd = dateEnd;
						
						return oneSample;
				}
				public long getDateLong()
				{ return dateBegin == null ? 0 : dateBegin.getTime(); }
		}
		
		/**
		 * ----------------------------------------------------------------
		 * 
		 * getActivityTimeLine
		 * 
		 * @return
		 */
		public static String getActivityTimeLine(String title, List<ActivityTimeLine> listSamples){
				Logger logger = Logger.getLogger("org.bonitasoft");
				
				/** structure 
				 * "rows": [
           {
        		 c: [
        		      { "v": "January" },"
                  { "v": 19,"f": "42 items" },
                  { "v": 12,"f": "Ony 12 items" },
                ]
           },
           {
        		 c: [
        		      { "v": "January" },"
                  { "v": 19,"f": "42 items" },
                  { "v": 12,"f": "Ony 12 items" },
                ]
           },

				 */
				String resultValue="";
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy,MM,dd,HH,mm,ss,SSS");
				Date currentDate = new Date( System.currentTimeMillis() ) ;
				
				for (int i=0;i<listSamples.size();i++)
				{
						Date dateEnd = listSamples.get( i ).dateEnd==null ? currentDate : listSamples.get( i ).dateEnd;
						if (listSamples.get( i ).dateBegin!=null)
								resultValue+= "{ \"c\": [ { \"v\": \""+listSamples.get( i ).activityName+"\" }," 
								+ " { \"v\": \""+listSamples.get( i ).activityName +"\" }, " 
								+ " { \"v\": \"Date("+ simpleDateFormat.format( listSamples.get( i ).dateBegin) +")\" }, " 
								+ " { \"v\": \"Date("+ simpleDateFormat.format( dateEnd ) +")\" } " 
								+ "] },";
				}
				if (resultValue.length()>0)
						resultValue = resultValue.substring(0,resultValue.length()-1);
				
				String resultLabel = "{ \"type\": \"string\", \"id\": \"Role\" },{ \"type\": \"string\", \"id\": \"Name\"},{ \"type\": \"datetime\", \"id\": \"Start\"},{ \"type\": \"datetime\", \"id\": \"End\"}";
				
				String valueChart = "	{"
						+ "\"type\": \"Timeline\", "
					  + "\"displayed\": true, "
					  + "\"data\": {"
					  +   "\"cols\": ["+resultLabel+"], "
					  +   "\"rows\": ["+resultValue+"] "
					  /*
					  +   "\"options\": { "
					  +         "\"bars\": \"horizontal\","
					  +         "\"title\": \""+title+"\", \"fill\": 20, \"displayExactValues\": true,"
					  +         "\"vAxis\": { \"title\": \"ms\", \"gridlines\": { \"count\": 100 } }"
					  */
					  +   "}"
					  +"}";
// 				+"\"isStacked\": \"true\","
 	          
//		    +"\"displayExactValues\": true,"
//		    
//		    +"\"hAxis\": { \"title\": \"Date\" }"
//		    +"},"
				logger.info("GraphCaseTimeLine >"+valueChart+"<");

				
				return valueChart;		
		}	
		
		
		/*
		valueChart ="	{"
						+"\"type\": \"ColumnChart\", \"displayed\": true,"
					  +"\"data\": {"
					  +"\"cols\": ["
					  +  "{      \"id\": \"month\",      \"label\": \"Month\",   \"type\": \"string\",    \"p\": {}      },"
						+  "{      \"id\": \"laptop-id\",   \"label\": \"Laptop\",      \"type\": \"number\",      \"p\": {}     },"
					  +  "{   \"id\": \"desktop-id\", \"label\": \"Desktop\",\"type\": \"number\", \"p\": {}			      },"
					  +  "{   \"id\": \"server-id\",  \"label\": \"Server\", \"type\": \"number\", \"p\": {}   },"
					  +  "{   \"id\": \"cost-id\",    \"label\": \"Shipping\", \"type\": \"number\" }"
					  +"],"
					  +"\"rows\": ["
					    +"{  \"c\": [ {"
					        		+"\"v\": \"January\""
					            				+"},"
					          +"{"
					          		+"							            \"v\": 19,"
					            +"\"f\": \"42 items\""
					            +"},"
					          +"{"
					           +" \"v\": 12,"
					            +"\"f\": \"Ony 12 items\""
					            +"},"
					          +"{"
					          		+"\"v\": 7,"
					            +"\"f\": \"7 servers\""
					            +"},"
					          +"{"
					          		+"\"v\": 4"
					            +"}"
					          +"]"
					        				+"},"
					      +"{"
					      		+"\"c\": ["
					        +"{"
					        		+"\"v\": \"February\""
					            				+"},"
					          +"{"
					          		+"\"v\": 13"
					            +"},"
					          +"{"
					          		+"\"v\": 1,"
					            +"\"f\": \"1 unit (Out of stock this month)\""
					            +"},"
					          +"{"
					          		+"\"v\": 12"
					            +"},"
					          +"{"
					          		+"\"v\": 2"
					            +"}"
					          +"]"
					        				+"},"
					      +"{"
					      		+"\"c\": ["
					        +"{"
					        		+"\"v\": \"March\""
					            				+"},"
					          +"{"
					          		+"\"v\": 24"
					            +"},"
					          +"{"
					          		+"\"v\": 5"
					            +"},"
					          +"{"
					          		+"\"v\": 11"
					            +"},"
					          +"{"
					          		+"\"v\": 6"
					            +"}"
					          +"  ]"
					        				+"}"
					    +"]"
					    				+"},"
					  +"\"options\": {"
					  +"\"title\": \"Sales per month\","
					    +"\"isStacked\": \"true\","
					    +"\"fill\": 20,"
					    +"\"displayExactValues\": true,"
					    +"\"vAxis\": {"
					    +"\"title\": \"Sales unit\","
					      +"\"gridlines\": {"
					      +"\"count\": 10"
					        +"      }"
				+"},"
					    +"\"hAxis\": {"
					    +"\"title\": \"Date\""
					      +"				    }"
		+"},"
					  +"  \"formatters\": {}"
					+"}"; */
		// result in a chart
		/*
		String valueChart = "	{"
						+"\"type\": \"ColumnChart\","
			  +"							  \"displayed\": true,"
			  +"\"data\": {"
			  +"\"cols\": ["
			    +"{      \"id\": \"month\",      \"label\": \"Month\",   \"type\": \"string\",    \"p\": {}      },"
				+"{      \"id\": \"laptop-id\",   \"label\": \"Laptop\",      \"type\": \"number\",      \"p\": {}     },"
			      		+"{"
			      		+"\"id\": \"desktop-id\","
			        +"\"label\": \"Desktop\","
			        +"\"type\": \"number\","
			        +"\"p\": {}"
			        +"							      },"
			      +"{"
			      		+"\"id\": \"server-id\","
			        +"\"label\": \"Server\","
			        +"\"type\": \"number\","
			        +"\"p\": {}"
			        +"},"
			      +"{"
			      		+"\"id\": \"cost-id\","
			        +"\"label\": \"Shipping\","
			        +"\"type\": \"number\""
			        +"}"
			      +"],"
			    +"\"rows\": ["
			    +"{"
			    		+"\"c\": ["
			        +"{"
			        		+"\"v\": \"January\""
			            				+"},"
			          +"{"
			          		+"							            \"v\": 19,"
			            +"\"f\": \"42 items\""
			            +"},"
			          +"{"
			           +" \"v\": 12,"
			            +"\"f\": \"Ony 12 items\""
			            +"},"
			          +"{"
			          		+"\"v\": 7,"
			            +"\"f\": \"7 servers\""
			            +"},"
			          +"{"
			          		+"\"v\": 4"
			            +"}"
			          +"]"
			        				+"},"
			      +"{"
			      		+"\"c\": ["
			        +"{"
			        		+"\"v\": \"February\""
			            				+"},"
			          +"{"
			          		+"\"v\": 13"
			            +"},"
			          +"{"
			          		+"\"v\": 1,"
			            +"\"f\": \"1 unit (Out of stock this month)\""
			            +"},"
			          +"{"
			          		+"\"v\": 12"
			            +"},"
			          +"{"
			          		+"\"v\": 2"
			            +"}"
			          +"]"
			        				+"},"
			      +"{"
			      		+"\"c\": ["
			        +"{"
			        		+"\"v\": \"March\""
			            				+"},"
			          +"{"
			          		+"\"v\": 24"
			            +"},"
			          +"{"
			          		+"\"v\": 5"
			            +"},"
			          +"{"
			          		+"\"v\": 11"
			            +"},"
			          +"{"
			          		+"\"v\": 6"
			            +"}"
			          +"  ]"
			        				+"}"
			    +"]"
			    				+"},"
			  +"\"options\": {"
			  +"\"title\": \"Sales per month\","
			    +"\"isStacked\": \"true\","
			    +"\"fill\": 20,"
			    +"\"displayExactValues\": true,"
			    +"\"vAxis\": {"
			    +"\"title\": \"Sales unit\","
			      +"\"gridlines\": {"
			      +"\"count\": 10"
			        +"      }"
		+"},"
			    +"\"hAxis\": {"
			    +"\"title\": \"Date\""
			      +"				    }"
+"},"
			  +"  \"formatters\": {}"
			+"}"; */

/*
 * "type": "AreaChart",
  "displayed": true,
  "data": {
    "cols": [
      {
        "id": "month",
        "label": "Month",
        "type": "string",
        "p": {}
      },
      {
        "id": "laptop-id",
        "label": "Laptop",
        "type": "number",
        "p": {}
      },
      {
        "id": "desktop-id",
        "label": "Desktop",
        "type": "number",
        "p": {}
      },
      {
        "id": "server-id",
        "label": "Server",
        "type": "number",
        "p": {}
      },
      {
        "id": "cost-id",
        "label": "Shipping",
        "type": "number"
      }
    ],
    "rows": [
      {
        "c": [
          {
            "v": "January"
          },
          {
            "v": 19,
            "f": "42 items"
          },
          {
            "v": 12,
            "f": "Ony 12 items"
          },
          {
            "v": 7,
            "f": "7 servers"
          },
          {
            "v": 4
          }
        ]
      },
      {
        "c": [
          {
            "v": "February"
          },
          {
            "v": 13
          },
          {
            "v": 1,
            "f": "1 unit (Out of stock this month)"
          },
          {
            "v": 12
          },
          {
            "v": 2
          }
        ]
      },
      {
        "c": [
          {
            "v": "March"
          },
          {
            "v": 24
          },
          {
            "v": 5
          },
          {
            "v": 11
          },
          {
            "v": 6
          }
        ]
      }
    ]
  },
  "options": {
    "title": "Sales per month",
    "isStacked": "true",
    "fill": 20,
    "displayExactValues": true,
    "vAxis": {
      "title": "Sales unit",
      "gridlines": {
        "count": 10
      }
    },
    "hAxis": {
      "title": "Date"
    }
  },
  "formatters": {}
}
				
 */
		
}
