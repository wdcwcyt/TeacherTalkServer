# TeacherTalkServer —— 后端开发约束

> 本文件约束本仓库（TeacherTalk 后端）的所有开发工作。**每次开工前必须先读本文件**，
> 并读 `docs/backend-plan.md`（接口契约、表结构、错误码的唯一事实来源）。
> 客户端仓库的 `AGENTS.md` 不跨仓库生效，但两边的接口约定必须一致。

## 0. 我是谁、你要怎么跟我说话

- 我是一年经验的 Android 开发实习生，**后端几乎不懂**：只用 Spring Boot 做过一个 jar，简单增删改查 + 部署到服务器。
- 所以后端主要由你设计实现，但你同时要**当我的老师**：
  - 始终用中文回答。
  - **先给结论，再给原因**；术语第一次出现时用一句话解释清楚（例如「JWT 是一个签过名的字符串，服务端用它认出你是谁」）。
  - 不要默认我懂某个注解、某个框架行为、某个 Maven 概念。涉及我没用过的写法时，给**最小可运行示例**。
  - 拒绝「黑盒魔法」：凡是靠框架自动完成的事（自动配置、拦截器、事务、ORM 映射），要告诉我它在什么时候起作用、坏了会看到什么现象。
- 每次任务结束必须告诉我四件事：**改了哪些文件 / 为什么这么改 / 我怎么验证 / 还有什么没做或有风险**。

## 1. 这个项目是什么

TeacherTalk：大学生家教交流平台（毕业设计）。Android 客户端 + Web 后端，本仓库是后端。

- 用户：大学生（当前可发需求也可接单，后期改为只能接单）、家长（可发需求）。
- 本期业务：账号体系、社区论坛（帖子/点赞/收藏/评论）、家教需求单（发布/接单）、文字聊天（客户端 3 秒轮询）。
- 本期明确不做：短信验证码、支付、评价、语音/图片消息、推送、WebSocket、管理员后台、举报、内容审核、评论盖楼。
- 这是毕业设计：**范围要克制**。做得完、讲得清 > 功能多。论文要素材（ER 图、架构图、接口清单），所以设计必须能画成图、讲得清。

## 2. 技术栈（未确认前不要换）

| 项 | 取值 | 说明 |
| --- | --- | --- |
| 语言 / JDK | Java 17 | 本机已装 JDK 17.0.12；不要用 Java 21 语法 |
| 框架 | Spring Boot 3.x | 单体应用，打一个 jar |
| 构建 | Maven（含 `mvnw` 包装器） | 不引入 Gradle |
| 数据库 | MySQL 8.0 | 本地开发，库名 `teacherttalk`（见第 6 节命名规则） |
| 持久层 | MyBatis-Plus | 用 `mybatis-plus-spring-boot3-starter` |
| 认证 | JWT（`Authorization: Bearer <token>`） | 无状态，不存 session |
| 参数校验 | `spring-boot-starter-validation` | 只在 DTO 上加注解，业务校验在 Service |

**不允许擅自引入**：Redis、消息队列、微服务拆分、Docker/K8s、Spring Security 全家桶（除非我明确同意）、Lombok（先用普通 getter/setter 或 record，够用再说）、Swagger/OpenAPI（先用 `docs/` 里的接口清单）。
确实需要时：**先说明它解决什么问题、不加会怎样、加了多多少工作量，等我确认**。

依赖版本一律集中在 `pom.xml` 的 `<properties>` 里，写明版本号；**不允许用 `LATEST`、范围版本或随手 `mvn versions:update`**。
新增依赖时在交付说明里列一句：坐标 + 版本 + 为什么需要。

## 3. 分层与包结构（硬性）

```text
com.zl.teacherttalk
├── TeacherTalkServerApplication.java
├── common/        统一响应体、错误码、全局异常处理、分页结构、常量
├── config/        Web、MyBatis-Plus、JWT、上传目录等配置
├── controller/    只做参数接收/校验/转发
├── service/       业务逻辑
│   └── impl/
├── mapper/        Mapper 接口（继承 MyBatis-Plus 的 BaseMapper）
├── entity/        数据库实体，与表一一对应
├── dto/           请求体（XxxRequest）与响应体（XxxResponse）
├── security/      JWT 生成/解析、登录拦截器、当前用户上下文
└── util/          无状态工具方法
```

依赖方向只能单向：`controller → service → mapper → entity`。`common` / `util` 可被任意层使用，但它们**不反向依赖**上面任何一层。

