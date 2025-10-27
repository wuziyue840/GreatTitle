# GreatTitle - Minecraft for JAVA 伟大的称号插件

![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21.4-green)

# 前情提要：但是非常抱歉！

### 我这个项目是ai写的，所以会非常的烂，但是如果有大佬帮我完善以下两个功能就是完全版，非常感谢！
1. 修复打开自定义称号时会报错，就是打开自定义称号界面会爆无限递归（尚未修复）
2. 创建发包拦截去实现创建自定义称号的方法（尚未实现）
3. 在玩家名字上面再创建一个计分板，去显示称号（别的玩家看过来的头顶）（尚未实现）

### 但是我的项目有一个非常大的优点
1. 兼容1.12~laster的minecraft
2. 比米饭的插件多了一个功能，可以在头顶别的玩家看过来的名字前显示称号（可配置！！！）
3. 支持数据库

## 功能概述

GreatTitle 是一个伟大的称号插件，兼容1.12版本至最新版本，主要功能包括：

## 安装
1. 将插件放入服务器的 `plugins` 文件夹
2. 重启服务器
3. 编辑 `plugins/GreatTitle` 内文件的进行配置
4. 重新加载配置：`/title reload`

## 使用说明

### 依赖插件(可选)：
1. PlaceholderAPI - 变量扩展
2. Vault - 金币经济系统
3. PlayerPoints - 点券系统

### 功能：
1. 颜色格式支持：完整支持 § 符号、& 符号和十六进制颜色代码
2. BUFF系统：每个称号可以配置多个药水效果，可设置时长和属性
3. 粒子效果集成：支持 SuperTrails、SuperTrailsPro 和 PlayerParticles
4. 多种货币购买：支持金币、点券、称号币等多种货币组合购买
5. 切换冷却时间：可设置全局和单个称号的切换冷却

## 权限系统（PlaceholderAPI 变量列表）

| 权限节点                            | 描述 | 默认 |
|---------------------------------|------|------|
| `%greattitle_active%`           | 当前使用的称号内容| op |
| `%greattitle_active_name%`      | 当前使用的称号显示名称 | op |
| `%greattitle_active_id%`        | 当前使用的称号ID | op |
| `%greattitle_has_title_<称号ID>%` | 是否拥有指定称号 | op |
| `%greattitle_count%`            | 拥有的称号总数 | op |
| `%greattitle_count_permanent%`  | 永久称号数量 | op |
| `%greattitle_count_expired%`    | 过期称号数量 | op |
| `%greattitle_display_head%`     | 是否开启头顶显示 | op |
| `%greattitle_status_<称号ID>%` | 指定称号的状态 | op |

# 指令系统

| 指令                    | 用途 | 权限 |
|-----------------------|------|----|
| `/grt` | 打开称号仓库 | 玩家 |
| `/grt shop` | 打开称号商店 | 玩家 |
| `/grt use <称号ID>` | 使用称号 | 玩家 |
| `/grt display on/off` | 切换头顶显示 | 玩家 |
| `/grt reload` | 重新加载配置 | op |
| `/grt clear` | 清除数据 | op |
更多请查看代码的TitleCommand
