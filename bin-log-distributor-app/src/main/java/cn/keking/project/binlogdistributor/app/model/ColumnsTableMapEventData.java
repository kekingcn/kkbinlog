package cn.keking.project.binlogdistributor.app.model;

import com.github.shyiko.mysql.binlog.event.TableMapEventData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author zhenhui
 * @Ddate Created in 2018/22/01/2018/3:35 PM
 * @modified by
 */
public class ColumnsTableMapEventData extends TableMapEventData {
    ArrayList<String> columnNames = new ArrayList<>();

    public ArrayList<String> getColumnNames() {
        return columnNames;
    }

    public void addColumnName(String columnName) {
        this.columnNames.add(columnName);
    }

    public ColumnsTableMapEventData() {
    }

    public ColumnsTableMapEventData(TableMapEventData tableMapEventData) {
        setTableId(tableMapEventData.getTableId());
        setTable(tableMapEventData.getTable());
        setDatabase(tableMapEventData.getDatabase());
        setColumnMetadata(tableMapEventData.getColumnMetadata());
        setColumnNullability(tableMapEventData.getColumnNullability());
        setColumnTypes(tableMapEventData.getColumnTypes());
    }


    public static boolean checkEqual(Object first,Object second){
        if (first == second) return true;
        if (!(first instanceof TableMapEventData)) return false;
        if (!(second instanceof TableMapEventData)) return false;
        TableMapEventData f = (TableMapEventData) first;
        TableMapEventData s = (TableMapEventData) second;
        return Objects.equals(f.getTableId(), s.getTableId()) &&
                Objects.equals(f.getDatabase(), s.getDatabase()) &&
                Objects.equals(f.getTable(), s.getTable()) &&
                Arrays.equals(f.getColumnMetadata(), s.getColumnMetadata()) &&
                Objects.equals(f.getColumnNullability(), s.getColumnNullability()) &&
                Arrays.equals(f.getColumnTypes(), s.getColumnTypes());
    }

}
