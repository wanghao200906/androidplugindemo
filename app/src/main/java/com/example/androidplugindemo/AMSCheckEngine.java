package com.example.androidplugindemo;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static com.example.androidplugindemo.Parameter.PLUGIN;

/**
 * Time: 2019-08-10
 * Author: Liudeli
 * Description: 专门处理绕过AMS检测，让LoginActivity可以正常通过
 */
public class AMSCheckEngine {

    /**
     *
     *
     * @param mContext
     * @throws ClassNotFoundException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     */
    public static void mHookAMS(final Context mContext) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {

        // 公共区域
        Object mIActivityManagerSingleton = null;
        Object mIActivityManager = null;
        //没有30的手机所以没有测试。
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {//version 30

            Class actvityManager = Class.forName("android.app.ActivityTaskManager");
//得到的是IActivityTaskManager ，因为都是object类型的所以赋值给了mIActivityManager也无所谓
            mIActivityManager = actvityManager.getMethod("getService").invoke(null);//


            Field singletonField = actvityManager.getDeclaredField("IActivityTaskManagerSingleton");
            singletonField.setAccessible(true);
            //拿到了IActivityTaskManagerSingleton 对象。因为都是object类型的所以赋值给了mIActivityManagerSingleton也无所谓
            mIActivityManagerSingleton = singletonField.get(null);


        } else if (AndroidSdkVersion.isAndroidOS_26_27_28()) {
            // @3 的获取    系统的 IActivityManager.aidl
            Class mActivityManagerClass = Class.forName("android.app.ActivityManager");
            mIActivityManager = mActivityManagerClass.getMethod("getService").invoke(null);


            // @1 的获取    IActivityManagerSingleton
            Field mIActivityManagerSingletonField = mActivityManagerClass.getDeclaredField("IActivityManagerSingleton");
            mIActivityManagerSingletonField.setAccessible(true);
            mIActivityManagerSingleton = mIActivityManagerSingletonField.get(null);

        } else if (AndroidSdkVersion.isAndroidOS_21_22_23_24_25()) {
            // @3 的获取
            Class mActivityManagerClass = Class.forName("android.app.ActivityManagerNative");
            Method getDefaultMethod = mActivityManagerClass.getDeclaredMethod("getDefault");
            getDefaultMethod.setAccessible(true);
            mIActivityManager = getDefaultMethod.invoke(null);

            // @1 的获取 gDefault
            Field gDefaultField = mActivityManagerClass.getDeclaredField("gDefault");
            gDefaultField.setAccessible(true);
            mIActivityManagerSingleton = gDefaultField.get(null);
        }

        // @2 的获取    动态代理
        Class mIActivityManagerClass = Class.forName("android.app.IActivityManager");
        final Object finalMIActivityManager = mIActivityManager;
        Object mIActivityManagerProxy = Proxy.newProxyInstance(mContext.getClassLoader(),
                new Class[]{mIActivityManagerClass},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                        if ("startActivity".equals(method.getName())) {
                            Intent intent = (Intent) args[2];
                            boolean isPlugin = intent.getBooleanExtra(PLUGIN, false);
                            if (isPlugin) {
                                // 把LoginActivity 换成 ProxyActivity
                                // TODO 狸猫换太子，把不能经过检测的LoginActivity 替换 成能够经过检测的ProxyActivity
                                Intent proxyIntent = new Intent(mContext, ProxyActivity.class);

                                // 把目标的LoginActivity 取出来 携带过去
                                Intent target = (Intent) args[2];
                                proxyIntent.putExtra(Parameter.TARGET_INTENT, target);
                                args[2] = proxyIntent;
                            }
                        }

                        // @3
                        return method.invoke(finalMIActivityManager, args);
                    }
                });

        if (mIActivityManagerSingleton == null || mIActivityManagerProxy == null) {
            throw new IllegalStateException("实在是没有检测到这种系统，需要对这种系统单独处理..."); // 10.0
        }

        Class mSingletonClass = Class.forName("android.util.Singleton");

        Field mInstanceField = mSingletonClass.getDeclaredField("mInstance");
        mInstanceField.setAccessible(true);

        // 把系统里面的 IActivityManager 换成 我们自己写的动态代理 【第一步】
        //  @1    @2
        mInstanceField.set(mIActivityManagerSingleton, mIActivityManagerProxy);

    }

}
