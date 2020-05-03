package com.smatechnologies.opcon.command.api.task.runnable;

import com.smatechnologies.opcon.command.api.arguments.OpConCliArguments;
import com.smatechnologies.opcon.command.api.interfaces.ICmdConstants;
import com.smatechnologies.opcon.command.api.interfaces.IJob;
import com.smatechnologies.opcon.command.api.modules.JobLogData;
import com.smatechnologies.opcon.command.api.task.IRunnableTask;
import com.smatechnologies.opcon.command.api.task.exception.TaskException;
import com.smatechnologies.opcon.command.api.task.exception.TaskJobExecutionException;
import com.smatechnologies.opcon.command.api.util.Utilities;
import com.smatechnologies.opcon.restapiclient.WsException;
import com.smatechnologies.opcon.restapiclient.api.OpconApi;
import com.smatechnologies.opcon.restapiclient.model.JobStatus;
import com.smatechnologies.opcon.restapiclient.model.JobType;
import com.smatechnologies.opcon.restapiclient.model.Version;
import com.smatechnologies.opcon.restapiclient.model.dailyjob.DailyJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DependencyTask implements IRunnableTask {

    private final static Logger LOG = LoggerFactory.getLogger(DependencyTask.class);

    private Utilities utilities = new Utilities();

    private void checkPreReq(OpconApi api, OpConCliArguments args) throws TaskException {
        if (args.getScheduleName() == null) {
            throw new TaskException("Required -sn (schedule name) argument missing for JobAdd task");
        }
        if (args.getJobName() == null) {
            throw new TaskException("Required -jn (job name) argument missing for JobAdd task");
        }

        final Version version = api.getVersion();
        if (!utilities.versionCheck(version.getOpConRestApiProductVersion(), args.getTask())) {
            throw new TaskException(MessageFormat.format("OpCon-API Version {0} not supported, must be 17.1.0 or greater", version.getOpConRestApiProductVersion()));
        }
    }

    @Override
    public void run(OpconApi api, OpConCliArguments args) throws TaskException {
        checkPreReq(api, args);

        LOG.info(MessageFormat.format(
                "Processing task ({0}) arguments : date ({1}) schedule ({2}) job ({3}) on ({4}) for completion status",
                args.getTask(),
                args.getTaskDate(),
                args.getScheduleName(),
                args.getJobName(),
                args.getOpConSystem()
        ));

        final IJob jobTask = new JobImpl();
        final DailyJob dailyJob = jobTask.getDailyJobByName(api, args);
        if (dailyJob != null) {
            final JobStatus status = waitJobCompletion(api, dailyJob.getId());
            int exitCode = 0;
            if (JobStatus.JobStatusCategory.FAILED.equals(status)) {
                parseJobExitCode(dailyJob);
            }

            LOG.info(MessageFormat.format(
                    "Job ({0}) of Schedule ({1}) for date ({2}) on system ({3}) completed with code ({4})",
                    args.getJobName(), args.getScheduleName(), args.getTaskDate(), args.getOpConSystem(), String.valueOf(exitCode))
            );
            if (!status.getCategory().isNeverRan()) {
                logJobExecutionOutput(jobTask, api, dailyJob);
            }

            if (exitCode != 0) {
                final String msg = MessageFormat.format(
                        "date ({0}) schedule ({1}) job ({2}) exit code ({3})",
                        args.getTaskDate(), args.getScheduleName(), args.getJobName(), exitCode
                );
                throw new TaskJobExecutionException(msg, exitCode);
            }
        } else {
            final String errorMessage = MessageFormat.format(
                    "Job ({0}) for Schedule ({1}) on date ({2}) on system ({3}) not found",
                    args.getJobName(), args.getScheduleName(), args.getTaskDate(), args.getOpConSystem()
            );
            throw new TaskException(errorMessage);
        }
    }

    private int parseJobExitCode(DailyJob job) {
        if (JobType.UNIX.equals(job.getJobType())) {
            // strip off leading + and only use values up to semi colon (:)
            String uCode = job.getTerminationDescription();
            int firstColon = uCode.indexOf(ICmdConstants.COLON);
            if (firstColon > -1) {
                uCode = uCode.substring(0, firstColon);
            }
            uCode = uCode.replaceAll(ICmdConstants.PLUS, ICmdConstants.EMPTY_STRING);
            return Integer.parseInt(uCode);
        } else if (JobType.WINDOWS.equals(job.getJobType())) {
            return Integer.parseInt(job.getTerminationDescription());
        } else {
            return 1; //other platforms do not support exit codes
        }
    }

    private void logJobExecutionOutput(IJob jobTask, OpconApi api, DailyJob job) throws TaskException {
        final List<JobLogData> jobLogDataList = jobTask.getJobLogByDailyJob(api, job);
        LOG.info("---------------------------------------------------------------------------------");
        LOG.info("Remote Execution Job Output");
        LOG.info("---------------------------------------------------------------------------------");
        jobLogDataList.stream().forEach(log -> log.getRecords().stream().forEach(record -> LOG.info(record)));
        LOG.info("---------------------------------------------------------------------------------");
    }

    private JobStatus waitJobCompletion(OpconApi opconApi, final String jobId) throws TaskException {
        while (true) {
            try {
                final DailyJob dailyJob = opconApi.dailyJobs().get(jobId);
                LOG.trace(MessageFormat.format("Daily Job Status: ({0}) Description: ({1})", dailyJob.getStatus().getId(), dailyJob.getStatus().getDescription()));
                if (isJobCompleted(dailyJob.getStatus())) {
                    LOG.debug("status : completed");
                    return dailyJob.getStatus();
                }

                LOG.trace(MessageFormat.format("Job Not Completed ({0}), wait before retry", dailyJob.getStatus().getDescription()));
                Thread.sleep(TimeUnit.SECONDS.toMillis(2));
            } catch (WsException | InterruptedException e) {
                throw new TaskException(utilities.getExceptionDetails(e));
            }
        }
    }

    private boolean isJobCompleted(final JobStatus status) {
        //TODO: clean this up to rather use category
        switch (status.getId()) {
            case 0:
                LOG.debug("status : On Hold");
                return true;

            case 210:
                LOG.debug("status : Initialization Error");
                return true;

            case 900:
                LOG.debug("status : Finished OK");
                return true;

            case 910:
                LOG.debug("status : Failed");
                return true;

            case 920:
                LOG.debug("status : Mark Finished OK");
                return true;

            case 921:
                LOG.debug("status : Mark Failed");
                return true;

            case 940:
                LOG.debug("status : Skipped");
                return true;

            case 950:
                LOG.debug("status : Cancelled");
                return true;
        }
        return false;
    }
}
