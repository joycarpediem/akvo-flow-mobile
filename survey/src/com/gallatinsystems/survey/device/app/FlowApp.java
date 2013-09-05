package com.gallatinsystems.survey.device.app;

import java.util.Arrays;
import java.util.Locale;

import android.app.Application;
import android.content.res.Configuration;
import android.database.SQLException;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.gallatinsystems.survey.device.R;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter;
import com.gallatinsystems.survey.device.util.ConstantUtil;

public class FlowApp extends Application {
    private static final String TAG = FlowApp.class.getSimpleName();
    private static FlowApp app;// Singleton
    
    private Locale mLocale;

    @Override
    public void onCreate() {
        super.onCreate();
        init();
        app = this;
    }
    
    public static FlowApp getApp() {
        return app;
    }
    
    public String getLanguage() {
        return mLocale.getLanguage();
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);
        
        // This config will contain system locale. We need a workaround
        // to enable our custom locale again. Note that this approach
        // is not very 'clean', but Android makes it really hard to
        // customize an application wide locale.
        if (mLocale != null && !mLocale.getLanguage().equalsIgnoreCase(
                newConfig.locale.getLanguage())) {
            // Re-enable our custom locale, using this newConfig reference
            newConfig.locale = mLocale;
            Locale.setDefault(mLocale);
            getBaseContext().getResources().updateConfiguration(newConfig, null);
        }
    }
    
    private void init() {
        // Load custom locale into the app.
        // If the locale has not previously been configured
        // check if the device has a compatible language active.
        // Otherwise, fall back to English
        String language = loadLocalePref();
        if (TextUtils.isEmpty(language)) {
            language = Locale.getDefault().getLanguage();
            // Is that available in our language list?
            if (!Arrays.asList(getResources().getStringArray(R.array.alllanguagecodes))
                    .contains(language)) {
                language = ConstantUtil.ENGLISH_CODE;
            }
        }
        setLocale(language, false);
    }
    
    public void setLocale(String language, boolean requireRestart) {
        // Override system locale
        mLocale = new Locale(language);
        Locale.setDefault(mLocale);
        Configuration config = getBaseContext().getResources().getConfiguration();
        config.locale = mLocale;
        getBaseContext().getResources().updateConfiguration(config, null);
            
        // Save it in the preferences
        saveLocalePref(language);
            
        if (requireRestart) {
            Toast.makeText(this, R.string.please_restart, Toast.LENGTH_LONG)
                    .show();
        }
    }
    
    private String loadLocalePref() {
        String language = null;
        SurveyDbAdapter database = new SurveyDbAdapter(this);
        try {
            database.open();
            language = database.findPreference(ConstantUtil.PREF_LOCALE);
        } catch (SQLException e) {
            Log.e(TAG, e.getMessage());
        } finally {
            database.close();
        }
        
        return language;
    }
    
    private void saveLocalePref(String language) {
        SurveyDbAdapter database = new SurveyDbAdapter(this);
        try {
            database.open();
            database.savePreference(ConstantUtil.PREF_LOCALE, language);
        } catch (SQLException e) {
            Log.e(TAG, e.getMessage());
        } finally {
            database.close();
        }
    }

}
