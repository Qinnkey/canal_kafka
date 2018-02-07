package com.huisou.config;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.common.utils.AddressUtils;
import com.alibaba.otter.canal.protocol.CanalEntry.Entry;
import com.huisou.common.FileUtil;
import com.alibaba.otter.canal.protocol.Message;
/**
 * canal核心配置，用于监听数据，并将数据写入消息队列中
 * @author Administrator
 * @Date 2017年10月17日 下午7:42:06
 *
 */
@SuppressWarnings("all")
@Component
public class CanalConfig  {
	private static final Logger logger = LoggerFactory.getLogger(CanalConfig.class);
	
	private static String zkServers = FileUtil.getApplicationPro("canalConfig.zkServers");
	private static String destination = FileUtil.getApplicationPro("canalConfig.destination");
	
	@Value(value = "${kafka.producer.thread}")
	private Integer threadNum;
	
	public synchronized  void  start() throws Exception{
		if (threadNum < 0) {
			throw new Exception("线程数必须大于0");
			
		}
		//连接zk集群
		CanalConnector connector = CanalConnectors.newClusterConnector(zkServers, destination, "", "");
//		CanalConnector connector = CanalConnectors.newClusterConnector(Arrays.asList(new InetSocketAddress(AddressUtils.getHostIp(),11111)), "example","", "");
		int batchSize = 1000;
		int emptyCount = 0;
		int count = 0;
		
		try {
			connector.connect();
			//过滤数据库的表
			connector.subscribe("dream_shop_new\\.ds_product");
			connector.rollback();
			int totalEmptryCount=1200;
			logger.info("start to enter--- canal");
			ExecutorService pool = Executors.newFixedThreadPool(threadNum);
			while (true) {
				Message message = connector.getWithoutAck(batchSize);
				long batchId = message.getId();
				int size = message.getEntries().size();
				 if (batchId == -1 || size == 0) {  
					 try {  
	                        Thread.sleep(1000);  
	                    } catch (InterruptedException e) {  
	                        e.printStackTrace();  
	                    }  
	                } else {  
	                    emptyCount = 0;  
	                    try {
	                    	//多线程处理数据
	                    	List<Entry> list = message.getEntries();
	                    	if (size > 10) {
	                    		int dealSize = size / threadNum;
	                    		int index = 0;
	                    		for (int i = 0; i < threadNum; index += dealSize) {
	                    			int start = index;
	                    			if (start >= list.size()) {
	                    				break;
	                    			}
	                    			int end = start + dealSize;
	                    			end = end > list.size() ? list.size() : end;
	                    			KafkaProducer producer = new KafkaProducer(list,start,end);
	                    			pool.submit(producer);
	                    			producer = null;
	                    		}
							}else {
								KafkaProducer dealDate = new KafkaProducer(list,0,size);
								dealDate.run();
							}
						} catch (Exception e) {
							e.printStackTrace();
							connector.rollback(batchId); // 处理失败, 回滚数据  
						}  
	                    logger.info("get the number of  canal --" + size);
	                }
	              connector.ack(batchId); // 提交确认  
	             
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			connector.disconnect();
		}
		
	}


}
