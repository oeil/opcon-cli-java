package com.smatechnologies.opcon.command.api.task.runnable;

import com.smatechnologies.opcon.command.api.arguments.OpConCliArguments;
import com.smatechnologies.opcon.command.api.task.IRunnableTask;
import com.smatechnologies.opcon.command.api.task.exception.TaskException;
import com.smatechnologies.opcon.restapiclient.api.OpconApi;
import com.smatechnologies.opcon.restapiclient.model.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;

public class VersionTask implements IRunnableTask {

    private final static Logger LOG = LoggerFactory.getLogger(VersionTask.class);

    @Override
    public void run(OpconApi api, OpConCliArguments args) throws TaskException {
        Version version = api.getVersion();
        LOG.info(MessageFormat.format("OpCon-API Version {0}", version.getOpConRestApiProductVersion()));
    }
}
