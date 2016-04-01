import java.lang.management.RuntimeMXBean;
import java.lang.management.ManagementFactory;

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.text.SimpleDateFormat;
import java.util.logging.Logger;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.lang.Runtime;

import org.json.simple.JSONObject;
import org.codehaus.groovy.tools.shell.CommandAlias;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;


import javax.naming.Context;
import javax.naming.InitialContext;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import java.sql.DatabaseMetaData;

import org.apache.commons.lang3.StringEscapeUtils
 
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.console.common.server.page.PageContext
import org.bonitasoft.console.common.server.page.PageController
import org.bonitasoft.console.common.server.page.PageResourceProvider
import org.bonitasoft.engine.exception.AlreadyExistsException;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.CreationException;
import org.bonitasoft.engine.exception.DeletionException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;

import com.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.api.CommandAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.api.IdentityAPI;
import com.bonitasoft.engine.api.PlatformMonitoringAPI;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.bpm.flownode.ActivityInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.flownode.ArchivedActivityInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.flownode.ActivityInstance;
import org.bonitasoft.engine.bpm.flownode.ArchivedFlowNodeInstance;
import org.bonitasoft.engine.bpm.flownode.ArchivedActivityInstance;
import org.bonitasoft.engine.search.SearchOptions;
import org.bonitasoft.engine.search.SearchResult;

import org.bonitasoft.engine.command.CommandDescriptor;
import org.bonitasoft.engine.command.CommandCriterion;
import org.bonitasoft.engine.bpm.flownode.ActivityInstance;


import com.bonitasoft.custompage.awacs.monitoring.MonitoringProcesses;
import com.bonitasoft.custompage.awacs.monitoring.MonitoringProcesses.MonitorProcessInput;
import com.bonitasoft.custompage.awacs.monitoring.MonitoringUsers;
import com.bonitasoft.custompage.awacs.monitoring.MonitoringUsers.MonitorUsersInput;


