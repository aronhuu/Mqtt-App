package com.sn.miot.mqtt_app;

public class MessageContentClass {

	String moteID;
	float t;
	float h;
	float p;
	int bL;
	int accX;
	int accY;
	int accZ;
//
//	public float getTemp(){
//		return temp;
//	}
//
//	public float getHumd(){
//		return humd;
//	}
//
//	public float getPres(){
//		return pres;
//	}
//
//	public int getBatteryLevel(){
//		return batteryLevel;
//	}
//
//	public int getAccX(){
//		return AccX;
//	}
//
//	public int getAccY(){
//		return AccY;
//	}
//
//	public int getAccZ(){
//		return AccZ;
//	}
	
	@Override
	public String toString() {
		return "Message "+moteID+" [temp=" + t + ", humd=" + h + ", pres=" + p + ", batteryLevel=" + bL + ", AccX=" + accX + ", AccY=" + accY + ", AccZ=" + accZ +"]";
	}
}