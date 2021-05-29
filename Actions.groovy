import java.lang.management.RuntimeMXBean;
import java.lang.management.ManagementFactory;

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.logging.Logger;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
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
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.sql.Clob;
import java.util.Date;

import org.apache.commons.lang3.StringEscapeUtils


import org.bonitasoft.engine.service.TenantServiceSingleton

import org.bonitasoft.web.extension.page.PageContext;
import org.bonitasoft.web.extension.page.PageController;
import org.bonitasoft.web.extension.page.PageResourceProvider;

import org.bonitasoft.engine.exception.AlreadyExistsException;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.CreationException;
import org.bonitasoft.engine.exception.DeletionException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;

import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.api.IdentityAPI;


import org.bonitasoft.engine.service.TenantServiceAccessor;
import org.bonitasoft.engine.service.TenantServiceSingleton;



import com.bonitasoft.custompage.awacs.monitoring.MonitoringProcesses;
import com.bonitasoft.custompage.awacs.monitoring.MonitoringProcesses.MonitorProcessInput;
import com.bonitasoft.custompage.awacs.monitoring.MonitoringUsers;
import com.bonitasoft.custompage.awacs.monitoring.MonitoringUsers.MonitorUsersInput;


public class Actions {

    private static Logger logger= Logger.getLogger("org.bonitasoft.custompage.awacs.groovy");
    
    
 
    
      // 2018-03-08T00:19:15.04Z
    public final static SimpleDateFormat sdfJson = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    public final static SimpleDateFormat sdfHuman = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* doAction */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    public static Index.ActionAnswer doAction(HttpServletRequest request, String paramJsonSt, HttpServletResponse response, PageResourceProvider pageResourceProvider, PageContext pageContext) {
                
        // logger.info("#### PingActions:Actions start");
        Index.ActionAnswer actionAnswer = new Index.ActionAnswer(); 
        
        Object jsonParam = (paramJsonSt==null ? null : JSONValue.parse(paramJsonSt));
          
        try {
            String action=request.getParameter("action");
            logger.info("#### log:Actions  action is["+action+"] !");
            if (action==null || action.length()==0 )
            {
                actionAnswer.isManaged=false;
                logger.info("#### log:Actions END No Actions");
                return actionAnswer;
            }
            actionAnswer.isManaged=true;
            
            // Hello
            APISession apiSession = pageContext.getApiSession();
            HttpSession httpSession = request.getSession();            
            ProcessAPI processAPI = TenantAPIAccessor.getProcessAPI(apiSession);
            IdentityAPI identityAPI = TenantAPIAccessor.getIdentityAPI(apiSession);
			
            long tenantId = apiSession.getTenantId();          
            TenantServiceAccessor tenantServiceAccessor = TenantServiceSingleton.getInstance(tenantId);             
            //Make sure no action is executed if the CSRF protection is active and the request header is invalid
            if (! TokenValidator.checkCSRFToken(request, response)) {
                logger.severe("#### log:Actions  Token Validator failed on action["+action+"] !");
                actionAnswer.isResponseMap=false;
                return actionAnswer;
            }
 
            if ("monitoringprocess".equals(action))
                {
                    logger.info("#### awacsCustomPage:Groovy monitoringProcesses");
                    MonitorProcessInput monitorProcessInput =  MonitorProcessInput.getInstanceFromJsonSt( paramJsonSt );
                    actionAnswer.isResponseMap=true;
                    actionAnswer.responseMap = MonitoringProcesses.monitorProcesses( monitorProcessInput, processAPI );
                }
                else if ("monitoringuser".equals(action))
                {
                    logger.info("#### awacsCustomPage:Groovy monitoringUsers");
                    MonitorUsersInput monitorUsersInput =  MonitorUsersInput.getInstanceFromJsonSt( paramJsonSt );
                    actionAnswer.isResponseMap=true;
                    actionAnswer.responseMap = MonitoringUsers.monitorUsers( monitorUsersInput, identityAPI, processAPI );
                }
             
                
            
            logger.info("#### log:Actions END responseMap ="+actionAnswer.responseMap.size());
            return actionAnswer;
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionDetails = sw.toString();
            logger.severe("#### log:Groovy Exception ["+e.toString()+"] at "+exceptionDetails);
            actionAnswer.isResponseMap=true;
            actionAnswer.responseMap.put("Error", "log:Groovy Exception ["+e.toString()+"] at "+exceptionDetails);
            

            
            return actionAnswer;
        }
    }

   
}
