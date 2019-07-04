package cn.keking.project.binlogdistributor.app;

import cn.keking.project.binlogdistributor.param.model.dto.UpdateRowsDTO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@EnableKafka
public class BinLogDistributorAppApplicationTests {

	Logger logger = LoggerFactory.getLogger(getClass());

	private final static String TOPICS = "kl-bin-log666";

	@Autowired
	private KafkaTemplate template;

	@Test
	public void kafkaCreateTopic(){

	}
	@Test
	public void prod() throws Exception {
		UpdateRowsDTO dto = new UpdateRowsDTO();
		dto.setDatabase("abc");
		dto.setTable("a");
		dto.setUuid("123");

		org.springframework.util.concurrent.ListenableFuture<SendResult> future = template.send(TOPICS,6, dto);
		future.addCallback((success) -> {
			logger.info("success:" + success.getRecordMetadata().offset());
		}, (failure) -> {
			logger.info("failure:", failure.getCause());
		});
		System.in.read();
	}

}
