package my.netty.rpc.services.impl;

import my.netty.rpc.services.JdbcPersonManage;
import my.netty.rpc.services.pojo.Person;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class JdbcPersonManageImpl implements JdbcPersonManage {

    private DataSource dataSource;

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private String toString(Date date) {
        if(date == null) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(date);
    }

    @Transactional
    @Override
    public int save(Person p) {
        // your business logic code here!
        System.out.println("jdbc Person data[" + p + "] has save!");
        System.out.println(p);
        String sql = "insert into person(id, name, age, birthday) values(?, ?, ?, ?)"; // Java的Date可直接与mysql里的DATE对上
        System.out.println(sql);
        JdbcTemplate template = new JdbcTemplate(this.dataSource);
        template.update(sql, p.getId(), p.getName(), p.getAge(), toString(p.getBirthday()));

        return 0;
    }

    @Override
    public void query(Person p) {
        // your business logic code here!
        System.out.println("jdbc Person data[" + p + "] has query!");
        String sql = String.format("select * from person where id = %d", p.getId());
        JdbcTemplate template = new JdbcTemplate(this.dataSource);
        List<Map<String, Object>> rows = template.queryForList(sql);

        if(rows.size() == 0) {
            System.out.println("record doesn't exist!");
            return ;
        } else {
            for(Map row : rows) {
                System.out.println(Integer.parseInt(row.get("ID").toString()));
                System.out.println((String) row.get("NAME"));
                System.out.println(Integer.parseInt(row.get("AGE").toString()));
                System.out.println(toString((Date)row.get("BIRTHDAY")));
                System.out.println("\n");
            }
        }
    }

    @Override
    public List<Person> query() {
        // your business logic code here!
        System.out.println("jdbc Person query!");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String sql = "select * from person";
        JdbcTemplate template = new JdbcTemplate(this.dataSource);
        List<Map<String, Object>> rows = template.queryForList(sql);
        List<Person> list = new ArrayList<>();

        for(Map row : rows) {
            Person p = new Person();
            p.setId(Integer.parseInt(row.get("ID").toString()));
            p.setName((String) row.get("NAME"));
            p.setAge(Integer.parseInt(row.get("AGE").toString()));
            p.setBirthday((Date) row.get("BIRTHDAY"));
            list.add(p);
        }

        return list;
    }
}
