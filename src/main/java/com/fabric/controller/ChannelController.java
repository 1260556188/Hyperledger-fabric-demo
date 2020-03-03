package com.fabric.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fabric.common.CommonResult;
import com.fabric.service.impl.ChannelService;

@RestController
@RequestMapping("/channel")
public class ChannelController {

	@Autowired
	ChannelService channelService;
	
	/**
	 * 
	 * @param session
	 * @param channel_name
	 * @return
	 */
	@RequestMapping(value="/create",method=RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> create(HttpSession session,
			@RequestParam(value = "channel", defaultValue = "mychannel") String channel_name) {

		Map<String,Object> map = new HashMap<String, Object>();
		String username = (String) session.getAttribute("username");
		String orgname = (String) session.getAttribute("orgname");
		if(StringUtils.isEmpty(username)) {
			map.put("code", 303);
			map.put("status", false);
			map.put("msg", "您还未登录，请先登录！");
			return map;
		}
		if(StringUtils.isEmpty(orgname)) {
			map.put("code", 303);
			map.put("status", false);
			map.put("msg", "您还未登录，请先登录！");
			return map;
		}

		CommonResult result = channelService.constructChannel(channel_name, orgname);
		map.put("code", result.getCode());
		map.put("status", true);
		map.put("msg", result.getMessage());
		return map;
	}

	/**
	 * 节点加入通道控制器
	 * @param session
	 * @param channel_name
	 * @return
	 */
	@RequestMapping(value="/join",method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> join(HttpSession session,
			@RequestParam(value = "channel", defaultValue = "mychannel") String channel_name) {
		Map<String,Object> map = new HashMap<String, Object>();
		String username = (String) session.getAttribute("username");
		String orgname = (String) session.getAttribute("orgname");
		if(StringUtils.isEmpty(username)) {
			map.put("code", 303);
			map.put("status", false);
			map.put("msg", "您还未登录，请先登录！");
			return map;
		}
		if(StringUtils.isEmpty(orgname)) {
			map.put("code", 303);
			map.put("status", false);
			map.put("msg", "您还未登录，请先登录！");
			return map;
		}
		CommonResult result = channelService.joinChannel(channel_name, orgname);
		map.put("code", result.getCode());
		map.put("status", true);
		map.put("msg", result.getMessage());
		return map;
	}
}
