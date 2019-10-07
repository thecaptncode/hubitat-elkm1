/***********************************************************************************************************************
 *
 *  A Hubitat Child Driver using Telnet to connect to the Elk M1 via the M1XEP.
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

public static String version() { return "v0.1.6" }

import groovy.transform.Field

metadata {
	definition(name: "Elk M1 Driver Thermostat", namespace: "belk", author: "Mike Magrann") {
		capability "Thermostat"
		command "refresh"
	}
	preferences {
		input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
	}
}

def installed() {
	"Installed..."
	refresh()
}

def uninstalled() {
}

def parse(String description) {
	String mode = elkThermostatMode[description.substring(6, 7)]
	String hold = elkThermostatHold[description.substring(7, 8)]
	String fan = elkThermostatFan[description.substring(8, 9)]
	String cTemp = description.substring(9, 11)
	String hSet = description.substring(11, 13)
	String cSet = description.substring(13, 15)
	String cHumid = description.substring(15, 17)
	if (txtEnable)
		log.info "${device.label} ${mode} Mode, Hold temperature = ${hold}, ${fan}, Current Temperature = " +
				"${cTemp}, Heat Setpoint = ${hSet}, Cool Setpoint = ${cSet}, Humidity = ${cHumid}"
	sendEvent(name: "coolingSetpoint", value: cSet)
	sendEvent(name: "heatingSetpoint", value: hSet)
	sendEvent(name: "temperature", value: cTemp)
	sendEvent(name: "thermostatFanMode", value: fan)
	sendEvent(name: "thermostatMode", value: mode)
	if (mode == Heat || mode == EmergencyHeat) {
		sendEvent(name: "thermostatSetpoint", value: hSet)
	} else if (mode == Cool) {
		sendEvent(name: "thermostatSetpoint", value: cSet)
	} else {
		sendEvent(name: "thermostatSetpoint", value: "")
	}
}

def parse(List description) {
	log.warn "parse(List description) received ${description}"
	return
}

def auto() {
	parent.setThermostatMode(Auto, getThermID())
}

def cool() {
	parent.setThermostatMode(Cool, getThermID())
}

def emergencyHeat() {
	parent.setThermostatMode(EmergencyHeat, getThermID())
}

def fanAuto() {
	parent.setThermostatFanMode(Auto, getThermID())
}

def fanCirculate() {
	parent.setThermostatFanMode(Circulate, getThermID())
}

def fanOn() {
	parent.setThermostatFanMode(On, getThermID())
}

def heat() {
	parent.setThermostatMode(Heat, getThermID())
}

def off() {
	parent.setThermostatMode(Off, getThermID())
}

def setCoolingSetpoint(BigDecimal degrees) {
	parent.setCoolingSetpoint(degrees, getThermID())
}

def setHeatingSetpoint(BigDecimal degrees) {
	parent.setHeatingSetpoint(degrees, getThermID())
}

def setSchedule(schedule) {
}

def setThermostatFanMode(String fanmode) {
	parent.setThermostatFanMode(fanmode, getThermID())
}

def setThermostatHoldMode(String holdmode) {
	parent.setThermostatHoldMode(holdmode, getThermID())
}

def setThermostatMode(String thermostatmode) {
	parent.setThermostatMode(thermostatmode, getThermID())
}

def refresh() {
	parent.RequestThermostatData(getThermID())
}

String getThermID() {
	String DNID = device.deviceNetworkId
	return DNID.substring(DNID.length() - 2).take(2)
}

@Field final Map elkThermostatMode = ['0': Off, '1': Heat, '2': Cool, '3': Auto, '4': EmergencyHeat]
@Field final Map elkThermostatHold = ['0': False, '1': True]
@Field final Map elkThermostatFan = ['0': FanAuto, '1': FanOn]

@Field static final String On = "on"
@Field static final String Off = "off"
@Field static final String Heat = "heat"
@Field static final String Cool = "cool"
@Field static final String Auto = "auto"
@Field static final String Circulate = "circulate"
@Field static final String EmergencyHeat = "emergency heat"
@Field static final String False = "false"
@Field static final String True = "true"
@Field static final String FanAuto = "fan auto"
@Field static final String FanOn = "fan on"

/***********************************************************************************************************************
 *
 * Release Notes (see Known Issues Below)
 *
 * 0.1.6
 * Added Refresh Command
 * Added info logging
 * Simplified calls to parent driver
 *
 * 0.1.5
 * Strongly typed variables for performance
 *
 * 0.1.4
 * Rewrote code to use parent telnet
 *
 * 0.1.3
 * No longer requires a 6 digit code - Add leading zeroes to 4 digit codes
 * Code clean up
 * 0.1.2
 * Code clean up
 * 0.1.1
 * New child driver to support thermostats
 *
 ***********************************************************************************************************************/
/***********************************************************************************************************************
 *
 * Feature Request & Known Issues
 *
 * I - Set Hold Mode not currently supported
 * I - Set Schedule not currently supported
 *
 ***********************************************************************************************************************/
