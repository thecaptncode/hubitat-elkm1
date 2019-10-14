/***********************************************************************************************************************
 *
 *  A Hubitat Driver using Telnet on the local network to connect to the Elk M1 via the M1XEP or C1M1
 *
 *  License:
 *  This program is free software: you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 *  implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 *
 *  Name: Elk M1 Driver
 *
 *  A Special Thanks to Doug Beard for the framework of this driver!
 *
 *  I am not a programmer so alot of this work is through trial and error. I also spent a good amount of time looking
 *  at other integrations on various platforms. I know someone else was working on an Elk driver that involved an ESP
 *  setup. This is a more direct route using equipment I already owned.
 *
 *** See Release Notes at the bottom***
 ***********************************************************************************************************************/

public static String version() { return "v0.1.33" }

import groovy.transform.Field

metadata {
	definition(name: "Elk M1 Driver", namespace: "belk", author: "Mike Magrann") {
		capability "Actuator"
		capability "Switch"
		capability "Initialize"
		capability "Telnet"
		capability "ContactSensor"
		command "Disarm"
		command "ArmAway"
		command "ArmHome"
		command "ArmStayInstant"
		command "ArmNight"
		command "ArmNightInstant"
		command "ArmVacation"
		command "refreshArmStatus"
		command "refreshLightingStatus"
		command "refreshOutputStatus"
		command "refreshTemperatureStatus"
		command "refreshZoneStatus"
//This is used to run the zone import script
//		command "RequestTextDescriptions"
		command "RequestZoneDefinitions"
		command "sendMsg"
		attribute "ArmStatus", "string"
		attribute "ArmState", "string"
		attribute "AlarmState", "string"
		attribute "LastUser", "string"
	}
	preferences {
		input name: "ip", type: "text", title: "IP Address", description: "ip", required: true
		input name: "port", type: "text", title: "Port", description: "port", required: true
		input name: "passwd", type: "text", title: "Password", description: "password", required: true
		input name: "code", type: "text", title: "Code", description: "code", required: true
		input name: "dbgEnable", type: "bool", title: "Enable debug logging", defaultValue: false
		input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
	}
}

//general handlers
def installed() {
	log.warn "${device.label} installed..."
	initialize()
}

def updated() {
	log.info "${device.label} Updated..."
	if (dbgEnable)
		log.debug "${device.label}: Configuring IP: ${ip}, Port ${port}, Code: ${code}, Password: ${passwd}"
	initialize()
}

def initialize() {
	telnetClose()
	boolean success = true
	try {
		//open telnet connection
		telnetConnect([termChars: [13, 10]], ip, port.toInteger(), null, null)
		//give it a chance to start
		pauseExecution(1000)
		if (dbgEnable)
			log.debug "${device.label}: Telnet connection to Elk M1 established"
	} catch (e) {
		log.warn "${device.label}: initialize error: ${e.message}"
		success = false
	}
	if (success) {
		runIn(1, "refresh")
	}
}

def uninstalled() {
	telnetClose()
	removeChildDevices(getChildDevices())
}

private removeChildDevices(delete) {
	delete.each { deleteChildDevice(it.deviceNetworkId) }
}

def refresh() {
	runIn(1, "refreshVersionNumber")
	pauseExecution(1000)
	runIn(1, "refreshTemperatureStatus")
	pauseExecution(1000)
	runIn(1, "refreshArmStatus")
	pauseExecution(1000)
	runIn(1, "refreshOutputStatus")
	pauseExecution(1000)
	runIn(1, "refreshZoneStatus")
	pauseExecution(1000)
	refreshLightingStatus()
}

//Elk M1 Command Line Request - Start of
hubitat.device.HubAction off() {
	Disarm()
}

hubitat.device.HubAction on() {
	ArmAway()
}

hubitat.device.HubAction Disarm() {
	if (dbgEnable)
		log.debug "${device.label} Disarm"
	String cmd = elkCommands["Disarm"]
	String area = "1"
	sendMsg(cmd, area)
}


hubitat.device.HubAction ArmAway() {
	if (dbgEnable)
		log.debug "${device.label} ArmAway"
	String cmd = elkCommands["ArmAway"]
	String area = "1"
	sendMsg(cmd, area)
}

hubitat.device.HubAction ArmHome() {
	if (dbgEnable)
		log.debug "${device.label} ArmHome"
	def cmd = elkCommands["ArmHome"]
	String area = "1"
	sendMsg(cmd, area)
}

hubitat.device.HubAction ArmStayInstant() {
	if (dbgEnable)
		log.debug "${device.label} ArmStayInstant"
	String cmd = elkCommands["ArmStayInstant"]
	String area = "1"
	sendMsg(cmd, area)
}

hubitat.device.HubAction ArmNight() {
	if (dbgEnable)
		log.debug "${device.label} ArmNight"
	String cmd = elkCommands["ArmNight"]
	String area = "1"
	sendMsg(cmd, area)
}

hubitat.device.HubAction ArmNightInstant() {
	if (dbgEnable)
		log.debug "${device.label} ArmNightInstant"
	String cmd = elkCommands["ArmNightInstant"]
	String area = "1"
	sendMsg(cmd, area)
}

hubitat.device.HubAction ArmVacation() {
	if (dbgEnable)
		log.debug "${device.label} ArmVacation"
	String cmd = elkCommands["ArmVacation"]
	String area = "1"
	sendMsg(cmd, area)
}

hubitat.device.HubAction refreshVersionNumber() {
	if (dbgEnable)
		log.debug "${device.label} refreshVersionNumber"
	String cmd = elkCommands["RequestVersionNumber"]
	sendMsg(cmd)
}

hubitat.device.HubAction refreshArmStatus() {
	if (dbgEnable)
		log.debug "${device.label} refreshArmStatus"
	String cmd = elkCommands["RequestArmStatus"]
	sendMsg(cmd)
}

hubitat.device.HubAction refreshTemperatureStatus() {
	if (dbgEnable)
		log.debug "${device.label} refreshTemperatureStatus"
	String cmd = elkCommands["RequestTemperatureData"]
	sendMsg(cmd)
}

def RequestTextDescriptions(String deviceType, int startDev) {
	if (dbgEnable)
		log.debug "${device.label} RequestTextDescriptions Type: ${deviceType} Zone: ${startDev}"
	if (startDev == 1)
		state.creatingZone = true
	runIn(10, "stopCreatingZone")
	String cmd = elkCommands["RequestTextDescriptions"] + deviceType + String.format("%03d", startDev) + "00"
	cmd = addChksum(Integer.toHexString(cmd.length() + 2).toUpperCase().padLeft(2, '0') + cmd)
	if (dbgEnable)
		log.debug "${device.label}: sending ${cmd}"
	sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.TELNET))
}

def stopCreatingZone() {
	state.creatingZone = false
}

hubitat.device.HubAction ControlOutputOn(BigDecimal output = 0, String time = "0") {
	if (dbgEnable)
		log.debug "${device.label} ControlOutputOn output ${output}"
	String cmd = elkCommands["ControlOutputOn"]
	cmd = cmd + String.format("%03d", output.intValue()) + time.padLeft(5, '0')
	sendMsg(cmd)
}

hubitat.device.HubAction ControlOutputOff(BigDecimal output = 0) {
	if (dbgEnable)
		log.debug "${device.label} ControlOutputOff output ${output}"
	String cmd = elkCommands["ControlOutputOff"]
	cmd = cmd + String.format("%03d", output.intValue())
	sendMsg(cmd)
}

hubitat.device.HubAction ControlOutputToggle(BigDecimal output = 0) {
	if (dbgEnable)
		log.debug "${device.label} ControlOutputToggle output ${output}"
	String cmd = elkCommands["ControlOutputToggle"]
	cmd = cmd + String.format("%03d", output.intValue())
	sendMsg(cmd)
}

hubitat.device.HubAction TaskActivation(BigDecimal task = 0) {
	if (dbgEnable)
		log.debug "${device.label} TaskActivation task: ${task}"
	String cmd = elkCommands["TaskActivation"]
	cmd = cmd + String.format("%03d", task.intValue())
	sendMsg(cmd)
}

hubitat.device.HubAction refreshThermostatStatus(String thermostat) {
	if (dbgEnable)
		log.debug "${device.label} refreshThermostatStatus tstat: ${thermostat}"
	String cmd = elkCommands["RequestThermostatData"]
	cmd = cmd + thermostat
	sendMsg(cmd)
}

hubitat.device.HubAction setThermostatMode(String thermostatmode, String thermostat) {
	if (dbgEnable)
		log.debug "${device.label} setThermostatMode tstat: ${thermostat} mode ${thermostatmode}"
	String cmd = elkCommands["SetThermostatData"]
	String value = elkThermostatModeSet[thermostatmode].padLeft(2, '0')
	String element = "0"
	cmd = cmd + thermostat + value + element
	sendMsg(cmd)
}

hubitat.device.HubAction setThermostatFanMode(String fanmode, String thermostat) {
	if (dbgEnable)
		log.debug "${device.label} setThermostatFanMode tstat: ${thermostat} fanmode ${fanmode}"
	String cmd = elkCommands["SetThermostatData"]
	String value = elkThermostatFanModeSet[fanmode].padLeft(2, '0')
	String element = "2"
	cmd = cmd + thermostat + value + element
	sendMsg(cmd)
}

hubitat.device.HubAction setThermostatHoldMode(String holdmode, String thermostat) {
	if (dbgEnable)
		log.debug "${device.label} setThermostatHoldMode tstat: ${thermostat} hold ${holdmode}"
	String cmd = elkCommands["SetThermostatData"]
	String value = elkThermostatHoldModeSet[holdmode]
	String element = "1"
	cmd = cmd + thermostat + value + element
	sendMsg(cmd)
}

hubitat.device.HubAction setCoolingSetpoint(BigDecimal degrees, String thermostat) {
	if (dbgEnable)
		log.debug "${device.label} setCoolingSetpoint tstat: ${thermostat} temperature ${degrees}"
	String cmd = elkCommands["SetThermostatData"]
	String value = String.format("%02d", degrees.intValue())
	String element = "4"
	cmd = cmd + thermostat + value + element
	sendMsg(cmd)
}

hubitat.device.HubAction setHeatingSetpoint(BigDecimal degrees, String thermostat) {
	if (dbgEnable)
		log.debug "${device.label} setHeatingSetpoint tstat: ${thermostat} temperature ${degrees}"
	String cmd = elkCommands["SetThermostatData"]
	String value = String.format("%02d", degrees.intValue())
	String element = "5"
	cmd = cmd + thermostat + value + element
	sendMsg(cmd)
}

hubitat.device.HubAction RequestZoneDefinitions() {
	if (dbgEnable)
		log.debug "${device.label} RequestZoneDefinitions"
	String cmd = elkCommands["RequestZoneDefinitions"]
	sendMsg(cmd)
}

hubitat.device.HubAction refreshZoneStatus() {
	if (dbgEnable)
		log.debug "${device.label} refreshZoneStatus"
	String cmd = elkCommands["RequestZoneStatus"]
	sendMsg(cmd)
}

hubitat.device.HubAction refreshOutputStatus() {
	if (dbgEnable)
		log.debug "${device.label} refreshOutputStatus"
	String cmd = elkCommands["RequestOutputStatus"]
	sendMsg(cmd)
}

hubitat.device.HubAction controlLightingMode(String unitCode, String mode, String setting, String time) {
	if (dbgEnable)
		log.debug "${device.label} controlLightingMode ${unitCode} mode: ${mode} setting: ${setting} time: ${time}"
	String cmd = elkCommands["ControlLightingMode"] +
			unitCode + mode.padLeft(2, '0').take(2) + setting.padLeft(2, '0').take(2) + time.padLeft(4, '0').take(4)
	sendMsg(cmd)
}

