# DyLike

`DyLike` 是一个以 `dy-player` 为主入口的 Android 多模块播放器项目，应用侧名称为 `DyLike`。  

它既可以作为普通用户使用的 Android 视频播放器，竖屏短视频滑动，横屏长视频弹幕。  
也可以作为围绕播放器内核、弹幕系统、媒体库与资源库构建的 Android 工程继续开发。  
当前仓库包含多个应用入口和基础库模块，默认以 `dy-player` 作为主要使用与开发入口。  

## 项目简介

`dy-player` 的首页采用底部导航结构，围绕三条主流程展开：  

- `媒体库`：管理本地媒体库、在线媒体库、历史记录、收藏和详情页  
- `资源库`：浏览本地存储、外部存储、WebDav 和串流链接  
- `我的`：集中管理播放器、弹幕、备份恢复、图标切换和关于页  

普通用户可以把它理解为一个支持本地播放、网络资源浏览、弹幕和字幕能力的 Android 播放器，当前竖屏短视频播放也是重要能力之一。  
开发者可以把它理解为一个以 `dy-player` 为应用入口，配套 `lib-player`、`lib-dm`、`lib-base` 等模块的多模块播放器工程。  

项目主页及下载链接：https://zhaohuaxs.github.io/dy-like.html  

## 文档入口

- [用户指南](docs/user-guide.md)：功能说明、快速开始、内容来源、页面说明和使用状态  
- [开发者指南](docs/developer-guide.md)：仓库结构、关键入口、模块职责、构建命令和开发注意点  

## 界面预览

<p align="left">
<img src="docs/screenshots/Screenshot_home.jpg" alt="首页" width="360" />
<img src="docs/screenshots/Screenshot_media.jpg" alt="媒体库详情" width="360" />
<img src="docs/screenshots/Screenshot_source.jpg" alt="资源库" width="360" />
<img src="docs/screenshots/Screenshot_tool.jpg" alt="设置页" width="360" />
<img src="docs/screenshots/Screenshot_tool_player.jpg" alt="播放器设置" width="360" />
<img src="docs/screenshots/Screenshot_tool_dm.jpg" alt="弹幕设置" width="360" />
<img src="docs/screenshots/Screenshot_media_edit.jpg" alt="媒体库设置" width="360" />
<img src="docs/screenshots/Screenshot_media_short.jpg" alt="短视频媒体库详情" width="360" />
<img src="docs/screenshots/Screenshot_short.jpg" alt="竖屏播放" width="360" />
<img src="docs/screenshots/Screenshot_short_settings.jpg" alt="竖屏播放设置" width="360" />
<img src="docs/screenshots/Screenshot_dm.jpg" alt="弹幕播放" width="540" />
<img src="docs/screenshots/Screenshot_dm_font.jpg" alt="弹幕字体" width="540" />
</p>


## 当前欠缺与后续说明

这些问题不是使用前置条件，但后续开发者接手时需要优先关注：  

- `dy-player` 已有 Room 的实体、DAO、数据库管理代码，但媒体库、资源库、播放设置、弹幕设置和部分播放状态仍大量依赖 `SpUtil` / `SpBase` / `SharedPreferences`，数据层迁移尚未完全收口。  
- 旧媒体库数据可能存在未绑定具体资源源的兼容提示，继续改媒体库逻辑时需要保留迁移和兼容路径。  
- `lib-base` 中 `SmbStorage` 仍存在未实现的 TODO，SMB 存储能力不能按完整入口对外描述。  
- 新手引导、关于页文案和协议展示仍需要继续补齐。  
- 文档拆分后，更新功能说明时需要同步维护 `README.md`、`docs/user-guide.md` 和 `docs/developer-guide.md`，避免已完成能力继续被标记为待完成。  
- 后续开发者如果发现新的架构、数据迁移、兼容性或文档问题，请继续补充到本节，便于接手者优先排查。  
- 反馈群：`811806197`  

## 许可证

本项目基于 [MIT License](LICENSE) 开源。  

