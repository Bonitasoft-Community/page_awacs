package com.bonitasoft.custompage.awacs.casehistory;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import org.bonitasoft.engine.api.ApiAccessType;
import org.bonitasoft.engine.api.CommandAPI;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.LoginAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;
import org.bonitasoft.engine.platform.LoginException;
import org.bonitasoft.engine.service.TenantServiceAccessor;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.util.APITypeManager;
import org.junit.Test;

public class JunitCaseHistory {

		@Test
		public void test() {
				Map<String, String> map = new HashMap<String, String>();
				map.put("server.url", "http://localhost:8080");
				map.put("application.name", "bonita");
				APITypeManager.setAPITypeAndParams(ApiAccessType.HTTP, map);

				// Set the username and password
				// final String username = "helen.kelly";
				final String username = "walter.bates";
				final String password = "bpm";

				// get the LoginAPI using the TenantAPIAccessor
				LoginAPI loginAPI;
				try {
						loginAPI = TenantAPIAccessor.getLoginAPI();
						// log in to the tenant to create a session
						APISession session = loginAPI.login(username, password);
						ProcessAPI processAPI = TenantAPIAccessor.getProcessAPI(session);
						IdentityAPI identityAPI = TenantAPIAccessor.getIdentityAPI(session);
						CommandAPI commandAPI = TenantAPIAccessor.getCommandAPI(session);

						File commandFile = new File("target/CustomPageLongBoard-1.0.1.jar");
						FileInputStream fis;

						fis = new FileInputStream(commandFile);
					
						
						HashMap<String, Object> caseDetails = CaseHistory.getCaseDetails(6, false, fis, processAPI, identityAPI, commandAPI);
						System.out.print(caseDetails);
						} catch (FileNotFoundException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
						} catch (BonitaHomeNotSetException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
				} catch (ServerAPIException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
				} catch (UnknownAPITypeException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
				} catch (LoginException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
				}
		}

}
