package com.smatechnologies.opcon.command.api.task;

import com.smatechnologies.opcon.command.api.task.runnable.*;

public enum Task {

    GetApplicationToken("AppToken", new TokenTask()),
    WaitJobDependency("Dependency", new DependencyTask()),
    GetJobLog("GetJobLog", null),
    DoJobAction("JobAction", null),
    AddJob("JobAdd", null),
    ExecMachineAction("MachAction", null),
    CreateMachine("MachAdd", null),
    UpdateMachine("MachUpdate", null),
    AddMachineToGroup("MachGrpAdd", new MachineGroupAddTask()),
    RemoveMachineFromGroup("MachGrpRemove", new MachineGroupRemoveTask()),
    ExecPropertyExpression("PropExp", new PropertyExpTask()),
    UpdateProperty("PropUpdate", new GlobalPropertyTask()),
    ExecScheduleAction("SchedAction", null),
    BuildSchedule("SchedBuild", null),
    RebuildSchedule("SchedRebuild", null),
    UpdateThreshold("ThreshUpdate", new ThresholdTask()),
    GetVersion("Version", new VersionTask());

    private final String id;
    private final IRunnableTask runnableTask;

    Task(String id, IRunnableTask runnableTask) {
        this.id = id;
        this.runnableTask = runnableTask;
    }

    public static Task byId(final String id) throws Exception {
        for (Task task : Task.values()) {
            if (task.getId().equalsIgnoreCase(id)) {
                return task;
            }
        }
        throw new Exception("Unknown Task");
    }

    public String getId() {
        return this.id;
    }

    public IRunnableTask get() {
        return this.runnableTask;
    }
}
