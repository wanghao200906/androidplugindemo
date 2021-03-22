# androidplugindemo
一个练习用的插件化demo

# 简介
- 这个demo是一个插件化的练习
  - app是宿主
  - plugin_package 是插件包
  - stander 是宿主和插件包交互的规则，通过接口的形式来交互
- 宿主启动插件activity的原理
  - 先把plugin_package的apk放到手机的sd目录中
  - 使用
