package com.fabric.service;

import com.fabric.pojo.Channel;

public interface ChanService {

	Channel getChannelInfo(String name);

	int updateChannelInfo(String id, String string);
	
}
