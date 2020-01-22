package com.bonitasoft.custompage.awacs.monitoring;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.flownode.ActivityInstance;
import org.bonitasoft.engine.bpm.flownode.ActivityInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.flownode.ArchivedActivityInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.flownode.ArchivedFlowNodeInstance;
import org.bonitasoft.engine.bpm.flownode.ArchivedFlowNodeInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.flownode.FlowElementContainerDefinition;
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance;
import org.bonitasoft.engine.bpm.process.DesignProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessDefinitionNotFoundException;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfoSearchDescriptor;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.bpm.process.ProcessInstanceSearchDescriptor;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.engine.bpm.flownode.ActivityDefinition;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.execution.state.State;
import org.json.simple.JSONValue;

import com.bonitasoft.custompage.awacs.toolbox.AwacsGraph;
import com.bonitasoft.custompage.awacs.toolbox.AwacsGraph.ChartColumn;
import com.bonitasoft.custompage.awacs.toolbox.AwacsToolbox;
import com.bonitasoft.custompage.awacs.toolbox.AwacsGraph.ChartParameters;
import com.bonitasoft.custompage.awacs.toolbox.AwacsGraph.ChartSerie;

public class MonitoringProcesses {
		public static Logger logger = Logger.getLogger(MonitoringProcesses.class.getName());

		private final static String cstAttributNbActivityPeriod="nbActivitiesPeriod";
		
		
		public static class MonitorProcessInput {
				public Long processId;
				public long defaultWarningNearbyTasks = 100;
				public long defaultWarningNbOverflowTasks = 100;
				public long defaultWarningNbTasks = 0;
				public int activityPeriodInMn = 120;
				public int defaultMaxItems=1000;
				public boolean studypastactivities=true;
				
				
				public static MonitorProcessInput getInstanceFromJsonSt( String jsonSt)
				{
						MonitorProcessInput monitorProcessInput = new MonitorProcessInput();
						if (jsonSt == null) {
								return monitorProcessInput;
						}						
								HashMap<String, Object> jsonHash = (HashMap<String, Object>) JSONValue.parse(jsonSt);
								monitorProcessInput.defaultWarningNearbyTasks = AwacsToolbox.jsonToLong( jsonHash.get("defaultWarningNearbyTasks"),100L);
								monitorProcessInput.defaultWarningNbOverflowTasks = AwacsToolbox.jsonToLong(jsonHash.get("defaultWarningNbOverflowTasks"),100L);
								monitorProcessInput.defaultWarningNbTasks = AwacsToolbox.jsonToLong(jsonHash.get("defaultWarningNbTasks"),0L);
								monitorProcessInput.activityPeriodInMn  = AwacsToolbox.jsonToLong(jsonHash.get("activityPeriodInMn"),0L).intValue();
								monitorProcessInput.defaultMaxItems  = AwacsToolbox.jsonToLong(jsonHash.get("defaultmaxitems"),1000L).intValue();
								monitorProcessInput.studypastactivities= AwacsToolbox.jsonToBoolean(jsonHash.get("studypastactivities"),Boolean.TRUE);
								
								return monitorProcessInput;
				}
				
		}

		/**
		 * Activity Item
		 *
		 */
		public static class ActivityItem
		{
				public HashMap<String,Object> activityMap = new HashMap<String,Object>();
				public ProcessItem processItem;
				public long activityOnPeriod=0;
				public void put(String attribut, Object value)
				{
						activityMap.put(attribut, value);
				}
				
				public void addActivityPeriod( Date archivedDate )
				{
						activityOnPeriod++;
						processItem.addActivityPeriod( archivedDate );
				}
				
				
		}
		

		/**
		 * ProcessItem
		 *
		 */
		public static class ProcessItem
		{
				public HashMap<String,Object> processMap = new HashMap<String,Object>();
				ArrayList<ActivityItem> listActivities = new ArrayList<ActivityItem>();
				private Date baseDate = null;
				public ProcessItem(Date baseDate)
				{
						this.baseDate = baseDate;
				}
				public ActivityItem addActivity()
				{
						ActivityItem activityItem = new ActivityItem();
						activityItem.processItem = this;
						listActivities.add( activityItem );
						return activityItem;
				}
				public void put(String attribut, Object value)
				{
						processMap.put(attribut, value);
				}

