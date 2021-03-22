# androidplugindemo
一个练习用的插件化demo

# 简介
- 这个demo是一个插件化的练习
  - app是宿主
  - plugin_package 是插件包
  - stander 是宿主和插件包交互的规则，通过接口的形式来交互
## 宿主启动插件activity的原理
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
- 当想启动插件Activity的时候。我们要先拿到PackageManager，然后在通过getPackageArchiveInfo方法拿到PackageInfo，通过packageInfo.activities[0]获取里面的ActivityInfo，packageInfo.activities其实是一个list，因为插件里只有一个activity所以使用了第0个，如果activity很多，那么就得遍历获取指定的activity
- 启动站位的Activity，给intent中传入activityInfo.name
- 当启动了占坑的activity之后。在onCreate中 获取插件的activity。然后执行插件的onCreate ，执行插件里面的setContentView方法。这样虽然是启动了站位的Activity，但是现实的内容全是插件的Activity的内容
```

        // 真正的加载 插件里面的 Activity
        String className = getIntent().getStringExtra("className");

        try {
            Class mPluginActivityClass = getClassLoader().loadClass(className);
            // 实例化 插件包里面的 Activity
            Constructor constructor = mPluginActivityClass.getConstructor(new Class[]{});
            Object mPluginActivity = constructor.newInstance(new Object[]{});

            ActivityInterface activityInterface = (ActivityInterface) mPluginActivity;

            // 注入 
            activityInterface.insertAppContext(this);

            Bundle bundle = new Bundle();
            bundle.putString("appName", "我是宿主传递过来的信息");

            // 执行插件里面的onCreate方法
            activityInterface.onCreate(bundle);

        } catch (Exception e) {
            e.printStackTrace();
        }
```
- 这里有一个重点就是 activityInterface.insertAppContext(this); 将宿主的上下文，传入到插件中。这样插件有了上下文就可以使用了
- 这样插件的activity就启动了。
- 启动插件的service，BroadcastReceiver都是一样的。先从通过packageInfo里面拿到对应的service和BroadcastReceiver然后启动站位的servier或者广播来启动
## 解决上下文的问题
- 因为 startActivity ，findViewById 操作都需要上下文。插件是没有上下文的。所以通过activityInterface.insertAppContext(this)来注入了宿主的上下文
- 在使用startActivity ，findViewById等操作的时候也得使用宿主的 startActivity ，findViewById
- [BaseActivity.java](plugin_package/src/main/java/com/example/plugin_package/BaseActivity.java) 中完成了上下文的注入，和使用宿主的一些方法
```

public class BaseActivity extends Activity implements ActivityInterface {

    public Activity appActivity; // 宿主的环境

    @Override
    public void insertAppContext(Activity appActivity) {
        this.appActivity = appActivity;
    }
 
    public void setContentView(int resId) {
        if(appActivity!=null){
            appActivity.setContentView(resId);
        }else{
            super.setContentView(resId);
        }
    }

    public View findViewById(int layoutId) {
        return appActivity.findViewById(layoutId);
    }

    @Override
    public void startActivity(Intent intent) {
        Intent intentNew = new Intent();
        intentNew.putExtra("className", intent.getComponent().getClassName()); // TestActivity 全类名
        appActivity.startActivity(intentNew);
    }

    @Override
    public ComponentName startService(Intent service) {
        Intent intentNew = new Intent();
        intentNew.putExtra("className", service.getComponent().getClassName()); // TestService 全类名
        return appActivity.startService(intentNew);
    }

    // 注册广播, 使用宿主环境-appActivity
    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        return appActivity.registerReceiver(receiver, filter);
    }

    // 发送广播, 使用宿主环境-appActivity
    @Override
    public void sendBroadcast(Intent intent) {
        appActivity.sendBroadcast(intent);
    }
}

```
## 在插件中启动activity
```
     findViewById(R.id.bt_start_activity).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            //调用父类的startActivity
                startActivity(new Intent(appActivity, TestActivity.class));
            }
        });
```
- 使用baseactivity中的startActivity
```
    @Override
    public void startActivity(Intent intent) {
        Intent intentNew = new Intent();
        intentNew.putExtra("className", intent.getComponent().getClassName()); // TestActivity 全类名
        appActivity.startActivity(intentNew);
    }

```
- 使用调用宿主的startActivity。宿主就是[ProxyActivity.java](app/src/main/java/com/example/androidplugindemo/ProxyActivity.java)

```

    @Override
    public void startActivity(Intent intent) {
        String className = intent.getStringExtra("className");

        Intent proxyIntent = new Intent(this, ProxyActivity.class);
        proxyIntent.putExtra("className", className); // 包名+TestActivity

        // 要给TestActivity 进栈
        super.startActivity(proxyIntent);
    }

```
- 把插件的名字放到了intent中。启动宿主的站位activity。然后跟之前一样，在宿主站位activity的oncreate中通过classloader找到插件的activity创建对象。然后执行插件的oncreate。
- 宿主的activity就启动了。
## 在插件中启动service
- 思路和插件启动activity一样。先宿主启动插件的activity，然后在插件activity中启动插件service。
- 调用数组activity的startService方法，将要启动的插件service名字存到intent中
- 启动站位的ProxyService。
- 在ProxyService的onStartCommand中。通过classloader获取插件service。通过反射创建对象
- 调用插件的onStartCommand方法。
- 这样插件的service就启动了。它和宿主service的生命周期是一样的
## 在插件中启动动态广播
- 思路和插件启动activity一样。先宿主启动插件的activity，然后在插件activity中注册广播
- 执行宿主的registerReceiver方法，将插件中的 MyReceiver 和定义的IntentFilter传入
- 执行宿主activity中重写的registerReceiver
- 获取插件MyReceiver的全路径名
- 执行super.registerReceiver,注册一个占坑的ProxyReceiver
- ProxyReceiver的onReceive方法中，通过插件的全路径名通过getclassloader获取插件MyReceiver，通过反射创建对象
- 然后执行插件MyReceiver的onreveive方法。就可以监听到广播了。
## 缺点，必须得重写宿主的很多方法，比如startActivity，findViewById等。但是原理比较简单