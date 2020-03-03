package com.fabric.mappertest;

import java.util.Collection;

import org.apache.log4j.Logger;
import org.hyperledger.fabric.sdk.HFClient;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.fabric.common.CommonResult;
import com.fabric.config.HyperledgerConfiguration;
import com.fabric.model.Org;
import com.fabric.service.impl.ChainCodeService;
import com.fabric.service.impl.ChannelService;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ChaincodeTest2 {

    @Autowired
    private HyperledgerConfiguration hyperledgerConfiguration;
    
    @Autowired
    private ChannelService channelService;
    
    @Autowired
    private ChainCodeService chainCodeService;
    
    private static Logger log = Logger.getLogger(ChaincodeTest2.class);

	@Test
	@Ignore
	public void TestNotice() throws Exception {
//		List<Product> list = productMapper.getProductListByCategoryId(5);
//		System.out.println(list.size());
        HFClient client = HFClient.createNewInstance();
        hyperledgerConfiguration.checkConfig(client);
        Collection<Org> orgs=hyperledgerConfiguration.config.getSampleOrgs();
        System.out.println(orgs);

	}
	@Test
	public void TestChainCodeService2() {
		try {
			System.out.println("创建通道");
			CommonResult result = channelService.constructChannel("mychannel", "org1");
			System.out.println("msg:"+result.getMessage());
			
			System.out.println("节点加入到通道");
			Thread.sleep(6000);
			CommonResult result2 = channelService.joinChannel("mychannel", "org2");
			System.out.println(result2.getCode()+" "+result2.getMessage());
			
			Thread.sleep(3000);
			System.out.println("安装智能合约");
			String chaincodepath = "/home/ycy/workspace/fabric-samples/chaincode/src/github.com/fabcar/fabcar.go";
			CommonResult result3 = chainCodeService.installChaincode("org1", "mychannel", chaincodepath, "fabcar", "1.0");
			System.out.println(result3.getMessage());
			
			Thread.sleep(3000);
			System.out.println("安装智能合约");
			CommonResult result5 = chainCodeService.installChaincode("org2", "mychannel", chaincodepath, "fabcar", "1.0");
			System.out.println(result5.getMessage());
			
			Thread.sleep(3000);
			System.out.println("实例化智能合约");
			CommonResult result4 = chainCodeService.instantiateChaincode("org1",new String[] {"org1","org2"}, "mychannel");
			System.out.println(result4.getMessage());
			
			Thread.sleep(3000);
			System.out.println("调用智能合约");
			CommonResult result6 = chainCodeService.invokeChaincode("org1", new String[] {"org1","org2"}, "mychannel", "fabcar", "initLedger", new String[] {});
			System.out.println(result6.getMessage());
			/*Thread.sleep(3000);
			System.out.println("实例化智能合约");
			CommonResult result6 = chainCodeService.instantiateChaincode("org2", "mychannel", "fabcar");
			System.out.println(result6.getMessage());*/
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	

	
	@Test
	public void TestCreateChannel() {
		System.out.println("创建通道");
		CommonResult result = channelService.constructChannel("mychannel", "org1");
		System.out.println("msg:"+result.getMessage());
	}
	
	@Test
	public void TestJoinChannel() {
		System.out.println("节点加入到通道");
		CommonResult result2 = channelService.joinChannel("mychannel", "org2");
		System.out.println(result2.getCode()+" "+result2.getMessage());
	}
	
	
	@Test
	public void TestInstallChain() {
		System.out.println("安装智能合约");
		String chaincodepath = "/home/ycy/workspace/fabric-samples/chaincode/src/github.com/fabcar/fabcar.go";
		CommonResult result3 = chainCodeService.installChaincode("org2", "mychannel", chaincodepath, "fabcar", "1.0");
		System.out.println(result3.getMessage());
	}
	
	@Test
	public void TestInstantiatedChain() {
		System.out.println("实例化智能合约");
		CommonResult result4 = chainCodeService.instantiateChaincode("org1",new String[] {"org1","org2"}, "mychannel");
		System.out.println(result4.getMessage());
	}
	
	
	
	@Test
	public void TestInvoke() {
		try {
			Thread.sleep(3000);
			System.out.println("调用智能合约");
			CommonResult result6 = chainCodeService.invokeChaincode("org1", new String[] {"org1","org2"}, "mychannel", "fabcar", "initLedger", new String[] {});
			System.out.println(result6.getMessage());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void TestQueryChainCode() {
		CommonResult result = chainCodeService.queryChainCode("org1", "mychannel", "queryAllCars", new String[] {});
		System.out.println(result.getMessage());
	}
	
	@Test
	public void UpgradeChaincode() {
		String path = "/home/ycy/workspace/fabric-samples/chaincode/src/github.com/fabcar1/fabcar.go";
		CommonResult result = chainCodeService.updateChaincode("org1", new String[] {"org1","org2"}, "mychannel", path, "fabcar", "1.1");
		System.out.println(result.getMessage());
	}
	
}