				public void sortListActivity()
				{
				Collections.sort(listActivities, new Comparator<ActivityItem>() {
						public int compare(ActivityItem s1, ActivityItem s2) {
								return ((String) s1.activityMap.get("name")).compareTo((String) s2.activityMap.get("name"));
						}
				});
				};
				
				public HashMap<String,Long> registerRange = new HashMap<String,Long>();
				
				public String getKey( Calendar c)
				{
						return  c.get(Calendar.YEAR)+"/"+c.get(Calendar.MONTH)+"/"+c.get(Calendar.DAY_OF_MONTH)+" "+c.get(Calendar.HOUR_OF_DAY)+":"+ (int) (c.get(Calendar.MINUTE)/10)+"0";					
				}
				
				/**
				 * create a default value in all range
				 * @param numberOfMinutes
				 */
				public void createAllRanges( int numberOfMinutes)
				{
						Calendar c = Calendar.getInstance();
						c.setTime( baseDate );
						c.add(Calendar.MINUTE,  - numberOfMinutes );
						
						for (int i=0;i<numberOfMinutes/ 10;i++)
						{
								String key = getKey( c );
								if (registerRange.get(key)==null)
										registerRange.put(key, Long.valueOf(0));
								c.add( Calendar.MINUTE, 10 );
						}
				}
				public void addActivityPeriod( Date archivedDate )
				{
						Calendar c = Calendar.getInstance();
						c.setTime( archivedDate );
						String key = getKey( c );					
						if (registerRange.get(key)!=null)
								registerRange.put(key, registerRange.get( key ) +1 );
						else
								registerRange.put(key, 1L);
				}

