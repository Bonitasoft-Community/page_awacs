package com.bonitasoft.custompage.awacs.casehistory.cmdtimer;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.bonitasoft.engine.builder.BuilderFactory;
import org.bonitasoft.engine.command.SCommandExecutionException;
import org.bonitasoft.engine.command.SCommandParameterizationException;
import org.bonitasoft.engine.command.TenantCommand;
import org.bonitasoft.engine.commons.exceptions.SBonitaException;
import org.bonitasoft.engine.core.process.definition.ProcessDefinitionService;
import org.bonitasoft.engine.core.process.definition.model.SFlowNodeDefinition;
import org.bonitasoft.engine.core.process.definition.model.SFlowNodeType;
import org.bonitasoft.engine.core.process.definition.model.SProcessDefinition;
import org.bonitasoft.engine.core.process.definition.model.event.SEventDefinition;
import org.bonitasoft.engine.core.process.definition.model.event.SStartEventDefinition;
import org.bonitasoft.engine.core.process.instance.api.ProcessInstanceService;
import org.bonitasoft.engine.core.process.instance.api.event.EventInstanceService;
import org.bonitasoft.engine.core.process.instance.model.SFlowElementsContainerType;
import org.bonitasoft.engine.core.process.instance.model.SProcessInstance;
import org.bonitasoft.engine.core.process.instance.model.event.SCatchEventInstance;
import org.bonitasoft.engine.core.process.instance.model.event.SEventInstance;
import org.bonitasoft.engine.execution.job.JobNameBuilder;
import org.bonitasoft.engine.jobs.TriggerTimerEventJob;
import org.bonitasoft.engine.log.technical.TechnicalLogSeverity;
import org.bonitasoft.engine.log.technical.TechnicalLoggerService;
import org.bonitasoft.engine.scheduler.SchedulerService;
import org.bonitasoft.engine.scheduler.builder.SJobDescriptorBuilderFactory;
import org.bonitasoft.engine.scheduler.builder.SJobParameterBuilderFactory;
import org.bonitasoft.engine.scheduler.model.SJobDescriptor;
import org.bonitasoft.engine.scheduler.model.SJobParameter;
import org.bonitasoft.engine.scheduler.trigger.OneShotTrigger;
import org.bonitasoft.engine.scheduler.trigger.Trigger;
import org.bonitasoft.engine.service.TenantServiceAccessor;



;

public class CmdGetTimer extends TenantCommand {

		public static String cstResultListEvents= "ListEvents";
		public static String cstResultEventId = "Id";
		public static String cstResultEventStateName = "StateName";
		public static String cstResultTimeInMs = "TIMEINMS";
		public static String cstResultExpl = "Expl";
		public static String cstResultEventJobName = "JobName";
		public static String cstResultEventJobClassName = "JobClassName";
		public static String cstResultEventJobParam = "JobParam";
		public static String cstResultEventJobIsStillSchedule = "JobIsStillSchedule";
		
		public static String cstResultStatus = "Status";
		public static String cstResultStatus_FAIL = "FAIL";
		public static String cstResultStatus_OK = "OK";
		public static String cstResultStatus_PING="PING";
		
		
		public static String cstParamProcessInstanceId = "processinstanceid";
		public static String cstParamPing = "ping";

		


