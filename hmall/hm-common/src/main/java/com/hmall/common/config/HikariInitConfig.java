package com.hmall.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.DispatcherServlet;

import javax.sql.DataSource;
import java.sql.Connection;


@Component
@ConditionalOnClass(DispatcherServlet.class)
@RequiredArgsConstructor
public class HikariInitConfig implements CommandLineRunner {

    private final DataSource dataSource;

    @Override
    public void run(String... args) throws Exception {
        //  会立即触发 HikariPool 初始化，而不是等待第一次请求
        try (Connection connection = dataSource.getConnection()) {
            System.out.println("✅ HikariCP 连接池已预热！");
        }

        //Try-with-Resources 语法（JDK 7+ 特性）：
        //要求资源对象（如 Connection）实现 AutoCloseable 接口。
        //编译器会自动插入 finally 块并调用 close()，确保资源释放。

        //连接池行为：
        //dataSource.getConnection() 从 HikariCP 池中借出连接。
        //connection.close() 实际将连接标记为空闲，归还给连接池。

        //写法	                代码示例	                                                                        缺点
        //Try-with-Resources	try (Connection c = dataSource.getConnection()) {}	                            无
        //传统 finally 关闭	    try { Connection c = dataSource.getConnection(); } finally { c.close(); }	    冗长，易漏写 close()
    }
}
