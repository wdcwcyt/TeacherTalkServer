# TeacherTalkServer 后端计划（表结构与业务决策）

> 版本：v0.9 草案（2026-10-26）
> 状态：**部分冻结**。已确认论坛表结构、手机验证码登录注册、图片上传、带图发帖和帖子列表；需求单和消息仍暂缓。
> 用途：**表结构和业务决策**的唯一事实来源。接口路径、字段、响应和错误码统一维护在
> [`api/openapi.yaml`](./api/openapi.yaml)，客户端只读。
> 上行约束见 [`../AGENTS.md`](../AGENTS.md)。

## 1. 范围

| 模块 | 本期内容 |
| --- | --- |
| 账号 | 注册、登录、当前用户信息、修改资料 |
| 论坛 | 帖子列表（分页）、详情、发帖、点赞、收藏、评论列表、发评论 |
| 需求单 | 发布、列表筛选、详情、接单、取消、我的需求单 |
| 消息 | 会话列表（含未读）、创建会话、增量拉消息、发文字消息、上报已读 |
| 文件 | 图片上传（发帖用） |

本期不做：支付、评价、语音/图片消息、推送、WebSocket、后台管理、举报、内容审核、评论盖楼。

## 2. 通用约定

### 2.1 统一响应体

```json
{ "code": 0, "message": "ok", "data": {} }
```

- `code == 0` 成功；其余为业务错误码。
- HTTP 状态码同时正确：200 / 400 / 401 / 403 / 404 / 409 / 500。
- 错误码表：

| code | HTTP | 含义 |
| --- | --- | --- |
| 0 | 200 | 成功 |
| 400 | 400 | 参数校验失败 |
| 401 | 401 | 未登录或 token 失效 |
| 403 | 403 | 已登录但无权限 |
| 404 | 404 | 资源不存在 |
| 409 | 409 | 状态冲突（重复操作、已被接走） |
| 500 | 500 | 服务端异常 |

> 待确认：是否需要细分业务码（如 1001 手机号已注册、1002 验证码错误）。**当前建议直接复用 HTTP 语义码，够用且好讲**。

### 2.2 分页

- 请求参数：`page`（从 1 开始，默认 1）、`size`（默认 10，上限 50）。
- 返回：

```json
{ "list": [], "page": 1, "size": 10, "total": 0 }
```

### 2.3 时间与认证

- 时间格式 `yyyy-MM-dd HH:mm:ss`，时区 `Asia/Shanghai`。
- 登录后所有需要身份的请求带 `Authorization: Bearer <token>`。
- 任意接口返回 401，客户端清会话并回登录页。

### 2.4 角色

- `STUDENT`（大学生）、`PARENT`（家长）。
- 当前版本：`STUDENT` 可发需求、可接单；`PARENT` 只能发需求。
- 规则集中在一处实现，后期改为「大学生只能接单、家长只能发需求」时只改这一处。

## 3. 接口契约

接口契约统一维护在 [`api/openapi.yaml`](./api/openapi.yaml)，共有 26 个操作：

- 账号与用户：5 个。
- 论坛：9 个。
- 需求单：6 个。
- 消息：5 个。
- 文件：1 个。

每个操作必须同时声明：

- `x-contract-status`：`confirmed` 表示契约已确认，`draft` 表示仍是草案。
- `x-implementation-status`：`implemented` 表示后端代码已实现，`planned` 表示尚未实现。

客户端只能接入 `confirmed + implemented` 的操作。后端修改接口时，同一轮更新
OpenAPI；本文件不再维护接口字段副本。

### 3.1 待确认清单（大部分已暂缓）

> **2026-10-21 决定：除登录注册相关的 A1、A2、A9、C1、C2、C3 外，其余条目全部暂缓**，
> 做到对应功能时再逐条确认。暂缓不等于删除，留着是为了以后有据可查。
> **2026-10-23 决定：A1、A2、A9、C1、C2、C3 已按 3.2 节手机验证码方案确认并实现。**

