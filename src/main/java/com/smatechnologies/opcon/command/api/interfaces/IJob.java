package com.smatechnologies.opcon.command.api.interfaces;

import java.util.List;

import com.smatechnologies.opcon.command.api.arguments.OpConCliArguments;
import com.smatechnologies.opcon.command.api.modules.JobLogData;
import com.smatechnologies.opcon.command.api.task.exception.TaskException;
import com.smatechnologies.opcon.restapiclient.api.OpconApi;
import com.smatechnologies.opcon.restapiclient.model.dailyjob.DailyJob;

public interface IJob {

	public Integer jobActionRequest(OpconApi opconApi, OpConCliArguments _OpConCliArguments) throws TaskException;
	public Integer jobAddRequest(OpconApi opconApi, OpConCliArguments _OpConCliArguments) throws TaskException;
	public List<JobLogData> getJobLog(OpconApi opconApi, OpConCliArguments _OpConCliArguments) throws TaskException;
	public List<JobLogData> getJobLogByDailyJob(OpconApi opconApi, DailyJob dailyJob) throws TaskException;
	public DailyJob getDailyJobByName(OpconApi opconApi, OpConCliArguments _OpConCliArguments) throws TaskException;
	public DailyJob getDailyJobById(OpconApi opconApi, String jobId) throws TaskException;

}
