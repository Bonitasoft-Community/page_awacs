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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Logger;

import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.flownode.ActivityDefinition;
import org.bonitasoft.engine.bpm.flownode.ActivityInstanceCriterion;
import org.bonitasoft.engine.bpm.flownode.ArchivedFlowNodeInstance;
import org.bonitasoft.engine.bpm.flownode.ArchivedFlowNodeInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.flownode.FlowElementContainerDefinition;
import org.bonitasoft.engine.bpm.flownode.HumanTaskDefinition;
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance;
import org.bonitasoft.engine.bpm.process.DesignProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessDefinitionNotFoundException;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.identity.Group;
import org.bonitasoft.engine.identity.GroupSearchDescriptor;
import org.bonitasoft.engine.identity.Role;
import org.bonitasoft.engine.identity.RoleNotFoundException;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.identity.UserCriterion;
import org.bonitasoft.engine.identity.UserNotFoundException;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.json.simple.JSONValue;

import com.bonitasoft.custompage.awacs.toolbox.AwacsGraph;
import com.bonitasoft.custompage.awacs.toolbox.AwacsGraph.ChartColumn;
import com.bonitasoft.custompage.awacs.toolbox.AwacsGraph.ChartParameters;
import com.bonitasoft.custompage.awacs.toolbox.AwacsGraph.ChartSerie;
import com.bonitasoft.custompage.awacs.toolbox.AwacsToolbox;

public class MonitoringUsers {

    public static Logger logger = Logger.getLogger(MonitoringUsers.class.getName());

    private final static String cstAttributNbActivityPeriod = "nbActivitiesPeriod";
    private final static String cstRangeSubmitted ="submitted";

    public static class MonitorUsersInput {

        public int activityPeriodInMn = 12*60;
        public int defaultMaxItems = 1000;
        public String filterGroup = null;
        public String filterRole = null;
        public String filterUser = null;
        public long levelShowWarningOverflowTasks = 1;
        public long timeToBeActiveInMs = 5*60*1000;

        public static MonitorUsersInput getInstanceFromJsonSt(final String jsonSt)
        {
            final MonitorUsersInput monitorUsersInput = new MonitorUsersInput();
            if (jsonSt == null) {
                return monitorUsersInput;
            }
            final HashMap<String, Object> jsonHash = (HashMap<String, Object>) JSONValue.parse(jsonSt);
            monitorUsersInput.activityPeriodInMn = AwacsToolbox.jsonToLong(jsonHash.get("activityPeriodInMn"), 0L).intValue();
            monitorUsersInput.defaultMaxItems = AwacsToolbox.jsonToLong(jsonHash.get("defaultmaxitems"), 1000L).intValue();
            monitorUsersInput.filterGroup = AwacsToolbox.jsonToString(jsonHash.get("filtergroup"), null, true);
            monitorUsersInput.filterRole = AwacsToolbox.jsonToString(jsonHash.get("filterrole"), null, true);
            monitorUsersInput.filterUser = AwacsToolbox.jsonToString(jsonHash.get("filteruser"), null, true);

            return monitorUsersInput;
        }

        public String getInfos()
        {
            return "activityPeriodInMn=[" + activityPeriodInMn + "], defaultMaxItems=[" + defaultMaxItems + "],filterGroup=[" + filterGroup + "],filterRole=["
                    + filterRole + "],filterUser=[" + filterUser + "]";
        }

    }

    /**
     * Activity Item
     * When one activity is found, then it's register. One activity is a task (ready, complete)
     */
    public static class ActivityItem
    {

        public Long userId;
        public Long defFlowNodeDefid;
        public Long processDefinitionId;

        // calcul is this activity took to much time according it's due date
        public Boolean isOverflowed;

        public Date dateComplete;
        public Date dateReady;

        public void setDateCompleted(final Date dateComplete)
        {
            this.dateComplete = dateComplete;
        }

        public void setDateReady(final Date dateReady)
        {
            this.dateReady = dateReady;
        }

        public void setUserId(final long userId)
        {
            if (userId != 0) {
                this.userId = userId;
            }
        }