hubitat.device.HubAction controlLightingOn(String unitCode = "A03") {
	if (dbgEnable)
		log.debug "${device.label} controlLightingOn: ${unitCode}"
	String cmd = elkCommands["ControlLightingOn"] + unitCode
	sendMsg(cmd)
}

hubitat.device.HubAction controlLightingOff(String unitCode) {
	if (dbgEnable)
		log.debug "${device.label} controlLightingOff: ${unitCode}"
	String cmd = elkCommands["ControlLightingOff"] + unitCode
	sendMsg(cmd)
}

hubitat.device.HubAction controlLightingToggle(String unitCode) {
	if (dbgEnable)
		log.debug "${device.label} controlLightingToggle: ${unitCode}"
	String cmd = elkCommands["ControlLightingToggle"] + unitCode
	sendMsg(cmd)
}

hubitat.device.HubAction refreshLightingStatus() {
	if (dbgEnable)
		log.debug "${device.label} refreshLightingStatus"
	runIn(1, "refreshLightingStatus", [data: "0"])
	pauseExecution(1000)
	runIn(1, "refreshLightingStatus", [data: "1"])
	pauseExecution(1000)
	runIn(1, "refreshLightingStatus", [data: "2"])
	pauseExecution(1000)
	runIn(1, "refreshLightingStatus", [data: "3"])
}

hubitat.device.HubAction refreshLightingStatus(String unitCode) {
	String bank = unitCode.take(1)
	if (bank >= "A" && bank <= "D")
		bank = "0"
	else if (bank >= "E" && bank <= "H")
		bank = "1"
	else if (bank >= "I" && bank <= "L")
		bank = "2"
	else if (bank >= "M" && bank <= "P")
		bank = "3"

	if (dbgEnable)
		log.debug "${device.label} refreshLightingStatus: ${bank}"
	String cmd = elkCommands["RequestLightingStatus"] + bank
	sendMsg(cmd)
}
//Elk M1 Command Line Request - End of


//Elk M1 Message Send Lines - Start of
hubitat.device.HubAction sendMsg(String cmd, String area = null) {
	String msg
	if (area == null)
		msg = cmd + "00"
	else
		msg = cmd + area.padLeft(1, '0').reverse().take(1) + code.padLeft(6, '0') + "00"
	String msgStr = addChksum(Integer.toHexString(msg.length() + 2).toUpperCase().padLeft(2, '0') + msg)
	if (dbgEnable)
		log.debug "${device.label} sendMsg: $msgStr"
	return new hubitat.device.HubAction(msgStr, hubitat.device.Protocol.TELNET)
}

hubitat.device.HubAction sendMsg(hubitat.device.HubAction action = null) {
	return action
}

String addChksum(String msg) {
	char[] msgArray = msg.toCharArray()
	int msgSum = 0
	msgArray.each { (msgSum += (int) it) }
	String chkSumStr = msg + Integer.toHexString(256 - (msgSum % 256)).toUpperCase().padLeft(2, '0')
}

//Elk M1 Message Send Lines - End of


//Elk M1 Event Receipt Lines
private parse(String message) {
	if (dbgEnable)
		log.debug "${device.label} Parsing Incoming message: " + message

	switch (message.substring(2, 4)) {
		case "ZC":
			zoneChange(message);
			break;
		case "CC":
			outputChange(message);
			break;
		case "TC":
			taskChange(message);
			break;
		case "EE":
			entryExitChange(message);
			break;
		case "AS":
			armStatusReport(message);
			break;
		case "IC":
			userCodeEntered(message);
			break;
		case "AR":
			alarmReporting(message);
			break;
		case "CS":
			outputStatus(message);
			break;
		case "DS":
			lightingDeviceStatus(message);
			break;
		case "PS":
			lightingBankStatus(message);
			break;
		case "PC":
			lightingDeviceChange(message);
			break;
		case "LD":
			logData(message);
			break;
		case "LW":
			temperatureData(message);
			break;
		case "SD":
			stringDescription(message);
			break;
		case "ST":
			statusTemperature(message);
			break;
		case "TR":
			thermostatData(message);
			break;
		case "AM":
			//if (dbgEnable) log.debug "${device.label}: The event is unknown: ";
			break;
		case "XK":
			// Ethernet Test
			break;
		case "VN":
			versionNumberReport(message);
			break;
		case "ZD":
			zoneDefinitionReport(message);
			break;
		case "ZS":
			zoneStatusReport(message);
			break;
		default:
			if (dbgEnable) log.debug "${device.label}: The ${message.substring(2, 4)} command is unknown";
			break;
	}
}

def zoneChange(String message) {
	String zoneNumber = message.substring(4, 7)
	String zoneStatus = message.substring(7, 8)
	switch (zoneStatus) {
		case "1":
			if (dbgEnable) log.debug "${device.label} ZoneChange: ${zoneNumber} - ${zoneStatus} - ${NormalOpen}";
			zoneNormal(message);
			break;
		case "2":
			if (dbgEnable) log.debug "${device.label} ZoneChange: ${zoneNumber} - ${zoneStatus} - ${NormalEOL}";
			zoneNormal(message);
			break;
		case "3":
			if (dbgEnable) log.debug "${device.label} ZoneChange: ${zoneNumber} - ${zoneStatus} - ${NormalShort}";
			zoneNormal(message);
			break;
		case "9":
			if (dbgEnable) log.debug "${device.label} ZoneChange: ${zoneNumber} - ${zoneStatus} - ${ViolatedOpen}";
			zoneViolated(message);
			break;
		case "A":
			if (dbgEnable) log.debug "${device.label} ZoneChange: ${zoneNumber} - ${zoneStatus} - ${ViolatedEOL}";
			zoneViolated(message);
			break;
		case "B":
			if (dbgEnable) log.debug "${device.label} ZoneChange: ${zoneNumber} - ${zoneStatus} - ${ViolatedShort}";
			zoneViolated(message);
			break;
//		case "0":
//			if (dbgEnable) log.debug "${device.label} ZoneChange: ${zoneNumber} - ${zoneStatus} - ${NormalUnconfigured}";
//			zoneNormal(message);
//			break;
//		case "5":
//			if (dbgEnable) log.debug "${device.label} ZoneChange: ${zoneNumber} - ${zoneStatus} - ${TroubleOpen}";
//			break;
//		case "6":
//			zoneStatus = TroubleEOL;
//			if (dbgEnable) log.debug "${device.label} ZoneChange: ${zoneNumber} - ${zoneStatus} - ${TroubleEOL}";
//			break;
//		case "7":
//			if (dbgEnable) log.debug "${device.label} ZoneChange: ${zoneNumber} - ${zoneStatus} - ${TroubleShort}";
//			break;
//		case "8":
//			if (dbgEnable) log.debug "${device.label} ZoneChange: ${zoneNumber} - ${zoneStatus} - ${NotUsed}";
//			break;
		default:
			if (dbgEnable) log.debug "${device.label} Unknown zone status message: ${zoneStatus}";
			break;
	}
}

def entryExitChange(String message) {
	String exitArea = message.substring(4, 5)
	String exitTime = message.substring(6, 12)
	String armStatus = elkArmStatuses[message.substring(12, 13)]
	if (dbgEnable)
		log.debug "${device.label} Area: $exitArea, Time: $exitTime, ArmStatus: $armStatus"
	if (exitArea == "1" && exitTime == "000000") {
		String newArmMode
		String newSetArm
		if (armStatus == Disarmed) {
			newArmMode = "Home"
			newSetArm = "disarm"
		} else if (armStatus == ArmedStay || armStatus == ArmedStayInstant) {
			newArmMode = "Stay"
			newSetArm = "armHome"
		} else if (armStatus == ArmedtoNight || armStatus == ArmedtoNightInstant) {
			newArmMode = "Night"
			newSetArm = "armNight"
		} else if (armStatus == ArmedtoVacation) {
			newArmMode = "Vacation"
			newSetArm = "armAway"
		} else {
			newArmMode = "Away"
			newSetArm = "armAway"
		}
		setMode(newArmMode, newSetArm, armStatus)
	}
}

def armStatusReport(String message) {
	String armStatus = elkArmStatuses[message.substring(4, 5)]
	String armUpState = elkArmUpStates[message.substring(12, 13)]
	String alarmState = elkAlarmStates[message.substring(20, 21)]
	if (dbgEnable) {
		log.debug "${device.label} ArmStatus: ${armStatus}"
		log.debug "${device.label} ArmUpState: ${armUpState}"
		log.debug "${device.label} AlarmState: ${alarmState}"
	}
	sendEvent(name: "ArmState", value: armUpState)
	if (state.alarmState != alarmState) {
		state.alarmState = alarmState
		sendEvent(name: "AlarmState", value: alarmState)
		if (txtEnable)
			log.info "${device.label} AlarmState changed to ${alarmState}"
		if (alarmState == PoliceAlarm || alarmState == BurgularAlarm) {
			device.sendEvent(name: "contact", value: "open")
		} else {
			device.sendEvent(name: "contact", value: "closed")
		}
	}
}

def userCodeEntered(String message) {
	String userCode = message.substring(16, 19)
	if (txtEnable)
		log.info "${device.label} LastUser was: ${userCode}"
	sendEvent(name: "LastUser", value: userCode)
}

def alarmReporting(String message) {
	log.warn "${device.label} AlarmReporting"
	String accountNumber = message.substring(4, 10)
	String alarmCode = message.substring(10, 14)
	String area = message.substring(14, 16)
	String zone = message.substring(16, 19)
	String telIp = message.substring(19, 20)
}

def outputChange(String message) {
	String outputNumber = message.substring(4, 7)
	String outputState = (message.substring(7, 8) == "1") ? "on" : "off"
	def zoneDevice = getChildDevice("${device.deviceNetworkId}_O_${outputNumber}")
	if (zoneDevice != null) {
		if (dbgEnable)
			log.debug "${device.label} outputChange: ${outputNumber} - ${outputState}"
		zoneDevice.parse(outputState)
		if (state.outputReport != null) {
			state.outputReport = sendReport(state.outputReport, zoneDevice, outputNumber, outputState == "on")
		}
	}
}

def outputStatus(String message) {
	String outputString = message.substring(4, 212)
	String outputState
	int i
	for (i = 1; i <= 208; i++) {
		outputState = outputString.substring(i - 1, i)
		//if (dbgEnable)
		//	log.debug "${device.label}: OutputStatus: Output " + i + " - " + elkOutputStates[outputState]
		outputChange("    " + String.format("%03d", i) + outputState)
		if (i <= 32) {
			taskStatus([Message: "    " + String.format("%03d", i), State: "off"])
		}
	}
}

def lightingDeviceStatus(String message) {
	int deviceNumber = message.substring(4, 7).toInteger()
	String level = message.substring(7, 9)
	int ndx = (deviceNumber - 1) / 16
	int unitNumber = deviceNumber - ndx * 16
	lightingDeviceChange("    " + "ABCDEFGHIJKLMNOP".substring(ndx, ndx + 1) + String.format("%02d", unitNumber) + level)
}

