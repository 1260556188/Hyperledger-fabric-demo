package com.fabric.dao;

import org.apache.ibatis.annotations.Param;

import com.fabric.pojo.Channel;

public interface ChannelDao {

	Channel getChannelInfo(@Param(value = "name")String name);

	int updateChannelInfo(@Param(value = "id")String id, @Param(value = "flag")String string);
	
}
