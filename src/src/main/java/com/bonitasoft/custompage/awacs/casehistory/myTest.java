package com.bonitasoft.custompage.awacs.casehistory;

import java.util.HashMap;
import java.util.List;

import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.connector.ConnectorDefinition;
import org.bonitasoft.engine.bpm.flownode.FlowElementInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.flownode.impl.FlowElementContainerDefinition;
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstance;
import org.bonitasoft.engine.bpm.process.DesignProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessDefinitionNotFoundException;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.expression.Expression;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;

public class myTest {

		
		public HashMap<String,Object> search( String flowNodeInstance,ProcessAPI processAPI )
		{
				
				HashMap<String,Object> result= new HashMap<String,Object>();
				// ProcessAPI processAPI = apiAccessor.getProcessAPI();
				try {
				SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0,100);
				searchOptionsBuilder.filter(FlowElementInstanceSearchDescriptor.NAME, flowNodeInstance);
				SearchResult<ProcessInstance> searchResult = processAPI.searchProcessInstances(searchOptionsBuilder.done());
				long processInstanceId = -1;
				long processDefinitionId = -1;
				if (searchResult.getCount()==0)
				{
						SearchResult<ArchivedProcessInstance> searchResultArchived = processAPI.searchArchivedProcessInstances(searchOptionsBuilder.done());
						if (searchResultArchived.getCount()==0)
						{
								result.put("msg",  "noInstance found with ["+flowNodeInstance+"]");
								return result;
						}
						processInstanceId = searchResultArchived.getResult().get(0).getId();
						processDefinitionId = searchResultArchived.getResult().get(0).getProcessDefinitionId();
				}
				else
				{
						processInstanceId = searchResult.getResult().get(0).getId();
						processDefinitionId = searchResult.getResult().get(0).getProcessDefinitionId();
				}
				
				result.put("processinstanceid", processInstanceId);
				result.put("processdefinitionid", processDefinitionId);
				
				result.put("processdefinitionname","");
				result.put("processdefinitionversion","");
				
				
				// search the groovy script now
				DesignProcessDefinition designProcessDefinition = processAPI.getDesignProcessDefinition( processDefinitionId);
				FlowElementContainerDefinition processContainer = designProcessDefinition.getProcessContainer();
				 List<ConnectorDefinition> listConnectors = processContainer.getConnectors();
				 StringBuffer info = new StringBuffer();
				 for (ConnectorDefinition connectorDefinition : listConnectors)
				 {
						 for (String key : connectorDefinition.getInputs().keySet())
						 {
								 info.append("----------------- Connector "+key+": Name["+connectorDefinition.getName()+"]\n");
								 Expression expr =connectorDefinition.getInputs().get(key);
								 info.append("Content:"+expr.getContent());
						 }
				 }
				 result.put("info", info.toString());
				} catch (SearchException e) {
						 result.put("msg", e.toString());
				} catch (ProcessDefinitionNotFoundException e) {
						 result.put("msg", e.toString());
				}
				return result;
				
		}
}
