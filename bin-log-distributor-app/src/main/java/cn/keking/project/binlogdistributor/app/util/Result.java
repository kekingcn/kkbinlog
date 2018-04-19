package cn.keking.project.binlogdistributor.app.util;

public class Result {
    public static final String SUCCESS = "success";
    public static final String ERROR = "error";

    private String code;
    private String msg;

    public Result(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
