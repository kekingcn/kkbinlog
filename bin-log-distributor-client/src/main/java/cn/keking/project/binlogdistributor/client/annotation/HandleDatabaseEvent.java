package cn.keking.project.binlogdistributor.client.annotation;

import cn.keking.project.binlogdistributor.param.enums.DatabaseEvent;
import cn.keking.project.binlogdistributor.param.enums.LockLevel;

import java.lang.annotation.*;

/**
 * @author zhenhui
 * @Ddate Created in 2018/18/01/2018/7:02 PM
 * @modified by
 */
@Documented
@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface HandleDatabaseEvent {
    String database();

    String table();

    DatabaseEvent[] events();

    LockLevel lockLevel() default LockLevel.NONE;

    String columnName() default "";
}
