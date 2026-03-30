package com.ezt.pdfreader.photoeditor.util;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.snackbar.Snackbar;

public class BarsUtils {

    ///////////////////////////////////////////////////////////////////////////
    // Status bar
    ///////////////////////////////////////////////////////////////////////////

    public static void setStatusBarColor(Activity activity, int color) {
        if (activity != null) {
            setStatusBarColor(activity.getWindow(), color);
        }
    }

    public static void setHideNavigation(Activity activity){
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(activity.getWindow(), activity.getWindow().getDecorView());
        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        );

        windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars());
    }

    public static void setStatusBarColor(@NonNull Window window, int color) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        window.setStatusBarColor(color);
    }

    public static void setAppearanceLightStatusBars(Activity activity, boolean isLight) {
        if (activity != null) {
            setAppearanceLightStatusBars(activity.getWindow(), isLight);
        }
    }

    public static void setAppearanceLightStatusBars(Window window, boolean isLight) {
        if (window == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsControllerCompat windowInsetsController =
                    WindowCompat.getInsetsController(window, window.getDecorView());
            windowInsetsController.setAppearanceLightStatusBars(isLight);
        } else {
            int uiVisibility = window.getDecorView().getSystemUiVisibility();
            if (isLight) {
                uiVisibility |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                uiVisibility &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            window.getDecorView().setSystemUiVisibility(uiVisibility);
        }
    }

    public static boolean isLightStatusBar(Activity activity) {
        if (activity != null) {
            return isLightStatusBar(activity.getWindow());
        }
        return false;
    }

    private static boolean isLightStatusBar(Window window) {
        if (window == null) {
            return false;
        }
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(window, window.getDecorView());
        return windowInsetsController.isAppearanceLightStatusBars();
    }

    public static void offsetStatusBar(View v, int offset) {
        if (v == null) {
            return;
        }
        v.setPadding(v.getPaddingLeft(), offset, v.getPaddingRight(), v.getBottom());
    }

    ///////////////////////////////////////////////////////////////////////////
    // Navigation bar
    ///////////////////////////////////////////////////////////////////////////

    public static void setNavigationBarDividerColor(Activity activity, int color) {
        if (activity != null) {
            setNavigationBarDividerColor(activity.getWindow(), color);
        }
    }

    public static void setNavigationBarDividerColor(@NonNull Window window, int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.setNavigationBarDividerColor(color);
        }
    }

    public static void setNavigationBarColor(Activity activity, int color) {
        if (activity != null) {
            setNavigationBarColor(activity.getWindow(), color);
        }
    }

    public static void setNavigationBarColor(@NonNull Window window, int color) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setNavigationBarColor(color);
    }

    public static void setAppearanceLightNavigationBars(Activity activity, boolean isLight) {
        if (activity != null) {
            setAppearanceLightNavigationBars(activity.getWindow(), isLight);
        }
    }

    public static void setAppearanceLightNavigationBars(Window window, boolean isLight) {
        if (window == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsControllerCompat windowInsetsController =
                    WindowCompat.getInsetsController(window, window.getDecorView());
            windowInsetsController.setAppearanceLightNavigationBars(isLight);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int uiVisibility = window.getDecorView().getSystemUiVisibility();
            if (isLight) {
                uiVisibility |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            } else {
                uiVisibility &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            window.getDecorView().setSystemUiVisibility(uiVisibility);
        }
    }

    public static boolean isLightNavigationBars(Activity activity) {
        if (activity != null) {
            return isLightNavigationBars(activity.getWindow());
        }
        return false;
    }

    private static boolean isLightNavigationBars(Window window) {
        if (window == null) {
            return false;
        }
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(window, window.getDecorView());
        return windowInsetsController.isAppearanceLightNavigationBars();
    }

    public static void offsetNavigationBar(View v, int offset) {
        if (v == null) {
            return;
        }
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
        lp.bottomMargin = offset;
        v.setLayoutParams(lp);
    }

    ///////////////////////////////////////////////////////////////////////////
    // SnackBar
    ///////////////////////////////////////////////////////////////////////////

    public static void showSnackBar(View view, int message) {
        if (view != null) {
            make(view).setText(message).show();
        }
    }

    public static void showSnackBar(View view, String message) {
        if (view != null) {
            make(view).setText(message).show();
        }
    }

    @NonNull
    public static Snackbar make(View view) {
        return Snackbar.make(view, "", Snackbar.LENGTH_SHORT);
    }

    private BarsUtils() {
        //no instance
    }

}

