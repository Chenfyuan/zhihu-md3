package com.github.zly2006.zhihu.screenshot

import android.content.Context
import android.content.pm.ApplicationInfo
import org.robolectric.Shadows.shadowOf

/**
 * 截图专用：让 Robolectric 里的应用看起来像正式包（隐藏 debug 专属的「复制调试数据」按钮，
 * 版本信息显示 release）。只在设置了环境变量 ZHIHU_SHOTS_RELEASE_LOOK=1 时生效，不影响普通测试。
 */
internal fun applyReleaseLookIfRequested(ctx: Context) {
    if (System.getenv("ZHIHU_SHOTS_RELEASE_LOOK") != "1") return
    ctx.applicationInfo.flags = ctx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE.inv()
    val pkg = shadowOf(ctx.packageManager).getInternalMutablePackageInfo(ctx.packageName)
    pkg.applicationInfo?.let { info ->
        info.flags = info.flags and ApplicationInfo.FLAG_DEBUGGABLE.inv()
        val meta = info.metaData ?: android.os.Bundle().also { info.metaData = it }
        meta.putString("com.github.zly2006.zhihu.BUILD_TYPE", "release")
    }
}
