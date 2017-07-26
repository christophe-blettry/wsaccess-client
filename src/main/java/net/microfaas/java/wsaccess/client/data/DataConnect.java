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
public class DataConnect extends DataWrap {

	private static final String ID_FIELD_NAME="id";
	private String vid;
	private String model;
	private String version;
	private long time;

	public DataConnect() {
		super(DataMapperEnum.DATACONNECT.getMessageType());
	}

	public DataConnect(String vid, String model, String version, long time) {
		super(DataMapperEnum.DATACONNECT.getMessageType());
		this.vid = vid;
		this.model = model;
		this.version = version;
		this.time = time;
	}

	public String getVid() {
		return vid;
	}

	public void setVid(String vid) {
		this.vid = vid;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

}
