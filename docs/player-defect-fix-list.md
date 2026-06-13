# Dy Player 缺陷修复清单

**生成日期**: 2026-06-13  
**验证方法**: 二次代码审查确认，不修改任何代码  
**原则**: 仅保留需要修复的问题，移除误报和低价值项

---

## 修复优先级说明

- **P0 - 必须修复**: 安全漏洞、数据丢失、高频崩溃
- **P1 - 应该修复**: 功能缺陷、明显逻辑错误、内存泄漏
- **P2 - 建议修复**: 代码质量、防御性编程、体验优化

---

## P0 - 必须修复（6 项）

### 1. SSL 证书验证完全禁用 [M-1]
- **文件**: `lib-base/.../okhttp/SslManager.kt` 第 16-23 行, `lib-base/.../okhttp/OkUtil.kt` 第 57-64 行、122-133 行
- **问题**: `trustManager` 的 `checkClientTrusted`/`checkServerTrusted` 均为空实现，`hostnameVerifier { _, _ -> true }` 无条件信任所有主机名
- **影响**: 所有 HTTPS 请求易受中间人攻击
- **修复方向**: 仅在用户明确选择信任自签名证书时禁用验证，默认启用系统证书验证

### 2. 备份明文存储敏感凭据 [K-1]
- **文件**: `dy-player/.../tool/BackupSettingsFragment.kt` 第 149 行
- **问题**: `seSsData` 等敏感凭据以明文 JSON 写入外部存储 `/Download/DyLike/`，任何应用可读取
- **影响**: WebDAV/SMB 账号密码泄露
- **修复方向**: 对敏感字段加密后再写入备份文件

### 3. 备份还原缺乏完整性校验 [K-2]
- **文件**: `dy-player/.../tool/BackupSettingsFragment.kt` 第 202 行
- **问题**: 还原时仅检查 `backupType == "dy_like"`，无 HMAC/签名/校验和
- **影响**: 恶意构造的备份文件可注入任意数据到 SharedPreferences
- **修复方向**: 添加校验和或 HMAC 验证

### 4. StorageManager.searchAllStorages 丢弃多存储搜索结果 [F-1]
- **文件**: `lib-base/.../storage/StorageManager.kt` 第 71-78 行
- **问题**: `combine(flows) { results -> results[0] }` 只返回第一个存储的结果，丢弃其余
- **影响**: 多存储场景下搜索结果不完整
- **修复方向**: 改为 `results.flatMap { it.asList() }` 合并所有结果

### 5. 缓存清理完全失效 [F-10]
- **文件**: `dy-player/.../media/MediaFragment.kt` 第 967-975 行
- **问题**: `clearCache()` 匹配 `.jpg` 后缀，但缩略图实际使用 `.webp` 格式（`AppUtil.THUMB_TYPE = "webp"`）
- **影响**: 缓存永远不会被清理，存储空间持续增长
- **修复方向**: 将 `.jpg` 改为 `".${AppUtil.THUMB_TYPE}"` 或 `.webp`

### 6. CodeUtil.md5() 前导零丢失 [I-1/N-1]
- **文件**: `lib-base/.../util/CodeUtil.kt` 第 54-58 行
- **问题**: `BigInteger(1, digest.digest()).toString(16)` 会省略前导零，MD5 结果可能不足 32 位
- **影响**: 同一输入在不同路径计算出的 MD5 不一致，影响数据去重和匹配
- **修复方向**: 改为 `digest.digest().joinToString("") { "%02x".format(it) }`，与 HashHelper.kt 保持一致

---

## P1 - 应该修复（20 项）

### 7. 返回键处理逻辑错误 [A-1]
- **文件**: `dy-player/.../main/MainActivity.kt` 第 102-119 行
- **问题**: `resolveActivity()` 返回 null 时 `finish()` 后 `return@apply` 仅退出 apply 块，`startActivity(homeIntent)` 仍会执行
- **修复方向**: 将 `return@apply` 改为 `return@addCallback` 或使用标签 return

