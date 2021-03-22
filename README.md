# androidplugindemo
一个练习用的插件化demo

# 简介
- 这个demo是一个插件化的练习
  - app是宿主
  - plugin_package 是插件包
  - stander 是宿主和插件包交互的规则，通过接口的形式来交互
- 宿主启动插件activity的原理
  - 先把plugin_package的apk放到手机的sd目录中
  - 使用[PluginManager.java](app/src/main/java/com/example/androidplugindemo/PluginManager.java)来获取DexClassLoader和Resources


 ```
        File file = new File(Environment.getExternalStorageDirectory() + File.separator + "p.apk");
        String path = file.getAbsolutePath();

        // 获取插件包 里面的 Activity
        PackageManager packageManager = getPackageManager();
        PackageInfo packageInfo = packageManager.getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES);
        ActivityInfo activityInfo = packageInfo.activities[0];// 这里是0就是com.example.plugin_package.PluginActivity，如果多了的话那就得遍历了。

        // 占位  代理Activity
        Intent intent = new Intent(this, ProxyActivity.class);
        intent.putExtra("className", activityInfo.name);
        startActivity(intent);
 ```

