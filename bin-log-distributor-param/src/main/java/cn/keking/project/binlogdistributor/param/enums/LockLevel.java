package cn.keking.project.binlogdistributor.param.enums;

/**
 * 保持顺序的级别
 * TABLE -> 同表按顺序执行
 * COLUMN -> 某列值一致的按顺序执行
 * NONE -> 无序
 *
 * @author zhenhui
 * @date Created in 2018/17/01/2018/4:06 PM
 * @modified by
 */
public enum LockLevel {

    /**
     * 表
     */
    TABLE,
    /**
     * 列
     */
    COLUMN,
    /**
     * 无序
     */
    NONE;

}