        public void setFlownodeDefId(final long defFlowNodeDefid)
        {
            this.defFlowNodeDefid = defFlowNodeDefid;
        }

        public void setProcessDefinitionId(final long processDefinitionId)
        {
            this.processDefinitionId = processDefinitionId;
        }

    }

    /**
     * UserItem
     */
    public static class UserItem
    {

        private final long userId;
        public String userName;
        // TODO
        public String totalName;

        public int nbTasksSubmitted = 0;

        // Number of task overflowed in the past
        public long nbOverflowedTasks = 0;

        // number of pending task at this moment
        public long nbPendingTasks = 0;

        // say is the label "Overflowed" has to be prompt
        public boolean showOverflowAssignedWarning = false;
        public boolean showOverflowPendingWarning = false;

        // number of task overflow at this moment, and assigned
        public long nbOverflowAssignedTasks = 0;

     // number of task overflow at this moment, and pending (not really assigned)
        public long nbOverflowPendingTasks =0;
        public long nbAssignedTasks = 0;

        /**
         * active mean the user did something in the last 5 mn
         */
        public boolean isActive = false;
        public Date dateLastAction;
        public long sumTimeWaitingms = 0;

        public UserItem(final long userId, final Date currentDate, final int numberOfMinutes)
        {
            this.userId = userId;
            createAllRanges(currentDate, numberOfMinutes);
        }


        public void register(final ActivityItem activityItem)
        {
            // logger.info(">>>>>>>>>> User[" + userId + "] Register one activity dateComplete=" + activityItem.dateComplete + " dateReady="
            //         + activityItem.dateReady);

            if (activityItem.dateComplete == null && activityItem.dateReady == null) {
                return;
            }
            if (activityItem.dateComplete != null) {
                nbTasksSubmitted++;
            }
            if (activityItem.dateComplete != null && activityItem.dateReady != null) {
                sumTimeWaitingms += activityItem.dateComplete.getTime() - activityItem.dateReady.getTime();
            }

            if (Boolean.TRUE.equals(activityItem.isOverflowed)) {
                nbOverflowedTasks++;
            }
            if (dateLastAction==null) {
                dateLastAction = activityItem.dateComplete;
            } else if (activityItem.dateComplete !=null && dateLastAction.getTime() < activityItem.dateComplete.getTime()) {
                dateLastAction = activityItem.dateComplete;
            }

            populateRange(activityItem.dateComplete, cstRangeSubmitted,1);
            // logger.info("  User[" + userId + "] nbTasksSubmitted=" + nbTasksSubmitted + " nbOverflowedTasks=" + nbOverflowedTasks);

        }

        /**
         * key is the range : example "2015/04/12 10:10"
         * Value is then the different
         */
        public class OneRange
        {
            public Date dateRange;
            public HashMap<String,Long> valuesOnTheRange = new  HashMap<String,Long>();
            public OneRange(final Date dateRange)
            {
                this.dateRange = dateRange;
            }
            public OneRange(final Date dateRange, final String keyValue)
            {
                this.dateRange = dateRange;
                valuesOnTheRange.put(keyValue,1L);
            }
            public void add(final String keyValue, final long nb)
            {
                valuesOnTheRange.put(keyValue, valuesOnTheRange.get(keyValue) ==null ? nb : valuesOnTheRange.get(keyValue)+nb);
            }
        }


        public LinkedHashMap<String, OneRange> registerRange = new LinkedHashMap<String, OneRange>();

        public String getKey(final Calendar c)
        {
            return c.get(Calendar.YEAR) + "/" + c.get(Calendar.MONTH) + "/" + c.get(Calendar.DAY_OF_MONTH) + " " + c.get(Calendar.HOUR_OF_DAY) + ":"
                    + c.get(Calendar.MINUTE) / 10 + "0";
        }

        public void populateRange(final Date dateRange, final String keyValue, final int nb)
        {
            final Calendar c = Calendar.getInstance();
            c.setTime(dateRange);
            final String keyRange = getKey(c);
            if (registerRange.get(keyRange) != null) {
                registerRange.get(keyRange).add( keyValue,nb);
            } else {
                registerRange.put(keyRange, new OneRange(dateRange, keyValue ));
            }
        }

