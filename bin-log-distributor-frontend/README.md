# bin-log-distributor-frontend

> 二进制日志分发前端

## Build Setup

``` bash
# install dependencies
npm install

# serve with hot reload at localhost:8080
npm run dev

# build for production with minification
npm run build

# build for production and view the bundle analyzer report
npm run build --report
```

## 功能简介

>前端项目基于vue+elementUI，前后端交互axios组件，如果你熟悉以上框架，上手是一件十分轻松的事。

### 菜单介绍
1、应用列表：列出应用（client）所对应库、表的相关操作（写，改，删等），你可以删除该条记录。

2、新增应用：你当然也可以新增一个应用与其对应的库、表、事件等。

3、队列监控：队列监控分为正常队列和异常队列，正常队列tab仅仅显示redis中的队列元素；异常队列tab提供显示异常队列中的元素，除此之外
， 还提供重新入队（如果该表的锁级别为NONE的话，还可以直接删除）的操作。

4、日志进度：显示日志文件所处的位置（队列中剩余元素的长度）