**A 组：影响表结构和 DTO，必须先定**

| 编号 | 待定项 | 选项 | 我的推荐 |
| --- | --- | --- | --- |
| A1 | 注册字段校验规则（已确认） | 手机号 11 位、验证码 6 位、昵称可空（非空时 2~20）、role 二选一 | 已按 3.2 节实现 |
| A2 | `user` 对外返回哪些字段 | 全字段 / 他人信息不含手机号 | 本人含 `phone`，他人只 `id/nickname/avatarUrl` |
| A3 | 帖子的点赞/收藏/评论计数（已确认） | 查询时 `COUNT` / 表里冗余计数字段 | 冗余计数字段 |
| A4 | `demand.budget` 类型与含义 | 文本区间 / `DECIMAL(10,2)` | `DECIMAL(10,2)`，单位「元/小时」 |
| A5 | `subject` / `grade` 存储方式 | 自由文本 / 枚举码 | 枚举码（`MATH`、`JUNIOR` 等） |
| A6 | **未读数怎么存（草案缺口）** | 现表结构无法算未读 | `conversation` 加 `user_a_last_read_id`、`user_b_last_read_id` |
| A7 | **`COMPLETED` 状态怎么进入（草案缺口）** | 保留但无接口 / 加完成接口 | 加 `POST /api/demands/{id}/complete`，仅发布者可操作 |
| A8 | 消息响应是否带发送者信息 | 只带 `senderId` / 带昵称头像 | 带 `senderNickname`、`senderAvatarUrl` |
| A9 | 注册是否直接返回 token | 返回 / 不返回 | 返回（省一次登录） |

**B 组：业务规则，影响 Service 判断，不动表结构**

| 编号 | 待定项 | 我的推荐 |
| --- | --- | --- |
| B1 | 需求单取消规则 | `PENDING` 仅发布者可取消；`ACCEPTED` 双方都可取消；`accepted_by` 保留便于追溯 |
| B2 | 接单限制 | 不能接自己发的单；家长不能接单；重复接单 409；不限制一人接多单 |
| B3 | 点赞/收藏重复调用 | 幂等，返回 200 + 当前状态 |
| B4 | 帖子编辑/删除 | 本期不做，只做发布 + 浏览 + 互动 |
| B5 | 会话创建范围 | 任意两个已登录用户都能创建会话 |
| B6 | 帖子列表「只看我发的」 | 本期不做 |
| B7 | `demand.status` 筛选值 | 固定 `PENDING/ACCEPTED/COMPLETED/CANCELLED` |
| B8 | `conversation` 两人顺序 | 约定 `user_a_id < user_b_id`，唯一键防重复 |
| B9 | **收藏后在哪看（草案缺口）** | 现有接口没有「我的收藏」列表，收藏了等于找不到入口。建议补 `GET /api/posts/favorites`，客户端在我的页加入口 |

**C 组：认证、错误码、上传**

| 编号 | 待定项 | 我的推荐 |
| --- | --- | --- |
| C1 | token 有效期 | 7 天，不做 refresh token，过期重新登录 |
| C2 | 错误码粒度 | 复用 HTTP 语义码（400/401/403/404/409/500），提示文字放 `message` |
| C3 | 认证失败提示 | 验证码错误或过期统一提示「验证码错误或已过期」 |
| C4 | 上传限制（已确认） | JPG/PNG，单文件 2MB，随机文件名，返回原图和 480px 列表缩略图 |
| C5 | 头像上传 | 本期不做，`avatar_url` 字段保留但为空，客户端用默认头像 |

> **接口迁移说明（2026-10-26）：** 以下 3.2～3.6 小节保留为认证、论坛和上传功能的
> 业务背景与决策记录，不再作为接口字段的唯一来源。路径、方法、请求、响应、错误码和
> 接口状态一律以 [`api/openapi.yaml`](./api/openapi.yaml) 为准。

### 3.2 手机验证码登录与注册（已确认）