        /**
         * create a default value in all ranges
         *
         * @param baseDate
         * @param numberOfMinutes
         */
        public void createAllRanges(final Date baseDate, final int numberOfMinutes)
        {
            final Calendar c = Calendar.getInstance();
            c.setTime(baseDate);
            c.add(Calendar.MINUTE, -numberOfMinutes);
            c.set(Calendar.MINUTE, c.get(Calendar.MINUTE) / 10 * 10 );
            c.set(Calendar.SECOND, 0 );
            c.set(Calendar.MILLISECOND, 0 );

            for (int i = 0; i < numberOfMinutes / 10; i++)
            {
                final String key = getKey(c);
                if (registerRange.get(key) == null) {
                    registerRange.put(key, new OneRange( c.getTime()));
                }
                c.add(Calendar.MINUTE, 10);
            }
        }

        public void complete(final IdentityAPI identityAPI)
        {
            try
            {
                final User user = identityAPI.getUser(userId);
                userName = user.getUserName();
                totalName = user.getLastName()+", "+user.getFirstName();
            } catch (final Exception e)
            {
            }

        }

        public void calcul()
        {

        }


        public HashMap<String, Object> getJson(final MonitorUsersInput monitorUsersInput)
        {
            final HashMap<String, Object> jsonMap = new HashMap<String, Object>();
            jsonMap.put("userName", userName);
            jsonMap.put("totalName", totalName);
            jsonMap.put("nbTasksSubmitted", nbTasksSubmitted);
            jsonMap.put("avgtimems", nbTasksSubmitted == 0 ? 0 : (int) sumTimeWaitingms / nbTasksSubmitted);
            jsonMap.put("avgtimemn", nbTasksSubmitted == 0 ? 0 : (int) sumTimeWaitingms / nbTasksSubmitted / 60000);
            jsonMap.put("nbOverflowedTasks", nbOverflowedTasks);

            jsonMap.put("nbPendingTasks", nbPendingTasks);
            jsonMap.put("showOverflowPendingWarning", showOverflowPendingWarning);
            jsonMap.put("nbOverflowPendingTasks", nbOverflowPendingTasks);

            jsonMap.put("nbAssignedTasks", nbAssignedTasks);
            jsonMap.put("nbOverflowAssignedTasks", nbOverflowAssignedTasks);
            jsonMap.put("showOverflowAssignedWarning", showOverflowAssignedWarning);

            jsonMap.put("isActive", isActive);


            // range activity !
            /*
             * String debug="";
             * for (String key : registerRange.keySet())
             * {
             * debug+=key+"="+registerRange.get(key)+",";
             * }
             * logger.info(">>>>>>>>>>>>>>>>>>>>>>>>>>Range "+debug);
             * processMap.put("range",debug);
             */

            final ArrayList<HashMap<String, Object>> graphListMap = new ArrayList<HashMap<String, Object>>();

            final List<ChartColumn> listValues = new ArrayList<ChartColumn>();

            for (final String key : registerRange.keySet())
            {
                //     List.add(new ChartSeriesOnColumn(key, "Value", registerRange.get(key).longValue()));
                String titleColum="";
                final OneRange oneRange = registerRange.get(key);
                final Calendar c = Calendar.getInstance();
                c.setTime( oneRange.dateRange );
                if (c.get(Calendar.MINUTE) ==0) {
                    titleColum = c.get(Calendar.HOUR_OF_DAY)+":00";
                }
                final ChartColumn charColumn = new ChartColumn( titleColum );

                charColumn.addSerie( new ChartSerie( "Performed", oneRange.valuesOnTheRange.get(cstRangeSubmitted)));
                listValues.add( charColumn );


            }
            final HashMap<String, Object> oneGraph = new HashMap<String, Object>();

            final AwacsGraph awacsGraph = new AwacsGraph();

            final ChartParameters chartParameters = new ChartParameters("Activity in the last "+monitorUsersInput.activityPeriodInMn+" mn", "AreaChart", "Tasks performed", "number",  "Tasks performed");
            chartParameters.displayTitleFrequency = 6;
            chartParameters.nullIsZero = true;
            awacsGraph.setChartParameters(chartParameters);
            awacsGraph.setValues(listValues);

            oneGraph.put("submittasks", awacsGraph.getJson());
            graphListMap.add(oneGraph);

            jsonMap.put("graphs", graphListMap);
            // logger.info(" UserId[" + userId + "] JSON=" + jsonMap.toString());

            return jsonMap;
        }
    }

