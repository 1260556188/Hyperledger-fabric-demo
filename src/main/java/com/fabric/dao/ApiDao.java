package com.fabric.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.fabric.pojo.Api;

public interface ApiDao {

	public int addApi(Api api);
	
	public Api getApi(@Param(value = "id")Integer id);
	
	public List<Api> getAllApi();
}