				public HashMap<String,Object> getJson()
				{						
						sortListActivity();
						long cumulActivityOnPeriod=0;
						ArrayList<HashMap<String, Object>> activitiesListMap = new ArrayList<HashMap<String, Object>>();
						for (ActivityItem activityItem : listActivities)
						{
								activityItem.activityMap.put(cstAttributNbActivityPeriod, activityItem.activityOnPeriod);
								cumulActivityOnPeriod +=activityItem.activityOnPeriod;
								activitiesListMap.add( activityItem.activityMap);
						}
						processMap.put("activities", activitiesListMap);
						processMap.put("cstAttributNbActivityPeriod", cumulActivityOnPeriod);
						
						// range activity !
						/* String debug="";
						for (String key : registerRange.keySet())
						{
								debug+=key+"="+registerRange.get(key)+",";
								
						}
						logger.info(">>>>>>>>>>>>>>>>>>>>>>>>>>Range "+debug);
						processMap.put("range",debug);
						 */
						
						
						ArrayList<HashMap<String, Object>> graphListMap = new ArrayList<HashMap<String, Object>>();
						
						List<ChartColumn> listValues = new ArrayList<ChartColumn>();
						
						for (String key : registerRange.keySet())
						{
								// listValue.add( new ChartColumn(key, "Value", registerRange.get(key).longValue()));
						    ChartColumn charColumn = new ChartColumn( key );
						    charColumn.addSerie( new ChartSerie( "Value", registerRange.get(key).longValue()));
						    listValues.add( charColumn );
						}
						HashMap<String, Object> oneGraph = new HashMap<String,Object>();
						
						AwacsGraph awacsGraph = new AwacsGraph();
						ChartParameters chartParameters = new ChartParameters("Activity","AreaChart", "Case created", "number", "Case created");
						chartParameters.displayTitleFrequency=50;
						awacsGraph.setValues(listValues);
						awacsGraph.setChartParameters( chartParameters );
						oneGraph.put("createcase", awacsGraph.getJson());
						graphListMap.add( oneGraph );
						
						processMap.put("processgraph",graphListMap);
						
						return processMap;
				}
		}
		/**
		 * 
		 * @param monitorProcessInput
		 * @param processAPI
		 * @return
		 */
		public static HashMap<String, Object> monitorProcesses(MonitorProcessInput monitorProcessInput, ProcessAPI processAPI) {

				HashMap<String, Object> mapResult = new HashMap<String, Object>();
				String status="";
				long currentTime = System.currentTimeMillis();
				long beginAnalysis = currentTime;
				logger.info("Input "+monitorProcessInput.defaultWarningNearbyTasks+" "+monitorProcessInput.defaultWarningNbOverflowTasks+" " +monitorProcessInput.defaultWarningNbTasks);
				Calendar c = Calendar.getInstance();
				Date dateReferenceTo = c.getTime();
				c.add(Calendar.HOUR_OF_DAY, -2);
				Date dateReferenceFrom = c.getTime();
				ArrayList<ProcessItem> listProcessItem = new ArrayList<ProcessItem>();
				
				long totalMaxItems = 0;
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
				try {
						// keep all activity to create the deep activity
						HashMap<Long,ActivityItem> activitiesSet = new HashMap<Long,ActivityItem>();
						
						
						SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0, 1000); // for process, 1000 is enought
						if (monitorProcessInput.processId != null)
								searchOptionsBuilder.filter(ProcessDeploymentInfoSearchDescriptor.ID, monitorProcessInput.processId);
						SearchResult<ProcessDeploymentInfo> searchResult = processAPI.searchProcessDeploymentInfos(searchOptionsBuilder.done());
						
						for (ProcessDeploymentInfo processDeployment : searchResult.getResult()) {
								ProcessItem processItem = new ProcessItem(dateReferenceFrom);
								processItem.createAllRanges( monitorProcessInput.activityPeriodInMn );
								listProcessItem.add(processItem);
								processItem.put("name", processDeployment.getName());
								processItem.put("version", processDeployment.getVersion());								
								processItem.put("id", processDeployment.getProcessId());
								processItem.put("state", processDeployment.getConfigurationState().toString());
								processItem.put("deployedDate", simpleDateFormat.format(processDeployment.getDeploymentDate()));
								processItem.put("showDetails", Boolean.FALSE);
								
								String description = processDeployment.getDescription();
								long warmOverflow  = decodeValue(description, "warmoverflowtasks", monitorProcessInput.defaultWarningNbOverflowTasks);
								long warmNearby  = decodeValue(description, "warnearbytasks", monitorProcessInput.defaultWarningNearbyTasks);
								long warmTasks  = decodeValue(description, "warmtask", monitorProcessInput.defaultWarningNbTasks);
								int maxItems  = (int) decodeValue(description, "maxitems", monitorProcessInput.defaultMaxItems);

								totalMaxItems += maxItems;
								 
								logger.info("Trace Process ["+processDeployment.getName()+"] description["+description
												+"] warmOverflow:["+warmOverflow
												+"] warmNearby:["+warmNearby
												+"] warmTask:["+warmTasks
												+"] maxItems:["+maxItems+"]");
								
								int controlProcess=0;
								
								long timeBeginProcess = System.currentTimeMillis();
								// now search all activity
								DesignProcessDefinition processDefinition = processAPI.getDesignProcessDefinition(processDeployment.getProcessId());
								FlowElementContainerDefinition flowElementContainer = processDefinition.getProcessContainer();
								List<ActivityDefinition> list = flowElementContainer.getActivities();
								long processNbOverflowDate = 0;
								long processNbNearbyDate = 0;
								long processNbFailedTask = 0;
								long processNbExecutingTask = 0;
								long processNbOpenTasks = 0;
								
								// ------------------------------------- loop on activity
								for (ActivityDefinition activityDefinition : list) {
										long timeBeginActivity = System.currentTimeMillis();
										ActivityItem activityItem = processItem.addActivity();
										activitiesSet.put(activityDefinition.getId(), activityItem);
										// keep a pointer to the parent process
										activityItem.put("name", activityDefinition.getName());
										activityItem.put("id", activityDefinition.getId());
										// how many case there ?
										SearchOptionsBuilder searchOptionBuilder = new SearchOptionsBuilder(0, maxItems+1);
										searchOptionBuilder.filter(ActivityInstanceSearchDescriptor.PROCESS_DEFINITION_ID, processDeployment.getProcessId());
										searchOptionBuilder.filter(ActivityInstanceSearchDescriptor.NAME, activityDefinition.getName());
										long nbOverflowDate = 0;
										long nbNearbyDate = 0;
										long nbFailedTask = 0;
										long nbExecutingTask = 0;
										try {
												SearchResult<ActivityInstance> searchResultActivitie = processAPI.searchActivities(searchOptionBuilder.done());
												for (ActivityInstance activityInstance : searchResultActivitie.getResult()) {
														if (activityInstance instanceof HumanTaskInstance) {
																HumanTaskInstance humanTask = (HumanTaskInstance) activityInstance;
																Date dueDate = humanTask.getExpectedEndDate();
																
																//                      11:20				11:30             11:40
																// DueDate                            + 
																// if currentTime:                                      x ==> OverflowDate
																// if currentTime               x                         ==> between dueDate - 10 mn and due Date
																if (dueDate != null) {
																		if ( currentTime > dueDate.getTime() )
																				nbOverflowDate++;
																		else if ( (currentTime > (dueDate.getTime() -  60 * 60 * 1000)) && (currentTime < dueDate.getTime()))
																				nbNearbyDate++;
																}
																/* logger.info("Trace Activity ["+activityDefinition.getName()+"] dueDate["+dueDate+"] currentTime["+currentTime
																				+"] Nearby ? ["+(dueDate.getTime() -  60 * 60 * 1000)+" < "+dueDate.getTime()+" ? "
																				+ " TotalOverflow "+nbOverflowDate + "Total near by "+ nbNearbyDate);
																				*/
														}
														if ("failed".equals(activityInstance.getState())) {
																nbFailedTask++;
														}

														else if ("executing".equals(activityInstance.getState())) {
																nbExecutingTask++;
														}
														// logger.info("state[" + activityInstance.getState() + "]");
												}

												activityItem.put("nbTasks", searchResultActivitie.getCount());
												activityItem.put("nbOverflowTasks", nbOverflowDate);
												activityItem.put("nbNearbyTasks", nbNearbyDate);
												activityItem.put("nbFailedTasks", nbFailedTask);
												activityItem.put("nbExecutingTasks", nbExecutingTask);
												activityItem.put(cstAttributNbActivityPeriod, Long.valueOf(0));

												int controlActivity=0;
												if (nbFailedTask > 0)
														controlActivity |= cstFailed;
												if (nbOverflowDate > warmOverflow)
														controlActivity |= cstWarmOverDueDate;
												if (searchResultActivitie.getCount() > warmTasks)
														controlActivity |= cstWarmOpenTask;
												if (nbNearbyDate > warmNearby)
														controlActivity |= cstWarmNearDueDate;
												if (searchResultActivitie.getCount() > maxItems)
														controlActivity |= cstErrorTooManyItems;
														
												controlProcess |= controlActivity;
												// logger.info("Name ["+processDeployment.getName()+"] : "+activityDefinition.getName()+":"+controlActivity+"->"+controlProcess);
												
												activityItem.put("analysisStatus", getDetail(controlActivity));
												
												activityItem.put("shownearbywarning", nbNearbyDate > warmNearby );
												activityItem.put("showoverflowwarning", nbOverflowDate > warmOverflow );
												activityItem.put("showtaskswarning", searchResultActivitie.getCount() > warmTasks );											
												activityItem.put("showtasksfailed", nbFailedTask > 0 );
												activityItem.put("showerrormaxitems", ( controlActivity & cstErrorTooManyItems) > 0 );

												long timeEndActivity = System.currentTimeMillis();
												activityItem.put("timeAnalysisMs", (timeEndActivity - timeBeginActivity));
												
												
												// logger.info("Trace activity ["+activityMap+"]");
												processNbOpenTasks += searchResultActivitie.getCount();
												processNbOverflowDate += nbOverflowDate;
												processNbNearbyDate += nbNearbyDate;
												processNbFailedTask += nbFailedTask;
												processNbExecutingTask += nbExecutingTask;
												
										} catch (SearchException e) {
												StringWriter sw = new StringWriter();
												e.printStackTrace(new PrintWriter(sw));

												logger.severe("Search exception [" + e.toString() + "] at " + sw);
												status+="Error "+e.toString()+";";
										}
										
										
								} // end loop on activities

								// ------------------ end loop on activity
								String expl="";
								if (warmOverflow>0)
										expl+="WarmOverflow("+warmOverflow+")";
								if (warmNearby>0)
											expl+="WarmNear("+warmNearby+")";
								if (warmTasks>0)
										expl+="WarmTasks("+warmTasks+")";
								
							
								
								processItem.put("analysisStatus", expl+":"+getDetail(controlProcess));
								
								processItem.put("nbTasks", processNbOpenTasks);
								processItem.put("nbOverflowTasks", processNbOverflowDate);
								processItem.put("nbNearbyTasks", processNbNearbyDate);
								processItem.put("nbFailedTasks", processNbFailedTask);
								processItem.put("nbExecutingTasks", processNbExecutingTask);
								processItem.put(cstAttributNbActivityPeriod, Long.valueOf(0));

								processItem.put("shownearbywarning", processNbNearbyDate > warmNearby );
								processItem.put("showoverflowwarning", processNbOverflowDate > warmOverflow );
								processItem.put("showtaskswarning", processNbOpenTasks > warmTasks );
								processItem.put("showtasksfailed", processNbFailedTask > 0 );
								processItem.put("showerrormaxitems", ( controlProcess & cstErrorTooManyItems) > 0 );

								SearchOptionsBuilder searchOptionBuilder = new SearchOptionsBuilder(0,  maxItems+1);
								searchOptionBuilder.filter(ProcessInstanceSearchDescriptor.PROCESS_DEFINITION_ID, processDeployment.getProcessId());
								
								SearchResult<ProcessInstance> searchProcessInstance = processAPI.searchProcessInstances(searchOptionBuilder.done());
								processItem.put("nbCases", searchProcessInstance.getCount());
								// logger.info("NbCases2 ["+processDeployment.getName()+"] : "+processDeployment.getProcessId()+": "+searchProcessInstance.getCount());
								if (searchProcessInstance.getCount() > maxItems)
								{
										processItem.put("showerrormaxitems", maxItems );
								}
								

								
								
								long timeEndProcess = System.currentTimeMillis();
								processItem.put("timeAnalysisMs", (timeEndProcess - timeBeginProcess));
						}
						
						
						
						
						
						
						// past activity
						// It's not possible to filter on the process definition (ArchivedActivityInstanceSearchDescriptor.PROCESS_DEFINITION_ID does not work)
						// So, run on each past activity
						
						if (monitorProcessInput.studypastactivities)
						{
						int index=0;
						do
						{
								// past activity on the process
								searchOptionsBuilder = new SearchOptionsBuilder(index, 1000);
								index+=10000;
								searchOptionsBuilder.between(ArchivedFlowNodeInstanceSearchDescriptor.REACHED_STATE_DATE, dateReferenceFrom.getTime(), dateReferenceTo.getTime());
								SearchResult<ArchivedFlowNodeInstance> searchActivityArchived = processAPI.searchArchivedFlowNodeInstances(searchOptionsBuilder.done());
								
								
								logger.info("################### NbActivity beetween ["+dateReferenceFrom+","+dateReferenceTo+"] - ("+index+"-10000) : "+searchActivityArchived.getCount()
										+" activitiesSet ["+activitiesSet+"]");
						
    						for (ArchivedFlowNodeInstance archivedFlowNodeInstance : searchActivityArchived.getResult())
    						{
    								Long activityId = archivedFlowNodeInstance.getFlownodeDefinitionId();
    								ActivityItem activityItem = activitiesSet.get( activityId );
    								/* logger.info("activityId ="+activityId+" found ? "+(activityItem==null ? "No":"Yes")
    												+" name="+archivedFlowNodeInstance.getFlownodeDefinitionId()
    												+" name="+archivedFlowNodeInstance.getName()
    												+ " type="+archivedFlowNodeInstance.getType()
    												+ " state="+archivedFlowNodeInstance.getState());
    												*/ 								
    								if (activityItem!=null)
    								{
    										activityItem.addActivityPeriod( archivedFlowNodeInstance.getArchiveDate());											
    								}
    						}
    						if (searchActivityArchived.getCount()<1000)
										break;
    						if (index > totalMaxItems)
    						{
    								logger.info("Stop search, the maximum item is over at "+totalMaxItems);
    								status+="Too much Archived items, stop analysis at "+totalMaxItems+";";
    								break;
    						}
						
						} while (1==1);
						}
						
				} catch (SearchException e) {
						StringWriter sw = new StringWriter();
						e.printStackTrace(new PrintWriter(sw));

						logger.severe("Search exception [" + e.toString() + "] at " + sw);
						status+="Error "+e.toString();
				} catch (ProcessDefinitionNotFoundException e1) {
						StringWriter sw = new StringWriter();
						e1.printStackTrace(new PrintWriter(sw));

						logger.severe("Search exception [" + e1.toString() + "] at " + sw);
						status+="Error "+e1.toString();
				}
				
