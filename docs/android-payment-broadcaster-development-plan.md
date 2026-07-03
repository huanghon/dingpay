# 安卓到账语音播报 App 开发规划

生成时间：2026-06-27

## 项目定位与目标

这是一个安卓端“到账通知监听 + 语音播报 + 卡密授权”的工具类项目，目标用户是需要通过手机实时听到银行、Yappy、邮箱等收款通知的商户或收款人员。

当前项目目录 `D:\KAI\DingPay` 已经是 Kotlin + Jetpack Compose 安卓工程，包名 `com.hege.dingpay`，但目前仍是模板状态：`MainActivity.kt` 只显示 `Hello Android`，`AndroidManifest.xml` 还没有通知监听服务、TTS、授权、数据库、网络请求等核心功能。因此本单应按“从零开发核心业务”报价，而不是按简单改界面报价。

截图参考功能包括：

- 到账语音提醒首页：银行到账、Yappy 到账、邮件到账三类开关。
- 收款记录：近 7 天笔数、总金额、逐条记录、清空记录。
- 系统设置：通知监听、后台保活、TTS 状态、语言设置、授权管理、关于我们。
- 授权激活：设备 ID、输入授权码、到期时间、剩余天数。
- 监听配置：预设应用包名和关键词，支持银行类 App、Yappy、系统邮箱。
- TTS：Android 原生 TTS、语速/发音设置、中文/西语播报。

## 需求拆解

### 安卓端核心功能

1. 通知监听
   - 使用 `NotificationListenerService` 监听系统通知。
   - 用户必须手动开启通知使用权。
   - 按包名和关键词过滤收款通知。
   - 预设三类监听项：
     - 银行到账：默认包名示例 `com.bgeneral`，关键词示例 `recibido`、`envio`、`pago`、`detalle`、`te envio`。
     - Yappy 到账：默认包名示例 `com.yappy`，关键词示例 `Yappy`、`yappy`、`envio`、`pago`、`detalle`、`te envio`、`por Yappy`。
     - 邮件到账：默认包名示例 `com.android.email`，关键词示例 `recibiste`、`recibido` 等。
   - 支持选择本机已安装应用，避免不同手机包名不一致导致监听失败。

2. 通知解析
   - 从 `Notification.extras` 中读取标题、正文、大文本、多行文本。
   - 通过关键词和正则识别到账金额、币种、来源、时间。
   - 做去重处理，避免同一通知重复播报和重复入库。
   - 本地保存收款记录，不上传通知正文。

3. 语音播报
   - 使用 Android 原生 `TextToSpeech`。
   - 支持中文和西语播报模板。
   - 支持测试播报、语速、音调设置。
   - TTS 未安装、语言包缺失、音频焦点被占用时给出提示。

4. 授权激活
   - 显示设备 ID。
   - 输入卡密/授权码激活。
   - 激活后显示到期时间、剩余天数、授权状态。
   - 支持本地缓存授权状态，但需要定期联网校验。
   - 授权过期后禁止监听和播报。

5. 系统设置与保活引导
   - 检测通知监听权限是否开启。
   - 跳转系统通知监听设置页。
   - 检测电池优化状态，引导用户加入白名单。
   - 跳转系统 TTS 设置页。
   - 显示监听状态、后台运行状态、语音播报可用状态。

6. 本地记录与统计
   - 本地 Room 数据库保存收款记录。
   - 首页展示运行状态。
   - 收款记录页展示近 7 天总笔数、总金额、记录列表。
   - 支持按来源过滤、清空记录。

### 后台卡密系统

1. 管理员登录
   - 管理员账号密码登录。
   - 登录态管理。

2. 卡密管理
   - 生成卡密：支持 1 天、7 天、30 天、90 天、365 天、自定义天数。
   - 卡密列表：未使用、已使用、已过期、已禁用。
   - 卡密导出。
   - 禁用/删除卡密。

3. 设备绑定
   - 一张卡密默认绑定一台设备。
   - 首次激活时绑定设备 ID。
   - 支持后台解绑或换绑。
   - 记录激活时间、到期时间、最后校验时间。

