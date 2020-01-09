package cn.keking.project.binlogdistributor.app.exception;

/**
 * @author wanglaomo
 * @since 2019/7/29
 **/
public class EtcdServerException extends RuntimeException {

    public EtcdServerException(String message) {
        super(message);
    }

    public EtcdServerException(String message, Exception cause) {
        super(message, cause);
    }
}
