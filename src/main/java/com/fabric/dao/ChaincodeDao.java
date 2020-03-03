package com.fabric.dao;

import org.apache.ibatis.annotations.Param;

import com.fabric.pojo.Chaincode;

public interface ChaincodeDao {

	//是否已安装链码 org channelname 是否有这个通道
	Chaincode queryChaincodeIsExist(@Param(value = "orgname")String orgname,@Param(value = "channelname")String channelname);
	
	//插入安装链码
	int insertChaincode(Chaincode code);
	
	//更新链码版本
	int updateChaincode(@Param(value = "id")String id, @Param(value = "path")String path,@Param(value = "version")String version);

	int updateChaincodeIns(@Param(value = "name")String id, @Param(value = "ins")String ins);
	
}
