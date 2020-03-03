package com.fabric.component;

import org.springframework.stereotype.Component;

import com.fabric.pojo.User;


@Component
public class HostHolder {
    private static ThreadLocal<User> users = new ThreadLocal<User>();

    public void setUser(User user){
        users.set(user);
    }

    public User getUser(){
        return users.get();
    }

    public void clear(){
        users.remove();
    }
}
