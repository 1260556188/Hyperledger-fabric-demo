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
import com.fabric.service.impl.ChainCodeService;

@RestController
@RequestMapping(value = "/chaincode")
public class ChaincodeController {

	@Autowired
	private ChainCodeService chaincodeService;

	/**
	 * 安装链码控制器
	 * 
	 * @param session
	 * @param channel_name
	 * @param chaincodePath
	 * @param chaincodeName
	 * @param chainCodeVersion
	 * @return
	 */
	@RequestMapping(value = "/install", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> install(HttpSession session,
			@RequestParam(value = "channel", defaultValue = "mychannel") String channel_name,
			@RequestParam(value = "codepath") String chaincodePath,
			@RequestParam(value = "codename") String chaincodeName,
			@RequestParam(value = "codeversion") String chainCodeVersion) {
		Map<String, Object> map = new HashMap<String, Object>();
		String username = (String) session.getAttribute("username");
		String orgname = (String) session.getAttribute("orgname");
		if (StringUtils.isEmpty(username)) {
			map.put("code", 303);
			map.put("status", false);
			map.put("msg", "您还未登录，请先登录！");
			return map;
		}
		if (StringUtils.isEmpty(orgname)) {
			map.put("code", 303);
			map.put("status", false);
			map.put("msg", "您还未登录，请先登录！");
			return map;
		}
		if (StringUtils.isEmpty(chaincodePath)) {
			map.put("code", 305);
			map.put("status", false);
			map.put("msg", "您未选择安装链码！");
			return map;
		}
		if (StringUtils.isEmpty(chaincodeName)) {
			map.put("code", 305);
			map.put("status", false);
			map.put("msg", "您未输入安装链码名称！");
			return map;
		}
		if (StringUtils.isEmpty(chainCodeVersion)) {
			map.put("code", 305);
			map.put("status", false);
			map.put("msg", "您未输入安装链码版本！");
			return map;
		}

		CommonResult result = chaincodeService.installChaincode(orgname, channel_name, chaincodePath, chaincodeName,
				chainCodeVersion);
		map.put("code", result.getCode());
		map.put("status", true);
		map.put("msg", result.getMessage());
		return map;
	}

	
	/**
	 * 实例化链码
	 * @param session
	 * @param channel_name
	 * @param peerWithOrgs
	 * @return
	 */
	@RequestMapping(value="/instantiate",method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> instantiate(HttpSession session,
			@RequestParam(value = "channel", defaultValue = "mychannel") String channel_name,
			@RequestParam(value = "orgs") String peerWithOrgs) {
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
		if(StringUtils.isEmpty(peerWithOrgs)) {
			map.put("code", 305);
			map.put("status", false);
			map.put("msg", "您未指定要实例化链码的组织！");
			return map;
		}
		
		//检测组织是否有效
		
		String[] orgs = peerWithOrgs.replace(" ", "").split(",");
		CommonResult result = chaincodeService.instantiateChaincode(orgname, orgs, channel_name);
		map.put("code", result.getCode());
		map.put("status", true);
		map.put("msg", result.getMessage());
		return map;
	}
	
	/**
	 * 调用链码
	 */
	
	
	
	/**
	 * 查询链码
	 */
	
	@RequestMapping(value="/querychaincode",method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> join(HttpSession session,
			@RequestParam(value = "channel", defaultValue = "mychannel") String channel_name,
			@RequestParam(value = "func") String chaincodeFunction,
			@RequestParam(value = "args") String chaincodeArgs) {//如有多个参数 逗号隔开封装成字符串
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
		if(StringUtils.isEmpty(chaincodeFunction)) {
			map.put("code", 305);
			map.put("status", false);
			map.put("msg", "您未指定查询链码功能！");
			return map;
		}
		
		String[] args = chaincodeArgs.split(",");
		CommonResult result = chaincodeService.queryChainCode(orgname, channel_name, chaincodeFunction, args);
		map.put("code", result.getCode());
		map.put("status", true);
		map.put("msg", result.getMessage());
		map.put("data",result.getData());
		return map;
	}
	
	/**
	 * 更新链码
	 */
	@RequestMapping(value = "/upgrade", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> upgrade(HttpSession session,
			@RequestParam(value = "channel", defaultValue = "mychannel") String channel_name,
			@RequestParam(value = "orgs") String peerWithOrgs,
			@RequestParam(value = "codepath") String chaincodePath,
			@RequestParam(value = "codename") String chaincodeName,
			@RequestParam(value = "codeversion") String chainCodeVersion) {//peerWithOrgs
		Map<String, Object> map = new HashMap<String, Object>();
		String username = (String) session.getAttribute("username");
		String orgname = (String) session.getAttribute("orgname");
		if (StringUtils.isEmpty(username)) {
			map.put("code", 303);
			map.put("status", false);
			map.put("msg", "您还未登录，请先登录！");
			return map;
		}
		if (StringUtils.isEmpty(orgname)) {
			map.put("code", 303);
			map.put("status", false);
			map.put("msg", "您还未登录，请先登录！");
			return map;
		}
		if (StringUtils.isEmpty(chaincodePath)) {
			map.put("code", 305);
			map.put("status", false);
			map.put("msg", "您未选择安装链码！");
			return map;
		}
		if (StringUtils.isEmpty(chaincodeName)) {
			map.put("code", 305);
			map.put("status", false);
			map.put("msg", "您未输入安装链码名称！");
			return map;
		}
		if (StringUtils.isEmpty(chainCodeVersion)) {
			map.put("code", 305);
			map.put("status", false);
			map.put("msg", "您未输入安装链码版本！");
			return map;
		}
		if (StringUtils.isEmpty(peerWithOrgs)) {
			map.put("code", 305);
			map.put("status", false);
			map.put("msg", "您未输入需要更新智能合约的组织！");
			return map;
		}
		String[] orgs = peerWithOrgs.replace(" ", "").split(",");
		CommonResult result = chaincodeService.updateChaincode(orgname, orgs, channel_name, chaincodePath, chaincodeName, chainCodeVersion);;
		map.put("code", result.getCode());
		map.put("status", true);
		map.put("msg", result.getMessage());
		return map;
	}
	
}
