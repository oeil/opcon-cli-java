package com.smatechnologies.opcon.command.api.task.runnable;

import com.smatechnologies.opcon.command.api.arguments.OpConCliArguments;
import com.smatechnologies.opcon.command.api.config.CmdConfiguration;
import com.smatechnologies.opcon.command.api.task.IRunnableTask;
import com.smatechnologies.opcon.command.api.task.exception.TaskException;
import com.smatechnologies.opcon.restapiclient.WsException;
import com.smatechnologies.opcon.restapiclient.api.OpconApi;
import com.smatechnologies.opcon.restapiclient.model.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;

public class TokenTask implements IRunnableTask {

    private final static Logger LOG = LoggerFactory.getLogger(TokenTask.class);

    private void checkArgs(OpConCliArguments args) throws TaskException {
        if(args.getApplicationName() == null) {
            throw new TaskException("Required -ap (application name) argument missing for AppToken task");
        }
    }

    @Override
    public void run(OpconApi api, OpConCliArguments args) throws TaskException {
        checkArgs(args);

        LOG.info(MessageFormat.format(
                "Processing task ({0}) arguments : appname ({1})",
                args.getTask(), args.getApplicationName())
        );

        try {
            final CmdConfiguration cfg = CmdConfiguration.getInstance();
            Token token = api.tokens().postApp(cfg.getUser(), cfg.getPassword(), args.getApplicationName());
            if(token.getId() == null) {
                throw new TaskException("Application Token create failed");
            }
            LOG.info(MessageFormat.format("Application {0} Token {1} successfully created", args.getApplicationName(), token.getId()));
        } catch (WsException e) {
            throw new TaskException(e.getMessage(), e);
        }
    }
}