def lightingBankStatus(String message) {
	int bank = message.substring(4, 5).toInteger()
	char[] statusString = message.substring(5, 69).toCharArray()
	String groups = "ABCDEFGHIJKLMNOP".substring(bank * 4, bank * 4 + 4)
	String search = device.deviceNetworkId + "_L_"
	int len = search.length()
	String childDeviceDNID
	int ndx
	int level
	getChildDevices().each {
		childDeviceDNID = it.deviceNetworkId
		if (childDeviceDNID.substring(0, len) == search) {
			ndx = groups.indexOf(childDeviceDNID.substring(len, len + 1))
			if (ndx != -1) {
				level = ((int) statusString[childDeviceDNID.substring(len + 1, len + 3).toInteger() + ndx * 16 - 1]) - 48
				lightingDeviceChange("    " + childDeviceDNID.substring(len, len + 3) + String.format("%02d", level))
			}
		}
	}
}

def lightingDeviceChange(String message) {
	String unitCode = message.substring(4, 7)
	String level = message.substring(7, 9)
	if (dbgEnable)
		log.debug "${device.label} Light: ${unitCode} Level: ${level}"
	getChildDevice("${device.deviceNetworkId}_L_${unitCode}")?.parse(level)
}

def logData(String message) {
	String eventCode = message.substring(4, 8)
	String eventValue = elkResponses[eventCode]
	if (eventValue != null && dbgEnable) {
		log.debug "${device.label} LogData: ${eventCode} - ${eventValue}"
	}
	if (eventCode == '1173' || eventCode == '1183' || eventCode == '1223' || eventCode == '1231') {
//		sendEvent(name:"Status", value: eventValue)
// 		systemArmed()
	} else if (eventCode == '1191' || eventCode == '1199') {
//		sendEvent(name:"Status", value: eventValue)
//		systemArmed()
	} else if (eventCode == '1174') {
//		sendEvent(name:"Status", value: eventValue)
//		disarming()
		setMode("Home", "disarm", "Disarmed")
	} else if ('1207' || eventCode == '1215') {
//		sendEvent(name:"Status", value: eventValue)
//		systemArmed()
	} else if (eventCode != '1000' && eventValue != null) {
//		sendEvent(name: "Status", value: eventValue)
	}
}

def stringDescription(String message) {
	String zoneNumber = message.substring(6, 9)
	if (zoneNumber != "000" && state.creatingZone) {
		String zoneName
		String zoneType = message.substring(4, 6)
		byte firstText = message.substring(9, 10)
		String zoneText = (String) ((char) (firstText & 0b01111111)) + message.substring(10, 25).trim()
		// Mask high order "Show On Keypad" bit in first letter
		if (zoneText != "") {
			createZone([zoneNumber: zoneNumber, zoneName: zoneName, zoneType: zoneType, zoneText: zoneText])
		}
		int i = zoneNumber.toInteger() // Request next zone description
		if (i < 208) {
			RequestTextDescriptions(zoneType, i + 1)
		}
	}
}

def temperatureData(String message) {
	String temp
	int i
	int zoneNumber
	for (i = 4; i <= 50; i += 3) {
		temp = message.substring(i, i + 3)
		if (temp != "000") {
			zoneNumber = (i - 1) / 3
			statusTemperature("    1" + String.format("%02d", zoneNumber) + temp + "    ")
		}
	}

	for (i = 52; i <= 98; i += 3) {
		temp = message.substring(i, i + 3)
		if (temp != "000") {
			zoneNumber = (i - 49) / 3
			statusTemperature("    0" + String.format("%02d", zoneNumber) + temp + "    ")
		}
	}
}

def statusTemperature(String message) {
	String group = elkTempTypes[message.substring(4, 5).toInteger()]
	String zoneNumber = message.substring(5, 7)
	int temp = message.substring(7, 10).toInteger()
	def zoneDevice
	if (group == TemperatureProbe) {
		temp = temp - 60
		if (dbgEnable)
			log.debug "${device.label} Zone ${zoneNumber} temperature is ${temp}°"
		zoneDevice = getChildDevice("${device.deviceNetworkId}_Z_0${zoneNumber}")
		if (zoneDevice?.hasCapability("TemperatureMeasurement")) {
			zoneDevice.sendEvent(name: "temperature", value: temp)
		}
	} else if (group == Keypads) {
		temp = temp - 40
		if (dbgEnable)
			log.debug "${device.label} Keypad ${zoneNumber} temperature is ${temp}°"
		zoneDevice = getChildDevice("${device.deviceNetworkId}_P_0${zoneNumber}")
		if (zoneDevice?.hasCapability("TemperatureMeasurement")) {
			zoneDevice.sendEvent(name: "temperature", value: temp)
		}
	} else if (group == Thermostats) {
		if (dbgEnable)
			log.debug "${device.label} Thermostat ${zoneNumber} temperature is ${temp}°"
		zoneDevice = getChildDevice("${device.deviceNetworkId}_T_0${zoneNumber}")
		if (zoneDevice?.hasCapability("Thermostat")) {
			zoneDevice.sendEvent(name: "temperature", value: temp)
		}
	}
}

def taskChange(String message) {
	taskStatus([Message: message, State: "on"])
	runIn(2, "taskStatus", [data: [Message: message, State: "off"]])
}

def thermostatData(String message) {
	String thermNumber = message.substring(4, 6).padLeft(3, '0')
	def zoneDevice = getChildDevice(device.deviceNetworkId + "_T_" + thermNumber)
	if (zoneDevice != null) {
		if (dbgEnable)
			log.debug "${device.label} thermostatData: ${thermNumber} - ${message.substring(6, 17)}"
		zoneDevice.parse(message)
	}
}

def versionNumberReport(String message) {
	BigInteger m1Version = new BigInteger(message.substring(4, 10), 16)
	BigInteger xepVersion = new BigInteger(message.substring(10, 16), 16)
	int m1U = m1Version / 65536
	int m1M = (m1Version - m1U * 65536) / 256
	int m1L = m1Version - m1U * 65536 - m1M * 256
	int xepU = xepVersion / 65536
	int xepM = (xepVersion - xepU * 65536) / 256
	int xepL = xepVersion - xepU * 65536 - xepM * 256
	state.m1Version = String.format("%3d.%3d.%3d", m1U, m1M, m1L).replace(" ", "")
	state.xepVersion = String.format("%3d.%3d.%3d", xepU, xepM, xepL).replace(" ", "")
	if (txtEnable)
		log.info "${device.label} panel version ${state.m1Version}, XEP Version ${state.xepVersion} found"
}

def zoneDefinitionReport(String message) {
	String zoneString
	String zoneDefinitions = message.substring(4, 212)
	int i
	for (i = 1; i <= 208; i++) {
		zoneString = elkZoneDefinitions[zoneDefinitions.substring(i - 1, i)]
		if (zoneString != null && zoneString != Disabled && txtEnable) {
			log.info "${device.label} zoneDefinitionReport: Zone " + i + " - " + zoneString
		}
	}
}

def zoneStatusReport(String message) {
	String zoneString = message.substring(4, 212)
	String zoneStatus
	int i
	for (i = 1; i <= 208; i++) {
		zoneStatus = zoneString.substring(i - 1, i)
		if (zoneStatus != null && zoneStatus != "0") {
			//if (dbgEnable)
			//	log.debug "${device.label} ZoneStatus: Zone " + i + " - " + elkZoneStatuses[zoneStatus]
			zoneChange("    " + String.format("%03d", i) + zoneStatus + "    ")
		}
	}
}

// Zone Status
def zoneViolated(String message) {
	String zoneNumber = message.substring(message.length() - 8).take(3)
	def zoneDevice = getChildDevice("${device.deviceNetworkId}_Z_${zoneNumber}")
	if (zoneDevice == null) { // For backwards capability
		zoneDevice = getChildDevice("${device.deviceNetworkId}_C_${zoneNumber}")
	}
	if (zoneDevice == null) { // For backwards capability
		zoneDevice = getChildDevice("${device.deviceNetworkId}_M_${zoneNumber}")
	}

	if (zoneDevice != null) {
		if (state.zoneReport != null) {
			state.zoneReport = sendReport(state.zoneReport, zoneDevice, zoneNumber, true)
		}
		def cmdList = zoneDevice.supportedCommands
		if (cmdList.find { it.name == "open" } != null) {
			if (zoneDevice.currentState("contact")?.value == null || zoneDevice.currentState("contact").value != "open") {
				zoneDevice.open()
			}
		} else if (cmdList.find { it.name == "active" } != null) {
			if (zoneDevice.currentState("motion")?.value == null || zoneDevice.currentState("motion").value != "active") {
				zoneDevice.active()
			}
		} else if (cmdList.find { it.name == "on" } != null) {
			if (zoneDevice.currentState("switch")?.value == null || zoneDevice.currentState("switch").value != "on") {
				zoneDevice.on()
			}
		} else if (cmdList.find { it.name == "detected" } != null) {
			if ((zoneDevice.currentState("smoke")?.value == null || zoneDevice.currentState("smoke").value != "detected") &&
					(zoneDevice.currentState("carbonMonoxide")?.value == null || zoneDevice.currentState("carbonMonoxide").value != "detected")) {
				zoneDevice.detected()
			}
		} else if (cmdList.find { it.name == "wet" } != null) {
			if (zoneDevice.currentState("water")?.value == null || zoneDevice.currentState("water").value != "wet") {
				zoneDevice.wet()
			}
		} else if (cmdList.find { it.name == "arrived" } != null) {
			zoneDevice.arrived()
		} else {
			def capList = zoneDevice.capabilities
			if (capList.find { it.name == "AccelerationSensor" } != null) {
				if (zoneDevice.currentState("acceleration")?.value == null || zoneDevice.currentState("acceleration").value != "active") {
					zoneDevice.sendEvent(name: "acceleration", value: "active")
					if (txtEnable)
						log.info "${zoneDevice.label}: Acceleration Sensor Active"
				}
			} else if (capList.find { it.name == "Beacon" } != null) {
				if (zoneDevice.currentState("presence")?.value == null || zoneDevice.currentState("presence").value != "present") {
					zoneDevice.sendEvent(name: "presence", value: "present")
					if (txtEnable)
						log.info "${zoneDevice.label}: Beacon Present"
				}
			} else if (capList.find { it.name == "CarbonMonoxideDetector" } != null) {
				if (zoneDevice.currentState("carbonMonoxide")?.value == null || zoneDevice.currentState("carbonMonoxide").value != "detected") {
					zoneDevice.sendEvent(name: "carbonMonoxide", value: "detected")
					if (txtEnable)
						log.info "${zoneDevice.label}: Carbon Monoxide Detected"
				}
			} else if (capList.find { it.name == "ContactSensor" } != null) {
				if (zoneDevice.currentState("contact")?.value == null || zoneDevice.currentState("contact").value != "open") {
					zoneDevice.sendEvent(name: "contact", value: "open")
					if (txtEnable)
						log.info "${zoneDevice.label}: Contact Open"
				}
			} else if (capList.find { it.name == "DoorControl" } != null) {
				if (zoneDevice.currentState("door")?.value == null || zoneDevice.currentState("door").value != "open") {
					zoneDevice.sendEvent(name: "door", value: "open")
					if (txtEnable)
						log.info "${zoneDevice.label}: DoorControl Open"
				}
			} else if (capList.find { it.name == "GarageDoorControl" } != null) {
				if (zoneDevice.currentState("door")?.value == null || zoneDevice.currentState("door").value != "open") {
					zoneDevice.sendEvent(name: "door", value: "open")
					if (txtEnable)
						log.info "${zoneDevice.label}: GarageDoorControl Open"
				}
			} else if (capList.find { it.name == "MotionSensor" } != null) {
				if (zoneDevice.currentState("motion")?.value == null || zoneDevice.currentState("motion").value != "active") {
					zoneDevice.sendEvent(name: "motion", value: "active")
					if (txtEnable)
						log.info "${zoneDevice.label}: Motion Active"
				}
			} else if (capList.find { it.name == "PresenceSensor" } != null) {
				if (zoneDevice.currentState("presence")?.value == null || zoneDevice.currentState("presence").value != "present") {
					zoneDevice.sendEvent(name: "presence", value: "present")
					if (txtEnable)
						log.info "${zoneDevice.label}: Presence Present"
				}
			} else if (capList.find { it.name == "RelaySwitch" } != null) {
				if (zoneDevice.currentState("switch")?.value == null || zoneDevice.currentState("switch").value != "on") {
					zoneDevice.sendEvent(name: "switch", value: "on")
					if (txtEnable)
						log.info "${zoneDevice.label}: Relay Switch On"
				}
			} else if (capList.find { it.name == "ShockSensor" } != null) {
				if (zoneDevice.currentState("shock")?.value == null || zoneDevice.currentState("shock").value != "detected") {
					zoneDevice.sendEvent(name: "shock", value: "detected")
					if (txtEnable)
						log.info "${zoneDevice.label}: Shock Sensor Detected"
				}
			} else if (capList.find { it.name == "SleepSensor" } != null) {
				if (zoneDevice.currentState("sleeping")?.value == null || zoneDevice.currentState("sleeping").value != "sleeping") {
					zoneDevice.sendEvent(name: "sleeping", value: "sleeping")
					if (txtEnable)
						log.info "${zoneDevice.label}: Sleep Sensor Sleeping"
				}
			} else if (capList.find { it.name == "SmokeDetector" } != null) {
				if (zoneDevice.currentState("smoke")?.value == null || zoneDevice.currentState("smoke").value != "detected") {
					zoneDevice.sendEvent(name: "smoke", value: "detected")
					if (txtEnable)
						log.info "${zoneDevice.label}: Smoke Detected"
				}
			} else if (capList.find { it.name == "SoundSensor" } != null) {
				if (zoneDevice.currentState("sound")?.value == null || zoneDevice.currentState("sound").value != "detected") {
					zoneDevice.sendEvent(name: "sound", value: "detected")
					if (txtEnable)
						log.info "${zoneDevice.label}: Sound Sensor Detected"
				}
			} else if (capList.find { it.name == "Switch" } != null) {
				if (zoneDevice.currentState("switch")?.value == null || zoneDevice.currentState("switch").value != "on") {
					zoneDevice.sendEvent(name: "switch", value: "on")
					if (txtEnable)
						log.info "${zoneDevice.label}: Switch On"
				}
			} else if (capList.find { it.name == "TamperAlert" } != null) {
				if (zoneDevice.currentState("tamper")?.value == null || zoneDevice.currentState("tamper").value != "detected") {
					zoneDevice.sendEvent(name: "tamper", value: "detected")
					if (txtEnable)
						log.info "${zoneDevice.label}: Tamper Alert Detected"
				}
			} else if (capList.find { it.name == "TouchSensor" } != null) {
				zoneDevice.sendEvent(name: "touch", value: "touched", isStateChange: true)
				if (txtEnable)
					log.info "${zoneDevice.label}: Touch Touched"
			} else if (capList.find { it.name == "Valve" } != null) {
				if (zoneDevice.currentState("valve")?.value == null || zoneDevice.currentState("valve").value != "open") {
					zoneDevice.sendEvent(name: "valve", value: "open")
					if (txtEnable)
						log.info "${zoneDevice.label}: Valve Open"
				}
			} else if (capList.find { it.name == "WaterSensor" } != null) {
				if (zoneDevice.currentState("water")?.value == null || zoneDevice.currentState("water").value != "wet") {
					zoneDevice.sendEvent(name: "water", value: "wet")
					if (txtEnable)
						log.info "${zoneDevice.label}: Sensor Wet"
				}
			}
		}
	}
}

