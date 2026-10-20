# TeacherTalkServer 后端计划（接口契约 / 表结构）

> 版本：v0.1 草案（2026-10-20）
> 状态：**未冻结**。第 3、4 节逐条确认后才写代码。
> 用途：接口契约和表结构的唯一事实来源。改代码必须同一轮改本文件；论文的接口清单、ER 图从这里出。
> 上行约束见 [`../AGENTS.md`](../AGENTS.md)。

## 1. 范围

| 模块 | 本期内容 |
| --- | --- |
| 账号 | 注册、登录、当前用户信息、修改资料 |
| 论坛 | 帖子列表（分页）、详情、发帖、点赞、收藏、评论列表、发评论 |
| 需求单 | 发布、列表筛选、详情、接单、取消、我的需求单 |
| 消息 | 会话列表（含未读）、创建会话、增量拉消息、发文字消息、上报已读 |
| 文件 | 图片上传（发帖用） |

本期不做：短信验证码、支付、评价、语音/图片消息、推送、WebSocket、后台管理、举报、内容审核、评论盖楼。

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

> 待确认：是否需要细分业务码（如 1001 手机号已注册、1002 密码错误）。**当前建议直接复用 HTTP 语义码，够用且好讲**。

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

## 3. 接口草案（待逐条确认）

| 模块 | 方法 | 路径 | 入参 | 返回 data |
| --- | --- | --- | --- | --- |
| 账号 | POST | `/api/auth/register` | `phone, password, nickname, role` | 同登录返回 |
| 账号 | POST | `/api/auth/login` | `phone, password` | `{token, user}` |
| 账号 | GET | `/api/auth/me` | 无 | `user` |
| 账号 | PUT | `/api/users/me` | `nickname`（可选 `avatarUrl`） | `user` |
| 论坛 | GET | `/api/posts?page=&size=` | 分页 | 分页，元素含 `liked, favorited` |
| 论坛 | GET | `/api/posts/{id}` | 路径参数 | 帖子详情（含图片、统计、我的点赞/收藏状态） |
| 论坛 | POST | `/api/posts` | `title, content, imageUrls[]` | 新帖子 |
| 论坛 | POST | `/api/posts/{id}/like` | 无 | `{liked:true, likeCount}` |
| 论坛 | DELETE | `/api/posts/{id}/like` | 无 | `{liked:false, likeCount}` |
| 论坛 | POST | `/api/posts/{id}/favorite` | 无 | `{favorited:true}` |
| 论坛 | DELETE | `/api/posts/{id}/favorite` | 无 | `{favorited:false}` |
| 论坛 | GET | `/api/posts/{id}/comments?page=&size=` | 分页 | 分页，元素含作者昵称 |
| 论坛 | POST | `/api/posts/{id}/comments` | `content` | 新评论 |
| 需求单 | GET | `/api/demands?page=&size=&subject=&status=` | 分页 + 筛选 | 分页 |
| 需求单 | GET | `/api/demands/{id}` | 路径参数 | 详情（含发布者信息） |
| 需求单 | POST | `/api/demands` | `title, subject, grade, budget, description` | 新需求单 |
| 需求单 | POST | `/api/demands/{id}/accept` | 无 | 接单后的详情；状态冲突返回 409 |
| 需求单 | POST | `/api/demands/{id}/cancel` | 无 | 取消后的详情 |
| 需求单 | GET | `/api/demands/mine?type=published\|accepted&page=&size=` | 分页 | 分页 |
| 消息 | GET | `/api/conversations` | 无 | 会话列表 + `unreadCount` + `lastMessage` |
| 消息 | POST | `/api/conversations` | `targetUserId` | 会话（不存在则创建） |
| 消息 | GET | `/api/conversations/{id}/messages?afterId=` | `afterId` 可选 | 消息列表（`afterId` 为空则全量） |
| 消息 | POST | `/api/conversations/{id}/messages` | `content` | 新消息 |
| 消息 | POST | `/api/conversations/{id}/read` | `lastReadId` | `{unreadCount:0}` |
| 文件 | POST | `/api/files` | multipart，字段名 `file` | `{url:"/uploads/xxx.jpg"}` |

待确认清单（逐条过）：

1. `register` 是否直接返回 token（省一次登录）——建议是。
2. 帖子列表是否需要「只看我发的」，本期先不做。
3. `demands` 的 `status` 筛选值固定为 `PENDING/ACCEPTED/COMPLETED/CANCELLED`。
4. `conversation` 两人顺序约定 `user_a_id < user_b_id`，用唯一键防重复。
5. 消息响应里是否带发送者昵称/头像（建议带昵称，省客户端二次请求）。
6. 图片上传限制 `jpg/png/webp`、单文件 5MB。

## 4. 数据库草图（待确认后固定）

| 表 | 关键字段 | 用途 |
| --- | --- | --- |
| `user` | id, phone, password_hash, nickname, avatar_url, role, created_at, updated_at | 两类用户共用；`phone` 唯一 |
| `post` | id, user_id, title, content, like_count, favorite_count, comment_count, created_at, updated_at | 论坛帖子；计数冗余便于列表展示 |
| `post_image` | id, post_id, url, sort_order | 帖子配图，最多 9 张 |
| `post_like` | id, post_id, user_id, created_at；唯一键 (post_id, user_id) | 点赞 |
| `post_favorite` | id, post_id, user_id, created_at；唯一键 (post_id, user_id) | 收藏 |
| `comment` | id, post_id, user_id, content, created_at | 评论，不做盖楼 |
| `demand` | id, publisher_id, title, subject, grade, budget, description, status, accepted_by, created_at, updated_at | 需求单；不用 `order` 因为它是 SQL 关键字 |
| `conversation` | id, user_a_id, user_b_id, last_message_at, created_at；唯一键 (user_a_id, user_b_id) | 两人会话，约定 a < b |
| `message` | id, conversation_id, sender_id, content, created_at | 文字消息 |

状态值：`demand.status` = `PENDING` / `ACCEPTED` / `COMPLETED` / `CANCELLED`。

> 待确认：`post` 上的三个计数字段是冗余设计（写入时 +1/-1），好处是列表查询不用 `COUNT(*)`，代价是要保证一致。
> 如果觉得多余，改成查询时 `COUNT` 也能跑，但列表页会有 N+1 或复杂 JOIN。

## 5. 开发顺序

1. 逐条确认本文件第 3、4 节 → 冻结 v1.0。
2. 建库 SQL（`src/main/resources/db/V1__init.sql`）。
3. 工程骨架：`pom.xml`、`application.yml`、`Result`、全局异常、JWT 拦截器。
4. 账号模块 → 论坛 → 需求单 → 消息 → 文件上传。
5. 每完成一个模块，补 curl 用例。

## 6. 变更记录

| 日期 | 版本 | 变更 |
| --- | --- | --- |
| 2026-10-20 | v0.1 | 从客户端 `docs/development-plan.md` 拆出后端相关部分，形成待确认草案 |