4. 授权 API
   - `POST /api/license/activate`：卡密激活。
   - `POST /api/license/check`：授权校验。
   - `POST /api/license/heartbeat`：可选心跳。
   - `GET /api/app/version`：可选版本检查。

5. 安全策略
   - 全程 HTTPS。
   - 服务端签发短期授权 token。
   - App 本地保存授权缓存和签名，降低短时间断网影响。
   - 不承诺无法破解，只做普通商用强度防护。

### 非功能需求

- Android 版本：建议最低 Android 7.0，对应现有 `minSdk 24`。
- 开发栈：现有项目继续使用 Kotlin + Jetpack Compose。
- 本地存储：Room + DataStore。
- 网络请求：Ktor Client 或 Retrofit。
- 后台：FastAPI + PostgreSQL/SQLite，或按客户服务器环境改为 Spring Boot / Node.js。
- 部署：Linux 服务器 + Nginx + HTTPS。
- 隐私：到账通知内容仅在本机处理，不上传；后台只接收设备 ID、卡密、授权状态、版本信息。
- 分发：建议私有 APK 分发，不建议直接承诺上架应用商店。

## 建议范围

### MVP 版本

适合作为第一期交付，控制开发风险：

- 安卓端 Compose UI，复刻截图中的主要页面。
- 通知监听权限引导。
- 三类预设监听：银行、Yappy、邮箱。
- 应用选择、关键词配置、启用/禁用开关。
- 通知解析金额、来源、时间。
- 本地记录和近 7 天统计。
- Android 原生 TTS 播报，支持中文/西语模板。
- 授权激活页，设备 ID，卡密激活，到期时间显示。
- 简单后台管理：登录、生成卡密、卡密列表、禁用、解绑。
- 授权 API 与安卓端联调。
- APK 打包和一份安装使用说明。

### 建议延期或另计费

- 多平台支付渠道深度适配。
- OCR、短信监听、无通知场景抓取。
- 云端同步收款记录。
- 多商户、多代理、分销后台。
- 自动更新 APK。
- 极强防破解、加固、混淆、反调试。
- 多语言完整国际化。
- 微信/支付宝/银行卡官方接口对接。
- 上架 Google Play 或国内应用市场。

## 难点与风险

1. 通知监听不是官方到账接口
   - App 只能读取系统通知，银行/Yappy/邮箱不发通知时无法播报。
   - 第三方 App 文案变化、语言变化、包名变化都会影响解析。
   - 不同 ROM 可能限制后台运行。

2. 后台保活无法百分百保证
   - NotificationListenerService 由系统绑定，仍可能被用户关闭权限或被系统限制。
   - 需要引导用户开启通知权限、电池白名单、自启动等设置。
   - 不应向客户承诺“绝不漏播”。

3. TTS 依赖手机环境
   - 部分手机没有 Google TTS 或语言包。
   - 系统 TTS 引擎、语音包、音量、音频焦点都会影响播报。

4. 卡密系统有安全边界
   - 能做设备绑定、签名 token、混淆、过期校验。
   - 不能承诺绝对无法破解，强防护需要额外加固和预算。

5. 收款金额解析需要真实通知样本
   - 必须让客户提供银行、Yappy、邮箱的真实通知截图或文本。
   - 没有样本只能做通用规则，验收时容易出现误播/漏播。

## 需要问客户的问题

### 功能范围

1. 只监听银行、Yappy、邮箱这 3 类，还是还要加 WhatsApp、短信、其他钱包？
2. 每类通知是否都只播报到账成功，不播报转出、失败、验证码、营销通知？
3. 播报文案要固定中文，还是根据语言选择中文/西语？
4. 是否需要用户自己新增监听规则，还是只用后台/内置预设？
5. 收款记录只保存在手机本地，还是后台也要能看？

### 通知样本

1. 请提供每个渠道 5-10 条真实到账通知文本或截图。
2. 金额格式是否只有美元，例如 `$1.25`，是否存在 `USD 1.25`、`B/. 1.25` 等格式？
3. 通知语言是否固定西语，是否会出现英文或中文？
4. 同一笔到账是否可能出现多条通知？