				// ----------------------- Json the result
			// sort process by name
				Collections.sort(listProcessItem, new Comparator<ProcessItem>() {
						public int compare(ProcessItem s1, ProcessItem s2) {
								return ((String) s1.processMap.get("name")).compareTo((String) s2.processMap.get("name"));
						}
				});
				ArrayList<HashMap<String,Object>> listProcessMap = new ArrayList<HashMap<String,Object>>();
				for (ProcessItem processItem : listProcessItem)
				{
						listProcessMap.add( processItem.getJson());
				}
				long endAnalysis = System.currentTimeMillis();
				status+="Analysis in "+(endAnalysis - beginAnalysis)+" ms;";
				
				mapResult.put("processes", listProcessMap);
				mapResult.put("status", status);
				return mapResult;
		}

		/**
		 * search a marker in the string
		 * 
		 * @param description
		 * @param marker
		 * @param defaultValue
		 * @return
		 */
		public static long decodeValue(String description, String marker, long defaultValue) {
				
				if (description == null || description.length()==0)
						return defaultValue;
				if (description.indexOf(marker + ":") == -1)
				{
						return defaultValue;
				}
				String valueSt = description.substring( description.indexOf(marker + ":") + marker.length()+1);
				int posEndBlank = valueSt.indexOf(" ");
				int posEndComma = valueSt.indexOf(";");
				if (posEndBlank == -1)
						posEndBlank = valueSt.length();
				if (posEndComma == -1)
						posEndComma = valueSt.length();
				valueSt = valueSt.substring(0, Math.min(posEndBlank, posEndComma));
				
				try {
						return Long.valueOf(valueSt);
				} catch (Exception e) {
				}
				;
				return defaultValue;
		}

		
		private static int cstFailed=1;
		private static int cstWarmOverDueDate=2;
		private static int cstWarmNearDueDate=4;
		private static int cstWarmOpenTask=8;
		private static int cstErrorTooManyItems = 16;
		
		
		public static String getDetail(int control)
		{
				String result="";
				if ( (control & cstFailed) >0 )
						result+=",Failed";
				if ((control & cstWarmOverDueDate)>0)
						result+=",DueDate";
				if ((control & cstWarmNearDueDate)>0)
						result+=",NearDueDate";
				if ((control & cstWarmOpenTask)>0)
						result+=",TooTasks";
				if (result.length()>0)
						result = result.substring(1);
				return result;
				
		}
		
}
