package com.bonitasoft.custompage.awacs.toolbox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.ListCellRenderer;

import org.jboss.logging.Logger;

public class AwacsGraph {

    public static Logger logger = Logger.getLogger(AwacsGraph.class.getName());

    /*
     * descrip the graph, parameters and values
     */
    public static class ChartParameters
    {

        // typeGraph ColumnChart, AreaChart
        public String titleChart;
        public String typeChart;
        public int displayTitleFrequency = 1;
        public boolean nullIsZero=false;
        /**
         * Label of the different serie
         * One column = multiple serie
         */
        public List<String> labelCol;
        public List<String> typeCol;
        public List<String> idCol;

    
        /**
         * constructor for ONE column
         * 
         * @param typeGraph ColumnChart, AreaChart
         * @param title
         * @param type
         * @param id
         */
        public ChartParameters(String titleChart, String typeGraph, String titleCol, String typeCol, String idCol)
        {
            this.titleChart = titleChart;
            this.typeChart = typeGraph;
            this.labelCol = new ArrayList<String>();
            this.labelCol.add(titleCol); // first is the Label
            this.labelCol.add(titleCol); // second is the first value
            this.typeCol = new ArrayList<String>();
            this.typeCol.add("string"); // label is a string
            this.typeCol.add(typeCol);
            this.idCol = new ArrayList<String>();
            this.idCol.add("titleCol"); // id of the first value, which are a label
            this.idCol.add(idCol);
        }

        /**
         * method to ADD a new column
         * @param titleChart
         * @param typeGraph
         * @param titleCol
         * @param typeCol
         * @param idCol
         * @param titleCol2
         * @param typeCol2
         * @param idCol2
         */
        public void addColumn(String titleCol, String typeCol, String idCol)
        {
            this.labelCol.add(titleCol);
            this.typeCol.add(typeCol);
            this.idCol.add(idCol);
        }
        
       
    }

    /**
     * The value of One Column. One column may have MULTIPLE value (call Serie) (then display in red, blue...).
     */
    public static class ChartColumn
    {

        public String titleColumn;

        // one column, multiple value
        private List<ChartSerie> series = new ArrayList<ChartSerie>();

        // one column = multiple serie
        // private List<String> name;
        // public List<Long> value;
        // construct with One Column
        public ChartColumn(String titleColumn)
        {
            this.titleColumn = titleColumn;
        }

        /* you can have Multiple serie in the same column */
        public void addSerie(ChartSerie chartSerie)
        {
            series.add(chartSerie);

        }

    }

    /**
         * 
         *
         */
    public static class ChartSerie
    {

        public String label;
        public Long value;

        public ChartSerie(String label, Long value)
        {
            this.label = label;
            this.value = value;
        }
    }

    
    public List<ChartColumn> listValues;
    public ChartParameters chartParameters;
    
    public void setValues(List<ChartColumn> listValues)
    {
        this.listValues = listValues;
    }
    
