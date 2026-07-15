package com.zhian;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

/**
 * 启动程序
 * 
 * @author zhian
 */
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
@EnableScheduling
public class ZhianApplication
{
    public static void main(String[] args)
    {
        // 手动指定系统默认时区
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        SpringApplication.run(ZhianApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  消安网关启动成功   ლ(´ڡ`ლ)ﾞ  \n" +
                " ''-'   `'-'    `-..-'              ");
    }
}
