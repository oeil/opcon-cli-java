package com.smatechnologies.opcon.command.api.task.runnable;

import com.smatechnologies.opcon.command.api.arguments.OpConCliArguments;
import com.smatechnologies.opcon.command.api.task.exception.TaskException;
import com.smatechnologies.opcon.restapiclient.WsException;
import com.smatechnologies.opcon.restapiclient.api.OpconApi;
import com.smatechnologies.opcon.restapiclient.model.MachineGroup;
import com.smatechnologies.opcon.restapiclient.model.machine.Machine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class MachineGroupRemoveTask extends AbstractMachineGroupTask {

    private final static Logger LOG = LoggerFactory.getLogger(MachineGroupRemoveTask.class);

    @Override
    public void run(OpconApi api, OpConCliArguments args) throws TaskException {
        checkPreReqs(api, args);

        LOG.info(MessageFormat.format("Processing task ({0}) arguments : Machine Group ({1}) Machine Name ({2})",
                args.getTask(),
                args.getMachineGroupName(),
                args.getMachineName(),
                "add"
        ));

        final Machine machine = fetchMachine(api, args);
        if (!machine.getGroups().stream().anyMatch(g -> g.getName().equalsIgnoreCase(args.getMachineGroupName()))) {
            LOG.info(MessageFormat.format("Machine {0} not in machine group {1}", args.getMachineName(), args.getMachineGroupName()));
            return;
        }

        LOG.info(MessageFormat.format("Removing machine {0} from machine group {1}", args.getMachineName(), args.getMachineGroupName()));
        final List<MachineGroup> updatedRemoveList = new ArrayList<>();
        for(MachineGroup machineGroup : machine.getGroups()) {
            if(!machineGroup.getName().equalsIgnoreCase(args.getMachineGroupName())) {
                updatedRemoveList.add(machineGroup);
            }
        }
        machine.setGroups(updatedRemoveList);

        try {
            api.machines().put(machine);
            LOG.info(MessageFormat.format("Machine {0} removed from machine group {1}", args.getMachineName(), args.getMachineGroupName()));
        } catch (WsException e) {
            throw new TaskException(e);
        }
    }
}