    /**
     * @param monitorProcessInput
     * @param processAPI
     * @return
     */
    public static HashMap<String, Object> monitorUsers(final MonitorUsersInput monitorUsersInput, final IdentityAPI identityAPI, final ProcessAPI processAPI) {

        logger.info("MonitorUsersInput : " + monitorUsersInput.getInfos());

        final HashMap<String, Object> mapResult = new HashMap<String, Object>();
        String status = "";
        final long currentTime = System.currentTimeMillis();
        final long beginAnalysis = currentTime;

        final Calendar c = Calendar.getInstance();
        final Date dateReferenceTo = c.getTime();
        c.add(Calendar.MINUTE, -monitorUsersInput.activityPeriodInMn);
        final Date dateReferenceFrom = c.getTime();
        try
        {

            final HashSet<Long> filterUsers = new HashSet<Long>();
            if (monitorUsersInput.filterGroup != null)
            {
                // get all user from user
                final SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0, 1000);
                searchOptionsBuilder.filter(GroupSearchDescriptor.DISPLAY_NAME, monitorUsersInput.filterGroup);
                final SearchResult<Group> searchResult = identityAPI.searchGroups(searchOptionsBuilder.done());
                if (searchResult.getCount() == 0)
                {
                    mapResult.put("errormessage", "No groups found with [" + monitorUsersInput.filterGroup + "]");
                    return mapResult;
                }
                for (final Group group : searchResult.getResult())
                {
                    final List<User> listUsers = identityAPI.getUsersInGroup(group.getId(), 0, 1000, UserCriterion.LAST_NAME_ASC);
                    for (final User user : listUsers) {
                        filterUsers.add(user.getId());
                    }
                }
                if (filterUsers.size() == 0)
                {
                    mapResult.put("errormessage", searchResult.getResult() + " groups found with [" + monitorUsersInput.filterGroup
                            + "], by no users referenced inside ");
                    return mapResult;
                }

            }
            else if (monitorUsersInput.filterRole != null)
            {
                Role role = null;
                try {
                    role = identityAPI.getRoleByName(monitorUsersInput.filterRole);
                } catch (final RoleNotFoundException e) {
                    mapResult.put("errormessage", "No Role found with [" + monitorUsersInput.filterRole + "]");
                    return mapResult;
                }

                final List<User> listUsers = identityAPI.getUsersInRole(role.getId(), 0, 1000, UserCriterion.LAST_NAME_ASC);
                for (final User user : listUsers) {
                    filterUsers.add(user.getId());
                }
                if (filterUsers.size() == 0)
                {
                    mapResult.put("errormessage", " role [" + monitorUsersInput.filterRole + "] has no users referenced inside ");
                    return mapResult;
                }
            }
            else if (monitorUsersInput.filterUser != null)
            {
                User user = null;
                try {
                    user = identityAPI.getUserByUserName(monitorUsersInput.filterUser);
                } catch (final UserNotFoundException e) {
                    mapResult.put("errormessage", "No User found with [" + monitorUsersInput.filterUser + "]");
                    return mapResult;
                }

                filterUsers.add(user.getId());
            }

            final HashMap<String, ActivityItem> mapActivityItem = new HashMap<String, ActivityItem>();

            final long totalMaxItems = 0;
            try {
                final SimpleDateFormat sdf = new SimpleDateFormat("YYYY/MM/dd HH:mm:ss");
                int index = 0;
                do
                {
                    // past activity on the process
                    final SearchOptionsBuilder searchOptionBuilder = new SearchOptionsBuilder(index, 1000);
                    index += 10000;
                    searchOptionBuilder.between(ArchivedFlowNodeInstanceSearchDescriptor.ARCHIVE_DATE, dateReferenceFrom.getTime(),
                            dateReferenceTo.getTime());

                    final SearchResult<ArchivedFlowNodeInstance> searchActivityArchived = processAPI.searchArchivedFlowNodeInstances(searchOptionBuilder.done());

                    // logger.info("Search activities between " + sdf.format(dateReferenceFrom) + " to " + sdf.format(dateReferenceTo) + " : " + searchActivityArchived.getCount());
                    for (final ArchivedFlowNodeInstance archivedFlowNodeInstance : searchActivityArchived.getResult())
                    {
                        final long userId = archivedFlowNodeInstance.getExecutedBy();
                        final long flowNodeDefId = archivedFlowNodeInstance.getFlownodeDefinitionId();
                        final long parentContainerId = archivedFlowNodeInstance.getParentContainerId();

                        // attention : don't filter NOW because all task does not have an userId !
                        ActivityItem activityItem = mapActivityItem.get(parentContainerId + "#" + flowNodeDefId);
                        if (activityItem == null) {
                            activityItem = new ActivityItem();
                        }

                        activityItem.setUserId(userId);
                        activityItem.setFlownodeDefId(flowNodeDefId);
                        activityItem.setProcessDefinitionId(archivedFlowNodeInstance.getProcessDefinitionId());

                        /*
                         * logger.info("Task [" + archivedFlowNodeInstance.getId() + "] flowNodeDefid[" + archivedFlowNodeInstance.getFlownodeDefinitionId()
                         * + "] processId[" + archivedFlowNodeInstance.getProcessInstanceId() + "] is["
                         * + archivedFlowNodeInstance.getState() + "]");
                         */
                        // we search the event completed and the event ready
                        if (archivedFlowNodeInstance.getState().equalsIgnoreCase("ready"))
                        {
                            activityItem.setUserId(userId);
                            activityItem.setDateReady(archivedFlowNodeInstance.getArchiveDate());
                            mapActivityItem.put(parentContainerId + "#" + flowNodeDefId, activityItem);
                        }
                        else if (archivedFlowNodeInstance.getState().equalsIgnoreCase("initializing"))
                        {
                            activityItem.setDateReady(archivedFlowNodeInstance.getArchiveDate());
                            mapActivityItem.put(parentContainerId + "#" + flowNodeDefId, activityItem);
                        }
                        else if (archivedFlowNodeInstance.getState().equalsIgnoreCase("completed"))
                        {
                            activityItem.setUserId(userId);
                            activityItem.setDateCompleted(archivedFlowNodeInstance.getArchiveDate());
                            mapActivityItem.put(parentContainerId + "#" + flowNodeDefId, activityItem);
                        }

                    }
                    if (searchActivityArchived.getCount() < 1000) {
                        break;
                    }
                    if (index > totalMaxItems)
                    {
                        logger.info("Stop search, the maximum item is over at " + totalMaxItems);
                        status += "Too much Archived items, stop analysis at " + totalMaxItems + ";";
                        break;
                    }

                } while (1 == 1);

            } catch (final SearchException e) {
                final StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));