客户端已有 `UserInfoBean(id, phone, nickname, avatarUrl, role)` 和 `SessionManager`，
所以后端返回的用户结构必须与之一一对应。昵称在数据库中仍为非空字段；注册未提供昵称时，
由后端生成 `新用户 + 6 位随机字符`，例如 `新用户K7M9P2`。

短信发送和核验使用阿里云号码认证服务的 V2.0 Java SDK：
`com.aliyun:dypnsapi20170525:1.2.3`。

#### 3.2.1 发送验证码

`POST /api/auth/sms-code`

```json
{ "phone": "13800138000" }
```

成功（200）：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "expiresIn": 300,
    "resendAfter": 60
  }
}
```

- `expiresIn`：验证码有效期，单位秒。
- `resendAfter`：同一手机号允许再次发送的间隔，单位秒。
- 验证码由阿里云生成，接口响应和日志中均不得出现验证码。
- 发送接口不区分手机号是否已注册，避免通过发送结果枚举用户。

#### 3.2.2 注册

`POST /api/auth/register`

```json
{
  "phone": "13800138000",
  "code": "123456",
  "nickname": null,
  "role": "STUDENT"
}
```

- `nickname` 可传 `null`、省略或传空白字符串；后端统一生成默认昵称。
- 用户主动填写昵称时，去除首尾空白后必须为 2～20 个字符。
- `role` 只允许字符串 `STUDENT` 或 `PARENT`，客户端不得传 `0/1`。
- 注册成功后直接返回 token，客户端调用 `SessionManager.saveSession()` 进入首页。

成功（200）：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "token": "<JWT>",
    "user": {
      "id": 1,
      "phone": "13800138000",
      "nickname": "新用户K7M9P2",
      "avatarUrl": null,
      "role": "STUDENT"
    }
  }
}
```

#### 3.2.3 登录

`POST /api/auth/login`

```json
{ "phone": "13800138000", "code": "123456" }
```

成功返回结构与注册完全一致。验证码核验通过但手机号没有用户时返回 404。

#### 3.2.4 自动登录校验

`GET /api/auth/me`

请求头：`Authorization: Bearer <token>`

