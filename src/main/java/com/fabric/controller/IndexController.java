package com.fabric.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IndexController {

	@RequestMapping(value="/index",method = RequestMethod.GET)
	public Map<String,Object> getData(HttpServletRequest request){
//		System.out.println(request);
//		System.out.println(request.getParameter("username"));
//		System.out.println(request.getParameter("password"));
//		System.out.println(request.getParameter("sex"));
//		System.out.println(request.getParameter("data"));
		Map<String,Object>  modelMap = new HashMap<>();
		modelMap.put("key1", "value1");
		modelMap.put("key2", "value2");
		return modelMap;	
	}


}