### 授权卡密

1. 一张卡密绑定几台设备？
2. 卡密有效期有哪些套餐？
3. 设备换机是否允许后台解绑？
4. 断网时允许继续使用多久？例如 1 天、3 天、7 天。
5. 是否需要代理商/销售人员账号批量发卡？

### 后台和部署

1. 后台部署在客户服务器还是由开发者代部署？
2. 是否已有域名、服务器、HTTPS 证书？
3. 管理员后台是否需要手机端适配？
4. 是否需要导出卡密 Excel？

### 验收标准

1. 用哪几台安卓手机验收？
2. 需要覆盖哪些 Android 版本和品牌 ROM？
3. 每个渠道各测试多少条通知算通过？
4. 是否接受免责声明：因对方 App 未推送通知、系统限制、TTS 缺失导致漏播不算程序 bug？

## 预算建议

### 低配 MVP：12000-18000 元

适用前提：

- 只做安卓端 + 简单卡密后台。
- 只支持 3 个预设监听渠道。
- 后台只做卡密生成、激活、禁用、解绑。
- 不做复杂防破解、不做云端收款记录、不做自动更新。
- 客户提供真实通知样本和测试手机。

### 标准商用版：18000-35000 元

适用前提：

- 安卓端页面完整复刻截图体验。
- 支持用户选择应用和编辑关键词。
- 授权缓存、定期校验、设备换绑。
- 收款记录统计更完整。
- 后台卡密列表、筛选、导出、操作日志。
- 做基本混淆和错误日志。
- 提供部署、APK 打包、使用说明。

### 增强版：35000-60000 元以上

适用前提：

- 多渠道规则管理。
- 代理商/分销卡密系统。
- 自动更新 APK。
- 云端日志/设备状态监控。
- 更强防破解、代码混淆、加固。
- 多语言完整国际化。
- 多机型兼容测试和长期维护。

建议收款方式：

- 40% 定金：确认需求和开始开发。
- 40% 联调款：安卓端 + 后台核心功能可演示。
- 20% 尾款：验收通过、交付源码/APK/部署文档。

维护建议：

- 免费修复期 7-15 天，仅限约定范围内 bug。
- 后续维护按月 1000-3000 元，或按次报价。

## 交付计划

### 第 1 阶段：需求确认，1-2 天

- 确认监听渠道、授权规则、部署方式。
- 收集真实通知样本。
- 确认 UI 是否按截图仿制。
- 输出最终 PRD 和验收清单。

### 第 2 阶段：安卓端基础框架，3-5 天

- Compose 页面结构。
- 导航、设置存储、本地数据库。
- 通知权限、TTS 设置、电池优化引导。

### 第 3 阶段：通知监听与播报，4-7 天

- NotificationListenerService。
- 通知文本提取。
- 关键词过滤、金额解析、去重。
- TTS 队列、语速音调、语言模板。
- 本地记录和统计。

### 第 4 阶段：卡密后台与授权 API，4-6 天

- 后台管理登录。
- 卡密生成、列表、禁用、解绑。
- 激活/校验 API。
- 安卓端授权页面和接口联调。

### 第 5 阶段：测试与交付，3-5 天

- 真实通知样本回归测试。
- 多机型权限与后台运行测试。
- APK 打包。
- 部署后台。
- 编写安装、激活、保活设置说明。

整体周期建议：15-25 个工作日。若客户通知样本齐全、服务器账号齐全，可压缩；若要兼容多品牌手机和多个支付 App，需要延长。

## 技术依据

- Android 官方 `NotificationListenerService` 文档说明该服务用于接收应用通知事件，且需要在 manifest 中声明 `BIND_NOTIFICATION_LISTENER_SERVICE` 权限和对应 intent filter。
- Android 官方 `TextToSpeech` 文档说明 TTS 需要初始化完成后才能播报，并支持语言、语速、音调、语音队列等能力。
- Android 前台服务文档说明，前台服务需要显示状态栏通知；Android 14+ 对前台服务类型和权限有更严格要求；Android 12+ 从后台启动前台服务也有限制。因此本项目的“后台保活”必须按系统能力做引导，不应承诺百分百常驻。

