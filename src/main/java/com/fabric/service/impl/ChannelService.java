package com.fabric.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.fabric.common.CommonResult;
import com.fabric.config.ChannelContext;
import com.fabric.config.Config;
import com.fabric.config.HyperledgerConfiguration;
import com.fabric.model.Org;
import com.fabric.service.ChanService;

import org.apache.log4j.Logger;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Set;

import static org.hyperledger.fabric.sdk.Channel.PeerOptions.createPeerOptions;

/**
 * @author: LeonMa
 * @date: 2019/01/10 12:01
 */
@Service
public class ChannelService {

    @Autowired
    private HyperledgerConfiguration hyperledgerConfiguration;
    
    @Autowired
    private ChanService chanService;
    
    private static Logger log = Logger.getLogger(ChannelService.class);

    /**
     * 
     * @param channelName 通道名称
     * @param peerWithOrg 组织名称
     * @return
     * @throws Exception
     */
    public CommonResult constructChannel(String channelName, String peerWithOrg){
        try {
        	
        	com.fabric.pojo.Channel channelInfo = chanService.getChannelInfo(channelName);
        	if(channelInfo==null) {
        		return CommonResult.failed("The channel named "+channelName+" is not existed!");
        	}
        	if(( channelInfo.getIsCreated() ).equals("true")) {
        		return CommonResult.failed("The channel named "+channelName+" is already existed!");
        	}
			HFClient client = HFClient.createNewInstance();
			hyperledgerConfiguration.checkConfig(client);
			Org sampleOrg = HyperledgerConfiguration.config.getSampleOrg(peerWithOrg);
			log.info("Constructing channel " + channelName);

			System.out.println(sampleOrg.getTls_ca_cert());
			
			client.setUserContext(sampleOrg.getPeerAdmin());

			Collection<Orderer> orderers = new LinkedList<>();
			for (String orderName : sampleOrg.getOrdererNames()) {
				//获取order的tls配置
			    Properties ordererProperties = HyperledgerConfiguration.config.getOrdererProperties(peerWithOrg,orderName);

			    orderers.add(client.newOrderer(orderName, sampleOrg.getOrdererLocation(orderName),
			            ordererProperties));
			}
			// ?
			Orderer anOrderer = orderers.iterator().next();
			//orderers.remove(anOrderer);
			String channelpath = Config.Path + "/channel-artifacts/channel.tx";
			File file = new File(channelpath);
			if(!file.exists()) {
				throw new RuntimeException("could not find the channel config perm!");
			}
			ChannelConfiguration channelConfiguration = new ChannelConfiguration(
					file);
			/*System.out.println(channelName);
			System.out.println(anOrderer.getName()+" "+anOrderer.getUrl()+" "+anOrderer.getProperties());
			System.out.println(sampleOrg.getPeerAdmin().getMspId());
			System.out.println(sampleOrg.getPeerAdmin().getEnrollment().getKey());*/
			byte[] channelConfigurationSignatures = client.getChannelConfigurationSignature(channelConfiguration,
					sampleOrg.getPeerAdmin());
			
			Channel newChannel = client.newChannel(channelName, anOrderer, channelConfiguration,
					channelConfigurationSignatures);


			log.info("Created channel " + channelName);
			for (String peerName : sampleOrg.getPeerNames()) {
			    String peerLocation = sampleOrg.getPeerLocation(peerName);
				Properties peerproperties = new Properties();
				
				peerproperties.put("pemFile", sampleOrg.getTls_ca_cert());
				peerproperties.setProperty("sslProvider", "openSSL");
				peerproperties.setProperty("negotiationType", "TLS");
			    //Properties peerProperties = HyperledgerConfiguration.config.getPeerProperties(peerName);

			    Peer peer = client.newPeer(peerName, peerLocation, peerproperties);
			    newChannel.joinPeer(peer, createPeerOptions());
			    log.info("Peer " + peerName + " joined channel " + channelName);
			    sampleOrg.addPeer(peer);
			}

			for (Orderer orderer : orderers) {
			    newChannel.addOrderer(orderer);
			}

			newChannel.initialize();
			Config.channelMap.put(channelName,newChannel);
			log.info("Finished initialization channel " + channelName);

			
			int flag = chanService.updateChannelInfo(channelInfo.getId(),"true");
			if(flag>=1) {
				return CommonResult.success("Channel created successfully");
			}else {
				return CommonResult.failed("Channel created failed");
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			return CommonResult.failed(e.getMessage());
		}

    }



    public void reconstructChannel(String[] peerWithOrgs, String channelName, HFClient client) throws Exception {
        peerWithOrgs = StringUtils.sortStringArray(peerWithOrgs);
        try {

            Channel newChannel = Config.channelMap.get(channelName+ JSONObject.toJSONString(peerWithOrgs));
            if(newChannel == null) {
                newChannel = client.newChannel(channelName);
                for (String peerWithOrg : peerWithOrgs) {
                    hyperledgerConfiguration.loadOrderersAndPeers(client, peerWithOrg);
                    Org sampleOrg = HyperledgerConfiguration.config.getSampleOrg(peerWithOrg);

                    for (String orderName : sampleOrg.getOrdererNames()) {

                        newChannel.addOrderer(client.newOrderer(orderName, sampleOrg.getOrdererLocation(orderName),
                        		HyperledgerConfiguration.config.getOrdererProperties(peerWithOrg,orderName)));
                    }

                    for (String peerName : sampleOrg.getPeerNames()) {
                        log.debug(peerName);
                        //将机构下面的背书peer加入
                        String peerLocation = sampleOrg.getPeerLocation(peerName);
                        Properties peerproperties = new Properties();
        				peerproperties.put("pemFile", sampleOrg.getTls_ca_cert());
        				peerproperties.setProperty("sslProvider", "openSSL");
        				peerproperties.setProperty("negotiationType", "TLS");
                        
                        Peer peer = client.newPeer(peerName, peerLocation, peerproperties);

                        try {
                            Set<String> channels = client.queryChannels(peer);
                            if (!channels.contains(channelName)) {
                                log.info("Peer " + peerName + " does not appear to belong to channel " + channelName);
                            }
                            newChannel.addPeer(peer);
                        } catch (Exception e) {
                            e.printStackTrace();
                            log.error(e.getMessage());
                            continue;
                        }
                    }


                }

                newChannel.initialize();
                Config.channelMap.put(channelName+JSONObject.toJSONString(peerWithOrgs),newChannel);
                ChannelContext.set(newChannel);
            }else{
                ChannelContext.set(newChannel);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
        }
    }

    public void reconstructChannel(String peerWithOrg, String channelName, HFClient client) throws Exception {

        try {
            Channel newChannel = Config.channelMap.get(channelName+peerWithOrg);
            if(newChannel == null) {
                newChannel = client.newChannel(channelName);

                hyperledgerConfiguration.loadOrderersAndPeers(client, peerWithOrg);
                Org sampleOrg = HyperledgerConfiguration.config.getSampleOrg(peerWithOrg);

                for (String orderName : sampleOrg.getOrdererNames()) {

                    newChannel.addOrderer(client.newOrderer(orderName, sampleOrg.getOrdererLocation(orderName),
                    		HyperledgerConfiguration.config.getOrdererProperties(peerWithOrg,orderName)));
                }

                for (String peerName : sampleOrg.getPeerNames()) {
                    log.debug(peerName);
                    String peerLocation = sampleOrg.getPeerLocation(peerName);
                    
                    Properties peerproperties = new Properties();
    				peerproperties.put("pemFile", sampleOrg.getTls_ca_cert());
    				peerproperties.setProperty("sslProvider", "openSSL");
    				peerproperties.setProperty("negotiationType", "TLS");
                    
                    Peer peer = client.newPeer(peerName, peerLocation, peerproperties);

                    // Query the actual peer for which channels it belongs to and check
                    // it belongs to this channel
//                try {
//                    Set<String> channels = client.queryChannels(peer);
//                    if (!channels.contains(channelName)) {
//                        log.info("Peer " + peerName + " does not appear to belong to channel " + channelName);
//                    }

                    newChannel.addPeer(peer);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    log.error(e.getMessage());
//                    continue;
//                }
                }

//                for (String eventHubName : sampleOrg.getEventHubNames()) {
//
//                    final Properties eventHubProperties = config.getEventHubProperties(eventHubName);
//                    EventHub eventHub = client.newEventHub(eventHubName, sampleOrg.getEventHubLocation(eventHubName),
//                            eventHubProperties);
//                    newChannel.addEventHub(eventHub);
//                }


                newChannel.initialize();
                Config.channelMap.put(channelName+peerWithOrg,newChannel);
                ChannelContext.set(newChannel);
            }else{
                ChannelContext.set(newChannel);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
        }
    }


    /**
     * 通道名称
     * 组织名称
     * @param channelName
     * @param peerWithOrg
     * @return
     * @throws Exception
     */
    public CommonResult joinChannel(String channelName, String peerWithOrg) {
        try {
        	
        	com.fabric.pojo.Channel channelInfo = chanService.getChannelInfo(channelName);
        	if(channelInfo==null) {
        		return CommonResult.failed("The channel named "+channelName+" is not existed!");
        	}
        	
			HFClient client = HFClient.createNewInstance();
			hyperledgerConfiguration.checkConfig(client);
			client.setUserContext(HyperledgerConfiguration.config.getSampleOrg(peerWithOrg).getPeerAdmin());
			//
			Channel newChannel = client.newChannel(channelName);

			System.out.println("----"+newChannel.getPeers().size());

			//hyperledgerConfiguration.loadOrderersAndPeers(client, peerWithOrg);
			Org sampleOrg = HyperledgerConfiguration.config.getSampleOrg(peerWithOrg);


			for (String orderName : sampleOrg.getOrdererNames()) {

			    newChannel.addOrderer(client.newOrderer(orderName, sampleOrg.getOrdererLocation(orderName),
			            HyperledgerConfiguration.config.getOrdererProperties(peerWithOrg,orderName)));
			}

			for (String peerName : sampleOrg.getPeerNames()) {
			    String peerLocation = sampleOrg.getPeerLocation(peerName);

			    //Properties peerProperties = HyperledgerConfiguration.config.getPeerProperties(peerName);

				Properties peerproperties = new Properties();
				peerproperties.put("pemFile", sampleOrg.getTls_ca_cert());
				peerproperties.setProperty("sslProvider", "openSSL");
				peerproperties.setProperty("negotiationType", "TLS");


			    Peer peer = client.newPeer(peerName, peerLocation, peerproperties);
			    Set<String> channels = client.queryChannels(peer);
			    if (!channels.contains(channelName)) {
			    	//Config.channelMap.get(channelName).joinPeer(peer);
			        newChannel.joinPeer(peer, createPeerOptions());
			    } else {
			        log.info("Peer " + peerName + "already joined channel " + channelName);
			        return CommonResult.failed("Peer " + peerName + "already joined channel " + channelName);
			    }

			    log.info("Peer " + peerName + " joined channel " + channelName);
			    sampleOrg.addPeer(peer);
			}

			newChannel.initialize();
//			Channel channel = Config.channelMap.get(channelName);
//			if(channel)

			log.info("Finished joined channel" + channelName);
			return CommonResult.success("Channel joined successfully");
		}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return CommonResult.failed(e.getMessage());
		}

    }
}
