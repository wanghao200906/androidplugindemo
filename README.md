# androidplugindemo
一个练习用的插件化demo

# 简介
- 这个demo是一个插件化的练习
## 原理简介
   - 这种用法360的插件化就使用了dexElements融合的方法
   - 将宿主apk和插件apk的dexElements合在一起，这样就可以去加载插件里面的class
      - 第一步：找到宿主 dexElements 得到此对象，PathClassLoader代表是宿主
      - 第二步：找到插件 dexElements 得到此对象，代表插件 DexClassLoader--代表插件
      -  第三步：创建出 新的 newDexElements[]，类型必须是Element，必须是数组对象
      -  第四步：宿主dexElements + 插件dexElements =----> 融合  新的 newDexElements for遍历
      -  第五步：把新的 newDexElements，设置到宿主中去
   -  获取插件的资源文件。
      -  创建一个AssetManager，通过反射将插件apk的地址赋值给AssetManager的addAssetPath变量
      -  在创建一个Resource，参数分别是新建的AssetManager，宿主的DisplayMetrics，Configuration
   -  不在manifest注册activity如何启动
      -  startActivity会执行到Instrumentation的execStartActivity，ams那边儿会去pskm的activitys里面去检查是否有要启动的activity，如果manifest中没有注册，则有一个返回值
      -  返回值传入checkStartActivityResult中抛出异常
      -  绕过这个检查就是在进行pkms检查之前，将要启动的intent存起来。启动给一个占坑的ProxyActivity，ProxyActivity在manifest中注册了，所以不会报错
      -  当ams要启动ProxyActivity的时候。会执行到ApplicationThread中的handler里面机型操作，这个handler就是mH
      -  可以拦截这个mH，将要启动的ProxyActivity，替换成我们插件的Activity。
      -  这样就绕过了检查
      -  总之：打算启动A，但是告诉Ams启动B，当Ams检查完打算通过handler启动B的时候，拦截Handler，启动A。
## 代码原理分析
### classloader过程
   - PathClassLoader.loadClass  ---》
   - BaseDexClassLoader --》
   - ClassLoader.loadClass--findClass(空方法) 让覆盖的子类方法去完成 --》
   - BaseDexClassLoader.findClass() ---》pathList.findClass
   - for遍历 Element[]  dexElements
   - DexFile.loadClassBinaryName 是一个native的方法了。
### dexElements代码融合
   - DexElementFuse.java
   - PathClassLoader和 DexClassLoader的父类都是BaseDexClassLoader。
   - 在创建 PathClassLoader和 DexClassLoader的时候都要在BaseDexClassLoader中创建一个 DexPathList pathList
   - 所以需要反射拿到BaseDexClassLoader中的pathList变量。
   - 在创建DexPathList的时候，会调用一个DexPathList中的makeDexElements方法，将apk解析成Element[] dexElements 数组。所以我们通过反射拿到dexElements变量
   - 拿到了宿主和插件的dexElements变量之后。将两个dexElements 放到一个新的newDexElements[]中
   - 将新的newDexElements[]替换宿主的dexElements
   -  这样就融合了 PathClassLoader 就能加载到 插件/宿主  都可以加载到了
   - 创建一个AssetManager，通过反射将插件apk的地址赋值给AssetManager的addAssetPath变量
   - 在创建一个Resource，参数分别是新建的AssetManager，宿主的DisplayMetrics，Configuration
### 绕过检查
   - AMSCheckEngine.java
   - startActivity(TestActivity) ---> Activity --> Instrumentation.execStartActivity ---> ActivityManagerNative.getDefault()IActivityManager.startActivity --->  (Hook)   AMS.startActivity（检测，当前要启动的Activity是否注册了）
   - 在调用IActivityManager是一个接口它里面有一个startActivity方法。我们可以hook这个方法来讲要启动的Activity替换为已经在manifest中注册了的activity
   - 动态代理：由于执行startActivity之前，我们需要先执行我们的代码(把TestActivity 替换成 已经注册的 Activity)
   - 大于等于29：通过 android.app.ActivityTaskManager类中 IActivityTaskManagerSingleton,拿到了Singleton
   - 大于等于26 27 28：通过 android.app.ActivityManager IActivityTaskManagerSingleton,拿到了Singleton
   - 大于等于26 27 28：通过 android.app.ActivityManagerNative IActivityTaskManagerSingleton,拿到了Singleton
   - 目的就是替换Singleton中的mInstance变量。替换成我们代理的对象
   - 大于等于29：动态代理android.app.IActivityTaskManager接口
   - 小于29：动态代理android.app.IActivityManager接口
   - 最后将动态代理的对象，赋值给Singleton的mInstance。
   - 动态代理的时候讲要启动的Activity 放到intent中。启动占坑activity
### 恢复创建插件Activity
   - ActivityThreadmHRestore.java
   - ASM检查过后，要把这个ProxyActivity 换回来 --> TestActivity
   - startActivity --->  TestActivity -- （Hook ProxyActivity）（AMS）检测，当前要启动的Activity是否注册了）ok ---》ActivityThread（即将加载启动Activity）--mH(handler)--(要把这个ProxyActivity 换回来 --> TestActivity)
   - 我们要在Handler。handleMessage 之前执行，就是为了(要把这个ProxyActivity 换回来 --> TestActivity)，所有需要Hook
   - 因为最后会执行mH的handleMessage方法，所以我们可以在handleMessage执行前，执行Handler.Callback方法。因为Handler.Callback优先于handleMessage。
   - 所以我们可以定义类实现Handler.Callback。将activity进行替换，然后再讲自定义的callback赋值给mH。
       - 大于等于27：启动activity是表示是159 EXECUTE_TRANSACTION
           - 通过ClientTransaction 拿到 一个list 。这个list里面是LaunchActivityItem，这里面就有Intent。就是我们要启动的插件的Intent
           - ams中要启动activity的时候都会创建一个LaunchActivityItem
       - 小于27  ： 启动activity是表示是100 LAUNCH_ACTIVITY
           - handler的obj是ActivityClientRecord。可以拿到Intent。这个Intent就是我们打算启动的插件Activity的Intent。