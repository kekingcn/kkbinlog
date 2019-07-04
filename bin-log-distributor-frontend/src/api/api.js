import axios from 'axios';

/**
 * 后端地址
 * @type {string}
 */

import {backendUrl} from '../config'

/**
 * 获取应用列表
 * @param params
 * @returns {AxiosPromise<any>}
 */
export const getClientList=params=>{
  return axios.get(`${backendUrl}client/list`,{params:params})
}

/**
 * 获取Redis应用列表
 * @param params
 * @returns {AxiosPromise<any>}
 */
export const getRedisClientList=params=>{
  return axios.get(`${backendUrl}client/listRedis`,{params:params})
};

/**
 * 获取Rabbit应用列表
 * @param params
 * @returns {AxiosPromise<any>}
 */
export const getRabbitClientList=params=>{
  return axios.get(`${backendUrl}client/listRabbit`,{params:params})
};

/**
 * 获取Kafka应用列表
 * @param params
 * @returns {AxiosPromise<any>}
 */
export const getKafkaClientList = params => {
  return axios.get(`${backendUrl}client/listKafka`,{params:params})
};

export const getErrorClientList=params=>{
  return axios.get(`${backendUrl}client/listErr`,{params:params})
};


/**
 * 新增一个应用
 * @param params
 * @returns {AxiosPromise<any>}
 */
export const addClient=params=>{
  return axios.post(`${backendUrl}client/add`,params)
}
/**
 * 删除一个应用
 * @param params
 * @returns {Promise.<TResult>}
 */
export const  deleteClient=params=> {
  return axios.post(`${backendUrl}client/delete`,params).then(res=>res.data)
}
/**
 * 获取日志文件的所处状态
 * @param params
 */
export const getLogStatus=params=>{
  return axios.get(`${backendUrl}client/getlogstatus`,{params:params})
}
/**
 * 获取队列长度
 * @param params
 * @returns {AxiosPromise<any>}
 */
export const getqueuesize=params=>{
  return axios.get(`${backendUrl}client/getqueuesize`, {params: params});
};

export const deleteFromQueue=params=> {
  return axios.get(`${backendUrl}client/deleteFromQueue`, {params: params});
};

export const getNamespaceList = params=>{
  return axios.get(`${backendUrl}client/namespaceList`, {params: params});
};

export const getDatasourceList = params => {
  return axios.get(`${backendUrl}datasource/list`, {params: params});
}

export const startDatasource = params => {
  return axios.post(`${backendUrl}datasource/start`, params);
}

export const stopDatasource = params => {
  return axios.post(`${backendUrl}datasource/stop`, params);
}

export const persistDatasource = params => {
  return axios.post(`${backendUrl}datasource/persist`, params);
}

export const removeDatasource = params => {
  return axios.post(`${backendUrl}datasource/remove`, params);
}