成功（200）：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "id": 1,
    "phone": "13800138000",
    "nickname": "新用户K7M9P2",
    "avatarUrl": null,
    "role": "STUDENT"
  }
}
```

客户端启动时用它校验 token；返回 401 就清会话并回登录页。

#### 3.2.5 验证码与 JWT 规则

- 验证码为 6 位数字，有效期 300 秒，同一手机号发送间隔 60 秒。
- 重复发送时新验证码覆盖旧验证码。
- 阿里云核验结果必须同时满足 `Success=true`、`Code=OK`、
  `Model.VerifyResult=PASS`，接口返回成功本身不能视为验证码通过。
- JWT 使用 HS256，只包含 `userId`（`sub`）、`role` 和签发/过期时间。
- JWT 有效期为 7 天，不做 refresh token；过期后重新获取验证码登录。
- JWT 密钥只通过 `JWT_SECRET` 环境变量注入，至少 32 字节。

#### 3.2.6 本批次字段规则

| 字段 | 规则 | 说明 |
| --- | --- | --- |
| `phone` | 11 位，`^1[3-9]\d{9}$`，全库唯一 | 登录账号 |
| `code` | 6 位数字 | 不使用密码 |
| `nickname` | 可选；非空时 2～20 个字符，允许重复 | 为空时生成默认昵称 |
| `role` | 只允许 `STUDENT` / `PARENT` | 字符串枚举 |
| `avatarUrl` | 本批次恒为 `null` | 头像上传本期不做 |

本批次错误码：

| 场景 | HTTP | code | message |
| --- | --- | --- | --- |
| 手机号、验证码或角色格式不合法 | 400 | 400 | 具体哪个字段不合法 |
| 验证码错误或已过期 | 401 | 401 | 验证码错误或已过期 |
| token 缺失、无效或过期 | 401 | 401 | 登录已过期，请重新登录 |
| 登录时验证码通过但手机号未注册 | 404 | 404 | 手机号未注册，请先注册 |
| 注册手机号已存在 | 409 | 409 | 该手机号已注册 |
| 阿里云短信服务调用失败 | 500 | 500 | 服务暂时不可用，请稍后重试 |

#### 3.2.7 退出登录

**本期不需要后端接口**。客户端 `SessionManager.clearSession()` 清掉本地 token 即可。
以后要做「踢下线 / token 黑名单」时再加。

### 3.3 论坛列表（已确认）

`GET /api/posts?page=1&size=20`

- 需要登录，后端从 JWT 获取当前用户，不接收客户端传入的 `userId`。
- 安卓端固定传 `size=20`；请求未提供 `size` 时沿用通用默认值 10，上限仍为 50。
- 帖子按 `created_at DESC, id DESC` 排序，保证同一时间的帖子顺序稳定。
- `contentPreview` 是服务端截取的正文预览，长度为前 200 个字符；完整正文只在详情接口返回。
- `thumbnailUrls` 按 `sort_order` 升序返回最多三张缩略图，没有图片时为空数组。
- `liked` / `favorited` 表示当前登录用户对该帖子的状态，不是 `post` 表字段。

成功（200）：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "list": [
      {
        "id": 1001,
        "title": "",
        "contentPreview": "这篇文章主要记录……",
        "authorId": 8,
        "authorNickname": "小明",
        "authorAvatarUrl": null,
        "thumbnailUrls": [
          "/uploads/6c7319f1024f4cbf86de40a7ed34f28e_thumb.jpg"
        ],
        "likeCount": 12,
        "favoriteCount": 5,
        "commentCount": 3,
        "liked": false,
        "favorited": true,
        "createdAt": "2026-10-22 14:30:00"
      }
    ],
    "page": 1,
    "size": 20,
    "total": 43
  }
}
```

### 3.4 发布帖子（已实现）

`POST /api/posts`

标题可选，正文必填。图片地址必须来自 `/api/files`，最多 5 张，且不能重复。

请求：

```json
{
  "title": "",
  "content": "这是帖子正文。",
  "imageUrls": [
    "/uploads/6c7319f1024f4cbf86de40a7ed34f28e.jpg"
  ]
}
```