def zoneNormal(String message) {
	String zoneNumber = message.substring(message.length() - 8).take(3)
	def zoneDevice = getChildDevice("${device.deviceNetworkId}_Z_${zoneNumber}")
	if (zoneDevice == null) { // For backwards capability
		zoneDevice = getChildDevice("${device.deviceNetworkId}_C_${zoneNumber}")
	}
	if (zoneDevice == null) { // For backwards capability
		zoneDevice = getChildDevice("${device.deviceNetworkId}_M_${zoneNumber}")
	}
	if (zoneDevice != null) {
		if (state.zoneReport != null) {
			state.zoneReport = sendReport(state.zoneReport, zoneDevice, zoneNumber, false)
		}
		def cmdList = zoneDevice.supportedCommands
		if (cmdList.find { it.name == "close" } != null) {
			if (zoneDevice.currentState("contact")?.value == null || zoneDevice.currentState("contact").value != "closed") {
				zoneDevice.close()
			}
		} else if (cmdList.find { it.name == "inactive" } != null) {
			if (zoneDevice.currentState("motion")?.value == null || zoneDevice.currentState("motion").value != "inactive") {
				zoneDevice.inactive()
			}
		} else if (cmdList.find { it.name == "off" } != null) {
			if (zoneDevice.currentState("switch")?.value == null || zoneDevice.currentState("switch").value != "off") {
				zoneDevice.off()
			}
		} else if (cmdList.find { it.name == "clear" } != null) {
			if ((zoneDevice.currentState("smoke")?.value == null || zoneDevice.currentState("smoke").value != "clear") &&
					(zoneDevice.currentState("carbonMonoxide")?.value == null || zoneDevice.currentState("carbonMonoxide").value != "clear")) {
				zoneDevice.clear()
			}
		} else if (cmdList.find { it.name == "dry" } != null) {
			if (zoneDevice.currentState("water")?.value == null || zoneDevice.currentState("water").value != "dry") {
				zoneDevice.dry()
			}
		} else if (cmdList.find { it.name == "departed" } != null) {
			zoneDevice.departed()
		} else {
			def capList = zoneDevice?.capabilities
			if (capList.find { it.name == "AccelerationSensor" } != null) {
				if (zoneDevice.currentState("acceleration")?.value == null || zoneDevice.currentState("acceleration").value != "inactive") {
					zoneDevice.sendEvent(name: "acceleration", value: "inactive")
					if (txtEnable)
						log.info "${zoneDevice.label}: Acceleration Sensor Inactive"
				}
			} else if (capList.find { it.name == "Beacon" } != null) {
				if (zoneDevice.currentState("presence")?.value == null || zoneDevice.currentState("presence").value != "not present") {
					zoneDevice.sendEvent(name: "presence", value: "not present")
					if (txtEnable)
						log.info "${zoneDevice.label}: Beacon Not Present"
				}
			} else if (capList.find { it.name == "CarbonMonoxideDetector" } != null) {
				if (zoneDevice.currentState("carbonMonoxide")?.value == null || zoneDevice.currentState("carbonMonoxide").value != "clear") {
					zoneDevice.sendEvent(name: "carbonMonoxide", value: "clear")
					if (txtEnable)
						log.info "${zoneDevice.label}: Carbon Monoxide Clear"
				}
			} else if (capList.find { it.name == "ContactSensor" } != null) {
				if (zoneDevice.currentState("contact")?.value == null || zoneDevice.currentState("contact").value != "closed") {
					zoneDevice.sendEvent(name: "contact", value: "closed")
					if (txtEnable)
						log.info "${zoneDevice.label}: Contact Closed"
				}
			} else if (capList.find { it.name == "DoorControl" } != null) {
				if (zoneDevice.currentState("door")?.value == null || zoneDevice.currentState("door").value != "closed") {
					zoneDevice.sendEvent(name: "door", value: "closed")
					if (txtEnable)
						log.info "${zoneDevice.label}: DoorControl Closed"
				}
			} else if (capList.find { it.name == "GarageDoorControl" } != null) {
				if (zoneDevice.currentState("door")?.value == null || zoneDevice.currentState("door").value != "closed") {
					zoneDevice.sendEvent(name: "door", value: "closed")
					if (txtEnable)
						log.info "${zoneDevice.label}: GarageDoorControl Closed"
				}
			} else if (capList.find { it.name == "MotionSensor" } != null) {
				if (zoneDevice.currentState("motion")?.value == null || zoneDevice.currentState("motion").value != "inactive") {
					zoneDevice.sendEvent(name: "motion", value: "inactive")
					if (txtEnable)
						log.info "${zoneDevice.label}: Motion Inactive"
				}
			} else if (capList.find { it.name == "PresenceSensor" } != null) {
				if (zoneDevice.currentState("presence")?.value == null || zoneDevice.currentState("presence").value != "not present") {
					zoneDevice.sendEvent(name: "presence", value: "not present")
					if (txtEnable)
						log.info "${zoneDevice.label}: Presence Not Present"
				}
			} else if (capList.find { it.name == "RelaySwitch" } != null) {
				if (zoneDevice.currentState("switch")?.value == null || zoneDevice.currentState("switch").value != "off") {
					zoneDevice.sendEvent(name: "switch", value: "off")
					if (txtEnable)
						log.info "${zoneDevice.label}: Relay Switch Off"
				}
			} else if (capList.find { it.name == "ShockSensor" } != null) {
				if (zoneDevice.currentState("shock")?.value == null || zoneDevice.currentState("shock").value != "clear") {
					zoneDevice.sendEvent(name: "shock", value: "clear")
					if (txtEnable)
						log.info "${zoneDevice.label}: Shock Sensor Clear"
				}
			} else if (capList.find { it.name == "SleepSensor" } != null) {
				if (zoneDevice.currentState("sleeping")?.value == null || zoneDevice.currentState("sleeping").value != "not sleeping") {
					zoneDevice.sendEvent(name: "sleeping", value: "not sleeping")
					if (txtEnable)
						log.info "${zoneDevice.label}: Sleep Sensor Not Sleeping"
				}
			} else if (capList.find { it.name == "SmokeDetector" } != null) {
				if (zoneDevice.currentState("smoke")?.value == null || zoneDevice.currentState("smoke").value != "clear") {
					zoneDevice.sendEvent(name: "smoke", value: "clear")
					if (txtEnable)
						log.info "${zoneDevice.label}: Smoke Clear"
				}
			} else if (capList.find { it.name == "SoundSensor" } != null) {
				if (zoneDevice.currentState("sound")?.value == null || zoneDevice.currentState("sound").value != "not detected") {
					zoneDevice.sendEvent(name: "sound", value: "not detected")
					if (txtEnable)
						log.info "${zoneDevice.label}: Sound Sensor Not Detected"
				}
			} else if (capList.find { it.name == "Switch" } != null) {
				if (zoneDevice.currentState("switch")?.value == null || zoneDevice.currentState("switch").value != "off") {
					zoneDevice.sendEvent(name: "switch", value: "off")
					if (txtEnable)
						log.info "${zoneDevice.label}: Switch Off"
				}
			} else if (capList.find { it.name == "TamperAlert" } != null) {
				if (zoneDevice.currentState("tamper")?.value == null || zoneDevice.currentState("tamper").value != "clear") {
					zoneDevice.sendEvent(name: "tamper", value: "clear")
					if (txtEnable)
						log.info "${zoneDevice.label}: Tamper Clear"
				}
			} else if (capList.find { it.name == "TouchSensor" } != null) {
				zoneDevice.sendEvent(name: "touch", value: null, isStateChange: true)
			} else if (capList.find { it.name == "Valve" } != null) {
				if (zoneDevice.currentState("valve")?.value == null || zoneDevice.currentState("valve").value != "closed") {
					zoneDevice.sendEvent(name: "valve", value: "closed")
					if (txtEnable)
						log.info "${zoneDevice.label}: Valve Closed"
				}
			} else if (capList.find { it.name == "WaterSensor" } != null) {
				if (zoneDevice.currentState("water")?.value == null || zoneDevice.currentState("water").value != "dry") {
					zoneDevice.sendEvent(name: "water", value: "dry")
					if (txtEnable)
						log.info "${zoneDevice.label}: Sensor Dry"
				}
			}
		}
	}
}

