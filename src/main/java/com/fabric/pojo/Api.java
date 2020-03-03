package com.fabric.pojo;

public class Api {

	public Integer id;
	public String name;
	public String pwd;
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getPwd() {
		return pwd;
	}
	public void setPwd(String pwd) {
		this.pwd = pwd;
	}
	@Override
	public String toString() {
		return "Api [id=" + id + ", name=" + name + ", pwd=" + pwd + "]";
	}
	
}
