package com.bonitasoft.custompage.awacs.casehistory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.bonitasoft.engine.api.CommandAPI;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.flownode.ActivityInstance;
import org.bonitasoft.engine.bpm.flownode.ArchivedActivityInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.flownode.ArchivedFlowNodeInstance;
import org.bonitasoft.engine.bpm.flownode.EventCriterion;
import org.bonitasoft.engine.bpm.flownode.EventInstance;
import org.bonitasoft.engine.bpm.flownode.FlowNodeInstance;
import org.bonitasoft.engine.bpm.flownode.FlowNodeInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.flownode.GatewayInstance;
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstance;
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstanceNotFoundException;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.bpm.process.ProcessInstanceNotFoundException;
import org.bonitasoft.engine.command.CommandCriterion;
import org.bonitasoft.engine.command.CommandDescriptor;
import org.bonitasoft.engine.command.CommandNotFoundException;
import org.bonitasoft.engine.exception.AlreadyExistsException;
import org.bonitasoft.engine.exception.CreationException;
import org.bonitasoft.engine.exception.DeletionException;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.identity.UserNotFoundException;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;

import com.bonitasoft.custompage.awacs.casehistory.CaseGraphDisplay.ActivityTimeLine;
import com.bonitasoft.custompage.awacs.casehistory.cmdtimer.CmdGetTimer;

public class CaseHistory {

		final static Logger logger = Logger.getLogger( CaseHistory.class.getName() );
		public final static String cstActivityName="ActivityName";
		public final static String cstPerimeter = "Perimeter";
		public final static String cstActivityId = "ActivityId";
		public final static String cstActivityFlownodeDefId = "FlownodeDefId";
		public final static String cstActivityType = "Type";
		public final static String cstActivityState = "State";
		public final static String cstActivityDate ="ActivityDate";
		public final static String cstHumanActivityDate = "HumanActivityDate";
		public final static String cstActivitySourceObjectId="SourceObjectId";
		
		
		public final static String cstActivityDateBegin = "DateBegin";
		public final static String cstActivityDateEnd = "DateEnd";
		
		public final static String cstActivityIsTerminal = "IsTerminal";
		public final static String cstActivityJobIsStillSchedule = "JobIsStillSchedule";
		
		
		private final static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS");

		public static String  getPing() {
				return "PING:"+System.currentTimeMillis();
		}
						
		private static class TimeCollect{
				public Long activitySourceObjectId;
				public Long activityId;
				public String activityType;
				
				// a Task has the following markup
				// timeEntry 																										: 
				// timeAvailable : Available For User (all Input Connector ran) : 
				// timeUserExecute : User submit the task                       : 
				// timeFinish Complete (all end connector ran)                  : 
				
