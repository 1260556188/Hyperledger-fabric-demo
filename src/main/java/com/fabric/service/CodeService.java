package com.fabric.service;

import com.fabric.pojo.Chaincode;

public interface CodeService {

	Chaincode queryChaincodeIsExist(String orgname,String channelname);
	
	int insertChaincode(Chaincode code);
	
	int updateChaincode(String id,String path,String version);
	
	int updateChaincodeIns(String channame,String ins);
	
}