                logger.severe("Search exception [" + e.toString() + "] at " + sw);
                status += "Error " + e.toString();
            }

            // logger.info(">> NbActivityRegistered="+mapActivityItem.size());
            // 6002 is overflowed
            // We get all the activity, now build per user
            final HashMap<Long, UserItem> mapUserItem = new HashMap<Long, UserItem>();
            final HashMap<Long, HumanTaskDefinition> mapHumanTaskDefinition = new HashMap<Long, HumanTaskDefinition>();
            final List<UserItem> listUserItem = new ArrayList<UserItem>();
            for (final ActivityItem activityItem : mapActivityItem.values())
            {
                if (activityItem.userId == null) {
                    continue;
                }

                // now we can filter
                if (filterUsers.size() > 0 && !filterUsers.contains(activityItem.userId)) {
                    continue;
                }


                // complete the activity : is this task was overflowed ?
                if (!mapHumanTaskDefinition.containsKey(activityItem.defFlowNodeDefid))
                {
                    // load it
                    DesignProcessDefinition designProcessDefinition;
                    try {
                        designProcessDefinition = processAPI.getDesignProcessDefinition(activityItem.processDefinitionId);
                        final FlowElementContainerDefinition flowElementContainerDefinition = designProcessDefinition.getFlowElementContainer();
                        final List<ActivityDefinition> listActivityDefinition = flowElementContainerDefinition.getActivities();
                        for (final ActivityDefinition activityDefinition : listActivityDefinition)
                        {
                            if (activityDefinition.getId() == activityItem.defFlowNodeDefid && activityDefinition instanceof HumanTaskDefinition)
                            {
                                mapHumanTaskDefinition.put(activityItem.defFlowNodeDefid, (HumanTaskDefinition) activityDefinition);
                                break;
                            }
                        }
                    } catch (final ProcessDefinitionNotFoundException e) {
                        logger.severe("Can't load processDefinitionId "+e);
                    }
                    if (!mapHumanTaskDefinition.containsKey(activityItem.defFlowNodeDefid))
                     {
                        mapHumanTaskDefinition.put(activityItem.defFlowNodeDefid, null); // don't search again
                    }
                }
                final HumanTaskDefinition humanTaskDefinition = mapHumanTaskDefinition.get(activityItem.defFlowNodeDefid);
                if (humanTaskDefinition != null)
                {
                    // not possible to calculate the isOverflowed : duedate is dynamique, and this information is not saved
                    /* if (activityItem.dateReady != null && activityItem.dateComplete != null && humanTaskDefinition.getExpectedEndDate() != null)
                    {
                        activityItem.isOverflowed = activityItem.dateComplete.getTime() - activityItem.dateReady.getTime() > humanTaskDefinition.getExpectedEndDate();
                    }
                    */
                }

                UserItem userItem = mapUserItem.get(activityItem.userId);
                if (userItem == null)
                {
                    userItem = new UserItem(activityItem.userId, dateReferenceTo, monitorUsersInput.activityPeriodInMn);
                    userItem.complete(identityAPI);
                    mapUserItem.put(activityItem.userId, userItem);
                    listUserItem.add(userItem);
                }

                userItem.register(activityItem);
            }

            // Fullfill by all users
            for (final Long userId : filterUsers)
            {
                if (mapUserItem.containsKey(userId)) {
                    continue;
                }
                // create an empty value
                final UserItem userItem = new UserItem(userId, dateReferenceTo, monitorUsersInput.activityPeriodInMn);
                userItem.complete(identityAPI);
                mapUserItem.put(userId, userItem);
                listUserItem.add(userItem);
            }

            // logger.info("User [" + listUserItem.toString() + "]");

            // get the pending task per user
            for (final UserItem userItem : listUserItem)
            {
                userItem.nbAssignedTasks = processAPI.getNumberOfAssignedHumanTaskInstances(userItem.userId);
                userItem.nbPendingTasks = processAPI.getNumberOfPendingHumanTaskInstances(userItem.userId);
                // calculated the overflow
                for (final HumanTaskInstance humanTaskInstance : processAPI.getAssignedHumanTaskInstances(userItem.userId, 0, monitorUsersInput.defaultMaxItems, ActivityInstanceCriterion.PRIORITY_ASC))
                {
                    if (humanTaskInstance.getExpectedEndDate() != null && humanTaskInstance.getExpectedEndDate().getTime() < currentTime) {
                        userItem.nbOverflowAssignedTasks++;
                    }
                }
                for (final HumanTaskInstance humanTaskInstance : processAPI.getPendingHumanTaskInstances(userItem.userId, 0, monitorUsersInput.defaultMaxItems,ActivityInstanceCriterion.PRIORITY_ASC))
                {
                    if (humanTaskInstance.getExpectedEndDate() != null && humanTaskInstance.getExpectedEndDate().getTime() < currentTime) {
                        userItem.nbOverflowPendingTasks++;
                    }
                }

                userItem.showOverflowAssignedWarning = userItem.nbOverflowAssignedTasks >= monitorUsersInput.levelShowWarningOverflowTasks;
                userItem.showOverflowPendingWarning = userItem.nbOverflowPendingTasks >= monitorUsersInput.levelShowWarningOverflowTasks;

                if (userItem.dateLastAction!=null && currentTime - userItem.dateLastAction.getTime() >  monitorUsersInput.timeToBeActiveInMs) {
                    userItem.isActive=true;
                }
            }

            // ----------------------- Json the result
            // sort process by name
            Collections.sort(listUserItem, new Comparator<UserItem>() {

                public int compare(final UserItem s1, final UserItem s2) {
                    return s1.totalName.compareTo(s2.totalName);
                }
            });
            final ArrayList<HashMap<String, Object>> listUserItemJson = new ArrayList<HashMap<String, Object>>();
            for (final UserItem userItem : listUserItem)
            {
                listUserItemJson.add(userItem.getJson( monitorUsersInput ));
            }
            final long endAnalysis = System.currentTimeMillis();
            status += "Analysis in " + (endAnalysis - beginAnalysis) + " ms;";

            mapResult.put("users", listUserItemJson);

            // ----------------- build the graph user per user
            final List<ChartColumn> listValues = new ArrayList<ChartColumn>();

            for (final UserItem userItem : listUserItem)
            {
                // listValues.add(new ChartSeriesOnColumn(userItem.userName, userItem.userName, (long) userItem.nbTasksSubmitted));
                final ChartColumn charColumn = new ChartColumn( userItem.userName );
                charColumn.addSerie( new ChartSerie( "Value", (long) userItem.nbTasksSubmitted));
                listValues.add( charColumn );

            }

            final ChartParameters chartParameters = new ChartParameters("Submitted tasks", "ColumnChart", "User", "number", "Tasks submited");
            chartParameters.displayTitleFrequency = 1;
            final AwacsGraph awacsGraph = new AwacsGraph();
            awacsGraph.setChartParameters( chartParameters );
            awacsGraph.setValues(listValues);
            mapResult.put("graphtasksperusers", awacsGraph.getJson());
        } catch (final SearchException e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            logger.severe("Search exception [" + e.toString() + "] at " + sw);
            status += "Error " + e.toString();
            mapResult.put("errormessage", "Error " + e.toString());
        }
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
    public static long decodeValue(final String description, final String marker, final long defaultValue) {

        if (description == null || description.length() == 0) {
            return defaultValue;
        }
        if (description.indexOf(marker + ":") == -1)
        {
            return defaultValue;
        }
        String valueSt = description.substring(description.indexOf(marker + ":") + marker.length() + 1);
        int posEndBlank = valueSt.indexOf(" ");
        int posEndComma = valueSt.indexOf(";");
        if (posEndBlank == -1) {
            posEndBlank = valueSt.length();
        }
        if (posEndComma == -1) {
            posEndComma = valueSt.length();
        }
        valueSt = valueSt.substring(0, Math.min(posEndBlank, posEndComma));

        try {
            return Long.valueOf(valueSt);
        } catch (final Exception e) {
        };
        return defaultValue;
    }

    private static int cstFailed = 1;
    private static int cstWarmOverDueDate = 2;
    private static int cstWarmNearDueDate = 4;
    private static int cstWarmOpenTask = 8;
    private static int cstErrorTooManyItems = 16;

    public static String getDetail(final int control)
    {
        String result = "";
        if ((control & cstFailed) > 0) {
            result += ",Failed";
        }
        if ((control & cstWarmOverDueDate) > 0) {
            result += ",DueDate";
        }
        if ((control & cstWarmNearDueDate) > 0) {
            result += ",NearDueDate";
        }
        if ((control & cstWarmOpenTask) > 0) {
            result += ",TooTasks";
        }
        if (result.length() > 0) {
            result = result.substring(1);
        }
        return result;

    }
}
