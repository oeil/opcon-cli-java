package com.smatechnologies.opcon.command.api.task.runnable;

import com.smatechnologies.opcon.command.api.arguments.OpConCliArguments;
import com.smatechnologies.opcon.command.api.task.IRunnableTask;
import com.smatechnologies.opcon.command.api.task.exception.TaskException;
import com.smatechnologies.opcon.command.api.util.Utilities;
import com.smatechnologies.opcon.restapiclient.WsException;
import com.smatechnologies.opcon.restapiclient.api.OpconApi;
import com.smatechnologies.opcon.restapiclient.api.globalproperties.GlobalPropertiesCriteria;
import com.smatechnologies.opcon.restapiclient.model.GlobalProperty;
import com.smatechnologies.opcon.restapiclient.model.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.List;

public class GlobalPropertyTask implements IRunnableTask {

    private final static Logger LOG = LoggerFactory.getLogger(GlobalPropertyTask.class);

    private void checkPreReqs(OpconApi api, OpConCliArguments args) throws TaskException {
        if (args.getPropertyName() == null) {
            throw new TaskException("Required -pn (property name) argument missing for PropUpdate task");
        }
        if (args.getPropertyValue() == null) {
            throw new TaskException("Required -pv (property value) argument missing for PropUpdate task");
        }

        Version version = api.getVersion();
        if (!new Utilities().versionCheck(version.getOpConRestApiProductVersion(), args.getTask())) {
            throw new TaskException(MessageFormat.format("OpCon-API Version {0} not supported, must be 18.1.0 or greater", version.getOpConRestApiProductVersion()));
        }
    }

    @Override
    public void run(OpconApi api, OpConCliArguments args) throws TaskException {
        checkPreReqs(api, args);

        LOG.info(MessageFormat.format(
                "Processing task ({0}) arguments : property ({1}) value ({2}) encrypted ({3})",
                args.getTask(), args.getPropertyName(), args.getPropertyValue(), args.isPropertyEncrypted())
        );

        try {
            final GlobalPropertiesCriteria criteria = new GlobalPropertiesCriteria();
            criteria.setName(args.getPropertyName());

            final List<GlobalProperty> properties = api.globalProperties().get(criteria);
            if (properties.isEmpty()) {
                throw new TaskException(MessageFormat.format(
                        "Property ({0}) update failed : {1}",
                        args.getPropertyName(), "Property not found in Opcon database")
                );
            }

            final GlobalProperty property = properties.stream().findFirst().get();
            property.setValue(args.getPropertyValue());

            api.globalProperties().put(property);
            LOG.info(MessageFormat.format(
                    "Property ({0}) updated to ({1}) successfully",
                    args.getPropertyName(), args.getPropertyValue())
            );
        } catch (WsException e) {
            throw new TaskException(e);
        }
    }
}
