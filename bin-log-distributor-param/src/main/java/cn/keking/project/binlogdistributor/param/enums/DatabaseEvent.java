package cn.keking.project.binlogdistributor.param.enums;

/**
 * @author zhenhui
 * @date Created in 2018/17/01/2018/4:06 PM
 * @modified by
 */
public enum DatabaseEvent {

    /**
     * 增
     */
    WRITE_ROWS,
    /**
     * 改
     */
    UPDATE_ROWS,
    /**
     * 删
     */
    DELETE_ROWS,
    /**
     * 阻塞，向下分发时使用
     */
    BLOCKING_ROWS;

}
