package com.fabric.pojo;

public class Channel {
	
	public String id;
	public String channelName;
	public String isCreated;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getChannelName() {
		return channelName;
	}
	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}
	public String getIsCreated() {
		return isCreated;
	}
	public void setIsCreated(String isCreated) {
		this.isCreated = isCreated;
	}
	@Override
	public String toString() {
		return "Channel [id=" + id + ", channelName=" + channelName + ", isCreated=" + isCreated + "]";
	}
	
	
}
