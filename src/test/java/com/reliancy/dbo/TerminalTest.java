package com.reliancy.dbo;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;

import com.reliancy.rec.JSON;

import org.junit.BeforeClass;
import org.junit.Test;
public class TerminalTest {
    @Entity.Info(
        name="dbo.Maps"
    )
    public static class Maps extends DBO{
        public static Field map_id=Field.Int("Map_id").setPk(true);
        public static Field map_name=Field.Str("Map_name");
        public static Field created=Field.DateTime("Created");
        public static Field active=Field.Bool("Active");
        static{
            //Entity.publish(Maps.class);
        }
    }
    @Entity.Info(
        name="public.securable"
    )
    public static class Securable extends DBO{
        public static Field id=Field.Int("id").setPk(true).setAutoIncrement(true);
        public static Field kind=Field.Str("kind");
        public static Field name=Field.Str("name");
        public static Field display_name=Field.Str("display_name");
        public static Field created=Field.DateTime("created_on");
        public static Field is_essential=Field.Bool("is_essential");
        static{
            //Entity.publish(Maps.class);
        }
    }

    @Entity.Info(
        name="public.product"
    )
    public static class Product extends Securable{
        public static Field valid_since=Field.DateTime("valid_since");
        public static Field valid_until=Field.DateTime("valid_until");
        public static Field short_info=Field.Str("short_info");
    
    }

    static SQLTerminal t;
    
    @BeforeClass
    public static void beforeAllTestMethods() {
        System.out.println("Invoked once before all test methods");
        String url="jdbc:postgresql://postgres:Ramudin99@bigbang:5432/Test";
        t=new SQLTerminal(url);
    }
 
    /**
     * jdbc connectivity
     * @throws IOException
     * @throws SQLException
     */
    @Test
    public void connection() throws IOException, SQLException{
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
    @Test
    public void simpleCRUD() throws IOException, SQLException{
        System.out.println("SimpleCRUD");
        try(Action act=t.begin().load(Maps.class).execute()){
            for(DBO o:act){
                System.out.println("DBO:"+o);
            }
        }
        Entity.retract(Maps.class);
    }
    @Test
    public void complexCRUD() throws IOException, SQLException{
        System.out.println("ComplexCRUD");
        try(Action act=t.begin().load(Product.class).execute()){
            for(DBO o:act){
                System.out.println("DBO:"+o);
            }
        }
        Product p=new Product();
        p.setStatus(DBO.Status.USED);
        Product.kind.set(p,Product.class.getSimpleName());
        Product.name.set(p,"myproduct");
        Product.created.set(p,new Date());
        Product.short_info.set(p,"a sweet melody");
        Product.display_name.set(p,"first entry");
        System.out.println("P0:"+JSON.toString(p));
        t.save(p);
        System.out.println("P1:"+JSON.toString(p));
        Product pp=t.load(Product.class, 35);
        System.out.println("Returning:"+pp);
        //t.delete(pp);
        Entity.retract(Maps.class);
    }
    
}