def taskStatus(data) {
	String taskNumber = data.Message.substring(4, 7)
	def zoneDevice = getChildDevice(device.deviceNetworkId + "_K_" + taskNumber)
	if (zoneDevice != null) {
		if (dbgEnable)
			log.debug "${device.label} Task Change Update: ${taskNumber} - ${data.State}"
		zoneDevice.parse(data.State)
		if (data.State == "on" && state.taskReport != null) {
			state.taskReport = sendReport(state.taskReport, zoneDevice, taskNumber, true)
		}
	}
}

//NEW CODE
//Manage Zones
def createZone(zoneInfo) {
	String zoneNumber = zoneInfo.zoneNumber
	String zoneName = zoneInfo.zoneName
	String deviceNetworkId
	//if (dbgEnable)
	//	log.debug "${device.label}: zoneNumber: ${zoneNumber}, zoneName: ${zoneName}, zoneType: ${zoneInfo.zoneType}, zoneText: ${zoneInfo.zoneText}"
	if (zoneInfo.zoneType == "00") {
		if (zoneName == null) {
			zoneName = "Zone " + zoneNumber + " - " + zoneInfo.zoneText
		}
		if (getChildDevice("${device.deviceNetworkId}_C_${zoneNumber}") == null && getChildDevice("{$device.deviceNetworkId}_M_${zoneNumber}") == null &&
				getChildDevice("${device.deviceNetworkId}_Z_${zoneNumber}") == null) {
			deviceNetworkId = "${device.deviceNetworkId}_Z_${zoneNumber}"
			if (zoneName ==~ /(?i).*motion.*/) {
				if (txtEnable)
					log.info "${device.label}: Creating ${zoneName} with deviceNetworkId = ${deviceNetworkId} of type: Motion"
				addChildDevice("hubitat", "Virtual Motion Sensor", deviceNetworkId, [name: zoneName, isComponent: false, label: zoneName])
				def newDevice = getChildDevice(deviceNetworkId)
				newDevice.updateSetting("autoInactive", [type: "enum", value: 0])
			} else if (zoneName ==~ /(?i).*temperature.*/) {
				if (txtEnable)
					log.info "${device.label}: Creating ${zoneName} with deviceNetworkId = ${deviceNetworkId} of type: Temperature"
				addChildDevice("hubitat", "Virtual Temperature Sensor", deviceNetworkId, [name: zoneName, isComponent: false, label: zoneName])
				def newDevice = getChildDevice(deviceNetworkId)
			} else {
				if (txtEnable)
					log.info "${device.label}: Creating ${zoneName} with deviceNetworkId = ${deviceNetworkId} of type: Contact"
				addChildDevice("hubitat", "Virtual Contact Sensor", deviceNetworkId, [name: zoneName, isComponent: false, label: zoneName])
				def newDevice = getChildDevice(deviceNetworkId)
			}
		} else if (dbgEnable) {
			log.debug "${device.label}: deviceNetworkId = ${device.deviceNetworkId}_Z_${zoneNumber} already exists"
		}
	} else if (zoneInfo.zoneType == "03") {
		if (zoneName == null) {
			zoneName = "Keypad ${zoneNumber.substring(1, 3)} - ${zoneInfo.zoneText}"
		}
		deviceNetworkId = "${device.deviceNetworkId}_P_${zoneNumber}"
		if (getChildDevice(deviceNetworkId) == null) {
			if (txtEnable)
				log.info "${device.label}: Creating ${zoneName} with deviceNetworkId = ${deviceNetworkId} of type: Keypad"
			addChildDevice("hubitat", "Virtual Temperature Sensor", deviceNetworkId, [name: zoneName, isComponent: false, label: zoneName])
			def newDevice = getChildDevice(deviceNetworkId)
			newDevice.updateSetting("txtEnable", [value: "false", type: "bool"])
		} else if (dbgEnable) {
			log.debug "${device.label}: deviceNetworkId = ${deviceNetworkId} already exists"
		}
	} else if (zoneInfo.zoneType == "04") {
		if (zoneName == null) {
			zoneName = "Output ${zoneNumber} - ${zoneInfo.zoneText}"
		}
		deviceNetworkId = "${device.deviceNetworkId}_O_${zoneNumber}"
		if (getChildDevice(deviceNetworkId) == null) {
			if (txtEnable)
				log.info "${device.label}: Creating ${zoneName} with deviceNetworkId = ${deviceNetworkId} of type: Output"
			addChildDevice("belk", "Elk M1 Driver Outputs", deviceNetworkId, [name: zoneName, isComponent: false, label: zoneName])
		} else if (dbgEnable) {
			log.debug "${device.label}: deviceNetworkId = ${deviceNetworkId} already exists"
		}
	} else if (zoneInfo.zoneType == "05") {
		if (zoneName == null) {
			zoneName = "Task ${zoneNumber.substring(1, 3)} - ${zoneInfo.zoneText}"
		}
		deviceNetworkId = "${device.deviceNetworkId}_K_${zoneNumber}"
		if (getChildDevice(deviceNetworkId) == null) {
			if (txtEnable)
				log.info "${device.label}: Creating ${zoneName} with deviceNetworkId = ${deviceNetworkId} of type: Task"
			addChildDevice("belk", "Elk M1 Driver Tasks", deviceNetworkId, [name: zoneName, isComponent: false, label: zoneName])
		} else if (dbgEnable) {
			log.debug "${device.label}: deviceNetworkId = ${deviceNetworkId} already exists"
		}
	} else if (zoneInfo.zoneType == "07") {
		int deviceNumber = zoneNumber.toInteger()
		int ndx = (deviceNumber - 1) / 16
		int unitNumber = deviceNumber - ndx * 16
		zoneNumber = "ABCDEFGHIJKLMNOP".substring(ndx, ndx + 1) + String.format("%02d", unitNumber)
		if (zoneName == null) {
			zoneName = "Lighting ${zoneNumber} - ${zoneInfo.zoneText}"
		}
		deviceNetworkId = "${device.deviceNetworkId}_L_${zoneNumber}"
		if (getChildDevice(deviceNetworkId) == null) {
			if (zoneName ==~ /(?i).*dim.*/) {
				if (txtEnable)
					log.info "${device.label}: Creating ${zoneName} with deviceNetworkId = ${deviceNetworkId} of type: Lighting Dimmer"
				addChildDevice("captncode", "Elk M1 Driver Lighting Dimmer", deviceNetworkId, [name: zoneName, isComponent: false, label: zoneName])
			} else {
				if (txtEnable)
					log.info "${device.label}: Creating ${zoneName} with deviceNetworkId = ${deviceNetworkId} of type: Lighting Switch"
				addChildDevice("captncode", "Elk M1 Driver Lighting Switch", deviceNetworkId, [name: zoneName, isComponent: false, label: zoneName])
			}
		} else if (dbgEnable) {
			log.debug "${device.label}: deviceNetworkId = ${deviceNetworkId} already exists"
		}
	} else if (zoneInfo.zoneType == "11") {
		if (zoneName == null) {
			zoneName = "Thermostat ${zoneNumber} - ${zoneInfo.zoneText}"
		}
		deviceNetworkId = "${device.deviceNetworkId}_T_${zoneNumber}"
		if (getChildDevice(deviceNetworkId) == null) {
			if (txtEnable)
				log.info "${device.label}: Creating ${zoneName} with deviceNetworkId = ${deviceNetworkId} of type: Thermostat"
			addChildDevice("belk", "Elk M1 Driver Thermostat", deviceNetworkId, [name: zoneName, isComponent: false, label: zoneName])
		} else if (dbgEnable) {
			log.debug "${device.label}: deviceNetworkId = ${deviceNetworkId} already exists"
		}
	}
}

def removeZone(zoneInfo) {
	if (txtEnable)
		log.info "${device.label}: Removing ${zoneInfo.zoneName} with deviceNetworkId = ${zoneInfo.deviceNetworkId}"
	deleteChildDevice(zoneInfo.deviceNetworkId)
}

def setMode(String armMode, String setArm, String armStatus) {
	if (state.armStatus != armStatus) {
		state.armStatus = armStatus
		sendEvent(name: "ArmStatus", value: armStatus)
		if (txtEnable)
			log.info "${device.label} ArmStatus changed to ${armStatus}"
	}
	if (state.armState != armMode) {
		state.armState = armMode
		def allmodes = location.getModes()
		int idx = allmodes.findIndexOf { it.name == armMode }
		if (idx == -1 && armMode == "Vacation") {
			idx = allmodes.findIndexOf { it.name == "Away" }
		} else if (idx == -1 && armMode == "Stay") {
			idx = allmodes.findIndexOf { it.name == "Home" }
		}
		if (idx != -1) {
			String curmode = location.currentMode
			String newmode = allmodes[idx].getName()
			location.setMode(newmode)
			if (dbgEnable)
				log.debug "${device.label}: Location Mode changed from $curmode to $newmode"
		}
		if (setArm == "disarm") {
			parent.unlockIt()
			parent.speakDisarmed()
			sendEvent(name: "switch", value: "off")
		} else {
			parent.lockIt()
			parent.speakArmed()
			sendEvent(name: "switch", value: "on")
		}
		sendLocationEvent(name: "hsmSetArm", value: setArm)
		if (txtEnable)
			log.info "${device.label} changed to mode ${armMode}"
	}
}

//def armReady(){
//	if (state.armUpStates != "Ready To Arm"){
//		if (dbgEnable)
//			log.debug "${device.label}: ready to arm"
//		state.armUpStates = "Ready To Arm"
//		parent.lockIt()
//		parent.speakArmed()
//		if (location.hsmStatus == "disarmed") {
//			sendLocationEvent(name: "hsmSetArm", value: "armHome")
//		}
//	}
//}

def registerZoneReport(String deviceNetworkId, String zoneNumber) {
	if (dbgEnable)
		log.debug "${device.label}: registering zone ${zoneNumber} reporting for ${deviceNetworkId}"
	state.zoneReport = registerReport(state.zoneReport, deviceNetworkId, zoneNumber)
}

def unRegisterZoneReport(String deviceNetworkId, String zoneNumber = null) {
	if (dbgEnable)
		log.debug "${device.label}: unregistering zone ${zoneNumber} reporting for ${deviceNetworkId}"
	state.zoneReport = unRegisterReport(state.zoneReport, deviceNetworkId, zoneNumber)
}

def registerOutputReport(String deviceNetworkId, String outputNumber) {
	if (dbgEnable)
		log.debug "${device.label}: registering output ${outputNumber} reporting for ${deviceNetworkId}"
	state.outputReport = registerReport(state.outputReport, deviceNetworkId, outputNumber)
}

def unRegisterOutputReport(String deviceNetworkId, String outputNumber = null) {
	if (dbgEnable)
		log.debug "${device.label}: unregistering output ${outputNumber} reporting for ${deviceNetworkId}"
	state.outputReport = unRegisterReport(state.outputReport, deviceNetworkId, outputNumber)
}

def registerTaskReport(String deviceNetworkId, String taskNumber) {
	if (dbgEnable)
		log.debug "${device.label}: registering task ${taskNumber} reporting for ${deviceNetworkId}"
	state.taskReport = registerReport(state.taskReport, deviceNetworkId, taskNumber)
}

