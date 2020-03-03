package com.fabric.service.impl;

import com.fabric.common.CommonResult;
import com.fabric.common.ResultCode;
import com.fabric.config.ChannelContext;
import com.fabric.config.Config;
import com.fabric.config.HyperledgerConfiguration;
import com.fabric.model.Org;
import com.fabric.pojo.Chaincode;
import com.fabric.service.ChanService;
import com.fabric.service.CodeService;

import lombok.extern.slf4j.Slf4j;

import org.apache.log4j.Logger;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.BlockEvent.TransactionEvent;
import org.hyperledger.fabric.sdk.TransactionRequest.Type;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionEventException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hyperledger.fabric.sdk.Channel.NOfEvents.createNofEvents;

/**
 * 
 * @author ycy
 *
 */
@Service
@Slf4j
public class ChainCodeService {

	@Autowired
	private HyperledgerConfiguration hyperledgerConfiguration;

	@Autowired
	private ChannelService channelService;

	// 通道
	@Autowired
	private ChanService chanService;

	@Autowired
	private CodeService codeService;

	Type CHAIN_CODE_LANG = Type.GO_LANG;



	private static Logger log = Logger.getLogger(ChainCodeService.class);

	/**
	 * 链码地址的上传需要测试一下 智能合约的安装，需要给定智能合约的路径 xx/src/github.com/xx/xx.go
	 * /home/ycy/workspace/fabric-samples/chaincode/src/github.com/fabcar/fabcar.go
	 * 组织 通道（名称不存在，还未被创建） 链码地址 名称 版本
	 * 将信息分解出来，安装成功后存入到数据库中，更新智能合约用的到，这里也用的到，以安装智能合约则智能更新
	 * 
	 * @param name
	 * @param peerWithOrg
	 * @param channelName
	 * @param chaincodeName
	 * @param chainCodeVersion
	 * @return
	 * @throws Exception
	 */
	@Transactional
	public CommonResult installChaincode(String peerWithOrg, String channelName, String chaincodePath,
			String chaincodeName, String chainCodeVersion) {

		try {
			com.fabric.pojo.Channel channelInfo = chanService.getChannelInfo(channelName);

			if (channelInfo == null) {// 通道不存在
				return CommonResult.failed("The channel named " + channelName + " is not existed!");
			}
			if (!(channelInfo.isCreated).equals("true")) {// 通道未创建
				return CommonResult.failed("The channel named " + channelName + " is not created!");
			}
			Chaincode code = codeService.queryChaincodeIsExist(peerWithOrg, channelName);
			if (code != null) {
				return CommonResult.failed("You have already installed chaincode in the channel " + channelName);
			}

			HFClient client = HFClient.createNewInstance();
			hyperledgerConfiguration.checkConfig(client);

			hyperledgerConfiguration.loadOrderersAndPeers(client, peerWithOrg);
			Org sampleOrg = HyperledgerConfiguration.config.getSampleOrg(peerWithOrg);
			client.setUserContext(sampleOrg.getPeerAdmin());

			int indexOf1 = chaincodePath.lastIndexOf("src/github.com");
			if (indexOf1 == -1) {
				return CommonResult.failed("智能合约不在指定 src/github.com 路径下");
			}
			int indexOf2 = chaincodePath.lastIndexOf("github.com");
			int indexOf3 = chaincodePath.lastIndexOf("/");

			String codepath = chaincodePath.substring(indexOf2, indexOf3);
			String codelocation = chaincodePath.substring(0, indexOf1);

			// 改造
			ChaincodeID chaincodeID = hyperledgerConfiguration.getChaincodeId(chaincodeName, codepath,
					chainCodeVersion);

			log.info("Running channel " + channelName);

			log.info("Creating install proposal");
			InstallProposalRequest installProposalRequest = client.newInstallProposalRequest();
			installProposalRequest.setChaincodeID(chaincodeID);
			installProposalRequest.setChaincodeSourceLocation(new File(codelocation));
			installProposalRequest.setChaincodeVersion(chainCodeVersion);
			installProposalRequest.setChaincodeLanguage(CHAIN_CODE_LANG);
			installProposalRequest.setUserContext(sampleOrg.getPeerAdmin());
			log.info("Sending install proposal");
			int numInstallProposal = 0;

			Collection<ProposalResponse> responses;
			Collection<ProposalResponse> successful = new LinkedList<>();
			Collection<ProposalResponse> failed = new LinkedList<>();
			Collection<Peer> peers = sampleOrg.getPeers();
			numInstallProposal = numInstallProposal + peers.size();
			responses = client.sendInstallProposal(installProposalRequest, peers);

			for (ProposalResponse response : responses) {
				if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
					log.info("Successful install proposal response Txid: " + response.getTransactionID() + " from peer "
							+ response.getPeer().getName());
					successful.add(response);
				} else {
					failed.add(response);
				}
			}

			log.info("Received " + numInstallProposal + " install proposal responses. Successful+verified: "
					+ successful.size() + " . Failed: " + failed.size());

			if (failed.size() > 0) {
				ProposalResponse first = failed.iterator().next();
				log.error("Not enough endorsers for install :" + successful.size() + ".  " + first.getMessage());
				return CommonResult.failed("Not enough endorsers for install :" + first.getMessage());
				// return "Not enough endorsers for install :" + first.getMessage();
			}
			Chaincode newcode = new Chaincode();
			newcode.setOrgName(peerWithOrg);
			newcode.setChannelName(channelName);
			newcode.setChaincodeName(chaincodeName);
			newcode.setChaincodePath(codepath);
			newcode.setChaincodeVersion(chainCodeVersion);
			newcode.setIsInstantiated("false");
			int insertflag = codeService.insertChaincode(newcode);
			if (insertflag >= 1) {
				return CommonResult.success("Chaincode installed successfully");
			} else {
				return CommonResult.failed("Chaincode installed failed");
			}
		} catch (Exception e) {
			return CommonResult.failed(e.getMessage());
		}

	}

	/**
	 * 实例化智能合约
	 * @param name
	 * @param belongWithOrg 
	 * @param channelName
	 * @param chaincodeName
	 * @return
	 */
	@Transactional
	public CommonResult instantiateChaincode( String belongWithOrg,String[] peerWithOrgs,  String channelName){
		
		try {
			com.fabric.pojo.Channel channelInfo = chanService.getChannelInfo(channelName);

			if (channelInfo == null) {// 通道不存在
				return CommonResult.failed("The channel named " + channelName + " is not existed!");
			}
			if ((channelInfo.isCreated).equals("false")) {// 通道未创建
				return CommonResult.failed("The channel named " + channelName + " is not created!");
			}
			Chaincode code = codeService.queryChaincodeIsExist(belongWithOrg, channelName);
			if (code == null) {
				return CommonResult.failed("You have not  installed chaincode in the channel " + channelName);
			}
			
			/*if(!code.getChaincodeName().equals(chaincodeName)) {
				return CommonResult.failed("The chaincode name: "+channelName+" is corrected!");
			}*/
			//是否已经实例化 检测
			if(!(code.getIsInstantiated()).equals("false")) {
				return CommonResult.failed("The chaincode name: "+code.getChaincodeName()+" has been instantiated!");
			}
			
			HFClient client = HFClient.createNewInstance();
			hyperledgerConfiguration.checkConfig(client);

			client.setUserContext(HyperledgerConfiguration.config.getSampleOrg(belongWithOrg).getPeerAdmin());

			ChaincodeID chaincodeID = hyperledgerConfiguration.getChaincodeId(code.getChaincodeName(), code.getChaincodePath(),code.getChaincodeVersion());
			
			//Channel channel = Config.channelMap.get(channelName);
			channelService.reconstructChannel(peerWithOrgs, channelName, client);
			Channel channel = ChannelContext.get();

			log.info("Running channel " + channelName);

			Collection<Orderer> orderers = channel.getOrderers();

			InstantiateProposalRequest instantiateProposalRequest = client.newInstantiationProposalRequest();
			instantiateProposalRequest.setProposalWaitTime(HyperledgerConfiguration.config.getProposalWaitTime());
			instantiateProposalRequest.setChaincodeID(chaincodeID);
			instantiateProposalRequest.setChaincodeLanguage(CHAIN_CODE_LANG);
			instantiateProposalRequest.setFcn("init");
			instantiateProposalRequest.setArgs(new String[] {});
			instantiateProposalRequest.setChaincodeVersion(code.getChaincodeVersion());
			instantiateProposalRequest
					.setUserContext(HyperledgerConfiguration.config.getSampleOrg(belongWithOrg).getPeerAdmin());

			Map<String, byte[]> tm = new HashMap<>();
			tm.put("HyperLedgerFabric", "InstantiateProposalRequest:JavaSDK".getBytes(UTF_8));
			tm.put("method", "InstantiateProposalRequest".getBytes(UTF_8));
			instantiateProposalRequest.setTransientMap(tm);

			/*ChaincodeEndorsementPolicy chaincodeEndorsementPolicy = new ChaincodeEndorsementPolicy();
			chaincodeEndorsementPolicy
					.fromYamlFile(new File(HyperledgerConfiguration.PATH + "/artifacts/chaincodeendorsementpolicy.yaml"));
			instantiateProposalRequest.setChaincodeEndorsementPolicy(chaincodeEndorsementPolicy);*/
			Collection<ProposalResponse> responses;
			Collection<ProposalResponse> successful = new LinkedList<>();
			Collection<ProposalResponse> failed = new LinkedList<>();

			log.info("Sending instantiateProposalRequest to all peers with ");
			successful.clear();
			failed.clear();

			responses = channel.sendInstantiationProposal(instantiateProposalRequest,channel.getPeers());

			for (ProposalResponse response : responses) {
				if (response.isVerified() && response.getStatus() == ProposalResponse.Status.SUCCESS) {
					successful.add(response);
					log.info("Succesful instantiate proposal response Txid: " + response.getTransactionID() + " from peer "
							+ response.getPeer().getName());
				} else {
					failed.add(response);
				}
			}
			log.info("Received " + responses.size() + " instantiate proposal responses. Successful+verified: "
					+ successful.size() + " . Failed: " + failed.size());
			if (failed.size() > 0) {
				for (ProposalResponse fail : failed) {

					log.info("Not enough endorsers for instantiate :" + successful.size() + "endorser failed with "
							+ fail.getMessage() + ", on peer" + fail.getPeer());

				}
				ProposalResponse first = failed.iterator().next();
				log.error("Not enough endorsers for instantiate :" + successful.size() + "endorser failed with "
						+ first.getMessage() + ". Was verified:" + first.isVerified());
				return CommonResult.failed("endorser failed");
				//return "endorser failed";
			}
			log.info("Sending instantiateTransaction to orderer ");
			log.info("orderers" + orderers);

			TransactionEvent transactionEvent = channel.sendTransaction(responses).get(HyperledgerConfiguration.config.getTransactionWaitTime(), TimeUnit.SECONDS);
			if(transactionEvent.isValid()) {
				int updateChaincodeIns = codeService.updateChaincodeIns(code.getChannelName(),"true");
				if(updateChaincodeIns>=1) {
					return CommonResult.success("Chaincode instantiated Successfully! The Txid is"+successful.iterator().next().getTransactionID());
				}else {
					return CommonResult.failed("Chaincode instantiated failed!");
				}
			}else {
				return CommonResult.failed("Chaincode instantiated failed!");
			}
			
		}catch (Exception e) {
			e.printStackTrace();
			return CommonResult.failed("Chaincode instantiated failed! Reason: "+e.getMessage());
		}
		
	
	}

	
	
	/**
	 * 调用智能合约代码
	 * @param name
	 * @param belongWithOrg
	 * @param peerWithOrgs
	 * @param channelName
	 * @param chaincodeName
	 * @param chaincodeFunction
	 * @param chaincodeArgs
	 * @return
	 * @throws Exception
	 */
	@Transactional
	public CommonResult invokeChaincode(String belongWithOrg, String[] peerWithOrgs, String channelName,
			String chaincodeName, String chaincodeFunction, String[] chaincodeArgs){

		try {
			com.fabric.pojo.Channel channelInfo = chanService.getChannelInfo(channelName);

			if (channelInfo == null) {// 通道不存在
				return CommonResult.failed("The channel named " + channelName + " is not existed!");
			}
			if ((channelInfo.isCreated).equals("false")) {// 通道未创建
				return CommonResult.failed("The channel named " + channelName + " is not created!");
			}
			Chaincode code = codeService.queryChaincodeIsExist(belongWithOrg, channelName);
			if (code == null) {
				return CommonResult.failed("You have not  installed chaincode in the channel " + channelName);
			}
			
			if(!code.getChaincodeName().equals(chaincodeName)) {
				return CommonResult.failed("The chaincode name: "+channelName+" is corrected!");
			}
			//是否已经实例化 检测 已经实例化才可以调用
			if((code.getIsInstantiated()).equals("false")) {
				return CommonResult.failed("The chaincode name: "+channelName+" has not been instantiated!");
			}
			
			HFClient client = HFClient.createNewInstance();
			hyperledgerConfiguration.checkConfig(client);

			client.setUserContext(HyperledgerConfiguration.config.getSampleOrg(belongWithOrg).getPeerAdmin());

			ChaincodeID chaincodeID = hyperledgerConfiguration.getChaincodeId(code.getChaincodeName(), code.getChaincodePath(),code.getChaincodeVersion());
			channelService.reconstructChannel(peerWithOrgs, channelName, client);
			Channel channel = ChannelContext.get();

			log.info("Running channel " + channelName);

			log.debug("chaincodeFunction" + chaincodeFunction);
			log.debug("chaincodeArgs" + chaincodeArgs);

			TransactionProposalRequest transactionProposalRequest = client.newTransactionProposalRequest();
			transactionProposalRequest.setChaincodeID(chaincodeID);
			transactionProposalRequest.setChaincodeLanguage(Type.GO_LANG);
			transactionProposalRequest.setFcn(chaincodeFunction);
			transactionProposalRequest.setProposalWaitTime(HyperledgerConfiguration.config.getProposalWaitTime());
			transactionProposalRequest.setArgs(chaincodeArgs);
			transactionProposalRequest.setChaincodeVersion(code.getChaincodeVersion());
			transactionProposalRequest
					.setUserContext(HyperledgerConfiguration.config.getSampleOrg(belongWithOrg).getPeerAdmin());

//        ChaincodeEndorsementPolicy chaincodeEndorsementPolicy = new ChaincodeEndorsementPolicy();
//        chaincodeEndorsementPolicy
//                .fromYamlFile(new File(PATH + "/artifacts/chaincodeendorsementpolicy.yaml"));
//        transactionProposalRequest.setChaincodeEndorsementPolicy(chaincodeEndorsementPolicy);

			Map<String, byte[]> tm2 = new HashMap<>();
			tm2.put("HyperLedgerFabric", "TransactionProposalRequest:JavaSDK".getBytes(UTF_8));
			tm2.put("method", "TransactionProposalRequest".getBytes(UTF_8));
			tm2.put("result", ":)".getBytes(UTF_8)); /// This should be returned

			Collection<ProposalResponse> successful = new LinkedList<>();
			Collection<ProposalResponse> failed = new LinkedList<>();

			transactionProposalRequest.setTransientMap(tm2);

			log.info("sending transactionProposal to all peers with arguments: " + chaincodeFunction + "," + chaincodeArgs);
			Collection<ProposalResponse> transactionPropResp = channel.sendTransactionProposal(transactionProposalRequest,
					channel.getPeers());
			for (ProposalResponse response : transactionPropResp) {
				if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
					log.info("Successful transaction proposal response Txid: " + response.getTransactionID() + " from peer "
							+ response.getPeer().getName());
					successful.add(response);
				} else {
					failed.add(response);
				}
			}

			Collection<Set<ProposalResponse>> proposalConsistencySets = SDKUtils
					.getProposalConsistencySets(transactionPropResp);
			if (proposalConsistencySets.size() != 1) {
				log.error(format("Expected only one set of consistent proposal responses but got "
						+ proposalConsistencySets.size()));
			}

			log.info("Received " + transactionPropResp.size() + " transaction proposal responses. Successful+verified: "
					+ successful.size() + " . Failed: " + failed.size());
			if (failed.size() > 0) {
				ProposalResponse firstTransactionProposalResponse = failed.iterator().next();
				log.error("Not enough endorsers for invoke:" + failed.size() + " endorser error: "
						+ firstTransactionProposalResponse.getMessage() + ". Was verified: "
						+ firstTransactionProposalResponse.isVerified());
				return CommonResult.failed(firstTransactionProposalResponse.getMessage());
			}
			log.info("Successfully received transaction proposal responses.");
			ProposalResponse resp = successful.iterator().next();
			byte[] x = resp.getChaincodeActionResponsePayload(); // This is the data returned by the chaincode.
			String resultAsString = null;
			if (x != null) {
				resultAsString = new String(x, "UTF-8");
			}
			log.debug("getChaincodeActionResponseReadWriteSetInfo:::" + resp.getChaincodeActionResponseReadWriteSetInfo());
			ChaincodeID cid = resp.getChaincodeID();

			log.info("Sending chaincode transaction " + chaincodeName + "_" + chaincodeFunction + " to orderer.");
			log.info("transactionID==>" + resp.getTransactionID());
			String result = "";
			try {
				BlockEvent.TransactionEvent event = channel.sendTransaction(successful)
						.get(HyperledgerConfiguration.config.getTransactionWaitTime(), TimeUnit.SECONDS);
				// 事务处理成功
				if (event.isValid()) {
					log.info("事物处理成功");
					result = "Transaction invoked successfully";
				} else {
					log.info("事物处理失败");
					result = "Transaction invoked Failed";
				}
			} catch (Exception e) {
				log.error("IntermediateChaincodeID==>toOrdererResponse==>Exception:" + e.getMessage());
				result = "Transaction invoked Error";
			}

			log.info("Transaction invoked " + result);

			return CommonResult.success(result);
		}catch (Exception e) {
			return CommonResult.failed(e.getMessage());
		}
		
		
	}

	
	public CommonResult queryChainCode(String peerWithOrg, String channelName,
			String chaincodeFunction, String[] chaincodeArgs){
		
		try {
			com.fabric.pojo.Channel channelInfo = chanService.getChannelInfo(channelName);

			if (channelInfo == null) {// 通道不存在
				return CommonResult.failed("The channel named " + channelName + " is not existed!");
			}
			if ((channelInfo.isCreated).equals("false")) {// 通道未创建
				return CommonResult.failed("The channel named " + channelName + " is not created!");
			}
			Chaincode code = codeService.queryChaincodeIsExist(peerWithOrg, channelName);
			if (code == null) {
				return CommonResult.failed("You have not  installed chaincode in the channel " + channelName);
			}
			
			/*if(!code.getChaincodeName().equals(chaincodeName)) {
				return CommonResult.failed("The chaincode name: "+channelName+" is not corrected!");
			}*/
			// 链码是否被实例化
			if((code.getIsInstantiated()).equals("false")) {
				return CommonResult.failed("The chaincode name: "+channelName+" has not been instantiated!");
			}
			
			HFClient client = HFClient.createNewInstance();
			hyperledgerConfiguration.checkConfig(client);
			client.setUserContext(HyperledgerConfiguration.config.getSampleOrg(peerWithOrg).getPeerAdmin());

			ChaincodeID chaincodeID = hyperledgerConfiguration.getChaincodeId(code.getChaincodeName(),code.getChaincodePath(), code.getChaincodeVersion());
			channelService.reconstructChannel(peerWithOrg, channelName, client);
			Channel channel = ChannelContext.get();

			log.info("Running channel " + channelName);
			QueryByChaincodeRequest queryByChaincodeRequest = client.newQueryProposalRequest();
			queryByChaincodeRequest.setArgs(chaincodeArgs);
			queryByChaincodeRequest.setFcn(chaincodeFunction);
			queryByChaincodeRequest.setChaincodeID(chaincodeID);
			queryByChaincodeRequest.setChaincodeVersion(code.getChaincodeVersion());
			queryByChaincodeRequest.setChaincodeLanguage(Type.GO_LANG);
			queryByChaincodeRequest.setUserContext(HyperledgerConfiguration.config.getSampleOrg(peerWithOrg).getPeerAdmin());

			Map<String, byte[]> tm2 = new HashMap<>();
			tm2.put("HyperLedgerFabric", "QueryByChaincodeRequest:JavaSDK".getBytes(UTF_8));
			tm2.put("method", "QueryByChaincodeRequest".getBytes(UTF_8));
			queryByChaincodeRequest.setTransientMap(tm2);

			Collection<ProposalResponse> queryProposals = channel.queryByChaincode(queryByChaincodeRequest,
					channel.getPeers());
			for (ProposalResponse proposalResponse : queryProposals) {
				if (!proposalResponse.isVerified() || proposalResponse.getStatus() != ProposalResponse.Status.SUCCESS) {
					log.error("Failed query proposal from peer " + proposalResponse.getPeer().getName() + " status: "
							+ proposalResponse.getStatus() + ". Messages: " + proposalResponse.getMessage()
							+ ". Was verified : " + proposalResponse.isVerified());
					return CommonResult.failed(proposalResponse.getMessage());
					//throw new Exception(proposalResponse.getMessage());
				} else {
					String payload = proposalResponse.getProposalResponse().getResponse().getPayload().toStringUtf8();
					log.info(
							"Query payload of b from peer" + proposalResponse.getPeer().getName() + " returned " + payload);
					return CommonResult.success("query chaincode successfully",payload);
					//return payload;
				}
			}
		}catch (Exception e) {
			e.printStackTrace();
			return CommonResult.failed("Caught an exception while quering chaincode");
		}
		return CommonResult.failed("Caught an exception while quering chaincode");

	}
	
	/**
	 * 更新智能合约
	 * @param belongWithOrg
	 * @param peerWithOrgs
	 * @param channelName
	 * @param chaincodePath
	 * @param chaincodeName
	 * @param chainCodeVersion
	 * @return
	 */
	@Transactional
	public CommonResult updateChaincode(String belongWithOrg, String[] peerWithOrgs, String channelName,String chaincodePath,
			String chaincodeName, String chainCodeVersion){
		
		try {
			com.fabric.pojo.Channel channelInfo = chanService.getChannelInfo(channelName);

			if (channelInfo == null) {// 通道不存在
				return CommonResult.failed("The channel named " + channelName + " is not existed!");
			}
			if ((channelInfo.isCreated).equals("false")) {// 通道未创建
				return CommonResult.failed("The channel named " + channelName + " is not created!");
			}
			Chaincode code = codeService.queryChaincodeIsExist(belongWithOrg, channelName);
			if (code == null) {
				return CommonResult.failed("You have not  installed chaincode in the channel " + channelName);
			}
			
			if(!code.getChaincodeName().equals(chaincodeName)) {
				return CommonResult.failed("The chaincode name: "+channelName+" is not corrected!");
			}
			// 链码是否被实例化
			if((code.getIsInstantiated()).equals("false")) {
				return CommonResult.failed("The chaincode name: "+channelName+" has not been instantiated!");
			}
			if(code.getChaincodeVersion().equals(chainCodeVersion)) {
				return CommonResult.failed("The chaincode name: "+channelName+" version should not be same as the previous");
			}
			HFClient client = HFClient.createNewInstance();
			hyperledgerConfiguration.checkConfig(client);
			client.setUserContext(HyperledgerConfiguration.config.getSampleOrg(belongWithOrg).getPeerAdmin());
			
			/*int indexOf1 = chaincodePath.lastIndexOf("src/github.com");
			if (indexOf1 == -1) {
				return CommonResult.failed("智能合约不在指定 src/github.com 路径下");
			}
			int indexOf2 = chaincodePath.lastIndexOf("github.com");
			int indexOf3 = chaincodePath.lastIndexOf("/");

			String codepath = chaincodePath.substring(indexOf2, indexOf3);
			String codelocation = chaincodePath.substring(0, indexOf1);*/
			
			int indexOf2 = chaincodePath.lastIndexOf("github.com");
			int indexOf3 = chaincodePath.lastIndexOf("/");

			String codepath = chaincodePath.substring(indexOf2, indexOf3);
			
			for (int i = 0; i < peerWithOrgs.length; i++) {
				CommonResult commonResult = install_Upgrade_Chaincode(peerWithOrgs[i], channelName, chaincodePath, chaincodeName, chainCodeVersion);
				
				if(commonResult.getCode()!=ResultCode.SUCCESS.getCode()) {
					return CommonResult.failed("Upgrade : "+channelName+" failed in install proposal!");
				}
			}
			System.out.println("update proposal 1 install complete!");
			
			ChaincodeID chaincodeID = hyperledgerConfiguration.getChaincodeId(chaincodeName,codepath, chainCodeVersion);
			channelService.reconstructChannel(peerWithOrgs, channelName, client);
			Channel channel = ChannelContext.get();

			log.info("Running channel " + channelName);

			Collection<Orderer> orderers = channel.getOrderers();

			UpgradeProposalRequest upgradeProposalRequest = client.newUpgradeProposalRequest();
			upgradeProposalRequest.setProposalWaitTime(HyperledgerConfiguration.config.getProposalWaitTime());
			upgradeProposalRequest.setChaincodeID(chaincodeID);
			upgradeProposalRequest.setChaincodeLanguage(CHAIN_CODE_LANG);
			upgradeProposalRequest.setFcn("init");
			upgradeProposalRequest.setArgs(new String[] {});
			upgradeProposalRequest.setChaincodeVersion(chainCodeVersion);
			upgradeProposalRequest
					.setUserContext(HyperledgerConfiguration.config.getSampleOrg(belongWithOrg).getPeerAdmin());

			Map<String, byte[]> tm = new HashMap<>();
			tm.put("HyperLedgerFabric", "UpgradeProposalRequest:JavaSDK".getBytes(UTF_8));
			tm.put("method", "UpgradeProposalRequest".getBytes(UTF_8));
			upgradeProposalRequest.setTransientMap(tm);
			/*
			ChaincodeEndorsementPolicy chaincodeEndorsementPolicy = new ChaincodeEndorsementPolicy();
			chaincodeEndorsementPolicy
					.fromYamlFile(new File(HyperledgerConfiguration.PATH + "/artifacts/chaincodeendorsementpolicy.yaml"));
			upgradeProposalRequest.setChaincodeEndorsementPolicy(chaincodeEndorsementPolicy);*/
			Collection<ProposalResponse> responses;
			Collection<ProposalResponse> successful = new LinkedList<>();
			Collection<ProposalResponse> failed = new LinkedList<>();

			log.info("Sending instantiateProposalRequest to all peers");
			successful.clear();
			failed.clear();
			responses = channel.sendUpgradeProposal(upgradeProposalRequest, channel.getPeers());

			for (ProposalResponse response : responses) {
				if (response.isVerified() && response.getStatus() == ProposalResponse.Status.SUCCESS) {
					successful.add(response);
					log.info("Succesful update proposal response Txid: " + response.getTransactionID() + " from peer "
							+ response.getPeer().getName());
				} else {
					failed.add(response);
				}
			}
			log.info("Received " + responses.size() + " update proposal responses. Successful+verified: "
					+ successful.size() + " . Failed: " + failed.size());
			if (failed.size() > 0) {
				for (ProposalResponse fail : failed) {

					log.info("Not enough endorsers for instantiate :" + successful.size() + "endorser failed with "
							+ fail.getMessage() + ", on peer" + fail.getPeer());

				}
				ProposalResponse first = failed.iterator().next();
				log.error("Not enough endorsers for instantiate :" + successful.size() + "endorser failed with "
						+ first.getMessage() + ". Was verified:" + first.isVerified());
			}
			log.info("Sending updateTransaction to orderer ");
			log.info("orderers" + orderers);


			TransactionEvent transactionEvent = channel.sendTransaction(responses).get(HyperledgerConfiguration.config.getTransactionWaitTime(), TimeUnit.SECONDS);
			if(transactionEvent.isValid()) {
				int updateChaincode = codeService.updateChaincode(code.getChannelName(), codepath, chainCodeVersion);
				if(updateChaincode>=1) {
					return CommonResult.success("Chaincode instantiated Successfully! The Txid is"+successful.iterator().next().getTransactionID());
				}else {
					return CommonResult.failed("Chaincode instantiated failed!");
				}
			}else {
				return CommonResult.failed("Chaincode instantiated failed!");
			}
			
			//return "Chaincode upgrade Successfully";
		}catch (Exception e) {
			return CommonResult.failed("Chaincode instantiated failed! Reason : "+e.getMessage());
		}
	}

	
	
	@Transactional
	private CommonResult install_Upgrade_Chaincode(String peerWithOrg, String channelName, String chaincodePath,
			String chaincodeName, String chainCodeVersion) {

		try {


			HFClient client = HFClient.createNewInstance();
			hyperledgerConfiguration.checkConfig(client);

			hyperledgerConfiguration.loadOrderersAndPeers(client, peerWithOrg);
			Org sampleOrg = HyperledgerConfiguration.config.getSampleOrg(peerWithOrg);
			client.setUserContext(sampleOrg.getPeerAdmin());

			int indexOf1 = chaincodePath.lastIndexOf("src/github.com");
			if (indexOf1 == -1) {
				return CommonResult.failed("智能合约不在指定 src/github.com 路径下");
			}
			int indexOf2 = chaincodePath.lastIndexOf("github.com");
			int indexOf3 = chaincodePath.lastIndexOf("/");

			String codepath = chaincodePath.substring(indexOf2, indexOf3);
			String codelocation = chaincodePath.substring(0, indexOf1);

			// 改造
			ChaincodeID chaincodeID = hyperledgerConfiguration.getChaincodeId(chaincodeName, codepath,
					chainCodeVersion);

			log.info("Running channel " + channelName);

			log.info("Creating install proposal");
			InstallProposalRequest installProposalRequest = client.newInstallProposalRequest();
			installProposalRequest.setChaincodeID(chaincodeID);
			installProposalRequest.setChaincodeSourceLocation(new File(codelocation));
			installProposalRequest.setChaincodeVersion(chainCodeVersion);
			installProposalRequest.setChaincodeLanguage(CHAIN_CODE_LANG);
			installProposalRequest.setUserContext(sampleOrg.getPeerAdmin());
			log.info("Sending install proposal");
			int numInstallProposal = 0;

			Collection<ProposalResponse> responses;
			Collection<ProposalResponse> successful = new LinkedList<>();
			Collection<ProposalResponse> failed = new LinkedList<>();
			Collection<Peer> peers = sampleOrg.getPeers();
			numInstallProposal = numInstallProposal + peers.size();
			responses = client.sendInstallProposal(installProposalRequest, peers);

			for (ProposalResponse response : responses) {
				if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
					log.info("Successful install proposal response Txid: " + response.getTransactionID() + " from peer "
							+ response.getPeer().getName());
					successful.add(response);
				} else {
					failed.add(response);
				}
			}

			log.info("Received " + numInstallProposal + " install proposal responses. Successful+verified: "
					+ successful.size() + " . Failed: " + failed.size());

			if (failed.size() > 0) {
				ProposalResponse first = failed.iterator().next();
				log.error("Not enough endorsers for install :" + successful.size() + ".  " + first.getMessage());
				return CommonResult.failed("Not enough endorsers for install :" + first.getMessage());
				// return "Not enough endorsers for install :" + first.getMessage();
			}

			return CommonResult.success("Chaincode installed successfully");
		} catch (Exception e) {
			return CommonResult.failed(e.getMessage());
		}

	}
	
}
