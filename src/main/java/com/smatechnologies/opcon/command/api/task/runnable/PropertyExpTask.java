package com.smatechnologies.opcon.command.api.task.runnable;

import com.smatechnologies.opcon.command.api.arguments.OpConCliArguments;
import com.smatechnologies.opcon.command.api.task.IRunnableTask;
import com.smatechnologies.opcon.command.api.task.exception.TaskException;
import com.smatechnologies.opcon.command.api.util.Utilities;
import com.smatechnologies.opcon.restapiclient.WsException;
import com.smatechnologies.opcon.restapiclient.api.OpconApi;
import com.smatechnologies.opcon.restapiclient.model.PropertyExpression;
import com.smatechnologies.opcon.restapiclient.model.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;

public class PropertyExpTask implements IRunnableTask {

    private final static Logger LOG = LoggerFactory.getLogger(PropertyExpTask.class);

    private void checkPreReqs(OpconApi api, OpConCliArguments args) throws TaskException {
        if(args.getPropertyExpression() == null) {
            throw new TaskException("Required -ev (expression) argument missing for PropExp task");
        }

        final Version version = api.getVersion();
        if (!new Utilities().versionCheck(version.getOpConRestApiProductVersion(), args.getTask())) {
            throw new TaskException(MessageFormat.format(
                    "OpCon-API Version {0} not supported, must be 17.1.0 or greater",
                    version.getOpConRestApiProductVersion())
            );
        }
    }

    @Override
    public void run(OpconApi api, OpConCliArguments args) throws TaskException {
        checkPreReqs(api, args);

        LOG.info(MessageFormat.format(
                "Processing task ({0}) arguments : expression ({1})",
                args.getTask(),
                args.getPropertyExpression()
        ));

        try {
            final PropertyExpression propertyExpression = new PropertyExpression();
            propertyExpression.setExpression(args.getPropertyExpression());

            PropertyExpression resultExpression = api.propertyExpressions().post(propertyExpression);
            if(resultExpression.getResult() == null) {
                throw new TaskException(MessageFormat.format(
                        "Expression evaluation ({0}) failed : {1} : message {2}",
                        args.getPropertyExpression(), resultExpression.getResult(), resultExpression.getMessage())
                );
            }
            LOG.info(MessageFormat.format("Expression evaluation ({0}) completed successfully",  args.getPropertyExpression()));
        } catch (WsException e) {
            throw new TaskException(e);
        }
    }
}
