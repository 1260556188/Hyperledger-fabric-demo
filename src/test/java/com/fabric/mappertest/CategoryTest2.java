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
import com.fabric.service.impl.ChannelService;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CategoryTest2 {

    @Autowired
    private HyperledgerConfiguration hyperledgerConfiguration;
    
    
    @Autowired
    private ChannelService channelService;
    private static Logger log = Logger.getLogger(CategoryTest2.class);

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
	public void TestChannelService() {
		try {
			CommonResult result = channelService.constructChannel("mychannel", "org1");
			System.out.println("msg:"+result.getMessage());
			Thread.sleep(6000);
			CommonResult result2 = channelService.joinChannel("mychannel", "org2");
			System.out.println(result2.getCode()+" "+result2.getMessage());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