def unRegisterTaskReport(String deviceNetworkId, String taskNumber = null) {
	if (dbgEnable)
		log.debug "${device.label}: unregistering task ${taskNumber} reporting for ${deviceNetworkId}"
	state.taskReport = unRegisterReport(state.taskReport, deviceNetworkId, taskNumber)
}

HashMap registerReport(HashMap reportList, String deviceNetworkId, String deviceNumber) {
	if (reportList == null) {
		reportList = [:]
	}
	reportList[deviceNumber] = deviceNetworkId
	return reportList
}

HashMap unRegisterReport(HashMap reportList, String deviceNetworkId, String deviceNumber) {
	if (reportList == null) {
		reportList = [:]
	} else if (deviceNumber == null) {
		HashMap newreport = [:]
		reportList.each {
			if (it.value != deviceNetworkId) {
				newreport[it.key] = value
			}
		}
		reportList = newreport
	} else {
		reportList -= [deviceNumber: deviceNetworkId]
	}
	if (reportList.size() == 0)
		reportList = null
	return reportList
}

HashMap sendReport(HashMap reportList, zoneDevice, String deviceNumber, boolean violated) {
	String reportDNID = reportList[deviceNumber]
	if (reportDNID != null) {
		def otherChild = getChildDevice(reportDNID)
		if (otherChild != null && otherChild.hasCommand("report")) {
			otherChild.report(zoneDevice.deviceNetworkId, violated)
		} else {
			reportList = unRegisterReport(reportList, reportDNID, deviceNumber)
		}
	}
	return reportList
}

//Telnet
int getReTry(Boolean inc) {
	int reTry = (state.reTryCount ?: 0).toInteger()
	if (inc) reTry++
	state.reTryCount = reTry
	return reTry
}

def telnetStatus(String status) {
	log.warn "${device.label}: telnetStatus- error: ${status}"
	if (status != "receive error: Stream is closed") {
		getReTry(true)
		log.error "Telnet connection dropped..."
		initialize()
	} else {
		log.warn "${device.label}: Telnet is restarting..."
	}
}

////REFERENCES AND MAPPINGS////
// Event Mapping Readable Text
@Field static final String NoEvent = "No Event"
@Field static final String FIREALARM = "Fire Alarm"
@Field static final String FIRESUPERVISORYALARM = "Fire Supervisory Alarm"
@Field static final String BURGLARALARMANYAREA = "Burglar Alarm, Any Area"
@Field static final String MEDICALALARMANYAREA = "Medical Alarm, Any Area"
@Field static final String POLICEALARMANYAREA = "Police Alarm, Any Area"
@Field static final String AUX124HRANYAREA = "Aux1 24 Hr, Any Area"
@Field static final String AUX224HRANYAREA = "Aux2 24 Hr, Any Area"
@Field static final String CARBONMONOXIDEALARMANYAREA = "Carbon Monoxide Alarm, Any Area"
@Field static final String EMERGENCYALARMANYAREA = "Emergency Alarm, Any Area"
@Field static final String FREEZEALARMANYAREA = "Freeze Alarm, Any Area"
@Field static final String GASALARMANYAREA = "Gas Alarm, Any Area"
@Field static final String HEATALARMANYAREA = "Heat Alarm, Any Area"
@Field static final String WATERALARMANYAREA = "Water Alarm, Any Area"
@Field static final String ALARMANYAREA = "Alarm, Any Area"
@Field static final String CODELOCKOUTANYKEYPAD = "Code Lockout, Any Keypad"
@Field static final String FIRETROUBLEANYZONE = "Fire Trouble, Any Zone"
@Field static final String BURGLARTROUBLEANYZONE = "Burglar Trouble, Any Zone"
@Field static final String FAILTOCOMMUNICATETROUBLE = "Fail To Communicate Trouble"
@Field static final String RFSENSORLOWBATTERYTROUBLE = "Rf Sensor Low Battery Trouble"
@Field static final String LOSTANCMODULETROUBLE = "Lost Anc Module Trouble"
@Field static final String LOSTKEYPADTROUBLE = "Lost Keypad Trouble"
@Field static final String LOSTINPUTEXPANDERTROUBLE = "Lost Input Expander Trouble"
@Field static final String LOSTOUTPUTEXPANDERTROUBLE = "Lost Output Expander Trouble"
@Field static final String EEPROMMEMORYERRORTROUBLE = "Eeprom Memory Error Trouble"
@Field static final String FLASHMEMORYERRORTROUBLE = "Flash Memory Error Trouble"
@Field static final String ACFAILURETROUBLE = "Ac Failure Trouble"
@Field static final String CONTROLLOWBATTERYTROUBLE = "Control Low Battery Trouble"
@Field static final String CONTROLOVERCURRENTTROUBLE = "Control Over Current Trouble"
@Field static final String EXPANSIONMODULETROUBLE = "Expansion Module Trouble"
@Field static final String OUTPUT2SUPERVISORYTROUBLE = "Output 2 Supervisory Trouble"
@Field static final String TELEPHONELINEFAULTTROUBLE1 = "Telephone Line Fault Trouble1"
@Field static final String RESTOREFIREZONE = "Estore Fire Zone"
@Field static final String RESTOREFIRESUPERVISORYZONE = "Restore Fire Supervisory Zone"
@Field static final String RESTOREBURGLARZONE = "Restore Burglar Zone"
@Field static final String RESTOREMEDICALZONE = "Restore Medical Zone"
@Field static final String RESTOREPOLICEZONE = "Restore Police Zone"
@Field static final String RESTOREAUX124HRZONE = "Restore Aux1 24 Hr Zone"
@Field static final String RESTOREAUX224HRZONE = "Restore Aux2 24 Hr Zone"
@Field static final String RESTORECOZONE = "Restore Co Zone"
@Field static final String RESTOREEMERGENCYZONE = "Restore Emergency Zone"
@Field static final String RESTOREFREEZEZONE = "Restore Freeze Zone"
@Field static final String RESTOREGASZONE = "Restore Gas Zone"
@Field static final String RESTOREHEATZONE = "Restore Heat Zone"
@Field static final String RESTOREWATERZONE = "Restore Water Zone"
@Field static final String COMMUNICATIONFAILRESTORE = "Communication Fail Restore"
@Field static final String ACFAILRESTORE = "Ac Fail Restore"
@Field static final String LOWBATTERYRESTORE = "Low Battery Restore"
@Field static final String CONTROLOVERCURRENTRESTORE = "Control Over Current Restore"
@Field static final String EXPANSIONMODULERESTORE = "Expansion Module Restore"
@Field static final String OUTPUT2RESTORE = "Output2 Restore"
@Field static final String TELEPHONELINERESTORE = "Telephone Line Restore"
@Field static final String ALARMMEMORYANYAREA = "Alarm Memory, Any Area"
@Field static final String AREAARMED = "Area Armed"
@Field static final String AREADISARMED = "Area Disarmed"
@Field static final String AREA1ARMSTATE = "Area 1 Armed State"
@Field static final String AREA1ISARMEDAWAY = "Area 1 Is Armed Away"
@Field static final String AREA1ISARMEDSTAY = "Area 1 Is Armed Stay"
@Field static final String AREA1ISARMEDSTAYINSTANT = "Area 1 Is Armed Stay Instant"
@Field static final String AREA1ISARMEDNIGHT = "Area 1 Is Armed Night"
@Field static final String AREA1ISARMEDNIGHTINSTANT = "Area 1 Is Armed Night Instant"
@Field static final String AREA1ISARMEDVACATION = "Area 1 Is Armed Vacation"
@Field static final String AREA1ISFORCEARMED = "Area 1 Is Force Armed"
@Field static final String ZONEBYPASSED = "Zone Bypassed"
@Field static final String ZONEUNBYPASSED = "Zone Unbypassed"
@Field static final String ANYBURGLARZONEISFAULTED = "Any Burglar Zone Is Faulted"
@Field static final String BURGLARSTATUSOFALLAREAS = "Burglar Status Of All Areas"
@Field static final String AREA1CHIMEMODE = "Area 1 Chime Mode"
@Field static final String AREA1CHIMEALERT = "Area 1 Chime Alert"
@Field static final String ENTRYDELAYANYAREA = "Entry Delay, Any Area"
@Field static final String EXITDELAYANYAREA = "Exit Delay, Any Area"
@Field static final String AREA1EXITDELAYENDS = "Area 1 Exit Delay Ends"

// Event Mapping
@Field final Map elkResponses = [
		'1000': NoEvent,
		'1001': FIREALARM,
		'1002': FIRESUPERVISORYALARM,
		'1003': BURGLARALARMANYAREA,
		'1004': MEDICALALARMANYAREA,
		'1005': POLICEALARMANYAREA,
		'1006': AUX124HRANYAREA,
		'1007': AUX224HRANYAREA,
		'1008': CARBONMONOXIDEALARMANYAREA,
		'1009': EMERGENCYALARMANYAREA,
		'1010': FREEZEALARMANYAREA,
		'1011': GASALARMANYAREA,
		'1012': HEATALARMANYAREA,
		'1013': WATERALARMANYAREA,
		'1014': ALARMANYAREA,
		'1111': CODELOCKOUTANYKEYPAD,
		'1128': FIRETROUBLEANYZONE,
		'1129': BURGLARTROUBLEANYZONE,
		'1130': FAILTOCOMMUNICATETROUBLE,
		'1131': RFSENSORLOWBATTERYTROUBLE,
		'1132': LOSTANCMODULETROUBLE,
		'1133': LOSTKEYPADTROUBLE,
		'1134': LOSTINPUTEXPANDERTROUBLE,
		'1135': LOSTOUTPUTEXPANDERTROUBLE,
		'1136': EEPROMMEMORYERRORTROUBLE,
		'1137': FLASHMEMORYERRORTROUBLE,
		'1138': ACFAILURETROUBLE,
		'1139': CONTROLLOWBATTERYTROUBLE,
		'1140': CONTROLOVERCURRENTTROUBLE,
		'1141': EXPANSIONMODULETROUBLE,
		'1142': OUTPUT2SUPERVISORYTROUBLE,
		'1143': TELEPHONELINEFAULTTROUBLE1,
		'1144': RESTOREFIREZONE,
		'1145': RESTOREFIRESUPERVISORYZONE,
		'1146': RESTOREBURGLARZONE,
		'1147': RESTOREMEDICALZONE,
		'1148': RESTOREPOLICEZONE,
		'1149': RESTOREAUX124HRZONE,
		'1150': RESTOREAUX224HRZONE,
		'1151': RESTORECOZONE,
		'1152': RESTOREEMERGENCYZONE,
		'1153': RESTOREFREEZEZONE,
		'1154': RESTOREGASZONE,
		'1155': RESTOREHEATZONE,
		'1156': RESTOREWATERZONE,
		'1157': COMMUNICATIONFAILRESTORE,
		'1158': ACFAILRESTORE,
		'1159': LOWBATTERYRESTORE,
		'1160': CONTROLOVERCURRENTRESTORE,
		'1161': EXPANSIONMODULERESTORE,
		'1162': OUTPUT2RESTORE,
		'1163': TELEPHONELINERESTORE,
		'1164': ALARMMEMORYANYAREA,
		'1173': AREAARMED,
		'1174': AREADISARMED,
		'1175': AREA1ARMSTATE,
		'1183': AREA1ISARMEDAWAY,
		'1191': AREA1ISARMEDSTAY,
		'1199': AREA1ISARMEDSTAYINSTANT,
		'1207': AREA1ISARMEDNIGHT,
		'1215': AREA1ISARMEDNIGHTINSTANT,
		'1223': AREA1ISARMEDVACATION,
		'1231': AREA1ISFORCEARMED,
		'1239': ZONEBYPASSED,
		'1240': ZONEUNBYPASSED,
		'1241': ANYBURGLARZONEISFAULTED,
		'1242': BURGLARSTATUSOFALLAREAS,
		'1251': AREA1CHIMEMODE,
		'1259': AREA1CHIMEALERT,
		'1267': ENTRYDELAYANYAREA,
		'1276': EXITDELAYANYAREA,
		'1285': AREA1EXITDELAYENDS
]


