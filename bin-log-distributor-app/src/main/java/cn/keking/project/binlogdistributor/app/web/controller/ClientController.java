package cn.keking.project.binlogdistributor.app.web.controller;

import cn.keking.project.binlogdistributor.app.service.ClientService;
import cn.keking.project.binlogdistributor.app.util.Result;
import cn.keking.project.binlogdistributor.param.model.ClientInfo;
import cn.keking.project.binlogdistributor.param.model.dto.EventBaseDTO;
import cn.keking.project.binlogdistributor.param.model.dto.EventBaseErrDTO;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import javafx.geometry.Pos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.springframework.web.bind.annotation.RequestMethod.*;

/**
 * @author zhenhui
 * @date Created in 2018/16/01/2018/3:52 PM
 * @modified by
 */
@RestController
@RequestMapping("/client")
public class ClientController {
    private static final Logger log = LoggerFactory.getLogger(ClientController.class);

    @Autowired
    ClientService clientService;

    @RequestMapping(value = "/add", method = POST)
    public Result add(@RequestBody String data) {
        ClientInfo clientInfo = JSON.parseObject(data, ClientInfo.class);
        clientService.addClient(clientInfo);
        return new Result(Result.SUCCESS, "添加成功");
    }

    @RequestMapping(value = "/addAll", method = POST)
    public Result addAll(@RequestBody String data) {
        log.info(data);
        List<ClientInfo> clientInfos = JSON.parseArray(data, ClientInfo.class);
        clientInfos.stream().forEach(clientService::addClient);
        return new Result(Result.SUCCESS, "添加成功");
    }

    /**
     * 列出正常队列
     * @return
     */
    @RequestMapping(value = "/list", method = GET)
    public Set<ClientInfo> list() {
        return clientService.listClient();
    }

    /**
     * 列出异常队列
     * @return
     */
    @RequestMapping(value = "/listErr",method = GET)
    public List<String> listErr(){
        return clientService.listErrorClient();
    }

    @RequestMapping(value = "/delete", method = POST)
    public Result delete(@RequestBody ClientInfo clientInfo) {
        clientService.deleteClient(clientInfo);
        return new Result(Result.SUCCESS, "删除成功!");
    }

    /**
     * 获取队列长度
     *
     * @return
     */
    @RequestMapping(value = "/getqueuesize", method = GET)
    public String getQueueSize(String clientName,String type,int page) {
        return clientService.getqueuesize(clientName,type,page);
    }
    /**
     * 获取日志文件状态
     *
     * @return
     */
    @RequestMapping(value = "/getlogstatus", method = GET)
    public String getLogStatus() {
        return clientService.getLogStatus();
    }

    /**
     * 错误队列重新入队
     * @param uuid uuid
     * @param type 类型：delete:删除；enqueue:重新入队
     * @return
     */
    @RequestMapping(value = "/enqueueAgainOrDelete",method =GET)
    public boolean enqueueAgain(String uuid,String dataKey,String type,String errClient){
        return clientService.enqueueOrdDlete(uuid,dataKey,type,errClient);
    }
}

