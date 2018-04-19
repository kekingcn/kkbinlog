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
export const getErrorClientList=params=>{
  return axios.get(`${backendUrl}client/listErr`,{params:params})
}
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
export const  deleteClient=params=>{
  return axios.post(`${backendUrl}client/delete`,params).then(res=>res.data)
}
/**
 * 获取日志文件的所处状态
 * @param params
 * @returns {AxiosPromise<any>}
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
  return axios.get(`${backendUrl}client/getqueuesize`, {params: params})
}

export const enqueueAgainOrDelete=params=> {
  return axios.get(`${backendUrl}client/enqueueAgainOrDelete`, {params: params})
}