参考链接：

- https://developer.android.com/reference/android/service/notification/NotificationListenerService
- https://developer.android.com/reference/android/speech/tts/TextToSpeech
- https://developer.android.com/develop/background-work/services/fgs
- https://developer.android.com/about/versions/14/changes/fgs-types-required
- https://developer.android.com/develop/background-work/services/fgs/launch

## 给 Codex 的开发提示词

```text
你现在在 D:\KAI\DingPay 项目中开发一个安卓到账语音播报 App。现有项目是 Kotlin + Jetpack Compose 安卓工程，包名 com.hege.dingpay，当前 MainActivity 仍是模板页面。请基于现有工程实现 MVP，不要重建项目。

产品目标：
开发一个安卓 App，通过系统通知监听读取银行、Yappy、邮箱等收款到账通知，在本机解析金额并用 Android 原生 TTS 语音播报。通知内容只在本机处理，不上传后台。App 需要卡密授权激活，后台提供卡密管理和授权校验 API。

推荐技术栈：
安卓端：Kotlin、Jetpack Compose、Material3、Navigation Compose、Room、DataStore、Ktor Client 或 Retrofit、kotlinx.serialization。
后台：优先 FastAPI + PostgreSQL/SQLite + SQLAlchemy + JWT/签名 token；如当前仓库暂不包含后台，可在 server/ 目录新增独立后台工程。

安卓端必须实现的模块：
1. 首页 Dashboard
   - 标题：到账语音提醒
   - 三个监听卡片：银行到账、Yappy 到账、邮件到账
   - 每个卡片有启用开关、收款记录入口、语音设置入口
   - 底部显示：监听状态、后台运行、语音播报状态

2. 收款记录页
   - 显示近 7 天总笔数、总金额
   - 列表显示来源、时间、金额、状态
   - 支持清空记录
   - 本地 Room 表 payment_records：id、sourceType、sourceName、amount、currency、title、text、packageName、notificationKey、postedAt、createdAt、rawHash

3. 系统设置页
   - 通知监听配置入口
   - 语言设置：中文、Español
   - 授权管理入口，显示授权状态和到期时间
   - 关于我们弹窗，写明通知内容仅本机处理、免责声明

4. 监听配置页
   - 显示系统说明
   - 检测通知监听权限是否开启
   - 提供按钮跳转 Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
   - 检测是否忽略电池优化，提供跳转系统设置的引导
   - 显示 TTS 状态，提供测试播报和跳转系统 TTS 设置
   - 预设监听规则：
     a. 银行到账：默认 packageName com.bgeneral，关键词 recibido、envio、pago、detalle、te envio
     b. Yappy 到账：默认 packageName com.yappy，关键词 Yappy、yappy、envio、pago、detalle、te envio、por Yappy
     c. 邮件到账：默认 packageName com.android.email，关键词 recibiste、recibido、B/.、de
   - 支持从已安装应用列表选择包名
   - 支持启用/停用每条规则

5. 通知监听服务
   - 新增 PaymentNotificationListenerService 继承 NotificationListenerService
   - 在 AndroidManifest.xml 声明 android.permission.BIND_NOTIFICATION_LISTENER_SERVICE 和 android.service.notification.NotificationListenerService action
   - 在 onNotificationPosted 中读取 StatusBarNotification.packageName、key、postTime、Notification.extras 的 EXTRA_TITLE、EXTRA_TEXT、EXTRA_BIG_TEXT、EXTRA_TEXT_LINES
   - 根据启用规则做包名和关键词匹配
   - 用正则解析金额，支持 $1.25、+$1.25、USD 1.25、B/. 1.25
   - 用 notificationKey + postTime + rawHash 去重
   - 写入 Room，并调用 TTS 播报
   - 如果授权过期或未授权，不播报、不写入或只记录被拦截状态

6. TTS
   - 新增 TtsManager，封装 TextToSpeech 初始化、语言选择、语速、音调、队列播报、释放资源
   - 支持中文模板：{来源}到账{金额}{币种}
   - 支持西语模板：Pago recibido {币种}{金额}
   - 提供测试播报
   - TTS 不可用时在 UI 显示错误引导

7. 授权激活
   - 授权页显示设备 ID、授权状态、剩余天数、到期时间
   - 设备 ID 使用 Settings.Secure.ANDROID_ID 加本地安装 UUID 组合生成稳定标识，不读取敏感硬件号
   - 输入授权码调用后台 /api/license/activate
   - App 启动和每天定时调用 /api/license/check 或 heartbeat
   - 服务端返回 expiresAt、status、signedToken、offlineGraceDays
   - 本地 DataStore 缓存授权状态，离线只允许在 grace 期限内继续使用

后台必须实现的模块：
1. 管理员登录
2. 卡密生成：支持自定义数量和有效天数
3. 卡密列表：状态、有效期、绑定设备、激活时间、到期时间、最后校验时间
4. 禁用卡密、解绑设备
5. API：
   - POST /api/license/activate {licenseKey, deviceId, appVersion}
   - POST /api/license/check {licenseKey, deviceId, signedToken, appVersion}
   - POST /api/license/heartbeat {deviceId, signedToken, appVersion}
6. 数据表：
   - license_cards：id、keyHash、durationDays、status、createdAt、activatedAt、expiresAt、disabledAt
   - device_activations：id、licenseId、deviceId、appVersion、firstSeenAt、lastSeenAt、revokedAt
   - admin_users：id、username、passwordHash、createdAt
   - audit_logs：id、actor、action、targetId、createdAt、metadataJson

安全与隐私要求：
- 不上传到账通知正文和金额到后台。
- 后台只处理卡密、设备 ID、授权状态、版本号。
- 所有 API 设计为 HTTPS 部署。
- 本地授权 token 要有服务端签名校验。
- release 构建开启基本混淆。
- 不承诺绝对防破解。

UI 风格：
参考客户截图，整体为白底、蓝色主色、卡片式设置页面。卡片圆角克制，界面直接进入可用工具，不做营销落地页。

验收标准：
1. 安装 APK 后能打开权限设置并检测通知监听权限。
2. 开启监听后，模拟或真实发送符合关键词的通知，能解析金额、保存记录、播报语音。
3. 关闭某个渠道开关后，该渠道不再播报。
4. 未授权或授权过期时不能播报，并提示激活。
5. 卡密后台能生成卡密、激活、显示绑定设备、禁用、解绑。
6. 收款记录近 7 天统计正确，清空记录生效。
7. TTS 测试按钮可用，语言切换后播报模板改变。

开发方式：
先读取现有 Gradle、Manifest、MainActivity 和主题文件。保持现有 Compose 工程结构，按 feature/data/service/ui 分层新增文件。需要改依赖时修改 gradle/libs.versions.toml 和 app/build.gradle.kts。每次实现后运行 Gradle 构建或至少说明无法构建的原因。遇到阻塞才提问，否则根据本文做保守实现。
```

## 可发给客户的话术

```text
这个项目可以做，技术路线是安卓端读取系统通知并本机语音播报，后台做卡密授权管理。需要先说明一点：它不是银行/Yappy 官方接口，而是监听手机系统通知，所以稳定性取决于对方 App 是否及时推送通知、通知文案是否变化、手机系统是否限制后台运行。我们可以做权限引导、白名单引导、关键词规则和去重，但不能承诺 100% 不漏播。

第一期建议做 MVP：安卓端支持银行、Yappy、邮箱三类到账播报，本地收款记录和统计，中文/西语 TTS，卡密激活，以及后台卡密生成、禁用、解绑。周期大约 15-25 个工作日，标准版预算建议 1.8 万-3.5 万，具体看是否需要代理后台、自动更新、云端记录和防破解加固。

为了准确报价和避免验收争议，需要您先提供：每个收款渠道 5-10 条真实到账通知截图或文本、卡密有效期规则、一张卡密绑定几台设备、是否允许换机、后台部署服务器是否由您提供、准备用哪些手机验收。
```
