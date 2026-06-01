# 电商管理后台 - 启动说明

## 环境要求
- Java 17+
- Maven 3.6+
- Node.js 18+
- MySQL 8（默认端口 3308，账号 root/123456）

## 第一步：初始化数据库
打开 MySQL 客户端，执行：
```bash
mysql -uroot -p < sql/init_all.sql
```
如果端口不是默认 3306，用：
```bash
mysql -uroot -p -P3308 < sql/init_all.sql
```

## 第二步：启动后端
```bash
mvn spring-boot:run
```
后端运行在 http://localhost:8080

## 第三步：启动前端
```bash
cd admin-frontend
npm install
npm run dev
```
前端运行在 http://localhost:5173 或 http://localhost:3000

## 第四步：登录
- 地址：http://localhost:3000
- 超级管理员：admin / admin123

## 端口/数据库配置
如需修改，编辑 `src/main/resources/application.yml`：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3308/shopping?...
    username: root
    password: 123456
```

## 角色体系
| 角色 | 登录后首页 | 核心功能 |
|------|-----------|---------|
| 超级管理员 | 运营总览 | 全部权限 |
| 运营管理员 | 商户管理 | 商户/商品/营销审核 |
| 客服 | 售后管理 | 售后/纠纷/客服消息 |
| 审计员 | 数据报表 | 报表/日志审计 |
| 风控专员 | 异常订单 | 风控/异常/商户信用 |
| 财务专员 | 订单管理 | 退款/结算/财务数据 |
| 内容审核员 | 商品管理 | 商品/评论/内容审核 |