### 8. onNewIntent 路径缺少播放器/弹幕清理 [B-7]
- **文件**: `dy-player/.../long_video/LongVideoActivity.kt` 第 266-269 行
- **问题**: `onNewIntent()` 调用 `fromOpen()` 触发新播放，但未释放旧播放器、清理弹幕、取消协程
- **修复方向**: 在 `fromOpen()` 前执行与 `startPlay()` 相同的清理流程

### 9. fromOpen() 中 parentFile!! 非空断言 [B-2]
- **文件**: `dy-player/.../long_video/LongVideoActivity.kt` 第 294 行
- **问题**: `File(filepath).parentFile!!` 可能 NPE
- **修复方向**: 使用安全调用 `parentFile?.let {}` 并处理 null 情况

### 10. 6 秒节流导致播放完成进度丢失 [B-8]
- **文件**: `dy-player/.../long_video/PlayHelper.kt` 第 317 行
- **问题**: 播放完成时调用 `saveCacheInfo(0, true)` 走 `updateInfo()`，6 秒节流可能跳过保存
- **修复方向**: 播放完成等关键节点应绕过节流，直接调用 `saveInfo()`

### 11. 字幕吸附方法从未调用（长视频） [B-3]
- **文件**: `dy-player/.../long_video/LongVideoActivity.kt`
- **问题**: `SubtitleControlView` 提供了 `setSubtitleLayoutBounds()`/`setSubtitleDockMode()` 方法，ShortVideoActivity 有调用但 LongVideoActivity 未调用
- **修复方向**: 在 `attachSubtitle()` 后调用字幕吸附配置方法

### 12. notifyItemRangeChanged 参数错误 [C-1/C-6]
- **文件**: `dy-player/.../short_video/ShortVideoActivity.kt` 第 1075、1087 行
- **问题**: 第二个参数传了列表总大小而非变更数量
- **修复方向**: 修正为正确的新增/变更数量，或使用 `notifyItemRangeInserted`

### 13. onNewIntent 未处理新 Intent（短视频） [C-2]
- **文件**: `dy-player/.../short_video/ShortVideoActivity.kt` 第 188-190 行
- **问题**: `onNewIntent` 方法体为空，singleTask 模式下无法响应新播放请求
- **修复方向**: 在 `onNewIntent` 中解析新 Intent 数据并更新播放列表

### 14. ShortVideoAdapter binding 共享问题 [C-12]
- **文件**: `dy-player/.../short_video/ShortVideoAdapter.kt` 第 25 行
- **问题**: `binding` 是类字段而非 ViewHolder 字段，多个 ViewHolder 创建时互相覆盖
- **修复方向**: 将 `binding` 移入 ViewHolder 内部

### 15. LongVideoControlView 返回按钮逻辑错误 [D-1]
- **文件**: `dy-player/.../view/LongVideoControlView.java` 第 69-71 行
- **问题**: `ivBack` 点击直接 `finish()`，绕过了 `goBack()` 中全屏时先退出全屏的逻辑
- **修复方向**: 将 `ivBack` 点击事件改为调用 `goBack()`

### 16. Switch 滑动不触发保存 [G-1]
- **文件**: `dy-player/.../tool/SettingFragment.kt` 第 64-72 行, `PlayerSettingsFragment.kt`, `DanmakuSettingsFragment.kt`, `LabSettingsFragment.kt`
- **问题**: 使用 `setOnClickListener` 而非 `setOnCheckedChangeListener`，滑动切换不会保存
- **修复方向**: 改用 `setOnCheckedChangeListener`

### 17. Fragment ViewBinding 内存泄漏 [G-10]
- **文件**: `PlayerSettingsFragment.kt`, `DanmakuSettingsFragment.kt`, `LabSettingsFragment.kt`, `BackupSettingsFragment.kt`
- **问题**: `_binding` 未在 `onDestroyView` 中置空
- **修复方向**: 重写 `onDestroyView` 并设置 `_binding = null`

### 18. PlayerApp.getAppContext() 非空断言 [J-1]
- **文件**: `dy-player/.../PlayerApp.kt` 第 22-26 行
- **问题**: `appContext!!` 在 `Application.onCreate()` 前调用会 NPE
- **修复方向**: 返回 `Context?` 或在 `getAppContext()` 中添加 null 检查和错误提示

