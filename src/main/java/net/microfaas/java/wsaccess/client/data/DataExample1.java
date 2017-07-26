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
public class DataExample1 extends DataWrap{

	private long time;
	private long id;

	public DataExample1() {
		super(DataMapperEnum.DATAEXEMPLE1.getMessageType());
	}

	public DataExample1(long time, long id) {
		super(DataMapperEnum.DATAEXEMPLE1.getMessageType());
		this.time = time;
		this.id = id;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

}