public class Index implements PageController {

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response, PageResourceProvider pageResourceProvider, PageContext pageContext) {
	
		Logger logger= Logger.getLogger("org.bonitasoft.custompage.awacs.groovy");
		
		
		try {
			def String indexContent;
			pageResourceProvider.getResourceAsStream("Index.groovy").withStream { InputStream s-> indexContent = s.getText() };
			response.setCharacterEncoding("UTF-8");
			PrintWriter out = response.getWriter()

			String action=request.getParameter("action");
			logger.info("#### awacsCustomPage:Groovy  action is["+action+"] !");
			if (action==null || action.length()==0 )
			{
				runTheBonitaIndexDoGet( request, response,pageResourceProvider,pageContext);
				return;
			}
			String paramJson= request.getParameter("paramjson");
			
			
			APISession session = pageContext.getApiSession()
			ProcessAPI processAPI = TenantAPIAccessor.getProcessAPI(session);
			PlatformMonitoringAPI platformMonitoringAPI = TenantAPIAccessor.getPlatformMonitoringAPI(session);
			IdentityAPI identityAPI = TenantAPIAccessor.getIdentityAPI(session);

			CommandAPI commandAPI = TenantAPIAccessor.getCommandAPI(session);
			
			HashMap<String,Object> answer = null;
			
			
			if ("monitoringprocess".equals(action))
			{
				logger.info("#### awacsCustomPage:Groovy monitoringProcesses");
				MonitorProcessInput monitorProcessInput =  MonitorProcessInput.getInstanceFromJsonSt( paramJson );
				answer  = MonitoringProcesses.monitorProcesses( monitorProcessInput, processAPI );				
			}
			else if ("monitoringuser".equals(action))
			{
				logger.info("#### awacsCustomPage:Groovy monitoringUsers");
				MonitorUsersInput monitorUsersInput =  MonitorUsersInput.getInstanceFromJsonSt( paramJson );
				answer  = MonitoringUsers.monitorUsers( monitorUsersInput, identityAPI, processAPI );				
			}
			if (answer!=null)
			{
				String jsonSt = JSONValue.toJSONString( answer );
				out.write( jsonSt );
				logger.info("#### awacsCustomPage:Groovy return json["+jsonSt+"]" );
				out.flush();
				out.close();
				return;
			}
			out.write( "Unknow command" );
			out.flush();
			out.close();
			return;
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionDetails = sw.toString();
			logger.severe("#### awacsCustomPage:Groovy Exception ["+e.toString()+"] at "+exceptionDetails);
		}
	}

	
	/** -------------------------------------------------------------------------
	 *
	 * getCaseHistoryInJson
	 * 
	 */
	private String getCaseHistoryInJson( long processInstanceId, boolean showSubProcess, PageResourceProvider pageResourceProvider, ProcessAPI processAPI,IdentityAPI identityApi, CommandAPI commandAPI)
	{
		Logger logger= Logger.getLogger("org.bonitasoft.custompage.awacs.groovy");
		InputStream is = pageResourceProvider.getResourceAsStream("lib/CustomPageAwacs-1.0.1.jar");
		
		
		String ping= com.bonitasoft.custompage.awacs.casehistory.CaseHistory.getPing();
		logger.info("#### awacsCustomPage:Groovy Ping"+ping);
		HashMap<String,Object> mapDetails = com.bonitasoft.custompage.awacs.casehistory.CaseHistory.getCaseDetails(processInstanceId, showSubProcess, is ,processAPI,identityApi,commandAPI);
	 
		String jsonDetailsSt = JSONValue.toJSONString( mapDetails );
		logger.info("#### awacsCustomPage:Groovy End return ["+mapDetails+"] ==>"+jsonDetailsSt);
		return jsonDetailsSt;
	}
	
	
	
	/** -------------------------------------------------------------------------
	 *
	 *getIntegerParameter
	 * 
	 */
	private int getIntegerParameter(HttpServletRequest request, String paramName, int defaultValue)
	{
		String valueParamSt = request.getParameter(paramName);
		if (valueParamSt==null  || valueParamSt.length()==0)
		{
			return defaultValue;
		}
		int valueParam=defaultValue;
		try
		{
			valueParam = Integer.valueOf( valueParamSt );
		}
		catch( Exception e)
		{
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionDetails = sw.toString();
			
			logger.severe("#### awacsCustomPage:Groovy awacs: getinteger : Exception "+e.toString()+" on  ["+valueParamSt+"] at "+exceptionDetails );
			valueParam= defaultValue;
		}
		return valueParam;
	}
	/** -------------------------------------------------------------------------
	 *
	 *getBooleanParameter
	 * 
	 */
	private boolean getBooleanParameter(HttpServletRequest request, String paramName, boolean defaultValue)
	{
		String valueParamSt = request.getParameter(paramName);
		if (valueParamSt==null  || valueParamSt.length()==0)
		{
			return defaultValue;
		}
		boolean valueParam=defaultValue;
		try
		{
			valueParam = Boolean.valueOf( valueParamSt );
		}
		catch( Exception e)
		{
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionDetails = sw.toString();
			
			logger.severe("#### awacsCustomPage:Groovy awacs: getBoolean : Exception "+e.toString()+" on  ["+valueParamSt+"] at "+exceptionDetails );
			valueParam= defaultValue;
		}
		return valueParam;
	}
	
	/** -------------------------------------------------------------------------
	 *
	 *runTheBonitaIndexDoGet
	 * 
	 */
	private void runTheBonitaIndexDoGet(HttpServletRequest request, HttpServletResponse response, PageResourceProvider pageResourceProvider, PageContext pageContext) {
				try {
						def String indexContent;
						pageResourceProvider.getResourceAsStream("index.html").withStream { InputStream s->
								indexContent = s.getText()
						}
						
						def String pageResource="pageResource?&page="+ request.getParameter("page")+"&location=";
						
						// 7.0 Live application : do not do that
						// indexContent= indexContent.replace("@_USER_LOCALE_@", request.getParameter("locale"));
						// indexContent= indexContent.replace("@_PAGE_RESOURCE_@", pageResource);
						
						response.setCharacterEncoding("UTF-8");
						PrintWriter out = response.getWriter();
						out.print(indexContent);
						out.flush();
						out.close();
				} catch (Exception e) {
						e.printStackTrace();
				}
		}

}
