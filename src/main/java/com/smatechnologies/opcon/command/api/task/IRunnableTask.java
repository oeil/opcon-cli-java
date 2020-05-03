package com.smatechnologies.opcon.command.api.task;

import com.smatechnologies.opcon.command.api.arguments.OpConCliArguments;
import com.smatechnologies.opcon.command.api.task.exception.TaskException;
import com.smatechnologies.opcon.restapiclient.api.OpconApi;

public interface IRunnableTask {

    void run(OpconApi api, OpConCliArguments args) throws TaskException;
}