### 19. 搜索大小写不一致 [F-8]
- **文件**: `dy-player/.../media/MediaViewModel.kt` 第 28 行 vs `MediaFullViewModel.kt` 第 25 行
- **问题**: MediaViewModel 搜索区分大小写，MediaFullViewModel 不区分
- **修复方向**: 统一使用 `ignoreCase = true`

### 20. WebDavStorage SSL 验证禁用 [F-5]
- **文件**: `lib-base/.../storage/` WebDavStorage 相关代码
- **问题**: 自定义 TrustManager + `hostnameVerifier { _, _ -> true }`
- **影响**: WebDAV 连接易受中间人攻击
- **修复方向**: 提供用户可配置的证书信任选项，默认启用系统验证

### 21. 还原时 dataSchemaVersion 默认为 0 触发不必要迁移 [K-3]
- **文件**: `dy-player/.../tool/BackupSettingsFragment.kt` 第 283-287 行
- **问题**: 备份文件无 `dataSchemaVersion` 字段时设为 0，触发完整 schema 迁移
- **修复方向**: 使用当前最新版本号作为默认值，或跳过已迁移过的数据

### 22. 允许明文 HTTP 流量 [M-2]
- **文件**: `dy-player/src/main/res/xml/network_security_config.xml` 第 3 行
- **问题**: `cleartextTrafficPermitted="true"` 全局允许明文 HTTP
- **修复方向**: 仅对特定域名允许明文，默认禁止

### 23. 进度保存机制缺失（短视频） [C-7]
- **文件**: `dy-player/.../short_video/ShortVideoActivity.kt` 第 1109 行
- **问题**: `PlayHelper.saveHistory` 仅保存 VideoData 到列表，不保存 playSeek 播放位置
- **修复方向**: 在 `saveHistory` 中保存当前播放位置

### 24. HttpUtil 包含测试用 main() 函数 [I-3]
- **文件**: `lib-base/.../util/HttpUtil.kt` 第 125-152 行
- **问题**: 包含硬编码测试 URL 和注释中的凭据信息
- **修复方向**: 删除 `main()` 函数

### 25. clearComposingText() 误用 [D-7]
- **文件**: `dy-player/.../view/LongVideoControlView.java` 第 248-264 行
- **问题**: `clearComposingText()` 是清除 IME 组合文本的方法，不是清除文本内容
- **修复方向**: 改为 `setText("")` 或仅 `setVisibility(GONE)`

### 26. MD5 实现不一致 [N-1]
- **文件**: `CodeUtil.kt` vs `HashHelper.kt` vs `VideoData.md5()`
- **问题**: 三处 MD5 实现方式不同，同一输入可能产生不同输出
- **修复方向**: 统一使用 `HashHelper` 的正确实现，移除重复代码

---

## P2 - 建议修复（14 项）

### 27. checkUpdate() 方法体为空 [A-3]
- **文件**: `dy-player/.../main/MainViewModel.kt` 第 12-14 行
- **问题**: 方法体完全为空，版本检查逻辑分散在 Activity 中
- **修复方向**: 将 `initUpdate()` 逻辑迁移到 ViewModel

### 28. dayStr 网络失败时覆盖为无版本号 [A-4]
- **文件**: `dy-player/.../main/MainActivity.kt` 第 175 行
- **问题**: 网络失败时 `dayStr = "$today#"` 无版本号，下次启动可能重复弹窗
- **修复方向**: 网络失败时不更新 dayStr，或添加重试计数

### 29. modeNight() 硬编码且从未调用 [A-7]
- **文件**: `dy-player/.../main/MainActivity.kt` 第 181-192 行
- **问题**: `modeNight()` 硬编码为 `MODE_NIGHT_AUTO_BATTERY`，且从未被调用
- **修复方向**: 删除死代码或实现可配置的主题切换

### 30. LiveData 双重触发风险 [B-11]
- **文件**: `dy-player/.../long_video/LongVideoViewModel.kt` 第 23-28 行
- **问题**: `addAll()` 先 `postValue(false)` 再 `postValue(true)`，时序异常时可能触发两次
- **修复方向**: 移除 `postValue(false)` 或改用 `setValue`

