package com.sn.miot.mqtt_app;

public class Warning {

	String moteID;
	int bL;
	int FF;
	int PIR;

	@Override
	public String toString() {
		return "Message "+moteID+" [bL=" + bL + ", FF=" + FF + ", PIR=" + PIR +"]";
	}
}