@Field static final String Disarmed = "Disarmed"
@Field static final String ArmedAway = "Armed Away"
@Field static final String ArmedStay = "Armed Stay"
@Field static final String ArmedStayInstant = "Armed Stay Instant"
@Field static final String ArmedtoNight = "Armed To Night"
@Field static final String ArmedtoNightInstant = "Armed To Night Instance"
@Field static final String ArmedtoVacation = "Armed To Vacation"

@Field final Map elkArmStatuses = [
		'0': Disarmed,
		'1': ArmedAway,
		'2': ArmedStay,
		'3': ArmedStayInstant,
		'4': ArmedtoNight,
		'5': ArmedtoNightInstant,
		'6': ArmedtoVacation
]
@Field static final String NotReadytoArm = "Not Ready to Arm"
@Field static final String ReadytoArm = "Ready to Arm"
@Field static final String ReadytoArmBut = "Ready to Arm, but a zone is violated and can be force armed"
@Field static final String ArmedwithExit = "Armed with Exit Timer working"
@Field static final String ArmedFully = "Armed Fully"
@Field static final String ForceArmed = "Force Armed with a force arm zone violated"
@Field static final String ArmedwithaBypass = "Armed with a Bypass"

@Field final Map elkArmUpStates = [
		'0': NotReadytoArm,
		'1': ReadytoArm,
		'2': ReadytoArmBut,
		'3': ArmedwithExit,
		'4': ArmedFully,
		'5': ForceArmed,
		'6': ArmedwithaBypass
]

@Field static final String NoActiveAlarm = "No Active Alarm"
@Field static final String EntranceDelayisActive = "Entrance Delay is Active"
@Field static final String AlarmAbortDelayActive = "Alarm Abort Delay Active"
@Field static final String FireAlarm = "Fire Alarm"
@Field static final String MedicalAlarm = "Medical Alarm"
@Field static final String PoliceAlarm = "Police Alarm"
@Field static final String BurgularAlarm = "Burgular Alarm"
@Field static final String AuxAlarm1 = "Aux Alarm 1"
@Field static final String AuxAlarm2 = "Aux Alarm 2"
@Field static final String AuxAlarm3 = "Aux Alarm 3"
@Field static final String AuxAlarm4 = "Aux Alarm 4"
@Field static final String CarbonMonoxide = "Carbon Monoxide"
@Field static final String EmergencyAlarm = "Emergency Alarm"
@Field static final String FreezeAlarm = "Freeze Alarm"
@Field static final String GasAlarm = "Gas Alarm"
@Field static final String HeatAlarm = "Heat Alarm"
@Field static final String WaterAlarm = "Water Alarm"
@Field static final String FireSupervisory = "Fire Supervisory"
@Field static final String FireVerified = "Fire Verified"

@Field final Map elkAlarmStates = [
		'0': NoActiveAlarm,
		'1': EntranceDelayisActive,
		'2': AlarmAbortDelayActive,
		'3': FireAlarm,
		'4': MedicalAlarm,
		'5': PoliceAlarm,
		'6': BurgularAlarm,
		'7': AuxAlarm1,
		'8': AuxAlarm2,
		'9': AuxAlarm3,
		':': AuxAlarm4,
		';': CarbonMonoxide,
		'<': EmergencyAlarm,
		'=': FreezeAlarm,
		'>': GasAlarm,
		'?': HeatAlarm,
		'@': WaterAlarm,
		'A': FireSupervisory,
		'B': FireVerified
]

// Zone Status Mapping Readable Text
@Field static final String NormalUnconfigured = "Normal: Unconfigured"
@Field static final String NormalOpen = "Normal: Open"
@Field static final String NormalEOL = "Normal: EOL"
@Field static final String NormalShort = "Normal: Short"
@Field static final String TroubleOpen = "Trouble: Open"
@Field static final String TroubleEOL = "Trouble: EOL"
@Field static final String TroubleShort = "Trouble: Short"
@Field static final String notused = "not used"
@Field static final String ViolatedOpen = "Violated: Open"
@Field static final String ViolatedEOL = "Violated: EOL"
@Field static final String ViolatedShort = "Violated: Short"
@Field static final String SoftBypassed = "Soft Bypassed"
@Field static final String BypassedOpen = "Bypassed: Open"
@Field static final String BypassedEOL = "Bypassed: EOL"
@Field static final String BypassedShort = "Bypassed: Short"

// Zone Status Mapping
@Field final Map elkZoneStatuses = [
		'0': NormalUnconfigured,
		'1': NormalOpen,
		'2': NormalEOL,
		'3': NormalShort,
		'5': TroubleOpen,
		'6': TroubleEOL,
		'7': TroubleShort,
		'8': notused,
		'9': ViolatedOpen,
		'A': ViolatedEOL,
		'B': ViolatedShort,
		'C': SoftBypassed,
		'D': BypassedOpen,
		'E': BypassedEOL,
		'F': BypassedShort

]

@Field static final String Off = "off"
@Field static final String On = "on"

@Field final Map elkOutputStates = [
		"0": Off,
		"1": On
]

@Field static final String Fahrenheit = "Fahrenheit"
@Field static final String Celcius = "Celcius"

@Field final Map elkTemperatureModes = [
		F: Fahrenheit,
		C: Celcius
]

@Field static final String User = "User"

@Field final Map elkUserCodeTypes = [
		1: User,
]

@Field static final String ZoneName = "Zone Name"
@Field static final String AreaName = "Area Name"
@Field static final String UserName = "User Name"
@Field static final String Keypad = "Keypad"
@Field static final String OutputName = "Output Name"
@Field static final String TaskName = "Task Name"
@Field static final String TelephoneName = "Telephone Name"
@Field static final String LightName = "Light Name"
@Field static final String AlarmDurationName = "Alarm Duration Name"
@Field static final String CustomSettings = "Custom Settings"
@Field static final String CountersNames = "Counters Names"
@Field static final String ThermostatNames = "Thermostat Names"
@Field static final String FunctionKey1Name = "FunctionKey1 Name"
@Field static final String FunctionKey2Name = "FunctionKey2 Name"
@Field static final String FunctionKey3Name = "FunctionKey3 Name"
@Field static final String FunctionKey4Name = "FunctionKey4 Name"
@Field static final String FunctionKey5Name = "FunctionKey5 Name"
@Field static final String FunctionKey6Name = "FunctionKey6 Name"


@Field final Map elkTextDescriptionsTypes = [
		'0' : ZoneName,
		'1' : AreaName,
		'2' : UserName,
		'3' : Keypad,
		'4' : OutputName,
		'5' : TaskName,
		'6' : TelephoneName,
		'7' : LightName,
		'8' : AlarmDurationName,
		'9' : CustomSettings,
		'10': CountersNames,
		'11': ThermostatNames,
		'12': FunctionKey1Name,
		'13': FunctionKey2Name,
		'14': FunctionKey3Name,
		'15': FunctionKey4Name,
		'16': FunctionKey5Name,
		'17': FunctionKey6Name
]


@Field static final String TemperatureProbe = "Temperature Probe"
@Field static final String Keypads = "Keypads"
@Field static final String Thermostats = "Thermostats"

@Field final Map elkTempTypes = [
		0: TemperatureProbe,
		1: Keypads,
		2: Thermostats
]

//NEW CODE
@Field static final String off = "off"
@Field static final String heat = "heat"
@Field static final String cool = "cool"
@Field static final String auto = "auto"
@Field static final String on = "on"
@Field static final String circulate = "circulate"
@Field static final String False = "false"
@Field static final String True = "true"

@Field final Map elkThermostatModeSet = [off: '0', heat: '1', cool: '2', auto: '3', 'emergency heat': '4']
@Field final Map elkThermostatFanModeSet = [auto: '0', on: '1', circulate: '0']
@Field final Map elkThermostatHoldModeSet = [False: '0', True: '1']

@Field final Map elkCommands = [

		Disarm                    : "a0",
		ArmAway                   : "a1",
		ArmHome                   : "a2",
		ArmStayInstant            : "a3",
		ArmNight                  : "a4",
		ArmNightInstant           : "a5",
		ArmVacation               : "a6",
		ArmStepAway               : "a7",
		ArmStepStay               : "a8",
		RequestArmStatus          : "as",
		AlarmByZoneRequest        : "az",
		RequestTemperatureData    : "lw",
		RequestRealTimeClockRead  : "rr",
		RealTimeClockWrite        : "rw",
		RequestTextDescriptions   : "sd",
		Speakphrase               : "sp",
		RequestSystemTroubleStatus: "ss",
		Requesttemperature        : "st",
		Speakword                 : "sw",
		TaskActivation            : "tn",
		RequestThermostatData     : "tr",
		SetThermostatData         : "ts",
		Requestusercodeareas      : "ua",
		RequestVersionNumber      : "vn",
		ReplyfromEthernettest     : "xk",
		Zonebypassrequest         : "zb",
		RequestZoneDefinitions    : "zd",
		Zonepartitionrequest      : "zp",
		RequestZoneStatus         : "zs",
		RequestZoneanalogvoltage  : "zv",
		ControlOutputOn           : "cn",
		ControlOutputOff          : "cf",
		ControlOutputToggle       : "ct",
		RequestOutputStatus       : "cs",
		ControlLightingMode       : "pc",
		ControlLightingOn         : "pn",
		ControlLightingOff        : "pf",
		ControlLightingToggle     : "pt",
		RequestLightingStatus     : "ps"
]

@Field static final String Disabled = "Disabled"
@Field static final String BurglarEntryExit1 = "Burglar Entry/Exit 1"
@Field static final String BurglarEntryExit2 = "Burglar Entry/Exit 2"
@Field static final String BurglarPerimeterInstant = "Burglar Perimeter Instant"
@Field static final String BurglarInterior = "Burglar Interior"
@Field static final String BurglarInteriorFollower = "Burglar Interior Follower"
@Field static final String BurglarInteriorNight = "Burglar Interior Night"
@Field static final String BurglarInteriorNightDelay = "Burglar Interior Night Delay"
@Field static final String Burglar24Hour = "Burglar 24 Hour"
@Field static final String BurglarBoxTamper = "Burglar Box Tamper"
@Field static final String Keyfob = "Key fob"
@Field static final String NonAlarm = "Non Alarm"
@Field static final String PoliceNoIndication = "Police No Indication"
@Field static final String KeyMomentaryArmDisarm = "Key Momentary Arm / Disarm"
@Field static final String KeyMomentaryArmAway = "Key Momentary Arm Away"
@Field static final String KeyMomentaryArmStay = "Key Momentary Arm Stay"
@Field static final String KeyMomentaryDisarm = "Key Momentary Disarm"
@Field static final String KeyOnOff = "Key On/Off"
@Field static final String MuteAudibles = "Mute Audibles"
@Field static final String PowerSupervisory = "Power Supervisory"
@Field static final String Temperature = "Temperature"
@Field static final String AnalogZone = "Analog Zone"
@Field static final String PhoneKey = "Phone Key"
@Field static final String IntercomKey = "Intercom Key"

