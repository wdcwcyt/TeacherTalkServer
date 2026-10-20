# TeacherTalkServer

TeacherTalk（大学生家教交流平台）的后端服务：Spring Boot 3 + Java 17 + Maven + MySQL 8 + MyBatis-Plus + JWT。

## 开发约定

动手前先读 [`AGENTS.md`](./AGENTS.md)，接口契约与表结构以 [`docs/backend-plan.md`](./docs/backend-plan.md) 为准。

## 本地环境

| 工具 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 17 | 已安装，需设置 `JAVA_HOME` |
| Maven | 3.9.x | 用项目自带 `mvnw`，无需单独安装 |
| MySQL | 8.0 | 本地实例，库名 `teacherttalk` |
| IDE | IntelliJ IDEA | 打开本目录即可 |

## 待补充

- 启动步骤、`application.yml` 配置说明
- 建表脚本执行顺序
- 接口清单
