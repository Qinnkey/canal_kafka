package com.huisou;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;


/**
 * springboot的启动项
 * @author Administrator
 * @Date 2017年10月16日 上午9:58:10
 *
 */
@SpringBootApplication
@EnableKafka
@Configuration
public class Application {
	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(Application.class);
		//添加监听器
		application.run(args);
	}
}
