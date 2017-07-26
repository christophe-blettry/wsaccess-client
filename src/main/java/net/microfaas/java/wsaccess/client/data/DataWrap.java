/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.microfaas.java.wsaccess.client.data;

/**
 *
 * @author christophe
 */
public class DataWrap {

	public static final String MESSAGE_TYPE_FIELD_NAME="messageType";
	private int messageType;
	private long creationUtcTime;

	private DataWrap() {
	}

	public DataWrap(int messageType) {
		this.messageType = messageType;
		this.creationUtcTime=System.currentTimeMillis();
	}

	public int getMessageType() {
		return messageType;
	}

	public void setMessageType(int messageType) {
		this.messageType = messageType;
	}

	public long getCreationUtcTime() {
		return creationUtcTime;
	}

	public void setCreationUtcTime(long creationUtcTime) {
		this.creationUtcTime = creationUtcTime;
	}
	
}
