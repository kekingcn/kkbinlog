
let env = process.env.NODE_ENV

export const  backendUrl=env==='development'?'http://localhost:8886/'
  :env==='uat'?'http://192.168.204:8886'
  :env==='production'?'':''
