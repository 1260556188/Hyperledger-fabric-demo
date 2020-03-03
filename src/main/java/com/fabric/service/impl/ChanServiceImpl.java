package com.fabric.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fabric.dao.ChannelDao;
import com.fabric.pojo.Channel;
import com.fabric.service.ChanService;

@Service
public class ChanServiceImpl implements ChanService{

	@Autowired
	private ChannelDao channel;
	
	@Override
	public Channel getChannelInfo(String name) {
		// TODO Auto-generated method stub
		return channel.getChannelInfo(name);
	}

	@Override
	public int updateChannelInfo(String id, String string) {
		// TODO Auto-generated method stub
		return channel.updateChannelInfo(id,string);
	}

}
