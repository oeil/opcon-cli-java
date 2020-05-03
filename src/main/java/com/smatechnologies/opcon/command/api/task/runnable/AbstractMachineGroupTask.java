package com.smatechnologies.opcon.command.api.task.runnable;

import com.smatechnologies.opcon.command.api.arguments.OpConCliArguments;
import com.smatechnologies.opcon.command.api.task.IRunnableTask;
import com.smatechnologies.opcon.command.api.task.exception.TaskException;
import com.smatechnologies.opcon.command.api.util.Utilities;
import com.smatechnologies.opcon.restapiclient.WsException;
import com.smatechnologies.opcon.restapiclient.api.OpconApi;
import com.smatechnologies.opcon.restapiclient.api.machinegroups.MachineGroupsCriteria;
import com.smatechnologies.opcon.restapiclient.api.machines.MachinesCriteria;
import com.smatechnologies.opcon.restapiclient.model.MachineGroup;
import com.smatechnologies.opcon.restapiclient.model.Version;
import com.smatechnologies.opcon.restapiclient.model.machine.Machine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.List;

public abstract class AbstractMachineGroupTask implements IRunnableTask {

    private final static Logger LOG = LoggerFactory.getLogger(AbstractMachineGroupTask.class);

    protected void checkPreReqs(OpconApi api, OpConCliArguments args) throws TaskException {
        final Version version = api.getVersion();
        if (!new Utilities().versionCheck(version.getOpConRestApiProductVersion(), args.getTask())) {
            throw new TaskException("OpCon-API Version {0} not supported, must be 17.1.0 or greater");
        }

        if (args.getMachineGroupName() == null) {
            throw new TaskException("Required -mg (machine group) argument missing");
        }
        if (args.getMachineName() == null) {
            throw new TaskException("Required -mn (machine name) argument missing");
        }
    }

    public Machine fetchMachine(OpconApi api, OpConCliArguments args) throws TaskException {
        LOG.debug(MessageFormat.format("Fetch Machine ({0})", args.getMachineName()));

        final MachinesCriteria criteria = new MachinesCriteria();
        criteria.setName(args.getMachineName());
        criteria.setExtendedProperties(true);

        try {
            final List<Machine> machines = api.machines().get(criteria);
            if(machines.isEmpty()) {
                throw new TaskException(MessageFormat.format("Machine ({0}) not found in OpCon system", args.getMachineName()));
            }
            return machines.stream().findFirst().get();
        } catch (WsException e) {
            throw new TaskException(e);
        }
    }

    protected MachineGroup fetchMachineGroup(final OpconApi opconApi, final String groupName) throws TaskException {
        LOG.debug(MessageFormat.format("Fetch Machine Group ({0})", groupName));

        final MachineGroupsCriteria criteria = new MachineGroupsCriteria();
        criteria.setName(groupName);

        try {
            final List<MachineGroup> machineGroups = opconApi.machineGroups().get(criteria);
            if (machineGroups.isEmpty()) {
                throw new TaskException(MessageFormat.format("Machine Group ({0}) Not Found", groupName));
            }
            return machineGroups.stream().findFirst().get();
        } catch (WsException e) {
            throw new TaskException(e);
        }
    }
}
