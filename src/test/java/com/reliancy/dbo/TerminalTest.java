package com.reliancy.dbo;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.Test;
public class TerminalTest {
    public static class Maps extends DBO{
        public static Field map_id=new Field("Map_id",Integer.class);
        public static Field map_name=new Field("Map_name",String.class);
        static{
            Entity.publish(Maps.class);
        }
    }
    /**
     * Plain CRUD
     * @throws IOException
     * @throws SQLException
     */
    @Test
    public void connection() throws IOException, SQLException{
        String url="jdbc:postgresql://postgres:Ramudin99@bigbang:5432/Test";
        SQLTerminal t=new SQLTerminal(url);
        try(Connection c=t.getConnection()){
            System.out.println("Connection:"+c);
            try (Statement stmt = c.createStatement()) {
                // use stmt here
                String sql = "SELECT * from \"dbo\".\"Maps\"";
                        try (ResultSet resultSet = stmt.executeQuery(sql)) {
                            // use resultSet here
                            while (resultSet.next()) {
                                System.out.println("ROw:"+resultSet.getInt("Map_id")+":"+resultSet.getString("Map_name"));
                            }
                        }

            }
        }
    }
    
}
