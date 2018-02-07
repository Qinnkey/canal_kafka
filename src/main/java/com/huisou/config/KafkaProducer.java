package com.huisou.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import com.alibaba.otter.canal.protocol.CanalEntry.Column;
import com.alibaba.otter.canal.protocol.CanalEntry.Entry;
import com.alibaba.otter.canal.protocol.CanalEntry.EntryType;
import com.alibaba.otter.canal.protocol.CanalEntry.EventType;
import com.alibaba.otter.canal.protocol.CanalEntry.RowChange;
import com.alibaba.otter.canal.protocol.CanalEntry.RowData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huisou.common.FileUtil;
import com.huisou.common.SpringUtil;
import com.huisou.constant.ContextConstant;
/**
 * 用于多线程程处理数据，将cancal拿到的数据生产到kafka中去
 * 
 * 2.0 拉去一次，发送一次所有信息的集合到kafka中去，不是拿一个放一个。
 * @author Administrator
 * @Date 2017年10月19日 上午10:07:52
 *
 */
@SuppressWarnings("all")
public class KafkaProducer implements  Callable {
	
	private static final Logger logger = LoggerFactory.getLogger(KafkaProducer.class);
	private ObjectMapper mapper = new ObjectMapper();
	
	private static String delTopic = FileUtil.getApplicationPro("kafka.delete.topic");
	private static String updTopic = FileUtil.getApplicationPro("kafka.update.topic");
	private static String insTopic = FileUtil.getApplicationPro("kafka.insert.topic");
	
	private KafkaTemplate kafkaTemplate = SpringUtil.getBean(KafkaTemplate.class);
	private final List<Entry> entries;
	private final int start;
	private final int end;
	public KafkaProducer(List<Entry> list, int start, int end) {
		super();
		this.entries = list;
		this.start = start;
		this.end = end;
	}

	@Override
	public Object call() throws Exception {
		return run();
	}


	public  Integer run() {
		long begin = System.currentTimeMillis();
	 	try {
			if (this.entries != null && this.entries.size() > 0) {
				List<String> deleteList = new ArrayList<String>();
				List<String> insertList = new ArrayList<String>();
				List<String> updateList = new ArrayList<String>();
				for (int i = start;  i < end; i++) {
					Entry entry = this.entries.get(i);
					if(entry.getEntryType() == EntryType.TRANSACTIONBEGIN || entry.getEntryType() == EntryType.TRANSACTIONEND){
						continue;
					}
					RowChange rowChange = null;
					try {
						rowChange = rowChange.parseFrom(entry.getStoreValue());
					} catch (Exception e) {
						e.printStackTrace();
					}
					//获取事件类型
					EventType eventType = rowChange.getEventType();
					for (RowData rowData : rowChange.getRowDatasList()){
						//在elasticsearch中常用的就是删除，插入，修改
						if (eventType == EventType.DELETE) {
							String delete = delete(rowData.getBeforeColumnsList()); 
							if (StringUtils.isNotBlank(delete)) {
								deleteList.add(delete);
							}
						} else if (eventType == EventType.INSERT) {  
							String insert = insert(rowData.getAfterColumnsList());
							//kafkaTemplate.send("insert4", insert);
							if (insert !=null) {
								insertList.add(insert);
							}
						} else if (eventType == EventType.UPDATE) { //更新字段的名称
							String update = update(rowData.getAfterColumnsList());
							if (update !=null) {
								updateList.add(update);
							}
						}
					}
				}
				if (deleteList != null && deleteList.size() > 0) {
					kafkaTemplate.send(delTopic,  mapper.writeValueAsString(deleteList));
					if (logger.isInfoEnabled()) {
						logger.info("delete the number of ---" + deleteList.size());
					}
				}
				if (insertList != null && insertList.size() > 0) {
					String json = mapper.writeValueAsString(insertList);
					kafkaTemplate.send(insTopic, json);
					if (logger.isInfoEnabled()) {
						logger.info("insert the number of ----" + insertList.size());
					}
				}
				if (updateList != null && updateList.size() > 0) {
					String json = mapper.writeValueAsString(updateList);
					kafkaTemplate.send(updTopic, json);
					if (logger.isInfoEnabled()) {
						logger.info("update the number of - -" + updateList.size());
					}
				}
			}
	 	} catch (Exception e) {
	 		e.printStackTrace();
		}
	 	return null;
	}
	/**
	 * 用于插入
	 * @param afterColumnsList
	 */
	private String insert(List<Column> afterColumnsList) {
		Map<String,String> resultInset = new ConcurrentHashMap<String, String>();
		for (Column column : afterColumnsList) {
			//判断该列是否为空，值不为空格，是否修改过
			if (column.getUpdated() || column.hasIsKey()){
				if (!column.getIsNull() && StringUtils.isNotBlank(column.getValue())) {
					if (ContextConstant.getFields().contains(column.getName())) {
						resultInset.put(column.getName(), column.getValue());
					}
					
				}
			}
		}
		try {
			String json = mapper.writeValueAsString(resultInset);
			//kafkaTemplate.send("insert4", mapper.writeValueAsString(resultInset));
			resultInset = null;
			return json;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 当检测到数据库中的更新的时候，发送信息到kafka中
	 * @param afterColumnsList
	 */
	private String update(List<Column> afterColumnsList) {
		Map<String,String> resultUpsert =  new ConcurrentHashMap<String, String>();
		for (Column column : afterColumnsList) {
			if ((column.getUpdated() || column.getIsKey()) && StringUtils.isNotBlank(column.getValue()) ){
				if (ContextConstant.getFields().contains(column.getName())) {
					resultUpsert.put(column.getName(), column.getValue());
				}
			}
		}
		try {
			//kafkaTemplate.send("update4", mapper.writeValueAsString(resultUpsert));
			String json = mapper.writeValueAsString(resultUpsert);
			resultUpsert = null;
			return json;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 如果是删除的我们就将获取对应的id，然后传给ElasticSearch去删除
	 * @param beforeColumnsList
	 */
	private String delete(@NotNull List<Column> beforeColumnsList) {
		for (Column column : beforeColumnsList) {
			//判断是否是主键
			if (column.getIsKey() && column.getName().equals("id")) {
				String id = column.getValue();
				if (StringUtils.isNotBlank(id)) {
					return id;
				}
			}
		}
		return null;
	}
}
