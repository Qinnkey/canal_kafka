package com.huisou.config;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
/**
 * springboot启动的时候用来启动canal
 * @author Administrator
 * @Date 2017年10月18日 上午9:13:13
 *
 */
@Component
public class StartRunner implements ApplicationRunner {
	@Autowired
	private  CanalConfig canal;
	private static final Logger logger = LoggerFactory.getLogger(StartRunner.class);
	@Override
	public void run(ApplicationArguments args) throws Exception {
		if (logger.isInfoEnabled()) {
			logger.info("start into ======canal");
		}
		canal.start();
		
	}
}
