package com.fabric.utils;

public class test {

	public static void main(String[] args) {
		String str = "order";
		String[] split = str.split(",");
		System.out.println(split[0]);
		
		String path = "/home/ycy/workspace/fabric-samples/chaincode/src/github.com/fabcar/fabcar.go";
		int indexOf = path.lastIndexOf("github.com");
		int indexOf2 = path.lastIndexOf("/");
		int indexOf3 = path.lastIndexOf("src/github.com");
		System.out.println(path.length());
		System.out.println();
		System.out.println(indexOf+" "+indexOf2+" "+indexOf3);
		System.out.println(path.substring(0, indexOf3));
		System.out.println(path.substring(indexOf, indexOf2));
	}
	
}