		/**
		 * Change the time of an timer. parameters are tenantid : optional, 1 per
		 * default activityid : name of the activity ELSE the activityName +
		 * processinstanceid shoud be provided activityname (if not activityid is
		 * given) processinstanceid processinstanceid of the case to change
		 * timername the name of the boundary timer newtimerdate the new date of
		 * this process intance. Format is yyyyMMdd HH:MM:ss
		 */
		public Serializable execute(Map<String, Serializable> parameters, TenantServiceAccessor serviceAccessor) throws SCommandParameterizationException, SCommandExecutionException {

				TechnicalLoggerService technicalLoggerService = serviceAccessor.getTechnicalLoggerService();
				HashMap<String, Object> finalStatus = new HashMap<String, Object>();
				finalStatus.put(cstResultTimeInMs, System.currentTimeMillis());
				StringBuffer deepExplanation = new StringBuffer();
				ArrayList<HashMap<String,Object>> listEvents  = new ArrayList<HashMap<String,Object>>();
				HashMap<String,Object> result = new HashMap<String, Object>();
				result.put(cstResultListEvents, listEvents);
				
				try {
						// ------------------- ping ?
						Object ping = parameters.get(cstParamPing);
						if (ping != null) {
								result.put(cstResultStatus, cstResultStatus_PING);
								return result;
						}

						// ------------------- service
						ProcessDefinitionService processDefinitionService = serviceAccessor.getProcessDefinitionService();
						ProcessInstanceService processInstanceService = serviceAccessor.getProcessInstanceService();
						SchedulerService schedulerService = serviceAccessor.getSchedulerService();
						EventInstanceService eventInstanceService = serviceAccessor.getEventInstanceService();

						// ------------------- parameter
						Long processInstanceId = (Long) parameters.get(cstParamProcessInstanceId);
																																									 // is
					
					
						// ------------------- objects linked to the process
						SProcessInstance sProcessInstance = processInstanceService.getProcessInstance(processInstanceId);
						SProcessDefinition sProcessDefinition = processDefinitionService.getProcessDefinition(sProcessInstance.getProcessDefinitionId());
						int fromIndex = 0;
						List<SEventInstance> eventInstances;
						do {
								eventInstances = eventInstanceService.getEventInstances(processInstanceId, fromIndex, 100, null, null);
								fromIndex += 100;
								for (SEventInstance instance : eventInstances) {
										if (instance instanceof SCatchEventInstance)
										{
												HashMap<String,Object> oneEvent = new HashMap<String, Object>();
												listEvents.add( oneEvent );
												oneEvent.put(cstResultEventId, instance.getId());
												oneEvent.put(cstResultEventStateName, instance.getStateName());
												
												SCatchEventInstance sCatchEventInstance = (SCatchEventInstance) instance;
												// retrieve the eventDefinition
												SEventDefinition sEventDefinition = (SEventDefinition) sProcessDefinition.getProcessContainer().getFlowNode(sCatchEventInstance.getName());
												if (sEventDefinition == null) {
														// Backup plan !
														Set<SFlowNodeDefinition> setFlowNodeDefinition = sProcessDefinition.getProcessContainer().getFlowNodes();
														StringBuffer listEventsDefinitionFounds = new StringBuffer();
														for (SFlowNodeDefinition sFlowNodeDefinition : setFlowNodeDefinition) {
																listEventsDefinitionFounds.append(sFlowNodeDefinition.getName() + "[" + sFlowNodeDefinition.getClass().getName() + "],");
																if (sFlowNodeDefinition.getName().equals(sCatchEventInstance.getName()) && sFlowNodeDefinition instanceof SEventDefinition) {
																		sEventDefinition = (SEventDefinition) sFlowNodeDefinition;
																		break;
																}
														}
														if (sEventDefinition == null)
														{
																result.put(cstResultStatus, cstResultStatus_FAIL);
																result.put(cstResultExpl,  "sEventDefinition not found [" + listEventsDefinitionFounds + "]");
																return result;
														}
												}
												//-------------------- delete the current schedule
												final String jobName = getTimerEventJobName(sProcessDefinition.getId(), sEventDefinition, sCatchEventInstance);
												oneEvent.put(cstResultEventJobName, jobName);

												SJobDescriptor jobDescriptor = getJobDescriptor(jobName);
												if (jobDescriptor!=null)
												{
  												oneEvent.put(cstResultEventJobClassName,jobDescriptor.getJobClassName());
  												List<SJobParameter> jobParameters = getJobParameters(sProcessDefinition, sEventDefinition, sCatchEventInstance);
  												HashMap<String, Object> hashJobParameters = new HashMap<String, Object>();
  												for (SJobParameter jobParameter : jobParameters)
  												{
  														hashJobParameters.put(jobParameter.getKey(),jobParameter.getValue());
  												}
  												oneEvent.put(cstResultEventJobParam, hashJobParameters);
  
  												boolean isStillSchedule = schedulerService.isStillScheduled(jobDescriptor);
  												oneEvent.put(cstResultEventJobIsStillSchedule, isStillSchedule);
												}										
										}
								}
						} while (eventInstances.size() == 100);

						result.put(cstResultStatus, cstResultStatus_OK);

				} catch (SBonitaException e) {
						StringWriter sw = new StringWriter();
						e.printStackTrace(new PrintWriter(sw));
						String exceptionDetails = sw.toString();

						deepExplanation.append("Exception :" + e.toString() + "] -" + exceptionDetails);
						result.put(cstResultStatus, cstResultStatus_FAIL);
						result.put(cstResultExpl, deepExplanation);

				} catch (Exception e) {
						StringWriter sw = new StringWriter();
						e.printStackTrace(new PrintWriter(sw));
						String exceptionDetails = sw.toString();

						deepExplanation.append("Exception :" + e.toString() + "] -" + exceptionDetails);
						result.put(cstResultStatus, cstResultStatus_FAIL);
						result.put(cstResultExpl, deepExplanation);
				}

				return result;
		}

