package com.fabric.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fabric.dao.ChaincodeDao;
import com.fabric.pojo.Chaincode;
import com.fabric.service.CodeService;

@Service
public class CodeServiceImpl implements CodeService{

	@Autowired
	private ChaincodeDao codeDao;
	
	@Override
	public Chaincode queryChaincodeIsExist(String orgname, String channelname) {
		return codeDao.queryChaincodeIsExist(orgname, channelname);
	}

	@Override
	public int insertChaincode(Chaincode code) {
		return codeDao.insertChaincode(code);
	}

	@Override
	public int updateChaincode(String id, String path,String version) {
		return codeDao.updateChaincode(id, path,version);
	}

	@Override
	public int updateChaincodeIns(String channame, String ins) {
		// TODO Auto-generated method stub
		return codeDao.updateChaincodeIns(channame,ins);
	}

}
