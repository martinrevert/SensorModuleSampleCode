package com.alps.sample.sensorModule.command;

import com.alps.sample.log.Logg;
import com.alps.sample.sensorModule.SensorModule;
import com.alps.sample.sensorModule.command.control.CtrlCmd;

import java.util.ArrayList;
import java.util.List;


/**
 * [JP] 与えられたコマンドリストを順次実行します。
 */
public class Commander {
	private static final String TAG = "Controller";


	public Commander() {
		ctrlCmds = new ArrayList<CtrlCmd>();
	}


	private List<CtrlCmd> ctrlCmds;
	private int index;

	public void addCommand(CtrlCmd ctrlCmd) {
		ctrlCmds.add(ctrlCmd);
	}

	public enum RunMode {
		Stopped,
		WaitResponse,
	}
	private RunMode runMode;
	
	public interface ICommander {
		void onBatchFinish();
	}
	public interface ICommanderCareful extends ICommander {
		void onReceiveResponse(CtrlCmd cmdSent, CtrlCmd cmdResp);
	}
	private ICommanderCareful iCommanderCareful;

	private SensorModule target;


	public void run(RunMode runMode, SensorModule target, ICommanderCareful iCommanderCareful) {
		Logg.d(TAG, "run");
		this.runMode = runMode;
		this.target = target;
		this.iCommanderCareful = iCommanderCareful;

		index = 0;

		if (ctrlCmds.size() == 0) {
			Logg.d(TAG, "No reserved commands");
			this.runMode = RunMode.Stopped;
			iCommanderCareful.onBatchFinish();
		}
		else {
			doCommand();
		}
	}

	public void abort() {
		ctrlCmds.clear();
	}

	private CtrlCmd currentCommand;
	private void doCommand() {
		if (runMode == RunMode.Stopped) {
			return;
		}

		if (ctrlCmds.size() > index) {
			currentCommand = ctrlCmds.get(index);
			target.sendCommand(currentCommand);
		}
		else {
			this.runMode = RunMode.Stopped;
			iCommanderCareful.onBatchFinish();
		}
	}

	public void receiveResponse(CtrlCmd responseCommand) {
		if (iCommanderCareful == null) {
			Logg.d(TAG, "[ERROR] interface is released!");
			abort();
			return;
		}

		if (responseCommand != null) {
			iCommanderCareful.onReceiveResponse(currentCommand, responseCommand);
		}

		requestNextCommand();
	}

	private void requestNextCommand() {
		if (runMode == RunMode.Stopped) {
			return;
		}

		++index;
		doCommand();
	}
}
