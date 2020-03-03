package com.fabric.mappertest;

import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.fabric.dao.ApiDao;
import com.fabric.pojo.Api;
 
@RunWith(SpringRunner.class)
@SpringBootTest
public class CategoryTest {

	@Autowired
	private ApiDao apiDao;
	
	@Test
	//@Ignore
	public void TestNotice() {
		List<Api> allApi = apiDao.getAllApi();
		for(Api api:allApi) {
			System.out.println(api);
		}
		System.out.println(allApi.size());
	}

	
}
