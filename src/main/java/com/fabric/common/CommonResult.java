package com.fabric.common;


/**
 *
 * 统一响应结构
 */
public class CommonResult {
    /**
     * 响应状态码
     */
    private long code;
    /**
     * 响应内容
     */
    private String message;
    /**
     * 响应数据
     */
    private Object data;

    public CommonResult(){

    }

    public CommonResult(long code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static  CommonResult success(Object data){
        return new CommonResult(ResultCode.SUCCESS.getCode(),ResultCode.SUCCESS.getMessage(),data);
    }

    public static CommonResult success(String message){
        return new CommonResult(ResultCode.SUCCESS.getCode(),message,null);
    }

    public static CommonResult success(String message,Object data){
        return new CommonResult(ResultCode.SUCCESS.getCode(),message,data);
    }

    public static  CommonResult failed(Object data){
        return new CommonResult(ResultCode.FAILED.getCode(),ResultCode.FAILED.getMessage(),data);
    }

    /**
     * 失败返回结果
     */
    public static  CommonResult failed() {
        return new CommonResult(ResultCode.FAILED.getCode(),ResultCode.FAILED.getMessage(),null);
    }

    public static CommonResult failed(String message){
        return new CommonResult(ResultCode.FAILED.getCode(),message,null);
    }

    public static CommonResult failed(String message,Object data){
        return new CommonResult(ResultCode.FAILED.getCode(),message,data);
    }

    
    public long getCode() {
        return code;
    }

    public void setCode(long code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "CommonResult{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", data=" + data +
                '}';
    }
}
