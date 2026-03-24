# 豫途旅游服务平台（Yutu Travel Platform）

前后端分离毕业设计项目，三角色统一平台：
- 普通用户（USER）
- 商家（MERCHANT）
- 管理员（ADMIN）

后端：Spring Boot + Spring Security + JWT + MyBatis-Plus + MySQL + Redis  
前端：Vue3 + Vite + Element Plus + Axios + Pinia + ECharts

## 1. 项目结构

```text
project
├─ yutu-admin                # 后端
│  ├─ src/main/java/com/yutu
│  │  ├─ common              # 公共配置/异常/响应/安全
│  │  └─ modules             # 业务模块（auth/home/route/order/contract/complaint/merchant/admin/user）
│  └─ src/main/resources
│     ├─ application.yml
│     ├─ application-dev.yml
│     └─ sql/init.sql
├─ yutu-web                  # 前端
│  ├─ src/api
│  ├─ src/router
│  ├─ src/stores
│  ├─ src/layouts
│  └─ src/views
└─ README.md
```

## 2. 环境要求

- JDK 8（必须是 JDK，不是 JRE）
- Maven 3.8+
- MySQL 8.x
- Redis 6/7
- Node.js 20+

> 当前机器已验证：Node/NPM 可用；后端编译前请先正确配置 `JAVA_HOME` 指向 JDK8。

## 3. 数据库初始化

1. 创建数据库并导入脚本：  
   执行 [`yutu-admin/src/main/resources/sql/init.sql`](./yutu-admin/src/main/resources/sql/init.sql)
2. 默认数据库名：`yutu_travel`
3. 默认配置（可改）：
   - MySQL：`127.0.0.1:3306`
   - Redis：`127.0.0.1:6379`

## 4. 后端启动

1. 修改配置文件（按你的本机环境）：
   - [`yutu-admin/src/main/resources/application.yml`](./yutu-admin/src/main/resources/application.yml)
2. 启动命令：

```bash
cd yutu-admin
mvn spring-boot:run
```

默认端口：`8080`

## 5. 前端启动

```bash
cd yutu-web
npm install
npm run dev
```

默认端口：`5173`，已配置代理 `/api -> http://127.0.0.1:8080`

## 6. 测试账号

- 用户：`user01 / 123456`
- 商家：`merchant01 / 123456`
- 管理员：`admin01 / 123456`

> 首次登录时若数据库中为明文密码，后端会自动升级为 BCrypt。

## 7. 默认访问路径

- 登录页：`/login`
- 用户端：`/`
- 商家端：`/merchant`
- 管理端：`/admin`

## 8. 已实现内容（V1）

- 统一认证与鉴权：
  - `POST /api/auth/login`
  - `POST /api/auth/register`
  - `GET /api/auth/me`
  - JWT + Redis 会话校验
  - RBAC（菜单 + 接口权限码）

- 用户主链路：
  - 浏览路线 -> 收藏 -> 下单 -> 模拟支付 -> 自动生成合同 -> 签署合同 -> 发起投诉

- 商家端：
  - 店铺维护、路线管理、订单查看、合同附件补充、投诉回复、经营统计

- 管理端：
  - 用户管理、商家审核、分类标签管理、路线审核、合同模板管理、投诉闭环处理、数据大屏

## 9. 演示主线建议

1. 用 `user01` 登录，浏览路线并下单支付
2. 查看并签署合同
3. 发起投诉
4. 切换 `merchant01` 回复投诉
5. 切换 `admin01` 受理/判定/完成投诉
6. 回到 `user01` 查看投诉处理结果

## 10. 说明

- 本版本优先保障“可运行 + 可演示 + 业务闭环”，符合毕设展示需求。
- 支付采用 mock；电子签署采用点击签署+时间记录。
- 若需二期扩展，可在当前分层结构上继续细化 DTO/VO、分页查询和单元测试。