硬性规则（违反等于设计错了，不是风格问题）：

- **Controller 不写业务、不写 SQL、不 try/catch 吞异常**。它的职责就三件事：接参数、`@Valid` 校验、调 Service、返回 `Result<T>`。
- **Service 不接收 `HttpServletRequest`，不返回 `Result`**：Service 返回业务对象，统一响应体由 Controller 包。
- **Mapper 不写业务判断**：复杂查询用 XML（`resources/mapper/*.xml`）或 `@Select` 写清楚 SQL，不把 `if (status == ...)` 之类塞进 SQL 字符串拼接之外的地方。
- **Entity 不出 Controller**：对外一律返回 `XxxResponse` DTO，避免把 `password_hash` 等字段漏出去。
- `new XxxRepository()` 这类内部造协作者禁止；一律构造器注入（配合 `@RequiredArgsConstructor` 或显式构造器）。
- 一个类只做一件事；函数尽量 ≤ 50 行，嵌套 ≤ 3 层。
- 不创建「以后可能用到」的空接口、空 Controller、空 Service。**新建模块时才新建对应文件。**

## 4. 命名与数据约定（定了就不要中途改）

- 表名：全小写，**单数**，下划线分隔；避免 SQL 关键字（所以需求单表叫 `demand`，不叫 `order`）。
- 字段名：下划线分隔（`created_at`、`password_hash`、`publisher_id`）。
- 类名：`UpperCamelCase`；方法/变量：`lowerCamelCase`；常量：`static final` + 大写下划线。
- 主键：`BIGINT AUTO_INCREMENT`，字段名 `id`。
- 时间字段：`DATETIME`，存 `Asia/Shanghai` 本地时间；`created_at` / `updated_at` 由程序写入，不依赖数据库默认值时区猜测。
- 逻辑删除：**本期不用**。需要「取消」的用状态字段表达。
- 布尔含义的字段用 `is_` 前缀或状态枚举，不用 `0/1` 裸数字对外暴露。
- 对外枚举值统一大写（`PENDING` / `ACCEPTED` / `COMPLETED` / `CANCELLED`），数据库存字符串，方便查数据时看得懂。

## 5. 接口契约规则

- 所有接口以 `/api` 开头；路径用复数资源名（`/api/posts`、`/api/demands`）。
- **统一响应体**（成功与失败都包）：

```json
{ "code": 0, "message": "ok", "data": {} }
```

  由 `common/Result<T>` 承载；`code == 0` 表示成功。HTTP 状态码同时要正确（200/400/401/403/404/409/500），
  即 `code` 给客户端做业务分支，HTTP 状态码给日志、网关和调试看。
- 分页请求：`page`（从 1 开始）、`size`（默认 10，上限 50）。
  分页返回：`{ "list": [], "page": 1, "size": 10, "total": 0 }`。
- 时间格式统一 `yyyy-MM-dd HH:mm:ss`，时区 `Asia/Shanghai`（在 `application.yml` 里配好 Jackson 和 JVM 时区，不靠客户端猜）。
- 错误码：`0` 成功、`400` 参数错误、`401` 未登录/token 失效、`403` 无权限、`404` 不存在、`409` 状态冲突、`500` 服务端异常。
  新增错误码必须先写进 `docs/backend-plan.md`，再写代码。
- **破坏性变更**（改路径、改字段名、改返回结构、改状态值）必须**先告诉我**，因为客户端要同步改。
- 接口有任何改动，同一轮更新 `docs/backend-plan.md` 的接口表；论文的接口清单从这份文档抄。

## 6. 数据库规则

- 新增表/字段：先在 `docs/backend-plan.md` 写建表 SQL + 一句话用途，**我确认后再建**。
- 每张表必须有：`id`、`created_at`、`updated_at`（纯关系表可只留 `created_at`）。
- 关联字段必须有索引；唯一约束要在 SQL 里显式写出（如 `UNIQUE KEY uk_post_user (post_id, user_id)`），不要只在 Java 里判断。
- 状态流转只走 Service 里的显式方法，例如接单必须处理「并发两人同时接单」：用条件更新
  （`UPDATE demand SET status='ACCEPTED', accepted_by=? WHERE id=? AND status='PENDING'`）并检查影响行数，影响 0 行就返回 409。
