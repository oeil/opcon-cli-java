package com.smatechnologies.opcon.command.api.task;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smatechnologies.opcon.command.api.arguments.OpConCliArguments;
import com.smatechnologies.opcon.command.api.config.CmdConfiguration;
import com.smatechnologies.opcon.command.api.enums.TaskType;
import com.smatechnologies.opcon.command.api.interfaces.IJob;
import com.smatechnologies.opcon.command.api.interfaces.IMachine;
import com.smatechnologies.opcon.command.api.interfaces.ISchedule;
import com.smatechnologies.opcon.command.api.modules.JobLogData;
import com.smatechnologies.opcon.command.api.task.runnable.JobImpl;
import com.smatechnologies.opcon.command.api.task.runnable.MachineImpl;
import com.smatechnologies.opcon.command.api.task.runnable.ScheduleImpl;
import com.smatechnologies.opcon.command.api.ws.WsLogger;
import com.smatechnologies.opcon.restapiclient.DefaultClientBuilder;
import com.smatechnologies.opcon.restapiclient.WsErrorException;
import com.smatechnologies.opcon.restapiclient.WsException;
import com.smatechnologies.opcon.restapiclient.api.OpconApi;
import com.smatechnologies.opcon.restapiclient.api.OpconApiProfile;
import com.smatechnologies.opcon.restapiclient.jackson.DefaultObjectMapperProvider;
import com.smatechnologies.opcon.restapiclient.model.machine.Machine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.ext.ContextResolver;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TaskProcessor {


    private static final String JobAddProcessingTaskMsg = "Processing task ({0}) arguments : date ({1}) schedule ({2}) job ({3}) frequency ({4}) properties ({5}) onhold ({6})";
    private static final String JobAddMissingScheduleNameMsg = "Required -sn (schedule name) argument missing for JobAdd task";
    private static final String JobAddMissingJobNameMsg = "Required -jn (job name) argument missing for JobAdd task";
    private static final String JobAddMissingFrequencyMsg = "Required -jf (frequency) argument missing for JobAdd task";

    private static final String JobActionProcessingTaskMsg = "Processing task ({0}) arguments : date ({1}) schedule ({2}) job ({3}) action ({4}))";
    private static final String JobActionMissingScheduleNameMsg = "Required -sn (schedule name) argument missing for JobAction task";
    private static final String JobActionMissingJobNameMsg = "Required -jn (job name) argument missing for JobAction task";
    private static final String JobActionMissingJobActionMsg = "Required -ja (job action) argument missing for JobAction task";

    private static final String JobLogProcessingTaskMsg = "Processing task ({0}) arguments : date ({1}) schedule ({2}) job ({3}) file ({4})";
    private static final String JobLogMissingScheduleNameMsg = "Required -sn (schedule name) argument missing for JobLog task";
    private static final String JobLogMissingJobNameMsg = "Required -jn (job name) argument missing for JobLog task";
    private static final String JobLogFileMsg = "Writing joblog to file ({0})";

    private static final String MachineActionProcessingTaskMsg = "Processing task ({0}) arguments : machine ({1}) action ({2})";
    private static final String MachineActionMissingNameMsg = "Required -mn (machine names) argument missing for MachAction task";
    private static final String MachineActionMissingActionMsg = "Required -ma (machine action) argument missing for MachAction task";

    private static final String MachineAddProcessingTaskMsg = "Processing task ({0}) arguments : machine file name ({1})";
    private static final String MachineAddMissingFileNameMsg = "Required -mf (machine file name) argument missing for MachAdd task";
    private static final String MachineAddJsonParseErrorMsg = "MachAdd : Error parsing file : {0} : {1}";
    private static final String MachineAddFileErrorMsg = "MachAdd : Error reading file : {0} : {1}";

    private static final String MachineUpdateProcessingTaskMsg = "Processing task ({0}) arguments : machine ({1}) new name ({2}) ipadr ({3}) dns ({4})";
    private static final String MachineUpdateMissingNameMsg = "Required -mn (machine names) argument missing for MachUpdate task";



    private static final String ScheduleActionProcessingTaskMsg = "Processing task ({0}) arguments : date ({1}) days ({2}) indicator ({3}))";
    private static final String ScheduleActionMissingScheduleNameMsg = "Required -sn (schedule name) argument missing for SchedAction task";
    private static final String ScheduleActionMissingActionMsg = "Required -sa (schedule action) argument missing for SchedAction task";

    private static final String ScheduleBuildProcessingTaskMsg = "Processing task ({0}) arguments : date ({1}) schedule ({2}) action ({3})";
    private static final String ScheduleBuildMissingNameMsg = "Required -sn (schedule name) argument missing for SchedBuild task";

    private static final String ScheduleRebuildProcessingTaskMsg = "Processing task ({0}) arguments : date ({1}) days ({2}) indicator ({3}))";
    private static final String ScheduleRebuildMissingDaysToBuildMsg = "Required -sd (no of days) argument missing for SchedRebuild task";

    private static final String UrlFormatTls = "https://{0}:{1}/api";
    private static final String UrlFormatNonTls = "http://{0}:{1}/api";

    private final static Logger LOG = LoggerFactory.getLogger(TaskProcessor.class);
    private static CmdConfiguration cmdConfiguration = CmdConfiguration.getInstance();
    private DefaultObjectMapperProvider objectMapperProvider = new DefaultObjectMapperProvider();

    private IJob jobImpl = new JobImpl();
    private IMachine machineImpl = new MachineImpl();
    private ISchedule scheduleImpl = new ScheduleImpl();

    private String displayProperties = null;

    public Integer processRequest(OpConCliArguments arguments) throws Exception {

        Integer completionCode = null;
        String url = null;

        // create client connection
        if (cmdConfiguration.isUsingTls()) {
            url = MessageFormat.format(UrlFormatTls, cmdConfiguration.getServer(), String.valueOf(cmdConfiguration.getPort()));
        } else {
            url = MessageFormat.format(UrlFormatNonTls, cmdConfiguration.getServer(), String.valueOf(cmdConfiguration.getPort()));
        }
        OpconApiProfile profile = new OpconApiProfile(url);
        OpconApi opconApi = getClient(profile);

        try {
            //once completely refactored, replace all TaskType & switch code by the below line
            //Task.byId(arguments.getTask()).get().run();

            TaskType taskType = TaskType.valueOf(arguments.getTask());

            switch (taskType) {

                case AppToken:
                    Task.GetApplicationToken.get().run(opconApi, arguments);
                    completionCode = 0;
                    break;

                case Dependency:
                    Task.WaitJobDependency.get().run(opconApi, arguments);
                    completionCode = 0;
                    break;

                case GetJobLog:
                    if (arguments.getScheduleName() == null) {
                        LOG.error(JobLogMissingScheduleNameMsg);
                        return 1;
                    }
                    if (arguments.getJobName() == null) {
                        LOG.error(JobLogMissingJobNameMsg);
                        return 1;
                    }
                    LOG.info(MessageFormat.format(JobLogProcessingTaskMsg,
                            arguments.getTask(),
                            arguments.getTaskDate(),
                            arguments.getScheduleName(),
                            arguments.getJobName(),
                            arguments.getJobLogDirectory()
                    ));
                    List<JobLogData> jobLogsData = jobImpl.getJobLog(opconApi, arguments);
                    for (JobLogData jobLogData : jobLogsData) {
                        if (!jobLogData.getRecords().isEmpty()) {
                            if (arguments.getJobLogDirectory() != null) {
                                String filename = arguments.getJobLogDirectory() + File.separator + jobLogData.getFilename();
                                LOG.info(MessageFormat.format(JobLogFileMsg, filename));
                                FileWriter fwriter = new FileWriter(filename);
                                BufferedWriter bwrite = new BufferedWriter(fwriter);
                                // now add the shout entries
                                for (String record : jobLogData.getRecords()) {
                                    StringBuffer sbWriteRecord = new StringBuffer();
                                    sbWriteRecord.append(record);
                                    sbWriteRecord.append(System.getProperty("line.separator"));
                                    bwrite.write(sbWriteRecord.toString());
                                }
                                bwrite.close();
                            } else {
                                // append joblog to OpConCli output
                                for (String record : jobLogData.getRecords()) {
                                    LOG.info(record);
                                }
                            }
                        }
                    }
                    completionCode = 0;
                    break;

                case JobAction:
                    if (arguments.getScheduleName() == null) {
                        LOG.error(JobActionMissingScheduleNameMsg);
                        return 1;
                    }
                    if (arguments.getJobName() == null) {
                        LOG.error(JobActionMissingJobNameMsg);
                        return 1;
                    }
                    if (arguments.getJobAction() == null) {
                        LOG.error(JobActionMissingJobActionMsg);
                        return 1;
                    }
                    LOG.info(MessageFormat.format(JobActionProcessingTaskMsg, arguments.getTask(), arguments.getTaskDate(), arguments.getScheduleName(),
                            arguments.getJobName(), arguments.getJobAction()));
                    completionCode = jobImpl.jobActionRequest(opconApi, arguments);
                    break;

                case JobAdd:
                    if (arguments.getScheduleName() == null) {
                        LOG.error(JobAddMissingScheduleNameMsg);
                        return 1;
                    }
                    if (arguments.getJobName() == null) {
                        LOG.error(JobAddMissingJobNameMsg);
                        return 1;
                    }
                    if (arguments.getFrequencyName() == null) {
                        LOG.error(JobAddMissingFrequencyMsg);
                        return 1;
                    }
                    if (arguments.getInstanceProperties() == null) {
                        displayProperties = "None";
                    } else {
                        displayProperties = arguments.getInstanceProperties();
                    }
                    LOG.info(MessageFormat.format(JobAddProcessingTaskMsg, arguments.getTask(), arguments.getTaskDate(),
                            arguments.getScheduleName(), arguments.getJobName(), arguments.getFrequencyName(),
                            arguments.getInstanceProperties(), arguments.isAddOnHold()
                    ));
                    completionCode = jobImpl.jobAddRequest(opconApi, arguments);
                    break;

                case MachAction:
                    if (arguments.getMachineName() == null) {
                        LOG.error(MachineActionMissingNameMsg);
                        return 1;
                    }
                    if (arguments.getMachineAction() == null) {
                        LOG.error(MachineActionMissingActionMsg);
                        return 1;
                    }
                    LOG.info(MessageFormat.format(MachineActionProcessingTaskMsg, arguments.getTask(), arguments.getMachineName(), arguments.getMachineAction()));
                    completionCode = machineImpl.machineAction(opconApi, arguments);
                    break;

                case MachAdd:
                    if (arguments.getMachineFileName() == null) {
                        LOG.error(MachineAddMissingFileNameMsg);
                        return 1;
                    }
                    // get machine data
                    List<Machine> machines = null;
                    try {
                        machines = getMachineData(arguments.getMachineFileName());
                    } catch (JsonParseException pex) {
                        LOG.error(MessageFormat.format(MachineAddJsonParseErrorMsg, pex.getMessage(), arguments.getMachineFileName()));
                        return 1;
                    } catch (Exception ex) {
                        LOG.error(MessageFormat.format(MachineAddFileErrorMsg, ex.getMessage(), arguments.getMachineFileName()));
                        return 1;
                    }
                    // if only 1 machine definition, check if we need to replace name, ip-address or dns values
                    if (machines.size() == 1) {
                        List<Machine> machinesUpdate = new ArrayList<Machine>();
                        Machine machine = machines.get(0);
                        if (arguments.getMachineName() != null) {
                            machine.setName(arguments.getMachineName());
                        }
                        if (arguments.getMachineIpAddress() != null) {
                            machine.setTcpIpAddress(arguments.getMachineIpAddress());
                            machine.setFullyQualifiedDomainName("<Default>");
                        }
                        if (arguments.getMachineDnsAddress() != null) {
                            machine.setTcpIpAddress("<Default>");
                            machine.setFullyQualifiedDomainName(arguments.getMachineDnsAddress());
                        }
                        machinesUpdate.add(machine);
                        machines = machinesUpdate;
                    }
                    LOG.info(MessageFormat.format(MachineAddProcessingTaskMsg, arguments.getTask(), arguments.getMachineFileName()));
                    completionCode = machineImpl.machineAdd(opconApi, arguments, machines);
                    break;

                case MachGrpAdd:
                    Task.AddMachineToGroup.get().run(opconApi, arguments);
                    completionCode = 0;
                    break;

                case MachGrpRemove:
                    Task.RemoveMachineFromGroup.get().run(opconApi, arguments);
                    completionCode = 0;
                    break;

                case MachUpdate:
                    if (arguments.getMachineName() == null) {
                        LOG.error(MachineUpdateMissingNameMsg);
                        return 1;
                    }
                    LOG.info(MessageFormat.format(MachineUpdateProcessingTaskMsg,
                            arguments.getTask(),
                            arguments.getMachineName(),
                            arguments.getMachineNameUpdate(),
                            arguments.getMachineIpAddressUpdate(),
                            arguments.getMachineDnsAddressUpdate()
                    ));
                    completionCode = machineImpl.machineUpdate(opconApi, arguments);
                    break;

                case PropExp:
                    Task.ExecPropertyExpression.get().run(opconApi, arguments);
                    completionCode = 0;
                    break;

                case PropUpdate:
                    Task.UpdateProperty.get().run(opconApi, arguments);
                    completionCode = 0;
                    break;

                case SchedAction:
                    if (arguments.getScheduleName() == null) {
                        LOG.error(ScheduleActionMissingScheduleNameMsg);
                        return 1;
                    }
                    if (arguments.getScheduleAction() == null) {
                        LOG.error(ScheduleActionMissingActionMsg);
                        return 1;
                    }
                    LOG.info(MessageFormat.format(ScheduleActionProcessingTaskMsg,
                            arguments.getTask(),
                            arguments.getTaskDate(),
                            arguments.getScheduleName(),
                            arguments.getScheduleAction()
                    ));
                    completionCode = scheduleImpl.actionSchedule(opconApi, arguments);
                    break;

                case SchedBuild:
                    if (arguments.getScheduleName() == null) {
                        LOG.error(ScheduleBuildMissingNameMsg);
                        return 1;
                    }
                    if (arguments.getInstanceProperties() == null) {
                        displayProperties = "None";
                    } else {
                        displayProperties = arguments.getInstanceProperties();
                    }
                    LOG.info(MessageFormat.format(ScheduleBuildProcessingTaskMsg, arguments.getTask(), arguments.getTaskDate(), arguments.getScheduleName(), displayProperties, arguments.isBuildOnHold()));
                    completionCode = scheduleImpl.buildSchedule(opconApi, arguments);
                    break;

                case SchedRebuild:
                    if (arguments.getNoOfDaysToRebuild() == null) {
                        LOG.error(ScheduleRebuildMissingDaysToBuildMsg);
                        return 1;
                    }
                    LOG.info(MessageFormat.format(ScheduleRebuildProcessingTaskMsg,
                            arguments.getTask(),
                            arguments.getTaskDate(),
                            String.valueOf(arguments.getNoOfDaysToRebuild()),
                            arguments.getScheduleRebuildIndicator()
                    ));
                    completionCode = scheduleImpl.rebuildSchedule(opconApi, arguments);
                    break;

                case ThreshUpdate:
                    Task.UpdateThreshold.get().run(opconApi, arguments);
                    completionCode = 0;
                    break;

                case Version:
                    Task.GetVersion.get().run(opconApi, arguments);
                    completionCode = 0;
                    break;

            }

        } catch (Exception ex) {
            throw new Exception(ex);
        }
        return completionCode;
    }

    private OpconApi getClient(OpconApiProfile profile) throws Exception {
        OpconApi opconApi;
        Client client = null;
        ContextResolver<ObjectMapper> ctxObjectMapperProvider;

        if (cmdConfiguration.isDebug()) {
            DefaultClientBuilder clientBuilder = DefaultClientBuilder.get()
                    .setTrustAllCert(true);

            client = clientBuilder.build();
            DefaultObjectMapperProvider objectMapperProvider = new DefaultObjectMapperProvider();
            objectMapperProvider.getObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
            client.register(new WsLogger(objectMapperProvider));

            ctxObjectMapperProvider = objectMapperProvider;
        } else {
            DefaultClientBuilder clientBuilder = DefaultClientBuilder.get().setTrustAllCert(true);

            client = clientBuilder.build();
            DefaultObjectMapperProvider objectMapperProvider = new DefaultObjectMapperProvider();
            objectMapperProvider.getObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
            ctxObjectMapperProvider = objectMapperProvider;
        }

        opconApi = new OpconApi(client, profile, new OpconApi.OpconApiListener() {
            @Override
            public void onFailed(WsException e) {
                if (e.getResponse() == null) {
                    LOG.error("[OpconApi] A web service call has failed.", e);
                } else if (e instanceof WsErrorException) {
                    LOG.warn("[OpconApi] A web service call return API Error: {}", e.getResponse().readEntity(String.class));
                } else {
                    LOG.error("[OpconApi] A web service call has failed. Response: Header={} Body={}", e.getResponse().getHeaders(), e.getResponse().readEntity(String.class), e);
                }
            }
        }, ctxObjectMapperProvider);

        opconApi.login(cmdConfiguration.getUser(), cmdConfiguration.getPassword());

        return opconApi;
    }    // END : getClient

    private List<Machine> getMachineData(
            String fileName
    ) throws JsonParseException, Exception {

        Machine[] machines = objectMapperProvider.getObjectMapper().readValue(new FileInputStream(fileName), Machine[].class);
        return Arrays.asList(machines);

    }

}