		/**
		 * return the job timer name
		 * 
		 * @param processDefinitionId
		 * @param sCatchEventInstance
		 * @param sEventDefinition
		 * @return
		 */
		private String getTimerEventJobName(long processDefinitionId, SEventDefinition sEventDefinition, SCatchEventInstance sCatchEventInstance) {
				return JobNameBuilder.getTimerEventJobName(processDefinitionId, sEventDefinition, sCatchEventInstance);
		}

	

		/**
		 * copy from the TImerEventHandlerStrategy.java
		 * 
		 * @param processDefinition
		 * @param eventDefinition
		 * @param eventInstance
		 * @return
		 */
		private List<SJobParameter> getJobParameters(final SProcessDefinition processDefinition, final SEventDefinition eventDefinition, final SCatchEventInstance eventInstance) {
				final List<SJobParameter> jobParameters = new ArrayList<SJobParameter>();
				jobParameters.add(BuilderFactory.get(SJobParameterBuilderFactory.class).createNewInstance("processDefinitionId", processDefinition.getId()).done());
				jobParameters.add(BuilderFactory.get(SJobParameterBuilderFactory.class).createNewInstance("containerType", SFlowElementsContainerType.PROCESS.name()).done());
				jobParameters.add(BuilderFactory.get(SJobParameterBuilderFactory.class).createNewInstance("eventType", eventDefinition.getType().name()).done());
				jobParameters.add(BuilderFactory.get(SJobParameterBuilderFactory.class).createNewInstance("targetSFlowNodeDefinitionId", eventDefinition.getId()).done());
				if (SFlowNodeType.START_EVENT.equals(eventDefinition.getType())) {
						final SStartEventDefinition startEvent = (SStartEventDefinition) eventDefinition;
						jobParameters.add(BuilderFactory.get(SJobParameterBuilderFactory.class).createNewInstance("isInterrupting", startEvent.isInterrupting()).done());
				}
				if (eventInstance != null) {
						jobParameters.add(BuilderFactory.get(SJobParameterBuilderFactory.class).createNewInstance("flowNodeInstanceId", eventInstance.getId()).done());
				}
				return jobParameters;
		}

		/**
		 * copy from the TImerEventHandlerStrategy.java
		 */
		private SJobDescriptor getJobDescriptor(final String jobName) {
				return BuilderFactory.get(SJobDescriptorBuilderFactory.class).createNewInstance(TriggerTimerEventJob.class.getName(), jobName, false).done();
		}

		
}
