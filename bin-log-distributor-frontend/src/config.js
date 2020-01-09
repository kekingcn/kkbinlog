let env = process.env.NODE_ENV

/**
 * 数据分发后端地址，binlog
 * @type {string}
 */
export const backendUrl = env === 'development' ? 'http://localhost:8885/'
  : env === 'middleground-uat' ? 'http://localhost:8885'
  : env === 'uat' ? 'http://localhost:8885'
    : env === 'production' ? 'http://localhost:8885' : '';
/**
 * 获取token地址
 * @type {string}
 */
export const apiBaseUrl = env === 'development' ? 'http://192.168.1.204:8888'
  : env === 'uat' ? 'http://192.168.1.204:8888':"http://192.168.1.204:8888";