    public void setChartParameters( ChartParameters chartParameters )
    {
        this.chartParameters = chartParameters;
    }
    /**
     * build a JSON graph from the value and parameters
     * 
     * @param title
     * @param chartParameters
     * @param listValues
     * @return
     */
    public  HashMap<String, Object> getJson() {

        HashMap<String, Object> result = new HashMap<String, Object>();
        if ("test".equals(chartParameters.titleChart))
        {
            result.put("type", "ColumnChart");
            result.put("displayed", Boolean.TRUE);
            HashMap<String, Object> options = new HashMap<String, Object>();
            result.put("options", options);
            options.put("title", "Title of graph");

            HashMap<String, Object> resultData = new HashMap<String, Object>();
            result.put("data", resultData);

            ArrayList<HashMap<String, Object>> resultListCols = new ArrayList<HashMap<String, Object>>();
            resultData.put("cols", resultListCols);
            HashMap<String, Object> onColData = new HashMap<String, Object>();
            resultListCols.add(onColData);
            onColData.put("type", "string");
            onColData.put("id", "titleOfColum");
            onColData.put("label", "titleOfColum");

            onColData = new HashMap<String, Object>();
            resultListCols.add(onColData);
            onColData.put("type", "number");
            onColData.put("id", "Value 1");
            onColData.put("label", "valueLabel 1");

            onColData = new HashMap<String, Object>();
            resultListCols.add(onColData);
            onColData.put("type", "number");
            onColData.put("id", "value 2");
            onColData.put("label", "valueLabel 2");

            ArrayList<HashMap<String, Object>> resultListRows = new ArrayList<HashMap<String, Object>>();
            resultData.put("rows", resultListRows);

            HashMap<String, Object> onRowData = new HashMap<String, Object>();
            resultListRows.add(onRowData);
            onRowData.put("c", getValueOneDimension("January", 19L, "Jua19 items", 12L, "Only 12"));

            onRowData = new HashMap<String, Object>();
            resultListRows.add(onRowData);
            onRowData.put("c", getValueOneDimension("February", 21L, "Feb21 items", 15L, "Only 12"));

            return result;

        }

        /**
         * structure
         * "\"cols\": [ {"type": "string", "id":"id1", "label": "label1" },
         * {"type": "number", "id":"id1", "label": "label1" },], "
         * "rows": [
         * {
         * c: [
         * { "v": "January" },"
         * { "v": 19,"f": "42 items" },
         * { "v": 12,"f": "Ony 12 items" },
         * ]
         * },
         * {
         * c: [
         * { "v": "January" },"
         * { "v": 19,"f": "42 items" },
         * { "v": 12,"f": "Ony 12 items" },
         * ]
         * },
         */

        result.put("type", chartParameters.typeChart);

        HashMap<String, Object> options = new HashMap<String, Object>();
        result.put("options", options);
        options.put("title", chartParameters.titleChart);

        result.put("displayed", Boolean.TRUE);
        HashMap<String, Object> resultData = new HashMap<String, Object>();
        result.put("data", resultData);

        // label !
        ArrayList<HashMap<String, Object>> listLabels = new ArrayList<HashMap<String, Object>>();
        resultData.put("cols", listLabels);

        String resultLabel = "";
        for (int i = 0; i < chartParameters.labelCol.size(); i++)
        {
            HashMap<String, Object> oneLabel = new HashMap<String, Object>();
            listLabels.add(oneLabel);
            oneLabel.put("type", chartParameters.typeCol.get(i));
            oneLabel.put("id", chartParameters.idCol.get(i));
            oneLabel.put("label", chartParameters.labelCol.get(i));
        }

        // values !
        ArrayList<HashMap<String, Object>> listRows = new ArrayList<HashMap<String, Object>>();
        resultData.put("rows", listRows);
        int count = -1; // then the first count is OK
        for (ChartColumn column : listValues)
        {
            count++;
            if (column.series.size() != chartParameters.typeCol.size() - 1)
                logger.info("Bad number of parameters describe [" + (chartParameters.typeCol.size() - 1) + "] cols in the chartParameters, give ["
                        + column.series.size() + "] series");
            HashMap<String, Object> onRowData = new HashMap<String, Object>();
            listRows.add(onRowData);

            onRowData.put("c", getValueFromColumn(column, (count % chartParameters.displayTitleFrequency) == 0, chartParameters.nullIsZero));

        }
        /*
         * + "\"options\": { "
         * + "\"bars\": \"horizontal\","
         * + "\"title\": \""+title+"\", \"fill\": 20, \"displayExactValues\": true,"
         * + "\"vAxis\": { \"title\": \"ms\", \"gridlines\": { \"count\": 100 } }"
         */
        // 				+"\"isStacked\": \"true\","

        //		    +"\"displayExactValues\": true,"
        //		    
        //		    +"\"hAxis\": { \"title\": \"Date\" }"
        //		    +"},"
        // logger.info("TrackRangeChart >>"+ valueChart+"<<");
        //	String valueChartBar="{\"type\": \"BarChart\", \"displayed\": true, \"data\": {\"cols\": [{ \"id\": \"perf\", \"label\": \"Perf\", \"type\": \"string\" }, { \"id\": \"perfbase\", \"label\": \"ValueBase\", \"type\": \"number\" },{ \"id\": \"perfvalue\", \"label\": \"Value\", \"type\": \"number\" }], \"rows\": [{ \"c\": [ { \"v\": \"Write BonitaHome\" }, { \"v\": 550 },  { \"v\": 615 } ] },{ \"c\": [ { \"v\": \"Read BonitaHome\" }, { \"v\": 200 },  { \"v\": 246 } ] },{ \"c\": [ { \"v\": \"Read Medata\" }, { \"v\": 370 },  { \"v\": 436 } ] },{ \"c\": [ { \"v\": \"Sql Request\" }, { \"v\": 190 },  { \"v\": 213 } ] },{ \"c\": [ { \"v\": \"Deploy process\" }, { \"v\": 40 },  { \"v\": 107 } ] },{ \"c\": [ { \"v\": \"Create 100 cases\" }, { \"v\": 3600 },  { \"v\": 16382 } ] },{ \"c\": [ { \"v\": \"Process 100 cases\" }, { \"v\": 3700 },  { \"v\": 16469 } ] }]}, \"options\": { \"bars\": \"horizontal\",\"title\": \"Performance Measure\", \"fill\": 20, \"displayExactValues\": true,\"vAxis\": { \"title\": \"ms\", \"gridlines\": { \"count\": 100 } }}}";

        return result;
    }

    /**
     * gejnherate this [
     * + "     { \"v\": \"January\" }," <<< title of the serie
     * + "     { \"v\": 19,\"f\": \"42 items\" }," << one value
     * + "     { \"v\": 12,\"f\": \"Ony 12 items\" }," << one value
     * + "   ]"
     */

    private static List<HashMap<String, Object>> getValueFromColumn(ChartColumn seriesOneColumn, boolean generateTitle, boolean nullIsZero)
    {
        ArrayList<HashMap<String, Object>> listOneValue = new ArrayList<HashMap<String, Object>>();
        HashMap<String, Object> oneValue = new HashMap<String, Object>();
        oneValue.put("v", generateTitle ? seriesOneColumn.titleColumn : "");
        listOneValue.add(oneValue);
        for (ChartSerie serie : seriesOneColumn.series)
        {
            oneValue = new HashMap<String, Object>();
            
            oneValue.put("v", nullIsZero && serie.value==null ? 0 : serie.value);
            oneValue.put("f", serie.label);
            listOneValue.add(oneValue);
        }
        return listOneValue;

    }

    private static List<HashMap<String, Object>> getValueOneDimension(String label, Long firstValue, String firstLabel, Long secondValue, String secondLabel)
    {

        ArrayList<HashMap<String, Object>> listOneValue = new ArrayList<HashMap<String, Object>>();
        HashMap<String, Object> oneValue = new HashMap<String, Object>();
        oneValue.put("v", label);
        listOneValue.add(oneValue);
        if (firstValue != null)
        {
            oneValue = new HashMap<String, Object>();
            oneValue.put("v", firstValue);
            oneValue.put("f", firstLabel);
            listOneValue.add(oneValue);
        }
        if (secondValue != null)
        {
            oneValue = new HashMap<String, Object>();
            oneValue.put("v", secondValue);
            oneValue.put("f", secondLabel);
            listOneValue.add(oneValue);
        }
        return listOneValue;
    }
}