成功（200）：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "id": 1,
    "title": "",
    "content": "这是帖子正文。",
    "imageUrls": [
      "/uploads/6c7319f1024f4cbf86de40a7ed34f28e.jpg"
    ],
    "authorId": 1,
    "likeCount": 0,
    "favoriteCount": 0,
    "commentCount": 0,
    "createdAt": "2026-10-22 16:00:00",
    "updatedAt": "2026-10-22 16:00:00"
  }
}
```

字段规则：

| 字段 | 规则 |
| --- | --- |
| `title` | 可选，去除首尾空白后最多 100 个字符；空值保存为 `""` |
| `content` | 必填，去除首尾空白后 1～5000 个字符 |
| `authorId` | 服务端从当前用户上下文取得，不接受客户端传入 |
| `likeCount` / `favoriteCount` / `commentCount` | 发布时由服务端初始化为 0 |
| `createdAt` / `updatedAt` | 服务端写入，发布时取相同值 |

本批次错误码：

| 场景 | HTTP | code | message |
| --- | --- | --- | --- |
| 正文为空、标题超长 | 400 | 400 | 具体哪个字段不合法 |
| 本地开发用户未配置或数据库中不存在 | 401 | 401 | 登录已过期，请重新登录 |
| 数据库写入失败 | 500 | 500 | 服务暂时不可用，请稍后重试 |

帖子和图片分别写入 `post`、`post_image`，两者在同一事务中完成。

### 3.5 图片上传（已确认）

`POST /api/files`

- 需要登录，请求头必须带 `Authorization: Bearer <token>`。
- 请求类型为 `multipart/form-data`，字段名固定为 `file`。
- 只接收 JPG、PNG；文件大小不超过 2MB，图片单边不超过 4096px。
- 服务端根据文件头判断真实类型，不信任原始文件名和客户端声明的 Content-Type。
- 文件保存到 `uploads/`，使用随机文件名；原图和缩略图都通过 `/uploads/**` 访问。
- 缩略图最长边不超过 480px、JPEG 格式，文件名为 `原文件标识_thumb.jpg`。

成功（200）：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "url": "/uploads/6c7319f1024f4cbf86de40a7ed34f28e.jpg",
    "thumbnailUrl": "/uploads/6c7319f1024f4cbf86de40a7ed34f28e_thumb.jpg"
  }
}
```

本批次错误码：

| 场景 | HTTP | code | message |
| --- | --- | --- | --- |
| 未选择文件或文件为空 | 400 | 400 | 请选择要上传的图片 |
| 文件超过 2MB | 400 | 400 | 图片不能超过 2MB |
| 类型不是 JPG/PNG 或图片损坏 | 400 | 400 | 只允许上传 JPG 或 PNG 图片 / 图片文件无法解析 |
| 图片单边超过 4096px | 400 | 400 | 图片单边不能超过 4096px |
| token 缺失或过期 | 401 | 401 | 登录已过期，请重新登录 |
| 文件写入失败 | 500 | 500 | 服务暂时不可用，请稍后重试 |

### 3.6 其余接口

论坛详情、点赞、收藏、评论以及需求单、消息接口仍按第 3 节草案实现，
做到对应功能时再逐条确认请求和异常细节。

## 4. 数据库结构（账号与论坛已确认）

### 4.1 基础表：`user`

用户表是账号和论坛共同依赖的基础表。新建环境依次执行
[`V1__init.sql`](../src/main/resources/db/V1__init.sql) 和
[`V2__sms_auth.sql`](../src/main/resources/db/V2__sms_auth.sql)；下方展示的是执行迁移后的目标结构。

```sql
CREATE DATABASE IF NOT EXISTS teachertalk
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE teachertalk;

CREATE TABLE IF NOT EXISTS `user` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户主键',
    `phone`         VARCHAR(11)     NOT NULL                COMMENT '手机号，登录账号，全库唯一',
    `password_hash` VARCHAR(60)     DEFAULT NULL            COMMENT 'BCrypt 密码哈希；验证码用户为 NULL',
    `nickname`      VARCHAR(20)     NOT NULL                COMMENT '昵称',
    `avatar_url`    VARCHAR(255)    DEFAULT NULL            COMMENT '头像相对地址，本期为 NULL',
    `role`          VARCHAR(16)     NOT NULL                COMMENT 'STUDENT 大学生 / PARENT 家长',
    `created_at`    DATETIME        NOT NULL                COMMENT '注册时间',
    `updated_at`    DATETIME        NOT NULL                COMMENT '最后修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_phone` (`phone`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表';
```

字段说明：

| 字段 | 存什么 | 为什么不这样存 |
| --- | --- | --- |
| `id` | 用户唯一编号，帖子、需求单、消息都用它关联 | 不用手机号当主键，手机号将来可能更换 |
| `phone` | 登录账号 | 唯一索引防止两个账号同号 |
| `password_hash` | BCrypt 哈希；验证码用户为 `NULL` | 保留字段供历史数据使用，验证码用户不创建密码 |
| `nickname` | 展示名；注册未填写时为 `新用户+6位随机字符` | 允许重复，不做唯一约束 |
| `avatar_url` | 头像地址 | 本期不做上传，先留空，免得以后加字段要改表 |
| `role` | `STUDENT` / `PARENT` | 存字符串，导出数据时一眼能看懂，便于画 ER 图 |
| `created_at` / `updated_at` | 注册时间 / 修改时间 | 由程序写入，格式统一 `yyyy-MM-dd HH:mm:ss` |

### 4.2 论坛表（已确认）

论坛使用 5 张表。关联字段在数据库层建立索引，暂不添加物理外键；
帖子、点赞、收藏、评论之间的有效性由 Service 在同一事务中校验。

```sql
CREATE TABLE IF NOT EXISTS `post` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '帖子主键',
    `user_id`        BIGINT UNSIGNED NOT NULL                COMMENT '发布者用户 ID',
    `title`          VARCHAR(100)    NOT NULL                COMMENT '帖子标题',
    `content`        TEXT            NOT NULL                COMMENT '帖子完整正文',
    `like_count`     INT UNSIGNED    NOT NULL DEFAULT 0      COMMENT '点赞数',
    `favorite_count` INT UNSIGNED    NOT NULL DEFAULT 0      COMMENT '收藏数',
    `comment_count`  INT UNSIGNED    NOT NULL DEFAULT 0      COMMENT '评论数',
    `created_at`     DATETIME        NOT NULL                COMMENT '发布时间',
    `updated_at`     DATETIME        NOT NULL                COMMENT '最后修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_post_created_at_id` (`created_at`, `id`),
    KEY `idx_post_user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '论坛帖子表';

CREATE TABLE IF NOT EXISTS `post_image` (
    `id`         BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '帖子图片主键',
    `post_id`    BIGINT UNSIGNED  NOT NULL                COMMENT '所属帖子 ID',
    `url`        VARCHAR(255)     NOT NULL                COMMENT '图片相对访问地址',
    `sort_order` TINYINT UNSIGNED NOT NULL                COMMENT '图片顺序，从 0 开始',
    `created_at` DATETIME         NOT NULL                COMMENT '创建时间',
    `updated_at` DATETIME         NOT NULL                COMMENT '最后修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_post_image_sort` (`post_id`, `sort_order`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '帖子图片表';

CREATE TABLE IF NOT EXISTS `post_like` (
    `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '点赞记录主键',
    `post_id`    BIGINT UNSIGNED NOT NULL                COMMENT '帖子 ID',
    `user_id`    BIGINT UNSIGNED NOT NULL                COMMENT '点赞用户 ID',
    `created_at` DATETIME        NOT NULL                COMMENT '点赞时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_post_like_user` (`post_id`, `user_id`),
    KEY `idx_post_like_user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '帖子点赞关系表';

CREATE TABLE IF NOT EXISTS `post_favorite` (
    `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '收藏记录主键',
    `post_id`    BIGINT UNSIGNED NOT NULL                COMMENT '帖子 ID',
    `user_id`    BIGINT UNSIGNED NOT NULL                COMMENT '收藏用户 ID',
    `created_at` DATETIME        NOT NULL                COMMENT '收藏时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_post_favorite_user` (`post_id`, `user_id`),
    KEY `idx_post_favorite_user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '帖子收藏关系表';

CREATE TABLE IF NOT EXISTS `post_comment` (
    `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '评论主键',
    `post_id`    BIGINT UNSIGNED NOT NULL                COMMENT '帖子 ID',
    `user_id`    BIGINT UNSIGNED NOT NULL                COMMENT '评论者用户 ID',
    `content`    VARCHAR(500)    NOT NULL                COMMENT '评论内容',
    `created_at` DATETIME        NOT NULL                COMMENT '评论时间',
    `updated_at` DATETIME        NOT NULL                COMMENT '最后修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_post_comment_post_created` (`post_id`, `created_at`, `id`),
    KEY `idx_post_comment_user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '帖子评论表';
```

字段规则：

- `title`：1～100 个字符。
- `content`：1～5000 个字符；列表只返回前 200 个字符作为 `contentPreview`。
- `post_image`：每篇最多 5 张，`sort_order` 为 0～4；`url` 存 `/uploads/...` 原图相对地址。
- `post_comment.content`：1～500 个字符。
- `like_count` / `favorite_count` / `comment_count` 用事务和关系表同步更新，避免列表逐条 `COUNT(*)`。
- `created_at` / `updated_at` 由 Java 程序写入，不依赖数据库默认时间。

| 表 | 关键字段 | 用途 |
| --- | --- | --- |
| `user` | id, phone, password_hash, nickname, avatar_url, role, created_at, updated_at | 两类用户共用；`phone` 唯一 |
| `post` | id, user_id, title, content, like_count, favorite_count, comment_count, created_at, updated_at | 论坛帖子；计数冗余便于列表展示 |
| `post_image` | id, post_id, url, sort_order, created_at, updated_at；唯一键 (post_id, sort_order) | 帖子配图，最多 5 张 |
| `post_like` | id, post_id, user_id, created_at；唯一键 (post_id, user_id) | 点赞 |
| `post_favorite` | id, post_id, user_id, created_at；唯一键 (post_id, user_id) | 收藏 |
| `post_comment` | id, post_id, user_id, content, created_at, updated_at | 评论，不做盖楼 |
| `demand` | id, publisher_id, title, subject, grade, budget, description, status, accepted_by, created_at, updated_at | 需求单；不用 `order` 因为它是 SQL 关键字 |
| `conversation` | id, user_a_id, user_b_id, last_message_at, created_at；唯一键 (user_a_id, user_b_id) | 两人会话，约定 a < b |
| `message` | id, conversation_id, sender_id, content, created_at | 文字消息 |

状态值：`demand.status` = `PENDING` / `ACCEPTED` / `COMPLETED` / `CANCELLED`。

## 5. 开发顺序

1. 已完成账号契约、论坛表结构和帖子列表契约确认。
2. 已执行 `src/main/resources/db/V1__init.sql`，MySQL 中 6 张表已核对。
3. 工程骨架、Maven Wrapper、统一响应和全局异常已完成。
4. 已实现 `local` Profile 固定开发用户和纯文字发帖。
5. 手机验证码登录注册、JWT 签发和认证拦截已实现；数据库需执行 `V2__sms_auth.sql`。
6. 图片上传和 480px 列表缩略图已实现。
7. 接口契约已统一迁移到 `docs/api/openapi.yaml`。
8. 已实现带图发帖和帖子列表；标题可为空，列表返回前三张缩略图。
9. 下一步：帖子详情；再实现需求单和消息。
10. 每完成一个模块，补 curl 用例并更新 OpenAPI 状态。

## 6. 变更记录

| 日期 | 版本 | 变更 |
| --- | --- | --- |
| 2026-10-26 | v0.9 | 帖子标题改为可选并统一存空字符串；列表用 `thumbnailUrls` 返回排序后的前三张缩略图，移除 `coverImageUrl` |
| 2026-10-26 | v0.8 | 实现帖子分页列表和带图发帖；`POST /api/posts` 支持最多 5 张已上传原图，列表 `coverImageUrl` 返回缩略图 |
| 2026-10-26 | v0.7 | 接口契约统一迁移到 `docs/api/openapi.yaml`；本文件继续维护表结构和业务决策，不再维护接口字段副本 |
| 2026-10-26 | v0.6 | 确认并实现图片上传：JPG/PNG、单文件 2MB、单边 4096px、随机文件名、480px JPEG 缩略图；带图发帖后续实现 |
| 2026-10-23 | v0.5 | 确认并实现手机验证码发送、注册、登录、JWT 认证；昵称为空时生成“新用户+6位随机字符”；`password_hash` 改为可空 |
| 2026-10-22 | v0.4 | 确认本地固定开发用户和纯文字发帖契约；新增 `POST /api/posts` 实现说明，图片上传暂缓 |
| 2026-10-22 | v0.3 | 确认论坛 5 张表、帖子列表 20 条分页契约和列表响应字段；生成 V1 建库脚本 |
| 2026-10-21 | v0.2 | 契约暂缓：除登录注册外，其余接口与表结构等做到对应功能时再定；新增登录/注册接口契约与 `user` 建表 SQL |
| 2026-10-20 | v0.1 | 从客户端 `docs/development-plan.md` 拆出后端相关部分，形成待确认草案 |
