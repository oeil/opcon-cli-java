package com.smatechnologies.opcon.command.api.task.runnable;

import com.smatechnologies.opcon.command.api.arguments.OpConCliArguments;
import com.smatechnologies.opcon.command.api.task.IRunnableTask;
import com.smatechnologies.opcon.command.api.task.exception.TaskException;
import com.smatechnologies.opcon.command.api.util.Utilities;
import com.smatechnologies.opcon.restapiclient.WsException;
import com.smatechnologies.opcon.restapiclient.api.OpconApi;
import com.smatechnologies.opcon.restapiclient.api.thresholds.ThresholdsCriteria;
import com.smatechnologies.opcon.restapiclient.model.Threshold;
import com.smatechnologies.opcon.restapiclient.model.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.List;

public class ThresholdTask implements IRunnableTask {

    private final static Logger LOG = LoggerFactory.getLogger(ThresholdTask.class);

    private void checkPreReqs(OpconApi api, OpConCliArguments args) throws TaskException {
        if (args.getThresholdName() == null) {
            throw new TaskException("Required -tn (threshold name) argument missing for ThreshUpdate task");
        }
        if (args.getThresholdValue() == null) {
            throw new TaskException("Required -tv (threshold value) argument missing for ThreshUpdate task");
        }

        final Version version = api.getVersion();
        if (!new Utilities().versionCheck(version.getOpConRestApiProductVersion(), args.getTask())) {
            throw new TaskException(MessageFormat.format("OpCon-API Version {0} not supported, must be 17.1.0 or greater", version.getOpConRestApiProductVersion()));
        }
    }

    @Override
    public void run(OpconApi api, OpConCliArguments args) throws TaskException {
        checkPreReqs(api, args);

        LOG.info(MessageFormat.format(
                "Processing task ({0}) arguments : threshold ({1}) value ({2})",
                args.getTask(), args.getThresholdName(), args.getThresholdValue())
        );

        final ThresholdsCriteria criteria = new ThresholdsCriteria();
        criteria.setName(args.getThresholdName());

        try {
            final List<Threshold> thresholds = api.thresholds().get(criteria);
            LOG.debug(MessageFormat.format("({0}) found for Threshold : ({1})", thresholds.size(), criteria.getName()));
            if (thresholds.isEmpty()) {
                throw new TaskException(MessageFormat.format(
                        "Threshold ({0}) update failed : {1}",
                        args.getThresholdName(), "Threshold not found in Opcon database")
                );
            }

            //update threshold value
            final Threshold threshold = thresholds.stream().findFirst().get();
            threshold.setValue(args.getThresholdValue());
            Threshold resultThreshold = api.thresholds().put(threshold);
            LOG.info(MessageFormat.format(
                    "Threshold ({0}) updated to ({1}) successfully",
                    args.getThresholdName(), String.valueOf(args.getThresholdValue()))
            );
        } catch (WsException e) {
            throw new TaskException(e);
        }
    }
}