### 31. 弹幕双重释放和竞态 [B-9]
- **文件**: `dy-player/.../long_video/LongVideoActivity.kt`
- **问题**: `startPlay()` 和 `onPlayStart()` 中都调用 `danmakuView.release()`；`addDmFile()` 中主线程 release 后 IO 线程 loadDanMu 存在竞态
- **修复方向**: 统一释放入口，确保 release 和 load 在同一线程

### 32. 字幕吸附逻辑过于复杂 [C-8]
- **文件**: `dy-player/.../short_video/ShortVideoActivity.kt` 第 746-926 行
- **问题**: `updateSubtitleDocking()` 方法 180 行，多分支嵌套
- **修复方向**: 拆分为多个小方法

### 33. 排序操作只是反转 [E-3]
- **文件**: `dy-player/.../media_full/MediaItemAdapter.kt` 第 182-185 行
- **问题**: `sorted()` 方法实际只调用 `dataSet.reverse()`
- **修复方向**: 实现真正的排序（按名称/日期等）

### 34. 文件扫描串行处理 [E-1]
- **文件**: `dy-player/.../media/MediaFragment.kt` 第 375-451 行
- **问题**: `processCovers` 使用 `for` 循环串行处理缩略图生成
- **修复方向**: 使用 `coroutineScope` + `async` 并行处理

### 35. 无主题切换入口 [G-5]
- **文件**: `dy-player/.../tool/SettingFragment.kt`
- **问题**: 设置页面无任何主题/深色模式选项
- **修复方向**: 添加主题切换设置项

### 36. 完全无多语言支持 [G-6/L-1/L-2]
- **文件**: 全局
- **问题**: 无 `values-*/strings.xml`，代码中大量硬编码中文字符串
- **修复方向**: 提取字符串资源，添加多语言支持

### 37. 关于页面死代码 [G-8]
- **文件**: `dy-player/.../about/AboutActivity.kt` 第 53-56 行
- **问题**: `gotoPublicLicense()` 启动自身形成无限循环且从未被调用
- **修复方向**: 删除死代码，实现开源协议展示功能

### 38. 隐藏选项和死代码 [H-3]
- **文件**: `dy-player/.../tool/LabSettingsFragment.kt` 第 40-48 行
- **问题**: `labSurfaceRgba` 和 `labSurfaceZOrder` 布局 `visibility="gone"` 但代码仍绑定监听
- **修复方向**: 删除死代码或恢复 UI 可见性

### 39. targetSdk = 28 过期 [J-2/M-3]
- **文件**: `dy-player/build.gradle.kts` 第 25 行
- **问题**: 严重过时，不适用 Android 10+ 安全特性
- **修复方向**: 升级 targetSdk 到至少 33

### 40. FileUtil 与 FileOperator 扩展名列表重复定义 [I-4]
- **文件**: `dy-player/.../util/FileUtil.kt` 第 20-24 行, `lib-base/.../util/FileOperator.kt` 第 32-35 行
- **问题**: 两处定义不同步（FileUtil 9 项 vs FileOperator 10 项多了 `.m4s`）
- **修复方向**: 统一为一处定义，其他地方引用

---

## 已排除项（误报或无需修复）