- 密码只存 BCrypt 哈希，**任何日志、异常信息、接口响应里都不许出现明文密码或 token 全文**。
- 所有 SQL 必须参数化（用 `#{}`），禁止 `${}` 拼接用户输入。
- 建库脚本、表结构变更脚本放在 `src/main/resources/db/`，文件名带序号（`V1__init.sql` 这种风格），保证别人能照着建出同样的库。

## 7. 安全与权限

- 登录后返回 JWT；token 里只放 `userId`、`role`、过期时间，**不放手机号、密码、昵称**。
- 需要登录的接口由拦截器统一校验，Controller 里不要每个方法都手写一遍 token 解析。
- 「当前用户」从 `security/` 的上下文取，不从请求参数里信客户端传来的 `userId`。
- 权限判断必须服务端做：客户端隐藏按钮只是体验，服务端才是防线。角色规则（当前可发可接、后期分离）集中在一个类里实现，方便后面改口径。
- 上传文件：只接受图片类型、限制大小（先按 5MB）、重命名成随机文件名、禁止路径穿越（不拼接原始文件名）；存储目录 `uploads/` 通过 `/uploads/**` 静态访问。
- 异常信息对外只说人话，不返回堆栈、SQL、表名。

## 8. 日志

- 用 SLF4J（`log.info/warn/error`），禁止 `System.out.println`。
- 关键业务节点记一条（登录成功/失败原因类别、接单成功、状态冲突），**不记密码、token、完整请求体**。
- 全局异常处理里对 500 记 `error` 并带堆栈，对 4xx 记 `warn` 不带堆栈。

## 9. 过度设计预警（必须停下来提醒我）

出现下面任意一条，先停下来说明，不要直接铺代码：

- 新增的接口/抽象类/工厂只有 1 个实现、1 个调用方。
- 一个小功能拆出 4 个以上新文件，代码量翻倍而可读性没变好。
- 为了「通用」造 `BaseController` / `BaseService` / 泛型 CRUD 框架。
- 需要引入新中间件或新框架才能继续。
- 3 行直白赋值被改写成跨 3 个类的事件流。

提醒时一次说清四件事：**事实**（新增几个类/多少行）、**收益**、**成本**、**选项**（简单版 vs 完整版），我来选。
反过来，**该做的不要劝退**：把业务规则从 Controller 搬进 Service、重复 3 次以上的逻辑抽方法、把只能靠 postman 试的逻辑变成可单测的纯函数。

## 10. 验证规则（重要）

- 我的电脑有**加密限制，可能无法编译/构建**。默认按「跳过编译验证」处理。
- **除非我在当次对话里明确说「这次跑一下编译/启动」**，否则不要执行 `mvn`、`mvnw`、`spring-boot:run` 等构建或启动命令，也不要反复重试。
- 替代验证方式：
  1. 静态自检：import、注解是否成对（`@RestController`/`@RequestMapping`）、DTO 字段与 SQL 列名是否对应、Mapper XML 的 `namespace` 与方法名是否一致、`application.yml` 的 key 是否拼错、依赖是否在 `pom.xml` 里。
  2. 给出**可直接复制的 `curl` 命令**和**可直接执行的 SQL**，注明预期返回。
- 能跑的时候：用 `mvn spring-boot:run` 启动 + `curl` 打一遍关键接口，把真实响应贴给我看；**不许只说「应该没问题」**。
- 不能跑的时候：明确说「未验证」以及可能出问题的点，不要含糊带过。
- 不要编造测试结果、性能数据、并发测试结论。

## 11. 交付说明模板

每次改完按这个结构回复：

1. **结论**：这次完成了什么，现在系统能做什么。
2. **改了哪些文件**：路径 + 一句话作用。
3. **为什么这么改**：关键设计取舍（尤其是为什么不选另一个方案）。
4. **怎么验证**：curl / SQL / 点哪里看什么，写预期结果。
5. **没做 / 风险**：未验证的部分、待确认的契约、后续要补的东西。

## 12. Git 与文档

- **不要执行 `git commit` / `git push`**，我自己来。不要改 Git 历史。
- `docs/backend-plan.md` 是接口契约和表结构的唯一事实来源，改了代码同一轮改文档。
- 新增依赖、改表、改接口，都要在文档里留痕。

## 13. 还没定的事（遇到先问我）

- Spring Boot / MyBatis-Plus / JWT 库的**具体版本号**（我提议，你确认）。
- MySQL 8.0 的安装方式与 root 密码策略。
- 接口草案第 6 节的逐条确认（分页、时间格式、错误码、表名）。
- 部署目标服务器（现在没有，先本地开发）。
