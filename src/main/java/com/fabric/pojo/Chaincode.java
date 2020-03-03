package com.fabric.pojo;

public class Chaincode {

	String id;
	String orgName;//组织名称
	String channelName;//通道名称
	String chaincodeName;//链码名称
	
	String chaincodeVersion;//链码版本
	String chaincodePath;//go语言链码必须要的，用于获取chaincodeid
	String isInstantiated;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getOrgName() {
		return orgName;
	}
	public void setOrgName(String orgName) {
		this.orgName = orgName;
	}
	public String getChannelName() {
		return channelName;
	}
	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}
	public String getChaincodeName() {
		return chaincodeName;
	}
	public void setChaincodeName(String chaincodeName) {
		this.chaincodeName = chaincodeName;
	}
	public String getChaincodeVersion() {
		return chaincodeVersion;
	}
	public void setChaincodeVersion(String chaincodeVersion) {
		this.chaincodeVersion = chaincodeVersion;
	}
	public String getChaincodePath() {
		return chaincodePath;
	}
	public void setChaincodePath(String chaincodePath) {
		this.chaincodePath = chaincodePath;
	}
	public String getIsInstantiated() {
		return isInstantiated;
	}
	public void setIsInstantiated(String isInstantiated) {
		this.isInstantiated = isInstantiated;
	}
	@Override
	public String toString() {
		return "Chaincode [id=" + id + ", orgName=" + orgName + ", channelName=" + channelName + ", chaincodeName="
				+ chaincodeName + ", chaincodeVersion=" + chaincodeVersion + ", chaincodePath=" + chaincodePath
				+ ", isInstantiated=" + isInstantiated + "]";
	}
	
}