@Field final Map elkZoneDefinitions = [
		'0': Disabled,
		'1': BurglarEntryExit1,
		'2': BurglarEntryExit2,
		'3': BurglarPerimeterInstant,
		'4': BurglarInterior,
		'5': BurglarInteriorFollower,
		'6': BurglarInteriorNight,
		'7': BurglarInteriorNightDelay,
		'8': Burglar24Hour,
		'9': BurglarBoxTamper,
		':': FireAlarm,
		';': FireVerified,
		'<': FireSupervisory,
		'=': AuxAlarm1,
		'>': AuxAlarm2,
		'?': Keyfob,
		'@': NonAlarm,
		'A': CarbonMonoxide,
		'B': EmergencyAlarm,
		'C': FreezeAlarm,
		'D': GasAlarm,
		'E': HeatAlarm,
		'F': MedicalAlarm,
		'G': PoliceAlarm,
		'H': PoliceNoIndication,
		'I': WaterAlarm,
		'J': KeyMomentaryArmDisarm,
		'K': KeyMomentaryArmAway,
		'L': KeyMomentaryArmStay,
		'M': KeyMomentaryDisarm,
		'N': KeyOnOff,
		'O': MuteAudibles,
		'P': PowerSupervisory,
		'Q': Temperature,
		'R': AnalogZone,
		'S': PhoneKey,
		'T': IntercomKey
]

//Not currently using this
//@Field static final String Disabled = "Disabled"
@Field static final String ContactBurglarEntryExit1 = "Contact"
@Field static final String ContactBurglarEntryExit2 = "Contact"
@Field static final String ContactBurglarPerimeterInstant = "Contact"
@Field static final String MotionBurglarInterior = "Motion"
@Field static final String MotionBurglarInteriorFollower = "Motion"
@Field static final String MotionBurglarInteriorNight = "Motion"
@Field static final String MotionBurglarInteriorNightDelay = "Motion"
@Field static final String AlertBurglar24Hour = "Alert"
@Field static final String AlertBurglarBoxTamper = "Alert"
@Field static final String AlertFireAlarm = "Alert"
@Field static final String AlertFireVerified = "Alert"
@Field static final String AlertFireSupervisory = "Alert"
@Field static final String AlertAuxAlarm1 = "Alert"
@Field static final String AlertAuxAlarm2 = "Alert"
@Field static final String AlertCarbonMonoxide = "Alert"
@Field static final String AlertEmergencyAlarm = "Alert"
@Field static final String AlertFreezeAlarm = "Alert"
@Field static final String AlertGasAlarm = "Alert"
@Field static final String AlertHeatAlarm = "Alert"
@Field static final String AlertMedicalAlarm = "Alert"
@Field static final String AlertPoliceAlarm = "Alert"
@Field static final String AlertPoliceNoIndication = "Alert"
@Field static final String AlertWaterAlarm = "Alert"

//Not currently using this
@Field final Map elkZoneTypes = [
//'0': Disabled,
'1': ContactBurglarEntryExit1,
'2': ContactBurglarEntryExit2,
'3': ContactBurglarPerimeterInstant,
'4': MotionBurglarInterior,
'5': MotionBurglarInteriorFollower,
'6': MotionBurglarInteriorNight,
'7': MotionBurglarInteriorNightDelay,
'8': AlertBurglar24Hour,
'9': AlertBurglarBoxTamper,
':': AlertFireAlarm,
';': AlertFireVerified,
'<': AlertFireSupervisory,
'=': AlertAuxAlarm1,
'>': AlertAuxAlarm2,
'A': AlertCarbonMonoxide,
'B': AlertEmergencyAlarm,
'C': AlertFreezeAlarm,
'D': AlertGasAlarm,
'E': AlertHeatAlarm,
'F': AlertMedicalAlarm,
'G': AlertPoliceAlarm,
'H': AlertPoliceNoIndication,
'I': AlertWaterAlarm
]

/***********************************************************************************************************************
 *
 * Release Notes (see Known Issues Below)
 *
 * 0.1.33
 * Added import of lighting devices for use with new drivers.  If lighting text has "dim" in it, it will be assigned the dimmer
 *   driver.  Otherwise, it will be assigned the switch driver.
 * Fixed issue with M1 Touch Pro App sync conflict that caused this driver to crash
 * Fixed issue with thermostats not working
 * Removed redundant output and task command buttons.  The individual devices now have them.
 * Renamed the Request xxx Status command buttons to Refresh xxx Status for accuracy
 * Stopped zone statuses from writing info log when the status didn't change
 * Added retrieval of Elk M1 Version after initialization
 *
 * 0.1.32
 * Fixed issue with temperature readings.
 *
 * 0.1.31
 * Added handling of Temperature Data automatically sent starting with M1 Ver. 4.2.8
 * Added info logging only of Zone Definitions
 * Simplified thermostat code
 * Simplified Output and Task logging and events
 * Removed unneeded thermostat capability from this driver since the child thermostat driver has it
 *
 * 0.1.30
 * Added polling of device status once connected or reconnected to the panel.
 * Added Enable debug logging and Enable descriptionText logging to the main device.  Debug is no longer set for the
 *   driver from within the application.  Info logging can now be turned on or off for the main device.
 * Added zone, output and task reporting so child devices can register to be updated via the report command when another
 *   device status has changed.
 *   I am using this for a door control driver assigned to an output that needs to be aware of the state of the contact
 *   attached to the door.
 *
 * 0.1.29
 * Strongly typed variables for performance
 *
 * 0.1.28
 * Added ability to handle a zone with a Temperature Probe attached to the main board. Automatically assigned when
 *   the zone's description has the word "Temperature" in it or zone device can be manually changed.
 * Added the device type Keypad for temperature readings.
 * Both devices are assigned the Virtual Temperature Sensor driver.  This is an experimental feature.  Since the panel
 *   does not volunteer temperature data, it must be requested either manually for all devices using the Request Temperature
 *   Data button on the main driver or by setting up a rule to periodically execute the refreshTemperatureStatus command.
 * Added "ContactSensor" as a capability for HSM monitoring.  The contact will open if a Burglar Alarm or Police Alarm is triggered.
 * Changed the method of importing devices to greatly improve performance and reduce panel communication.
 * Fixed an issue not deleting child devices when the main driver is uninstalled.
 * Fixed an issue with the name when importing a device with the "Show On Keypad" flag set.
 *
 * 0.1.27
 * You can now change the port on the main device page.  You must click "Initialize" after you save preferences to take effect
 * Changed info logging to only happen if Enable descriptionText logging is turned on the device
 * Reenabled Request Zone Definition and Request Zone Status
 * Added Request Output Status
 *
 * 0.1.26
 * Added info logging when output or task status changes or Arm Mode changes
 * Added switch capability to main Elk M1 driver
 * Improved ArmStatus and AlarmState events to fire only when changed
 * Adding missing AlarmStates
 * Small tweaks to improve performance
 *
 * 0.1.25
 * Added sync of Elk M1 modes to Location Modes: Disarmed, Armed Away, Night, Stay, Vacation synced to modes Home, Away, Night,
 *    Stay (if available and Home if not), Vacation (if available and Away if not), respectively.
 * Added sync of hsmSetArm modes of disarm, armAway, armNight, ArmHome
 * Retooled zone-device creation to always create a device of type Virtual Contact unless "motion" is in the description.
 * Fixed issue that would create a lot of bogus devices when you connect to the panel with the M1 Touch Pro app
 * Added auto zone-device capability detection to support virtual devices with the following capabilities:
 *     ContactSensor, MotionSensor, SmokeDetector, CarbonMonoxideDetector, WaterSensor, TamperAlert, AccelerationSensor,
 *     Beacon, PresenceSensor, RelaySwitch, ShockSensor, SleepSensor, SoundSensor, Switch, TouchSensor and Valve
 * Changed registration of arm mode event to happen upon entry/exit timer expiration
 * Fixed issue of extra arm mode events firing when the panel is armed
 * Fixed status of EOL terminated zones
 * Added ability to read task status from the panel and set Contact device to open temporarily.
 * Tested use of the Elk C1M1 dual path communicator over local ethernet instead of using the M1XEP
 * Added setting of LastUser event which contains the user number who last triggered armed or disarmed
 *
 * 0.1.24
 * Rerouted some code for efficiency
 * Turned off some of the extra debugging
 *
 * 0.1.23
 * Outputs are now functional with Rule Machine
 * Change switch case to else if statements
 *
 * 0.1.22
 * Fixed code to show operating state and thermostat setpoint on dashboard tile
 * Reorder some code to see if it helps with some delay issues
 * Consolidated code for zone open and zone closed to see if it helps with some delay issues (need to check if this has any other impact elsewhere)
 *
 * 0.1.21
 * Updated mapping for output reporting code
 * Changed Reply Arming Status Report Data to work as Area 1 only and to report current states
 *
 * 0.1.20
 * Added back some code for 'Reply Arming Status Report Data (AS)' to clean up logging
 *
 * 0.1.19
 * Removed some code for 'Reply Arming Status Report Data (AS)' to clean up logging
 *
 * 0.1.18
 * Add support for Occupancy Sensors - this will be a work in progress since not sure how to code it
 *
 * 0.1.17
 * Changed devices 'isComponent' to 'False' - this will allow the removal of devices and changing of drivers
 *
 * 0.1.16
 * Changed the one import to be not case sensitive
 *
 * 0.1.15
 * Added support for manual inclusion of Elk M1 outputs and tasks
 * Added support for importing Elk M1 outputs, tasks and thermostats
 * Added better support for child devices (all communication goes through Elk M1 device)
 * Cleaned up some descriptions and instructions
 *
 * 0.1.14
 * Added support for importing Elk M1 zones
 * Fixed erroneous error codes
 * Added actuator capability to allow custom commands to work in dashboard and rule machine
 * Added command to request temperature data
 *
 * 0.1.13
 * Elk M1 Code - No longer requires a 6 digit code (Add leading zeroes to 4 digit codes)
 * Outputs and tasks can now be entered as a number
 * Code clean up - removed some unused code
 *
 * 0.1.12
 * Added support for outputs
 *
 * 0.1.11
 * Built seperate thermostat child driver should allow for multiple thermostats
 *
 * 0.1.10
 * Ability to control thermostat 1
 *
 * 0.1.9
 * Minor changes
 *
 * 0.1.8
 * Ability to read thermostat data (haven't confirmed multiple thermostat support)
 * Added additional mappings for thermostat support
 * Additional code clean up
 *
 * 0.1.7
 * Rewrite of the various commands
 *
 * 0.1.6
 * Changed text description mapping to string characters
 *
 * 0.1.5
 * Added zone types
 * Added zone definitions
 *
 * 0.1.4
 * Added additional command requests
 *
 * 0.1.3
 * Receive status messages and interpret data
 *
 * 0.1.2
 * Minor changes to script nomenclature and formating
 *
 * 0.1.1
 * Abiltiy to connect Elk M1 and see data
 * Ability to send commands to Elk M1
 * Changed code to input parameter
 *
 ***********************************************************************************************************************/
/***********************************************************************************************************************
 *
 * Feature Request & Known Issues
 * I - Area is hard coded to 1
 * I - Fan Circulate and set schedule not supported
 * F - Request text descriptions for zone setup, tasks and outputs (currently this must be done via the app)
 * I - A device with the same device network ID exists (this is really not an issue)
 * I - Zone, output and task reporting is limited to one report per child device
 *
 ***********************************************************************************************************************/