| 编号 | 原结论 | 排除原因 |
|------|--------|---------|
| A-2 | resultLauncher 死代码 | 二次验证发现第 163 行有调用点，非死代码，仅回调价值低 |
| A-5 | response.body()!!.string() NPE | 二次验证发现实际代码使用 `onSuccess { response -> }` 模式，不存在该断言 |
| A-6 | resultLauncher 逻辑正确 | 与 A-2 关联，非死代码，回调仅做日志 |
| B-1 | onNewIntent 和 init 重复调用 | onNewIntent 仅在 Activity 已存在时调用，不会与 init 重复；核心问题已归入 B-7 |
| B-5 | 横竖屏切换问题 | Activity 锁定横屏，不会发生方向变化 |
| B-10 | 解码切换问题 | 先 release 再切换，流程正确 |
| C-4 | 播放位置恢复问题 | 短视频场景下 pause/resume 机制可维持位置，重建场景需求弱 |
| C-5 | SurfaceRenderTrace 泄漏 | onDestroy 中有清理，风险低 |
| C-9 | 弹幕功能未完全实现 | 属于功能规划问题，非缺陷 |
| C-10 | 解码切换逻辑 | 属于设计权衡 |
| C-11 | 滑动冲突处理 | 已有 isUserScroll 标志处理 |
| D-2 | postDelayed 短暂泄漏 | View detach 时会清理 pending Runnable，风险极低 |
| D-3 | onVisibilityChanged 空实现 | 有意设计 |
| D-4 | 手势方向锁定 | 常见的手势冲突处理方式 |
| D-5 | 进度条拖动问题 | 三个控制层实现均正确 |
| D-6 | 弹幕开关使用 setSelected | 功能可用，仅违反惯例 |
| E-2 | 文件过滤扩展名不一致 | 已归入 I-4 统一处理 |
| E-4 | 刷新数据竞态条件 | 实际触发概率低 |
| E-5 | 内存泄漏风险低 | 正确管理了生命周期 |
| E-6 | 数据库迁移问题 | 有版本控制，逻辑正确 |
| E-7 | 文件操作问题 | 异常处理完善 |
| E-8 | searchItem?.isIconified!! | 二次验证降级，实际触发概率低 |
| F-2 | 重复写入缓存 | 第二次写入覆盖第一次，属冗余但不影响功能 |
| F-3 | refresh 参数未实现 | 功能缺失，非缺陷 |
| F-4 | 内存泄漏风险低 | LruCache 生命周期与应用一致 |
| F-6 | 双重 MD5 和禁用缓存 | 禁用缓存可能是有意设计 |
| F-7 | 资源分类逻辑基本正确 | 无实质问题 |
| F-9 | 排序只是反转 | 已归入 E-3 |
| G-2 | 设置读取问题 | 逻辑标准正确 |
| G-3 | 硬编码中文 | 已归入 G-6/L-2 |
| G-4 | resetView 空实现 | 无实质影响 |
| G-7 | 非空断言风险 | SPManager 默认值确保不会为 null |
| G-9 | 反馈功能简陋 | 功能可用，属设计改进 |
| H-1 | 播放设置问题 | 持久化和消费逻辑正确 |
| H-2 | dmStrokeMultipleMode 死代码 | 影响极小 |
| H-4 | 设置持久化问题 | 备份恢复逻辑正确 |
| H-5 | 设置仅在初始化时读取 | 设计限制，非缺陷 |
| H-6 | SeekBarDialog 状态丢失 | 风险低 |
| I-2 | FileUtil 资源泄漏 | 二次验证降级，正常路径有 close() |
| I-5 | 权限请求码冲突 | 值不同，实际冲突概率低 |
| I-6 | ToastUtil 单例复用 | 线程安全，仅部分 ROM 有问题 |
| I-7 | println 调试输出 | 影响极小 |
| I-8 | GlideOptions 未使用变量 | 影响极小 |
| J-3 | 异常处理器覆盖顺序 | 功能可行 |
| K-4 | 备份无版本兼容机制 | hasKey 逐字段判断已有一定兼容性 |
| L-3 | SimpleDateFormat Locale | 数字格式不受影响 |
| M-4 | 备份明文存储凭据 | 已归入 K-1 |
| M-5 | AES 密钥管理 | local.properties 不提交 VCS，风险可控 |
| N-2 | targetSdk 28 | 已归入 J-2 |
| N-3 | FileUtil 使用已废弃 API | 有抑制注解，开发者已知 |
| N-4 | PlayerApp 标记为 internal | 单模块可正常工作 |
| B-4 | 播放状态恢复竞态 | 恢复框架可用，竞态窗口极小 |
| B-6 | 内存泄漏风险低 | lifecycleScope 管理完善 |
| B-11 | 主线程文件 I/O | onSaveInstanceState 中写入缓存文件，文件小影响低 |

---

## 统计

| 优先级 | 数量 | 占比 |
|--------|------|------|
| P0 必须修复 | 6 | 15% |
| P1 应该修复 | 20 | 50% |
| P2 建议修复 | 14 | 35% |
| **合计** | **40** | 100% |

**已排除**: 52 项（误报 13 项 + 降级/合并/无需修复 39 项）