				// USER TASK
				// timeEntry    			initializing.reachedStateDate or initializing.archivedDate  API: YES
				// timeAvailable  		ready.reachedStateDate      	 API : No
				// timeUserExecute 		ready.archivedDate             API : YES
				// timeFinish					Completed.archivedDate         API : YES
				// ==> No way to calculated the time of input connector or the time the task is waiting
				
				
				// Service TASK
				// timeEntry    			initializing.archivedDate  API: YES
				// timeAvailable  		                           API : No
				// timeUserExecute 		                           API : No
				// timeFinish					Completed.archivedDate     API : YES
  			// ==> No way to calculated the time of input connector or Wait Connector
				
				
				public Long timeEntry;
				public Long timeAvailable;
				public Long timeUserExecute;
				public Long timeFinish;
				public String toString()
				{ return activityId+": timeEntry("+timeEntry+") available("+timeAvailable+") userExecute("+timeUserExecute+") complete("+timeFinish+")"; }
				
		}
		/**
		 * ----------------------------------------------------------------
		 * 
		 * getCaseDetails
		 * 
		 * @return
		 */
		public static HashMap<String, Object> getCaseDetails(long processInstanceId,  boolean showSubProcess, InputStream inputStreamJarFile,ProcessAPI processAPI, IdentityAPI identityAPI, CommandAPI commandAPI) {
				// Activities
				logger.info("############### start caseDetail on ["+processInstanceId+"] ShowSubProcess["+showSubProcess+"]");
				
				HashMap<String, Object> caseDetails = new HashMap<String, Object>();
				caseDetails.put("errormessage", "");
				try {
						ArrayList<HashMap<String, Object>> listHistoryJson = new ArrayList<HashMap<String, Object>>();
						// multi instance task : if the task is declare as a multi instance, it considere as finish ONLY when we see the 
						// MULTI_INSTANCE_ACTIVITY / completed
						Set<Long> listMultiInstanceActivity = new HashSet<Long>();
						
						SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0, 1000);
						// searchOptionsBuilder.filter(ActivityInstanceSearchDescriptor.PROCESS_INSTANCE_ID,
						// processInstanceId);

						if (showSubProcess)
								searchOptionsBuilder.filter(FlowNodeInstanceSearchDescriptor.ROOT_PROCESS_INSTANCE_ID, processInstanceId);
						else
								searchOptionsBuilder.filter(FlowNodeInstanceSearchDescriptor.PARENT_PROCESS_INSTANCE_ID, processInstanceId);

						// SearchResult<ActivityInstance> searchActivity =
						// processAPI.searchActivities(searchOptionsBuilder.done());
						SearchResult<FlowNodeInstance> searchFlowNode = processAPI.searchFlowNodeInstances(searchOptionsBuilder.done());
						if (searchFlowNode.getCount() == 0) {
								// caseDetails.put("errormessage", "No activities found");
						}
						for (FlowNodeInstance activityInstance : searchFlowNode.getResult()) {
								HashMap<String, Object> mapActivity = new HashMap<String, Object>();
								listHistoryJson.add(mapActivity);


								mapActivity.put( cstPerimeter, "ACTIVE");
								mapActivity.put( cstActivityName, activityInstance.getName());
								mapActivity.put( cstActivityId, activityInstance.getId());
								Date date = null;
								if (activityInstance instanceof ActivityInstance) {
										date = ((ActivityInstance) activityInstance).getLastUpdateDate();
								}
								if (activityInstance instanceof GatewayInstance)
								{
										date = null;
								}
								logger.info("##### FLOWNODE Activity["+activityInstance.getName()+"] Class["+activityInstance.getClass().getName()+"]");
								if (date != null) {
										mapActivity.put( cstActivityDate, date.getTime());
										mapActivity.put( cstHumanActivityDate, getDisplayDate(date));
								}
								// mapActivity.put("isterminal",
								// activityInstance.().toString());
								mapActivity.put( cstActivityType, activityInstance.getType().toString());
								mapActivity.put( cstActivityState, activityInstance.getState().toString());
								mapActivity.put( cstActivityFlownodeDefId, activityInstance.getFlownodeDefinitionId());
								mapActivity.put("parentcontainer", activityInstance.getParentContainerId());

								if ("MULTI_INSTANCE_ACTIVITY".equals(activityInstance.getType().toString())) {
										listMultiInstanceActivity.add(  activityInstance.getFlownodeDefinitionId() );
								}
								mapActivity.put("expl", "FlowNode :" + activityInstance.getFlownodeDefinitionId() + "] ParentContainer[" + activityInstance.getParentContainerId() + "] RootContainer["
												+ activityInstance.getRootContainerId() + "]");

								if (activityInstance.getExecutedBy() != 0) {
										try {
												User user = identityAPI.getUser(activityInstance.getExecutedBy());
												String userExecuted = (user != null ? user.getUserName() : "unknow") + " (" + activityInstance.getExecutedBy() + ")";
												mapActivity.put("ExecutedBy", userExecuted);
										} catch (UserNotFoundException ue) {
												mapActivity.put("ExecutedBy", "UserNotFound id=" + activityInstance.getExecutedBy());
										}
										;

								}
								logger.info("#### casehistory [" + mapActivity + "]");

						}
						logger.info("#### casehistory on processInstanceId[" + processInstanceId + "] : found [" + listHistoryJson.size() + "] activity");
						// archived
						searchOptionsBuilder = new SearchOptionsBuilder(0, 1000);
						if (showSubProcess)
								searchOptionsBuilder.filter(ArchivedActivityInstanceSearchDescriptor.ROOT_PROCESS_INSTANCE_ID, processInstanceId);
						else
								searchOptionsBuilder.filter(ArchivedActivityInstanceSearchDescriptor.PARENT_PROCESS_INSTANCE_ID, processInstanceId);

						SearchResult<ArchivedFlowNodeInstance> searchActivityArchived = processAPI.searchArchivedFlowNodeInstances(searchOptionsBuilder.done());
						for (ArchivedFlowNodeInstance activityInstance : searchActivityArchived.getResult()) {

								HashMap<String, Object> mapActivity = new HashMap<String, Object>();
								mapActivity.put( cstPerimeter, "ARCHIVED");
								mapActivity.put( cstActivityName, activityInstance.getName());
								mapActivity.put( cstActivityId, activityInstance.getId());

								Date date = activityInstance.getArchiveDate();
								mapActivity.put( cstActivityDate, date.getTime());
								mapActivity.put( cstHumanActivityDate, getDisplayDate(date));
								mapActivity.put( cstActivityIsTerminal, activityInstance.isTerminal() ? "Terminal" : "");
								mapActivity.put( cstActivityType, activityInstance.getType().toString());
								mapActivity.put( cstActivityState, activityInstance.getState().toString());
								mapActivity.put( cstActivityFlownodeDefId, activityInstance.getFlownodeDefinitionId());
								mapActivity.put("parentactivityid", activityInstance.getParentActivityInstanceId());
								mapActivity.put("parentcontainer", activityInstance.getParentContainerId());
								mapActivity.put( cstActivitySourceObjectId, activityInstance.getSourceObjectId());
								mapActivity.put("expl",
												"FlowNode :" + activityInstance.getFlownodeDefinitionId() + "] ParentActivityInstanceId[" + activityInstance.getParentActivityInstanceId() + "] ParentContainer["
																+ activityInstance.getParentContainerId() + "] RootContainer[" + activityInstance.getRootContainerId() + "] Source[" + activityInstance.getSourceObjectId() + "]");

								if (activityInstance.getExecutedBy() != 0) {
										try {
												User user = identityAPI.getUser(activityInstance.getExecutedBy());

												String userExecuted = (user != null ? user.getUserName() : "unknow") + " (" + activityInstance.getExecutedBy() + ")";
												mapActivity.put("ExecutedBy", userExecuted);
										} catch (UserNotFoundException ue) {
												mapActivity.put("ExecutedBy", "UserNotFound id=" + activityInstance.getExecutedBy());
												caseDetails.put("errormessage", "UserNotFound id=" + activityInstance.getExecutedBy());

										}
										;
								}
								logger.info("#### casehistory Activity[" + mapActivity + "]");

								listHistoryJson.add(mapActivity);
						}

						List<EventInstance> listEventInstance = processAPI.getEventInstances(processInstanceId, 0, 1000, EventCriterion.NAME_ASC);
						for (EventInstance eventInstance : listEventInstance) {
								HashMap<String, Object> mapActivity = new HashMap<String, Object>();
								mapActivity.put( cstPerimeter, "ARCHIVED");
								mapActivity.put( cstActivityName, eventInstance.getName());
								mapActivity.put( cstActivityId, eventInstance.getId());

								mapActivity.put( cstActivityIsTerminal, "");
								mapActivity.put( cstActivityType, eventInstance.getType().toString());
								mapActivity.put( cstActivityState, eventInstance.getState().toString());
								mapActivity.put( cstActivityFlownodeDefId, eventInstance.getFlownodeDefinitionId());
								mapActivity.put("parentcontainer", eventInstance.getParentContainerId());

								mapActivity.put(
												"expl",
												"EventInstance :" + eventInstance.getFlownodeDefinitionId() + "] ParentContainer[" + eventInstance.getParentContainerId() + "] RootContainer["
																+ eventInstance.getRootContainerId() + "]");
								listHistoryJson.add(mapActivity);
						}

						//-------------------------------------------- search the timer
						try {

								CommandDescriptor command = deployCommand("LongBoardgetTimer", 
												"Get timer", 
												"com.bonitasoft.custompage.longboard.casehistory.cmdtimer.CmdGetTimer", 
												inputStreamJarFile, "custompagelongboard", commandAPI);
								HashMap<String, Serializable> parameters = new HashMap<String, Serializable>();
								parameters.put(CmdGetTimer.cstParamProcessInstanceId, Long.valueOf(processInstanceId));
								Serializable resultCommand = commandAPI.execute(command.getId(), parameters);
								HashMap<String, Object> resultCommandHashmap = (HashMap<String, Object>) resultCommand;
								if (resultCommandHashmap == null)
										logger.info("#### Timer : Can't access the command");
								else {

										caseDetails.put("TimerStatus", resultCommandHashmap.get(CmdGetTimer.cstResultStatus));
										// caseDetails.put("TimerExpl", resultCommandHashmap.get(CmdGetTimer.cstResultExpl));
										ArrayList<HashMap<String, Object>> listEventsTimer = (ArrayList<HashMap<String, Object>>) resultCommandHashmap.get(CmdGetTimer.cstResultListEvents);
										caseDetails.put("TimerListEvents", listEventsTimer);
										for (HashMap<String, Object> eventTimer : listEventsTimer) {
												HashMap<String, Object> hashJobParameters = (HashMap<String, Object>) eventTimer.get(CmdGetTimer.cstResultEventJobParam);
												Long targerSflownodeDefinition = hashJobParameters == null ? null : (Long) hashJobParameters.get("targetSFlowNodeDefinitionId");

												// search if we find this activity in the list
												if (targerSflownodeDefinition != null)
														for (HashMap<String, Object> mapActivity : listHistoryJson) {
																if (targerSflownodeDefinition.equals(mapActivity.get(cstActivityFlownodeDefId))) {
																		mapActivity.put( cstActivityJobIsStillSchedule, "YES");
																		eventTimer.put( cstActivityId, mapActivity.get(cstActivityId));
																		eventTimer.put( cstActivityName, mapActivity.get(cstActivityName));
																		
																}
														}
												logger.info("#### Timer [" + eventTimer + "]");

										}
								}

						} catch (Exception e) {
								caseDetails.put("errormessage", "Error during getTimer" + e.toString());

								StringWriter sw = new StringWriter();
								e.printStackTrace(new PrintWriter(sw));
								logger.severe("During getTimer : " + e.toString() + " at " + sw.toString());
						}

						// now order all by the time
						Collections.sort(listHistoryJson, new Comparator<HashMap<String, Object>>() {
								public int compare(HashMap<String, Object> s1, HashMap<String, Object> s2) {
										Long d1 = (Long) s1.get(cstActivityDate);
										Long d2 = (Long) s2.get(cstActivityDate);
										if (d1 == null)
												d1 = Long.valueOf(0);
										if (d2 == null)
												d2 = Long.valueOf(0);

										return (d1 > d2 ? 1 : -1);
								}
						});

						caseDetails.put("Activities", listHistoryJson);

						// ---------------------------------- Synthesis
						HashMap<Long, HashMap<String, Object>> mapSynthesis = new HashMap<Long, HashMap<String, Object>>();
						for (HashMap<String, Object> mapActivity : listHistoryJson) {
								
								
								Long flowNodedefid = Long.valueOf((Long) mapActivity.get(cstActivityFlownodeDefId));
								if (mapSynthesis.get(flowNodedefid) != null)
										continue; // already analysis
								String type = (String) mapActivity.get( cstActivityType );
								if ("BOUNDARY_EVENT".equals(type))
										continue; // don't keep this kind of activity
						
								// analysis this one !
								HashMap<String, Object> oneSynthesisLine = new HashMap<String, Object>();
								mapSynthesis.put(flowNodedefid, oneSynthesisLine);
								
								oneSynthesisLine.put(cstActivityName, mapActivity.get(cstActivityName));
								oneSynthesisLine.put(cstActivityType, mapActivity.get(cstActivityType));
								
								
								
								String expl = "";
								boolean isReady=false;
								boolean isInitializing=false;
								boolean isExecuting=false;
								boolean isCanceled = false;
								boolean isCompleted= false;
								boolean isReallyTerminated= false;
								
								HashMap<Long, TimeCollect> timeCollectPerSource = new HashMap<Long, TimeCollect>();
								// calculate the line : check in the list all related event
								// to make the relation, the SOURCE ( activityInstance.getSourceObjectId()) is necessary.
								// in case of a multi instance, we will have multiple initializing / executing but with different source :
								// Instance 1 : initilizing source="344"
								// instance 2 : initializing source ="345"
								// Instance 1 executing : source ="344"
								// Instance 2 executing : source ="345"
								// then we collect the time per source
								
								// ------------------- sub loop
								for (HashMap<String, Object> mapRunActivity : listHistoryJson) {
										if (!mapRunActivity.get(cstActivityFlownodeDefId).equals(flowNodedefid))
												continue;
											
										expl += "Found state[" + mapRunActivity.get( cstActivityState ) + "]";
										Long key = (Long) mapRunActivity.get(cstActivitySourceObjectId);
										if (key==null)
												key= (Long) mapRunActivity.get(cstActivityId);
										TimeCollect timeCollect = timeCollectPerSource.get( key );
										
										if (timeCollect==null)
										{
												timeCollect = new TimeCollect();
												timeCollect.activitySourceObjectId = (Long) mapRunActivity.get(cstActivitySourceObjectId);
												timeCollect.activityId= (Long) mapRunActivity.get(cstActivityId);
												timeCollect.activityType = (String)  mapRunActivity.get( cstActivityType);
												timeCollectPerSource.put(key, timeCollect);
										}
										
										
										
										
										
										if (("initializing".equals(mapRunActivity.get(cstActivityState ))) 
														|| ("executing".equals(mapRunActivity.get(cstActivityState)))) {
												// attention : multiple initializing or executing, specialy in a Call Activity. get the min !
												// Long timeSynthesis = (Long) oneSynthesisLine.get(cstActivityDateBegin);
												timeCollect.timeEntry = (Long) mapRunActivity.get(cstActivityDate);
												logger.info("##### Synthesis Init activity["+oneSynthesisLine.get(cstActivityName)+" "+ timeCollect.toString());
										}
										
										if ("ready".equals(mapRunActivity.get(cstActivityState)))
										{
												timeCollect.timeUserExecute = (Long) mapRunActivity.get(cstActivityDate);
												isReady=true;
										}
										if ( ("completed".equals(mapRunActivity.get(cstActivityState)) 
														|| "cancelled".equals(mapRunActivity.get(cstActivityState))) 
														&&  (mapRunActivity.get( cstActivityDate) instanceof Long)) {
												timeCollect.timeFinish = (Long) mapRunActivity.get(cstActivityDate);
												isReallyTerminated=true;
												// attention ! if the task is a MULTI The task is considere
												if (listMultiInstanceActivity.contains( flowNodedefid ))
												{
														if ("MULTI_INSTANCE_ACTIVITY".equals( mapRunActivity.get( cstActivityType)))
														{
																isReallyTerminated=true;
														}
														else
																isReallyTerminated=false;
												}
										}
										
										if ("initializing".equals(mapRunActivity.get(cstActivityState)))
												isInitializing = true;
										if ("executing".equals(mapRunActivity.get(cstActivityState)))
												isExecuting = true;
										if ("ready".equals(mapRunActivity.get(cstActivityState)))
												isReady = true;
										if ("completed".equals(mapRunActivity.get(cstActivityState)) && isReallyTerminated)
												isCompleted = true;
										if ("cancelled".equals(mapRunActivity.get(cstActivityState)) && isReallyTerminated)
												isCanceled = true;
										
										
										
								} // end run sub activity lool
								// build the activity synthesis
								long mintimeInitial = -1;
								long maxtimeComplete = -1;
								long sumTimeEnterConnector=-1; // a marker
								long sumTimeWaitUser=-1; // a marker
								long sumTimeFinishConnector=0;
								for (TimeCollect timeCollect : timeCollectPerSource.values())
								{
										if (timeCollect.timeEntry!=null)
												if (mintimeInitial==-1 || timeCollect.timeEntry < mintimeInitial)
												mintimeInitial =  timeCollect.timeEntry;
										if (timeCollect.timeFinish !=null)
												if (maxtimeComplete==-1 || timeCollect.timeFinish > maxtimeComplete)
												maxtimeComplete =  timeCollect.timeFinish;
										// automatic task : we have only a timeInitial and a timeComplete 
										 
										// USER TASK
										// timeEntry    			initializing.reachedStateDate or initializing.archivedDate  API: YES
										// timeAvailable  		ready.reachedStateDate      	 API : No
										// timeUserExecute 		ready.archivedDate             API : YES
										// timeFinish					Completed.archivedDate         API : YES
										// ==> No way to calculated the time of input connector or the time the task is waiting
										
										
										// Service TASK
										// timeEntry    			initializing.archivedDate  API: YES
										// timeAvailable  		                           API : No
										// timeUserExecute 		                           API : No
										// timeFinish					Completed.archivedDate     API : YES
						  		
									
										
										
										
										if (timeCollect.timeAvailable == null)
												timeCollect.timeAvailable = timeCollect.timeEntry;

										if (timeCollect.timeUserExecute == null)
												timeCollect.timeUserExecute = timeCollect.timeAvailable;

										// multi instance is not part of the sum calculation
										if ("MULTI_INSTANCE_ACTIVITY".equals(timeCollect.activityType))
												continue;
										if (timeCollect.timeEntry != null && timeCollect.timeAvailable !=null)
										{
												if (sumTimeEnterConnector==-1)
														sumTimeEnterConnector=0;
												sumTimeEnterConnector += timeCollect.timeAvailable - timeCollect.timeEntry;
										}
										
										if (timeCollect.timeUserExecute != null && timeCollect.timeAvailable !=null)
										{
												if (sumTimeWaitUser==-1)
														sumTimeWaitUser=0;
												sumTimeWaitUser += timeCollect.timeUserExecute - timeCollect.timeAvailable;										
										}
										if (timeCollect.timeFinish != null && timeCollect.timeUserExecute !=null)
										{
												if (sumTimeFinishConnector==-1)
														sumTimeFinishConnector=0;
												sumTimeFinishConnector += timeCollect.timeFinish - timeCollect.timeUserExecute;										
										}
										// todo register connector time
										/*
										if (activityRegisterInConnector.contains(timeCollect.activityId ))
										{
												TimeConnector = connector.get(timeCollect.activityId)
										}
										*/
								}
								oneSynthesisLine.put(cstActivityDateBegin, mintimeInitial);
								oneSynthesisLine.put(cstActivityDateBegin+"ST", getDisplayDate(mintimeInitial));
								if (isReallyTerminated)
								{
										oneSynthesisLine.put(cstActivityDateEnd, maxtimeComplete);
										oneSynthesisLine.put(cstActivityDateEnd+"ST", getDisplayDate(maxtimeComplete));
								}
								
								if (isInitializing)
										oneSynthesisLine.put(cstActivityState, "initializing");
								if (isExecuting)
										oneSynthesisLine.put(cstActivityState, "Executing");
								if (isReady)
										oneSynthesisLine.put(cstActivityState, "ready");
								if (isCompleted)
										oneSynthesisLine.put(cstActivityState, "completed");
								if (isCanceled)
										oneSynthesisLine.put(cstActivityState, "cancelled");

								// now build the synthesis
								expl += "timeEnterConnector[" + sumTimeEnterConnector + "] timeUser[" + sumTimeWaitUser + "] timeFinishConnector[" + sumTimeFinishConnector + "]";
								oneSynthesisLine.put("expl", expl);
								
								oneSynthesisLine.put("enterconnector", sumTimeEnterConnector);
								oneSynthesisLine.put("user", sumTimeWaitUser);
								// case of gateway or automatic task
								oneSynthesisLine.put("finishconnector", sumTimeFinishConnector);
								
								logger.info("Calcul time:"+expl);
										
								// onAnalysis.put("end", (timeCompleted - timeCompleted));
						}
						// Then process instance information
						
						List< HashMap<String, Object>> listSynthesis = new ArrayList<HashMap<String, Object>>();
								
						// built the timeline
						
						Date currentDate = new Date();
						List<ActivityTimeLine> listTimeline = new ArrayList<ActivityTimeLine>();
						for (HashMap<String, Object> oneSynthesisLine : mapSynthesis.values()) {
								listSynthesis.add( oneSynthesisLine);
								if (oneSynthesisLine.get(cstActivityDateBegin) == null )
										continue;
								listTimeline.add(ActivityTimeLine.getActivityTimeLine((String) oneSynthesisLine.get(cstActivityName), new Date((Long) oneSynthesisLine.get(cstActivityDateBegin)), 
												oneSynthesisLine.get(cstActivityDateEnd)==null ? currentDate : new Date((Long) oneSynthesisLine.get(cstActivityDateEnd))));
						}
						// now order all by the time
						Collections.sort(listTimeline, new Comparator<ActivityTimeLine>() {
								public int compare(ActivityTimeLine s1, ActivityTimeLine s2) {
										Long d1 = s1.getDateLong();
										Long d2 = s2.getDateLong();
										return (d1 > d2 ? 1 : -1);
								}
						});
						// and order the list
						Collections.sort(listSynthesis, new Comparator<HashMap<String, Object>>() {
								public int compare(HashMap<String, Object> s1, HashMap<String, Object> s2) {
										Long d1 = s1.get(cstActivityDateBegin)==null ? 0L : (Long) s1.get(cstActivityDateBegin);
										Long d2 = s2.get(cstActivityDateBegin)==null ? 0L : (Long) s2.get(cstActivityDateBegin);
										return (d1 > d2 ? 1 : -1);
								}
						});
						caseDetails.put("synthesis", listSynthesis);
						

						
						
						String timeLineChart = CaseGraphDisplay.getActivityTimeLine("Activity", listTimeline);
						// logger.info("Return CHART>>" + timeLineChart + "<<");

						caseDetails.put("chartTimeline", timeLineChart);

						// ----------------------------------- overview
						boolean oneProcessInstanceIsFound = false;
						try {
								ProcessInstance processInstance = processAPI.getProcessInstance(processInstanceId);
								oneProcessInstanceIsFound = true;
								caseDetails.put("casestate", "ACTIF");
								caseDetails.put("startdate", processInstance.getStartDate() == null ? "" : getDisplayDate(processInstance.getStartDate()));
								caseDetails.put("enddate", processInstance.getEndDate() == null ? "" : getDisplayDate(processInstance.getEndDate()));
								caseDetails.put(
												"stringindex",
												" 1:[" + getDisplayString(processInstance.getStringIndex1()) + "] 2:[" + getDisplayString(processInstance.getStringIndex2()) + "] 3:["
																+ getDisplayString(processInstance.getStringIndex3()) + "] 4:[" + getDisplayString(processInstance.getStringIndex4()) + "] 5:["
																+ getDisplayString(processInstance.getStringIndex5()) + "]");
								ProcessDefinition processDefinition = processAPI.getProcessDefinition(processInstance.getProcessDefinitionId());
								caseDetails.put("processdefinition", processDefinition.getName() + " (" + processDefinition.getVersion() + ")");

						} catch (ProcessInstanceNotFoundException e1) {
								logger.info("processinstance ["+ processInstanceId+"] not found (not active) ");

						} catch (Exception e) {
								StringWriter sw = new StringWriter();
								e.printStackTrace(new PrintWriter(sw));

								logger.severe("During getProcessInstance : " + e.toString() + " at " + sw.toString());
								caseDetails.put("errormessage", "Error during get case history " + e.toString());
						}

						try {
								
								// search by the source
								if (! oneProcessInstanceIsFound)
								{
  								ArchivedProcessInstance archivedProcessInstance = processAPI.getFinalArchivedProcessInstance(processInstanceId);
  								logger.info("Case  ["+ processInstanceId+"]  found by getFinalArchivedProcessInstance ? "+(archivedProcessInstance==null ?  "No":"Yes"));
  								if (archivedProcessInstance!=null)
  								{
  										oneProcessInstanceIsFound = true;
  										caseDetails.put("casestate", "ARCHIVED");
  								}
  								caseDetails.put("startdate", archivedProcessInstance.getStartDate() == null ? "" : getDisplayDate(archivedProcessInstance.getStartDate()));
  								caseDetails.put("enddate", archivedProcessInstance.getEndDate() == null ? "" : getDisplayDate(archivedProcessInstance.getEndDate()));
  								
  								caseDetails.put("ArchivedDate", getDisplayDate(archivedProcessInstance.getArchiveDate()));
  								ProcessDefinition processDefinition = processAPI.getProcessDefinition(archivedProcessInstance.getProcessDefinitionId());
  								caseDetails.put("processdefinition", processDefinition.getName() + " (" + processDefinition.getVersion() + ")");
								}

						} catch (ArchivedProcessInstanceNotFoundException e1) {
								logger.info("Case found by getFinalArchivedProcessInstance ? exception so not found");
						} catch (Exception e) {
								StringWriter sw = new StringWriter();
								e.printStackTrace(new PrintWriter(sw));

								logger.severe("During getArchivedProcessInstance : " + e.toString() + " at " + sw.toString());
								caseDetails.put("errormessage", "Error during get case history " + e.toString());

						}
						;
						if (!oneProcessInstanceIsFound) {
								caseDetails.put("errormessage", "The caseId [" + processInstanceId + "] does not exist");
						}

				} catch (SearchException e1) {
						StringWriter sw = new StringWriter();
						e1.printStackTrace(new PrintWriter(sw));

						logger.severe("Error during get CaseHistory" + e1.toString() + " at " + sw.toString());
						caseDetails.put("errormessage", "Error during get case history " + e1.toString());

				} catch (Exception e) {
						StringWriter sw = new StringWriter();
						e.printStackTrace(new PrintWriter(sw));

						logger.severe("Error during get CaseHistory" + e.toString() + " at " + sw.toString());
						caseDetails.put("errormessage", "Error during get case history " + e.toString());

				}
				return caseDetails;
		}

		
		private static String getDisplayDate( Object dateObj)
		{
				if (dateObj==null)
						return "";
				if (dateObj instanceof Long)
				{
						return sdf.format( new Date( (Long) dateObj)) +"("+dateObj+")";
				}
				if (dateObj instanceof Date)
				{
						return sdf.format( (Date) dateObj) +"("+ (( (Date)dateObj).getTime())+")" ;
				}
				return "-";
		}
		/**
		 * return a string every time (if null, return "")
		 * 
		 * @param value
		 * @return
		 */
		private static String getDisplayString(String value) {
				if (value == null)
						return "";
				return value;
		}

		/**
		 * deploy a command on the server
		 * 
		 * @param commandName
		 * @param commandDescription
		 * @param className
		 * @param jarFileServer
		 * @param jarName
		 * @param commandAPI
		 * @return
		 * @throws IOException
		 * @throws CreationException
		 * @throws AlreadyExistsException
		 * @throws DeletionException
		 * @throws CommandNotFoundException
		 */
		private static CommandDescriptor deployCommand(String commandName, String commandDescription, String className, InputStream inputStreamJarFile, String jarName, CommandAPI commandAPI) throws IOException,
						AlreadyExistsException, CreationException, CommandNotFoundException, DeletionException {

				List<CommandDescriptor> listCommands = commandAPI.getAllCommands(0, 1000, CommandCriterion.NAME_ASC);
				for (CommandDescriptor command : listCommands) {
						if (commandName.equals(command.getName()))
								commandAPI.unregister(command.getId());
				}

				String message = "";
				/* File commandFile = new File(jarFileServer);
				FileInputStream fis = new FileInputStream(commandFile);
				byte[] fileContent = new byte[(int) commandFile.length()];
				fis.read(fileContent);
				fis.close();
*/
				ByteArrayOutputStream fileContent = new ByteArrayOutputStream();
				byte[] buffer = new byte[10000];
				int nbRead=0;
				while ( (nbRead = inputStreamJarFile.read(buffer))>0)
				{
						fileContent.write( buffer, 0, nbRead);
				}
				
				try {
						commandAPI.removeDependency(jarName);
				} catch (Exception e) {
				}
				;

				message += "Adding jarName [" + jarName + "] size[" + fileContent.size() + "]...";
				commandAPI.addDependency(jarName, fileContent.toByteArray());
				message += "Done.";

				message += "Registering...";
				CommandDescriptor commandDescriptor = commandAPI.register(commandName, commandDescription, className);

				return commandDescriptor;
		}
}
