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
public enum DataMapperEnum {

	DATACONNECT(1,DataConnect.class),
	DATAEXEMPLE1(2,DataExample1.class);

	private final int messageType;
	private final Class messageClass;

	private DataMapperEnum(int messageType, Class messageClass) {
		this.messageType = messageType;
		this.messageClass = messageClass;
	}

	public int getMessageType() {
		return messageType;
	}

	public Class getMessageClass() {
		return messageClass;
	}

	public static Class byType(int type){
		switch(type){
			case 1:
				return DATACONNECT.getMessageClass();
			default:
				return null;
		}
	}
}
