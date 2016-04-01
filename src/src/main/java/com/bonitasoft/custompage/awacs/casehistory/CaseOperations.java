package com.bonitasoft.custompage.awacs.casehistory;

import java.util.HashMap;
import java.util.logging.Logger;

import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.ProcessAPI;


public class CaseOperations {
		
		private static Logger logger = Logger.getLogger( CaseOperations.class.getName() );


		/**
		 * cancelCase
		 * @param processInstanceId
		 * @param processAPI
		 * @return
		 */
		public static HashMap<String, Object> cancelCase(long processInstanceId, ProcessAPI processAPI) {
				HashMap<String, Object> caseOperation = new HashMap<String, Object>();
				logger.info("Cancel ProcessInstance["+processInstanceId+"]");
				try
				{
						processAPI.cancelProcessInstance(processInstanceId);
						caseOperation.put("STATUS", "OK");
						logger.info("Cancel ProcessInstance["+processInstanceId+"] Ok");
				}
				catch(Exception e)
				{
						logger.info("Cancel ProcessInstance["+processInstanceId+"] Failed");
						caseOperation.put("STATUS", "FAILED");
				}
				return caseOperation;
		}
}
