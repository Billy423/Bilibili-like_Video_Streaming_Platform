package com.example.bilibili.domain.exception;

public class ConditionException extends RuntimeException{

    private static final long serialVersionUID = 1L; // for serialization

    private String code; // response code

    public ConditionException(String code, String name){
        super(name);
        this.code = code;
    }

    public ConditionException(String name){
        super(name);
        code = "500"; // common exception code
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
