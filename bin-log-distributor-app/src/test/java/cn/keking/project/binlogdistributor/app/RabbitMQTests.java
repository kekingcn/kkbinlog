package cn.keking.project.binlogdistributor.app;

import cn.keking.project.binlogdistributor.app.service.RabbitMQService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class RabbitMQTests {


	@Autowired
	private RabbitMQService rabbitMQService;

	@Test
	public void getMessageListTest() {

		rabbitMQService.getMessageList("BIN-LOG-DATA-example-service-rabbit-TABLE-sakila-actor", 1);
	}


}
